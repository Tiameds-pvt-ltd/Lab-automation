# JWT Authentication Overview

This document summarizes how JSON Web Tokens (JWT) are issued, validated, and invalidated in the Lab Automation platform.

## Token Issuance
- **Entry point**: `UserController.login` (`src/main/java/tiameds/com/tiameds/controller/auth/UserController.java`) authenticates credentials and then calls `JwtUtil.generateToken`.
- **Claims**: `JwtUtil.generateToken` includes the username (`sub`) plus an optional `tokenVersion` claim taken from `User.tokenVersion`.
- **Signing**: `JwtUtil` (`src/main/java/tiameds/com/tiameds/utils/JwtUtil.java`) signs tokens with a symmetric key derived from `spring.jwt.secret`. The helper `Keys.hmacShaKeyFor` returns an HMAC key, so the token uses the HS256 algorithm (or a stronger HS variant if the configured secret is longer than 256 bits).
- **Expiry**: Tokens are stamped with `iat` and an expiration (`exp`) set to 10 hours after issuance (`new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)`).

## Configuration
- **Secret management**: The signing secret is exposed via the `spring.jwt.secret` property (`src/main/resources/application.yml`). Profiles such as `application-ci.yml` provide defaults like `${JWT_SECRET:test-jwt-secret-for-zap-scanning}`; production deployments must override `JWT_SECRET` with a strong value.
- **Stateless sessions**: `SpringSecurityConfig` registers the JWT filter and configures `SessionCreationPolicy.STATELESS` so every request must supply a valid token.

## Request Validation Flow
1. `JwtFilter` (`src/main/java/tiameds/com/tiameds/filter/JwtFilter.java`) intercepts each request.
2. It looks for an `Authorization: Bearer <token>` header.  
3. The filter extracts the username and checks token expiry via `JwtUtil.validateToken`.
4. If a `tokenVersion` claim is present, it loads the user record and compares the claim to `User.tokenVersion`. A mismatch short-circuits authentication, preventing reuse of older tokens.
5. When all checks pass, the filter populates the `SecurityContext` with a `UsernamePasswordAuthenticationToken`, allowing downstream controllers to apply role-based rules.

## Token Versioning
- **Storage**: `User.tokenVersion` (`src/main/java/tiameds/com/tiameds/entity/User.java`) is persisted with each account.
- **Logout**: `LogoutController.logout` increments `tokenVersion` on successful logout, invalidating any previously issued tokens.
- **Other rotations**: Any future action that updates `tokenVersion` (e.g., password reset) automatically revokes all outstanding tokens without needing an explicit blacklist.

## Token Lifetime
- **Nominal lifetime**: 10 hours from issuance.
- **Renewal**: There is no automatic refresh flow; clients must re-authenticate when tokens expire or when `tokenVersion` changes.
- **Operational guidance**: Consider shorter expiration or refresh tokens if session hijacking risk increases, and ensure the `JWT_SECRET` uses at least a 256-bit random value to maintain HS256 security guarantees.

## Summary
- Signing algorithm: HMAC SHA (HS256 by default) using `spring.jwt.secret`.
- Token payload: subject (username) plus optional `tokenVersion`.
- Expiration: 10 hours.
- Revocation strategy: Incremented `tokenVersion` on logout (and any other rotation points), enforced per-request by `JwtFilter`.


