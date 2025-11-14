# JWT Authentication Overview

This document summarizes how JSON Web Tokens (JWT) are issued, validated, and revoked in the Lab Automation platform after the RS256-based refresh-token overhaul.

## Token Issuance
- **Entry point**: `AuthController.login` (`src/main/java/tiameds/com/tiameds/controller/auth/AuthController.java`) authenticates credentials, assembles the login response, and requests both an access token and a refresh token from `JwtUtil`.
- **Claims**:
  - Access tokens carry the subject (`sub`), `tokenVersion`, `tokenType=access`, along with standard `iss`, `aud`, `iat`, and `exp` values.
  - Refresh tokens include the same base claims plus a unique `jti` that maps to a persisted `RefreshToken` entity.
- **Signing**: `JwtUtil` (`src/main/java/tiameds/com/tiameds/utils/JwtUtil.java`) loads an RSA key pair and signs every JWT with RS256 (private key). Verification uses the matching public key.
- **Expiry**:
  - Access tokens default to 15 minutes (see `security.jwt.access-token-ttl`).
  - Refresh tokens default to 24 hours (`security.jwt.refresh-token-ttl`).
- **Rotation**: Each login or refresh issues a brand-new refresh token, hashes and stores it in the database, and revokes the previously active token for that session.

## End-to-End Authentication Flow

### 1. Login (`POST /auth/login`)
- **Request body**:
  ```json
  {
    "username": "Sunny3040",
    "password": "••••••••"
  }
  ```
- **Successful response**:
  ```json
  {
    "status": "OK",
    "message": "Login successful",
    "token": "eyJhbGciOiJSUzI1NiJ9....",     // Backward-compatible access token copy
    "data": {
      "username": "Sunny3040",
      "email": "Sunny3040@gmail.com",
      "firstName": "Sunny",
      "lastName": "Sunny",
      "roles": ["SUPERADMIN"],
      "modules": null,
      "phone": "9473337583",
      "address": "29287SN27247thmainroad4972",
      "city": "MYSORE",
      "state": "Karnataka",
      "zip": "570017",
      "country": "India",
      "enabled": true,
      "is_verified": false
    }
  }
  ```
  - The `token` field is maintained for legacy consumers, but clients should prefer the cookies (`accessToken`, `refreshToken`) that are issued alongside the response.
  - Example headers:
    ```
    Set-Cookie: accessToken=eyJhbGciOiJSUzI1NiJ9...; Max-Age=900; Path=/; HttpOnly; Secure; SameSite=Strict
    Set-Cookie: refreshToken=eyJhbGciOiJSUzI1NiJ9...; Max-Age=86400; Path=/; HttpOnly; Secure; SameSite=Strict
    ```
  - Both cookies store RS256 JWTs and inherit attributes from `security.jwt.*` (domain, path, SameSite, secure).

### 2. Authenticating API calls
- Browsers automatically attach the `accessToken` cookie on subsequent requests.  
- For non-browser clients, you can either reuse the cookie or supply the token in the `Authorization: Bearer <accessToken>` header.
- `JwtFilter` validates the token, ensures the `tokenVersion` matches the database value, and populates `SecurityContext`.

### 3. Refresh (`POST /auth/refresh`)
- Requires the `refreshToken` cookie. No body payload is needed.
- On success:
  - Responses mirror the login payload (`status`, `message`, `token`, `data`).
  - Set-Cookie headers rotate both access and refresh cookies.
- On failure (expired/revoked token):
  - Cookies are cleared (`Max-Age=0`).
  - Response includes `status: "error"` with `401/403`.

### 4. Logout (`POST /auth/logout`)
- Requires an authenticated request (access token).
- Behaviour:
  - Revokes the presented refresh token (if supplied) and increments `User.tokenVersion`.
  - Returns:
    ```json
    {
      "status": "success",
      "message": "Logged out successfully",
      "data": {
        "tokenVersion": 25
      }
    }
    ```
  - Cookies are cleared via `Set-Cookie` headers (max-age `0`).

### 5. Error Handling
- Invalid credentials → `401` with `status: "error"` and throttling may trigger `429`.
- Expired access token but valid refresh token → call `/auth/refresh`.
- Expired or revoked refresh token → cookies cleared, client must re-authenticate via `/auth/login`.

## Configuration
- **Key management**:
  - `security.jwt.private-key-location` and `security.jwt.public-key-location` define where the PEM-encoded RSA keys live. The dev profile ships with sample classpath keys (`src/main/resources/keys/`). Higher environments should use `file:` URLs that point to secrets outside the repo.
  - Additional `security.jwt.*` properties control issuer, audience, token TTLs, cookie names, domain/path, secure flag, and SameSite policy.
- **Stateless access**: `SpringSecurityConfig` registers `JwtFilter` ahead of the username/password filter and enforces `SessionCreationPolicy.STATELESS`, so every request must present a valid access token.
- **Cookie storage**: Access and refresh tokens are returned as HTTP-only cookies (defaults `accessToken` / `refreshToken`). JavaScript never handles the raw token strings; header-based Bearer tokens remain an optional fallback for legacy clients.

## Request Validation Flow
1. `JwtFilter` (`src/main/java/tiameds/com/tiameds/filter/JwtFilter.java`) runs on every non-public HTTP request.
2. It first looks for the access token in the configured cookie and falls back to the `Authorization: Bearer <token>` header if necessary.
3. `JwtUtil.parseAccessToken` validates signature, issuer, audience, token type (`access`), and expiration, returning the JWT claims.
4. The filter loads the `User` by subject, compares the embedded `tokenVersion` to the database field, and aborts authentication on mismatch (invalidating tokens after logout/rotation).
5. When checks pass, a `UsernamePasswordAuthenticationToken` is placed in the `SecurityContext`, enabling downstream role checks.

## Refresh Flow
1. `AuthController.refresh` expects the refresh cookie to be present. Missing/invalid tokens clear both cookies and force a fresh login.
2. `JwtUtil.parseRefreshToken` enforces RS256 signature, issuer/audience, token type (`refresh`), expiration, and extracts the token `jti`.
3. `RefreshTokenService.validateToken` retrieves the hashed record, verifies the SHA-256 hash, and ensures the token is neither expired nor revoked.
4. The previous refresh token row is marked revoked. New access/refresh tokens are generated, the refresh hash is persisted, and both cookies are rotated in the response.
5. Any failure returns `401/403`, clears cookies, and requires a full login.

## Token Versioning and Logout
- **Version storage**: `User.tokenVersion` (`src/main/java/tiameds/com/tiameds/entity/User.java`) tracks the current valid generation of access tokens per user.
- **Logout**: `AuthController.logout` revokes the provided refresh token (if readable), increments `tokenVersion`, persists the change, and clears both cookies. All previous access tokens become invalid instantly.
- **Global revocation**: Admin or security flows can increment `tokenVersion` or call `RefreshTokenService.revokeAllActiveTokens` to invalidate every outstanding session for a user.

## Summary
- Access tokens: RS256-signed JWTs, 15-minute TTL, enforced by `JwtFilter`, stored in HTTP-only cookies.
- Refresh tokens: RS256-signed JWTs, 24-hour TTL, hashed in `refresh_tokens`, rotated on every refresh/login.
- Revocation strategy: Combination of per-request `tokenVersion` checks and server-side refresh-token revocation guarantees old tokens cannot be reused.
- Configuration: Driven by `security.jwt.*` properties; production must provide secure RSA key locations and appropriate cookie settings (e.g., `Secure`, `SameSite`). 
