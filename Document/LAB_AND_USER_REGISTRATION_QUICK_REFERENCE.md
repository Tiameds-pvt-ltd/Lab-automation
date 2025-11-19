# Lab and User Registration - Quick Reference

## Lab Registration

### Endpoint
```
POST /lab/admin/add-lab
```

### Authentication
✅ **Required** - JWT Token

### Key Features
- Creates a new laboratory
- Automatically adds creator as lab member
- Imports default test data (price list & references)
- Validates lab name uniqueness

### Request Body (Required Fields)
```json
{
  "name": "Lab Name",
  "address": "Street Address",
  "city": "City",
  "state": "State",
  "licenseNumber": "LIC-12345",
  "labType": "Diagnostic",
  "labZip": "12345",
  "labCountry": "Country",
  "labPhone": "+1-555-123-4567",
  "labEmail": "lab@example.com",
  "directorName": "Director Name",
  "directorEmail": "director@example.com",
  "directorPhone": "+1-555-123-4568"
}
```

### Success Response
```json
{
  "status": "success",
  "message": "Lab created successfully and user added as a member",
  "data": { ... }
}
```

### Common Errors
- `401` - User not authenticated
- `400` - Lab name already exists
- `500` - Failed to add user as member

---

## User Registration

### Endpoint
```
POST /public/register
```

### Authentication
❌ **Not Required** - Public endpoint

### Key Features
- Creates new user account
- Generates unique user code (USR-00001, etc.)
- Encrypts password with BCrypt
- Validates username/email uniqueness

### Request Body (All Required)
```json
{
  "username": "johndoe",
  "password": "SecurePass123!",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1-555-123-4567",
  "address": "123 Main St",
  "city": "New York",
  "state": "NY",
  "zip": "10001",
  "country": "United States",
  "verified": false
}
```

### Validation Rules
- **Username**: Unique, not blank
- **Password**: Min 8 characters
- **Email**: Valid format, unique
- **Phone**: Pattern `^\+?[0-9. ()-]{7,25}$`
- **ZIP**: Pattern `^[0-9]{5}(?:-[0-9]{4})?$`

### Success Response
```json
{
  "status": "success",
  "message": "User registered successfully",
  "data": null
}
```

### Common Errors
- `400` - Username already taken
- `400` - Email already taken
- `400` - Validation failed

---

## Quick Comparison

| Feature | Lab Registration | User Registration |
|---------|------------------|-------------------|
| **Auth Required** | ✅ Yes | ❌ No |
| **Endpoint** | `/lab/admin/add-lab` | `/public/register` |
| **Auto Membership** | ✅ Creator added | ❌ No |
| **Default Data** | ✅ Test data imported | ❌ No |
| **Code Generation** | ❌ No | ✅ User code (USR-XXXXX) |
| **Password Encryption** | ❌ No | ✅ BCrypt |

---

## cURL Examples

### Register User
```bash
curl -X POST "http://localhost:8080/api/v1/public/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1-555-123-4567",
    "address": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zip": "10001",
    "country": "United States",
    "verified": false
  }'
```

### Create Lab
```bash
curl -X POST "http://localhost:8080/api/v1/lab/admin/add-lab" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "ABC Lab",
    "address": "123 Medical St",
    "city": "New York",
    "state": "NY",
    "licenseNumber": "LAB-12345",
    "labType": "Diagnostic",
    "labZip": "10001",
    "labCountry": "United States",
    "labPhone": "+1-555-123-4567",
    "labEmail": "info@abclab.com",
    "directorName": "Dr. John Smith",
    "directorEmail": "director@abclab.com",
    "directorPhone": "+1-555-123-4568"
  }'
```

---

## Process Flow

### Lab Registration Flow
```
1. Authenticate User
   ↓
2. Check Lab Name Uniqueness
   ↓
3. Create Lab Entity
   ↓
4. Add Creator as Member
   ↓
5. Import Default Test Data
   ↓
6. Return Lab Details
```

### User Registration Flow
```
1. Check Username Uniqueness
   ↓
2. Check Email Uniqueness
   ↓
3. Generate User Code
   ↓
4. Encrypt Password
   ↓
5. Create User Entity
   ↓
6. Save to Database
   ↓
7. Return Success
```

---

## Security Notes

### Lab Registration
- ✅ Requires authentication
- ✅ Validates lab name uniqueness
- ✅ Creator automatically becomes member

### User Registration
- ⚠️ Public endpoint (consider rate limiting)
- ✅ Password encryption (BCrypt)
- ✅ Input validation
- ✅ Duplicate prevention

---

For detailed documentation, see [LAB_AND_USER_REGISTRATION_API.md](./LAB_AND_USER_REGISTRATION_API.md)










