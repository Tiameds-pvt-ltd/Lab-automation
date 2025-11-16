# Lab and User Registration API Documentation

## Overview

This document describes the functionality of two key registration endpoints:
1. **Lab Registration** (`POST /lab/admin/add-lab`) - Creates a new laboratory
2. **User Registration** (`POST /public/register`) - Registers a new user account

---

## Table of Contents

1. [Lab Registration Endpoint](#lab-registration-endpoint)
2. [User Registration Endpoint](#user-registration-endpoint)
3. [Common Features](#common-features)
4. [Error Handling](#error-handling)
5. [Usage Examples](#usage-examples)
6. [Security Considerations](#security-considerations)

---

## Lab Registration Endpoint

### Endpoint Details

- **URL:** `/lab/admin/add-lab`
- **Method:** `POST`
- **Authentication:** Required (JWT Token)
- **Authorization:** User must be authenticated
- **Content-Type:** `application/json`

### Functionality

The lab registration endpoint allows an authenticated user to create a new laboratory. This endpoint performs several operations:

1. **Authentication Check**: Verifies the user is authenticated
2. **Duplicate Check**: Ensures the lab name doesn't already exist
3. **Lab Creation**: Creates a new lab with all provided details
4. **Member Assignment**: Automatically adds the creator as a member of the lab
5. **Default Data Import**: Automatically imports default test price list and test references from CSV files

### Request Body

The endpoint accepts a `LabRequestDTO` object with the following fields:

#### Basic Information
- `name` (String, required): Name of the laboratory
- `address` (String, required): Street address
- `city` (String, required): City name
- `state` (String, required): State/Province
- `description` (String, optional): Description of the lab
- `isActive` (Boolean, optional): Active status (defaults to `true`)

#### Lab Details
- `labLogo` (String, optional): Logo URL or path
- `licenseNumber` (String, required): Laboratory license number
- `labType` (String, required): Type of laboratory
- `labZip` (String, required): ZIP/Postal code
- `labCountry` (String, required): Country name
- `labPhone` (String, required): Laboratory phone number
- `labEmail` (String, required): Laboratory email address

#### Director Information
- `directorName` (String, required): Director's full name
- `directorEmail` (String, required): Director's email
- `directorPhone` (String, required): Director's phone number
- `directorGovtId` (String, optional): Director's government ID

#### Compliance & Certification
- `certificationBody` (String, optional): Certification body name
- `labCertificate` (String, optional): Certificate file path/URL
- `labBusinessRegistration` (String, optional): Business registration document
- `labLicense` (String, optional): License document
- `taxId` (String, optional): Tax identification number
- `labAccreditation` (String, optional): Accreditation details
- `dataPrivacyAgreement` (Boolean, optional): Data privacy agreement acceptance

### Request Example

```json
{
  "name": "ABC Diagnostic Laboratory",
  "address": "123 Medical Street",
  "city": "New York",
  "state": "NY",
  "description": "Full-service diagnostic laboratory",
  "labLogo": "https://example.com/logo.png",
  "licenseNumber": "LAB-12345",
  "labType": "Diagnostic",
  "labZip": "10001",
  "labCountry": "United States",
  "labPhone": "+1-555-123-4567",
  "labEmail": "info@abcdiag.com",
  "directorName": "Dr. John Smith",
  "directorEmail": "director@abcdiag.com",
  "directorPhone": "+1-555-123-4568",
  "directorGovtId": "DL-123456",
  "certificationBody": "CLIA",
  "labCertificate": "cert.pdf",
  "labBusinessRegistration": "reg.pdf",
  "labLicense": "license.pdf",
  "taxId": "12-3456789",
  "labAccreditation": "CAP Accredited",
  "dataPrivacyAgreement": true
}
```

### Response

#### Success Response (200 OK)

```json
{
  "status": "success",
  "message": "Lab created successfully and user added as a member",
  "data": {
    "id": 1,
    "name": "ABC Diagnostic Laboratory",
    "address": "123 Medical Street",
    "city": "New York",
    "state": "NY",
    "description": "Full-service diagnostic laboratory",
    "createdBy": {
      "id": 5,
      "username": "john_doe",
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe"
    }
  }
}
```

#### Error Responses

**401 Unauthorized** - User not authenticated
```json
{
  "status": "error",
  "message": "User not found",
  "data": null
}
```

**400 Bad Request** - Lab name already exists
```json
{
  "status": "error",
  "message": "Lab already exists",
  "data": null
}
```

**500 Internal Server Error** - Failed to add user as member
```json
{
  "status": "error",
  "message": "Failed to add user as member",
  "data": null
}
```

### Process Flow

1. **Authentication**: Validates JWT token and retrieves authenticated user
2. **Validation**: Checks if lab name already exists in the system
3. **Lab Creation**: Creates new `Lab` entity with all provided information
   - Sets `isActive` to `true`
   - Sets `createdBy` to the authenticated user
4. **Member Addition**: Automatically adds the creator as a lab member
5. **Default Data Import**: 
   - Imports default test price list from `tiamed_price_list.csv`
   - Imports default test references from `sample_test_references_with_reference_ranges.csv`
6. **Response**: Returns lab details with creator information

### Important Notes

- The lab creator is automatically added as a member of the lab
- Default test data is imported automatically (errors in import don't fail the lab creation)
- The lab is set as active by default
- All operations are transactional - if any step fails, the entire operation is rolled back

---

## User Registration Endpoint

### Endpoint Details

- **URL:** `/public/register`
- **Method:** `POST`
- **Authentication:** Not required (Public endpoint)
- **Content-Type:** `application/json`

### Functionality

The user registration endpoint allows anyone to create a new user account in the system. This endpoint:

1. **Validation**: Checks for duplicate username and email
2. **Password Encryption**: Securely hashes the password using BCrypt
3. **User Code Generation**: Generates a unique user code using the sequence generator
4. **User Creation**: Creates a new user account with all provided information
5. **Account Activation**: Sets the user account as enabled by default

### Request Body

The endpoint accepts a `RegisterRequest` object with the following fields:

#### Required Fields
- `username` (String, required): Unique username (must not be blank)
- `password` (String, required): Password (minimum 8 characters)
- `email` (String, required): Valid email address
- `firstName` (String, required): User's first name
- `lastName` (String, required): User's last name
- `phone` (String, required): Phone number (valid format: `+?[0-9. ()-]{7,25}`)
- `address` (String, required): Street address
- `city` (String, required): City name
- `state` (String, required): State/Province
- `zip` (String, required): ZIP code (format: `12345` or `12345-6789`)
- `country` (String, required): Country name
- `verified` (Boolean, required): Email verification status

### Validation Rules

- **Username**: Must not be blank, must be unique
- **Password**: Must be at least 8 characters long
- **Email**: Must be a valid email format, must be unique
- **Phone**: Must match pattern `^\+?[0-9. ()-]{7,25}$`
- **ZIP Code**: Must match pattern `^[0-9]{5}(?:-[0-9]{4})?$`

### Request Example

```json
{
  "username": "johndoe",
  "password": "SecurePass123!",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1-555-123-4567",
  "address": "123 Main Street",
  "city": "New York",
  "state": "NY",
  "zip": "10001",
  "country": "United States",
  "verified": false
}
```

### Response

#### Success Response (201 Created)

```json
{
  "status": "success",
  "message": "User registered successfully",
  "data": null
}
```

#### Error Responses

**400 Bad Request** - Username already taken
```json
{
  "status": "success",
  "message": "Username is already taken",
  "data": null
}
```

**400 Bad Request** - Email already taken
```json
{
  "status": "success",
  "message": "Email is already taken",
  "data": null
}
```

**400 Bad Request** - Validation error
```json
{
  "status": "error",
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email should be valid"
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters long"
    }
  ]
}
```

### Process Flow

1. **Duplicate Check**: 
   - Checks if username already exists
   - Checks if email already exists
2. **User Code Generation**: 
   - Generates unique user code using `SequenceGeneratorService`
   - Uses lab ID `0` for system-level users (users not associated with a lab)
   - Format: `USR-00001`, `USR-00002`, etc.
3. **User Creation**: 
   - Creates new `User` entity
   - Encrypts password using `BCryptPasswordEncoder`
   - Sets `enabled` to `true`
   - Sets `verified` status from request
4. **Persistence**: Saves user to database
5. **Response**: Returns success message

### Important Notes

- **Password Security**: Passwords are hashed using BCrypt before storage
- **User Code**: Automatically generated using sequence generator with lab ID `0`
- **Account Status**: New users are enabled by default but may need email verification
- **No Lab Association**: Registered users are system-level users (lab ID = 0) until they are added to a lab
- **Public Endpoint**: No authentication required, but rate limiting may apply

---

## Common Features

### Transaction Management

Both endpoints use `@Transactional` annotation to ensure data consistency:
- If any operation fails, all changes are rolled back
- Ensures atomicity of operations

### Error Handling

Both endpoints follow consistent error handling patterns:
- Return appropriate HTTP status codes
- Provide clear error messages
- Use consistent response format

### Response Format

All responses follow the `ApiResponse` format:
```json
{
  "status": "success|error",
  "message": "Human-readable message",
  "data": { ... } | null
}
```

---

## Usage Examples

### cURL Examples

#### Register a User

```bash
curl -X POST "http://localhost:8080/api/v1/public/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1-555-123-4567",
    "address": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "zip": "10001",
    "country": "United States",
    "verified": false
  }'
```

#### Create a Lab (Requires Authentication)

```bash
curl -X POST "http://localhost:8080/api/v1/lab/admin/add-lab" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "name": "ABC Diagnostic Laboratory",
    "address": "123 Medical Street",
    "city": "New York",
    "state": "NY",
    "description": "Full-service diagnostic laboratory",
    "licenseNumber": "LAB-12345",
    "labType": "Diagnostic",
    "labZip": "10001",
    "labCountry": "United States",
    "labPhone": "+1-555-123-4567",
    "labEmail": "info@abcdiag.com",
    "directorName": "Dr. John Smith",
    "directorEmail": "director@abcdiag.com",
    "directorPhone": "+1-555-123-4568",
    "dataPrivacyAgreement": true
  }'
```

### JavaScript/TypeScript Examples

#### Register a User

```typescript
async function registerUser(userData: RegisterRequest): Promise<ApiResponse> {
  const response = await fetch('http://localhost:8080/api/v1/public/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username: userData.username,
      password: userData.password,
      email: userData.email,
      firstName: userData.firstName,
      lastName: userData.lastName,
      phone: userData.phone,
      address: userData.address,
      city: userData.city,
      state: userData.state,
      zip: userData.zip,
      country: userData.country,
      verified: false
    })
  });
  
  return await response.json();
}
```

#### Create a Lab

```typescript
async function createLab(labData: LabRequestDTO, token: string): Promise<ApiResponse> {
  const response = await fetch('http://localhost:8080/api/v1/lab/admin/add-lab', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(labData)
  });
  
  return await response.json();
}
```

---

## Security Considerations

### Lab Registration

1. **Authentication Required**: Only authenticated users can create labs
2. **Authorization**: Users can only manage labs they created
3. **Duplicate Prevention**: Lab names must be unique
4. **Automatic Membership**: Creator is automatically added as a lab member

### User Registration

1. **Public Endpoint**: No authentication required, but consider implementing:
   - Rate limiting to prevent abuse
   - CAPTCHA for bot prevention
   - Email verification workflow
2. **Password Security**: 
   - Passwords are hashed using BCrypt
   - Minimum 8 characters required
   - Consider enforcing stronger password policies
3. **Data Validation**: 
   - All inputs are validated
   - Email format validation
   - Phone number format validation
   - ZIP code format validation
4. **Duplicate Prevention**: 
   - Username must be unique
   - Email must be unique

### Best Practices

1. **Input Validation**: Always validate input on both client and server side
2. **Error Messages**: Don't reveal sensitive information in error messages
3. **Rate Limiting**: Implement rate limiting for public endpoints
4. **Logging**: Log all registration attempts for audit purposes
5. **Email Verification**: Consider implementing email verification for user registration

---

## Troubleshooting

### Common Issues

#### Lab Registration

**Issue**: "Lab already exists" error
- **Cause**: Lab name is not unique
- **Solution**: Use a different lab name

**Issue**: "User not found" error
- **Cause**: JWT token is invalid or expired
- **Solution**: Re-authenticate and get a new token

**Issue**: "Failed to add user as member" error
- **Cause**: Internal error during member addition
- **Solution**: Check server logs, verify user exists

#### User Registration

**Issue**: "Username is already taken"
- **Cause**: Username already exists in database
- **Solution**: Choose a different username

**Issue**: "Email is already taken"
- **Cause**: Email already registered
- **Solution**: Use a different email or use forgot password

**Issue**: Validation errors
- **Cause**: Input doesn't meet validation requirements
- **Solution**: Check field requirements and format

---

## Related Documentation

- [JWT Authentication Overview](./JWT_AUTH_OVERVIEW.md)
- [Lab Management Documentation](./LAB_MANAGEMENT.md)
- [API Response Format](./API_RESPONSE_FORMAT.md)
- [Sequence Generator Service](./SEQUENCE_GENERATOR.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-16 | Initial documentation |

---

## Support

For questions or issues related to these endpoints, please contact the development team or refer to the main project documentation.

