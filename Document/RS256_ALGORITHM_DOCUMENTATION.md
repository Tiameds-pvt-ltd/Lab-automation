# RS256 Algorithm Documentation

## Overview

This document explains how the **RS256 (RSA Signature with SHA-256)** algorithm is implemented and used in the Lab Automation platform for JWT (JSON Web Token) signing and verification.

---

## What is RS256?

**RS256** is an asymmetric cryptographic algorithm that uses:
- **RSA (Rivest-Shamir-Adleman)** public-key cryptosystem
- **SHA-256** hashing algorithm for message digest

### Key Characteristics

1. **Asymmetric Cryptography**: Uses a key pair (public and private keys)
2. **Private Key**: Used to **sign** JWT tokens (kept secret on the server)
3. **Public Key**: Used to **verify** JWT token signatures (can be shared publicly)
4. **Security**: Even if the public key is exposed, tokens cannot be forged without the private key

---

## How RS256 Works in This Project

### Architecture Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Startup                      │
│  @PostConstruct initKeys() loads RSA key pair from files   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              JwtUtil Class (Component)                       │
│  • PrivateKey privateKey (loaded once at startup)           │
│  • PublicKey publicKey (loaded once at startup)             │
└─────────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┴─────────────────┐
        │                                   │
        ▼                                   ▼
┌──────────────────┐              ┌──────────────────┐
│  Token Signing   │              │ Token Verification│
│  (Login/Refresh) │              │  (Request Filter) │
│                  │              │                  │
│ Uses PrivateKey  │              │ Uses PublicKey   │
│ + RS256          │              │ + RS256          │
└──────────────────┘              └──────────────────┘
```

---

## Key Access and Loading Process

### 1. Configuration Properties

Keys are configured through `JwtProperties` class using Spring's `@ConfigurationProperties`:

**Configuration Class**: `src/main/java/tiameds/com/tiameds/config/JwtProperties.java`

```yaml
# application.yml
security:
  jwt:
    private-key-location: ${JWT_PRIVATE_KEY_LOCATION}
    public-key-location: ${JWT_PUBLIC_KEY_LOCATION}
```

**Properties**:
- `privateKeyLocation`: Spring `Resource` pointing to the private key file
- `publicKeyLocation`: Spring `Resource` pointing to the public key file

**Supported Resource Types**:
- **Classpath**: `classpath:keys/private.pem` (for development)
- **File System**: `file:/etc/tiameds/jwt/private.pem` (for production)
- **Environment Variables**: `JWT_PRIVATE_KEY_LOCATION` and `JWT_PUBLIC_KEY_LOCATION`

### 2. Key Loading Initialization

**Location**: `src/main/java/tiameds/com/tiameds/utils/JwtUtil.java`

**Method**: `initKeys()` (annotated with `@PostConstruct`)

**Process**:
1. **Trigger**: Automatically executed after Spring container initializes the `JwtUtil` bean
2. **Execution**: Runs once at application startup
3. **Actions**:
   - Calls `loadPrivateKey()` to load the RSA private key
   - Calls `loadPublicKey()` to load the RSA public key
   - Stores keys in instance variables (`privateKey` and `publicKey`)
4. **Error Handling**: If loading fails, throws `IllegalStateException`, preventing application startup

**Code Flow**:
```java
@PostConstruct
void initKeys() {
    try {
        this.privateKey = loadPrivateKey();
        this.publicKey = loadPublicKey();
    } catch (IOException | GeneralSecurityException e) {
        throw new IllegalStateException("Unable to load RSA keys for JWT processing", e);
    }
}
```

### 3. Private Key Loading

**Method**: `loadPrivateKey()`

**Process**:
1. **Read Key File**: 
   - Uses `jwtProperties.getPrivateKeyLocation()` to get the resource
   - Reads the file content as an `InputStream`
   - Converts to UTF-8 string

2. **Parse PEM Format**:
   - Expected format: `-----BEGIN PRIVATE KEY-----` ... `-----END PRIVATE KEY-----`
   - Removes PEM headers/footers
   - Removes all whitespace
   - Base64 decodes the key bytes

3. **Create Key Object**:
   - Uses `PKCS8EncodedKeySpec` (standard format for private keys)
   - Uses `KeyFactory.getInstance("RSA")` to generate `PrivateKey` object
   - Returns `java.security.PrivateKey`

**Key Format**: PKCS#8 (Private-Key Information Syntax Standard)

### 4. Public Key Loading

**Method**: `loadPublicKey()`

**Process**:
1. **Read Key File**:
   - Uses `jwtProperties.getPublicKeyLocation()` to get the resource
   - Reads the file content as an `InputStream`
   - Converts to UTF-8 string

2. **Parse PEM Format**:
   - Expected format: `-----BEGIN PUBLIC KEY-----` ... `-----END PUBLIC KEY-----`
   - Removes PEM headers/footers
   - Removes all whitespace
   - Base64 decodes the key bytes

3. **Create Key Object**:
   - Uses `X509EncodedKeySpec` (standard format for public keys)
   - Uses `KeyFactory.getInstance("RSA")` to generate `PublicKey` object
   - Returns `java.security.PublicKey`

**Key Format**: X.509 (standard format for public keys)

### 5. Key Reading Helper Method

**Method**: `readKey(Resource resource, String prefix, String suffix)`

**Purpose**: Common utility to read and parse PEM-encoded keys

**Steps**:
1. Opens `InputStream` from the resource
2. Reads all bytes and converts to UTF-8 string
3. Removes PEM prefix (e.g., `-----BEGIN PRIVATE KEY-----`)
4. Removes PEM suffix (e.g., `-----END PRIVATE KEY-----`)
5. Removes all whitespace (spaces, newlines, tabs)
6. Base64 decodes the remaining string to get raw key bytes
7. Returns byte array for key specification creation

---

## Key Usage in JWT Operations

### 1. Token Signing (Token Generation)

**When**: During login or token refresh operations

**Location**: `JwtUtil.createToken()`

**Process**:
1. Creates JWT claims (subject, issuer, audience, expiration, etc.)
2. Uses `Jwts.builder()` to build the token
3. **Signs with private key**: `.signWith(privateKey, SignatureAlgorithm.RS256)`
4. Returns compact JWT string

**Example Flow**:
```
User Login → AuthController.login() 
  → JwtUtil.generateAccessToken() 
  → JwtUtil.createToken() 
  → .signWith(privateKey, RS256) 
  → Returns signed JWT token
```

**Security**: Only the server with the private key can create valid tokens.

### 2. Token Verification (Token Validation)

**When**: On every authenticated API request

**Location**: `JwtUtil.parseClaims()` and `JwtFilter.doFilterInternal()`

**Process**:
1. **Token Extraction**: `JwtFilter` extracts token from cookie or Authorization header
2. **Parsing**: `JwtUtil.parseAccessToken()` is called
3. **Verification**:
   - Uses `Jwts.parserBuilder().setSigningKey(publicKey)`
   - Verifies signature using RS256 algorithm
   - Validates issuer, audience, expiration
   - Returns `Claims` object if valid
4. **Authentication**: If valid, sets Spring Security authentication context

**Example Flow**:
```
API Request → JwtFilter.doFilterInternal() 
  → Extract token from cookie/header 
  → JwtUtil.parseAccessToken() 
  → Jwts.parserBuilder().setSigningKey(publicKey) 
  → Verify signature with RS256 
  → Set authentication context
```

**Security**: Anyone with the public key can verify token authenticity, but cannot create new tokens.

---

## Key File Formats

### Private Key Format (PKCS#8)

**File Structure**:
```
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...
(base64 encoded key data)
...
-----END PRIVATE KEY-----
```

**Characteristics**:
- PEM (Privacy-Enhanced Mail) encoding
- Base64 encoded binary key data
- PKCS#8 format (standard for private keys)
- Must be kept **SECRET** and never exposed

### Public Key Format (X.509)

**File Structure**:
```
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
(base64 encoded key data)
...
-----END PUBLIC KEY-----
```

**Characteristics**:
- PEM (Privacy-Enhanced Mail) encoding
- Base64 encoded binary key data
- X.509 format (standard for public keys)
- Can be **PUBLICLY SHARED** (used for verification only)

---

## Security Considerations

### 1. Private Key Protection

**Critical Requirements**:
- **Never commit to version control**: Private keys should not be in Git repositories
- **Secure file permissions**: Restrict file access (e.g., `chmod 600 private.pem`)
- **Environment variables**: Use secure secret management (AWS Secrets Manager, HashiCorp Vault, etc.)
- **Server-only access**: Private key should only be accessible to the application server
- **Key rotation**: Implement key rotation strategy for enhanced security

### 2. Public Key Distribution

**Characteristics**:
- Can be shared publicly without security risk
- Used only for verification, not signing
- Can be embedded in client applications
- Can be exposed via API endpoints (e.g., `/auth/public-key`)

### 3. Key Pair Generation

**Recommended Tools**:
- **OpenSSL**: `openssl genrsa -out private.pem 2048` and `openssl rsa -in private.pem -pubout -out public.pem`
- **Java Keytool**: For Java KeyStore format
- **Online tools**: For development/testing only

**Key Size**: Minimum 2048 bits (RSA-2048) recommended for production

### 4. Asymmetric vs Symmetric Cryptography

**Why RS256 (Asymmetric) over HS256 (Symmetric)?**

| Aspect | RS256 (Asymmetric) | HS256 (Symmetric) |
|--------|-------------------|-------------------|
| **Key Management** | Public key can be shared | Secret key must be kept secret |
| **Scalability** | Multiple services can verify with public key | All services need same secret key |
| **Security** | Private key never leaves server | Secret key must be distributed |
| **Use Case** | Microservices, distributed systems | Single-service applications |

**This Project Uses RS256 Because**:
- Multiple services/clients can verify tokens independently
- Public key can be safely distributed
- Private key remains secure on the server
- Better suited for distributed architectures

---

## Configuration Examples

### Development Environment

**application.yml**:
```yaml
security:
  jwt:
    private-key-location: classpath:keys/private.pem
    public-key-location: classpath:keys/public.pem
```

**Key Location**: `src/main/resources/keys/private.pem` and `public.pem`

### Production Environment

**Environment Variables**:
```bash
JWT_PRIVATE_KEY_LOCATION=file:/etc/tiameds/jwt/private.pem
JWT_PUBLIC_KEY_LOCATION=file:/etc/tiameds/jwt/public.pem
```

**File System Structure**:
```
/etc/tiameds/jwt/
├── private.pem    (chmod 600, owned by app user)
└── public.pem     (chmod 644, readable by all)
```

### Docker/Container Environment

**Volume Mount**:
```yaml
volumes:
  - /host/path/to/keys:/etc/tiameds/jwt:ro
```

**Environment Variables**:
```yaml
environment:
  - JWT_PRIVATE_KEY_LOCATION=file:/etc/tiameds/jwt/private.pem
  - JWT_PUBLIC_KEY_LOCATION=file:/etc/tiameds/jwt/public.pem
```

---

## Key Lifecycle Management

### 1. Initial Key Generation

**Steps**:
1. Generate RSA key pair using OpenSSL or similar tool
2. Store private key securely (encrypted, restricted access)
3. Distribute public key to clients/services that need to verify tokens
4. Configure application with key file locations

### 2. Key Rotation

**When to Rotate**:
- Security breach suspected
- Key compromise detected
- Scheduled rotation (e.g., annually)
- Compliance requirements

**Rotation Process**:
1. Generate new key pair
2. Update configuration to point to new keys
3. Restart application (new tokens will use new keys)
4. Old tokens become invalid (may need grace period)
5. Archive old keys securely

### 3. Key Storage Best Practices

**Production Recommendations**:
- **AWS Secrets Manager**: Store keys as secrets, retrieve at runtime
- **HashiCorp Vault**: Centralized secret management
- **Kubernetes Secrets**: Encrypted at rest, mounted as volumes
- **Azure Key Vault / GCP Secret Manager**: Cloud-native solutions
- **Encrypted File System**: Encrypt keys at rest with disk encryption

---

## Troubleshooting

### Common Issues

1. **"Unable to load RSA keys for JWT processing"**
   - **Cause**: Key file not found or invalid format
   - **Solution**: Verify file paths, check file permissions, validate PEM format

2. **"Invalid signature" during token verification**
   - **Cause**: Public/private key mismatch
   - **Solution**: Ensure public key corresponds to the private key used for signing

3. **"Key format not supported"**
   - **Cause**: Wrong key format (e.g., using PKCS#1 instead of PKCS#8)
   - **Solution**: Convert key to correct format using OpenSSL

4. **File permission errors**
   - **Cause**: Application user doesn't have read permissions
   - **Solution**: Set appropriate file permissions (`chmod 600` for private, `chmod 644` for public)

---

## Summary

### Key Points

1. **RS256 Algorithm**: Uses RSA with SHA-256 for asymmetric JWT signing
2. **Key Loading**: Happens once at startup via `@PostConstruct` in `JwtUtil`
3. **Private Key**: Loaded from configured location, used for signing tokens
4. **Public Key**: Loaded from configured location, used for verifying tokens
5. **Security**: Private key never leaves server; public key can be shared
6. **Configuration**: Keys configured via `JwtProperties` from `application.yml` or environment variables
7. **Format**: Keys must be PEM-encoded (PKCS#8 for private, X.509 for public)

### Architecture Benefits

- **Stateless Authentication**: No need for shared secrets across services
- **Scalability**: Multiple services can verify tokens independently
- **Security**: Private key protection ensures token integrity
- **Flexibility**: Public key distribution enables microservices architecture

---

## Related Documentation

- **JWT Authentication Overview**: `Document/JWT_AUTH_OVERVIEW.md`
- **JwtUtil Class**: `src/main/java/tiameds/com/tiameds/utils/JwtUtil.java`
- **JwtProperties Class**: `src/main/java/tiameds/com/tiameds/config/JwtProperties.java`
- **JwtFilter Class**: `src/main/java/tiameds/com/tiameds/filter/JwtFilter.java`

---

**Last Updated**: 2024-12-30  
**Version**: 1.0

