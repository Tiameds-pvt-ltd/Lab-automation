# Tiameds Lab Automation System - Functional Specifications

## Table of Contents
1. [Overview](#overview)
2. [Module-wise Setup and Logic](#module-wise-setup-and-logic)
3. [Users](#users)
4. [Access Control](#access-control)
5. [System Architecture](#system-architecture)
6. [API Endpoints by Module](#api-endpoints-by-module)
7. [Business Logic Flow](#business-logic-flow)
8. [Data Flow Diagrams](#data-flow-diagrams)

---

## Overview

The Tiameds Lab Automation System is a comprehensive laboratory management platform designed for multi-lab environments with role-based access control. The system provides end-to-end functionality for laboratory operations, patient management, test processing, billing, and reporting.

### Key System Characteristics
- **Multi-tenant Architecture**: Support for multiple laboratories with isolated data
- **Role-based Access Control**: Four distinct user roles with hierarchical permissions
- **RESTful API Design**: Comprehensive REST API with OpenAPI documentation
- **Security-first Approach**: JWT authentication, rate limiting, and IP whitelisting
- **Scalable Design**: Microservice-ready architecture with clear separation of concerns

---

## Module-wise Setup and Logic

### 1. Authentication & Authorization Module

#### 1.1 Authentication Controller (`/auth`)
**Purpose**: Handles user authentication, registration, and session management

**Key Components**:
- `AdminController.java` - Admin-specific authentication
- `UserController.java` - User management and authentication
- `LogoutController.java` - Session termination
- `SecurityController.java` - Security configuration management

**Business Logic**:
```java
// Authentication Flow
1. User submits credentials
2. System validates credentials against database
3. JWT token generated with user roles and permissions
4. Token returned to client with 10-hour expiration
5. Subsequent requests include token in Authorization header
```

**Key Features**:
- JWT-based stateless authentication
- Token versioning for security
- Role-based access control
- Rate limiting on login attempts
- IP whitelisting capability

#### 1.2 User Management Module
**Purpose**: Manages user accounts, roles, and permissions

**Key Components**:
- `UserService.java` - Core user operations
- `UserDetailsServiceImpl.java` - Spring Security integration
- `MemberUserServices.java` - Lab member management

**Business Logic**:
```java
// User Creation Hierarchy
SUPERADMIN → Can create: SUPERADMIN, ADMIN, TECHNICIAN, DESKROLE
ADMIN → Can create: ADMIN, TECHNICIAN, DESKROLE
TECHNICIAN/DESKROLE → Cannot create users
```

### 2. Laboratory Management Module

#### 2.1 Lab Administration (`/lab/admin`)
**Purpose**: Core laboratory management functionality

**Key Components**:
- `LabController.java` - Lab CRUD operations
- `LabAdminController.java` - Lab administration
- `LabMemberController.java` - Lab member management

**Business Logic**:
```java
// Lab Creation Process
1. Authenticated user creates lab
2. User becomes lab owner/administrator
3. Lab gets unique identifier
4. Lab configuration (contact info, licensing, etc.)
5. Lab activation and member assignment
```

**Key Features**:
- Lab registration with comprehensive details
- Lab member management
- Lab statistics and reporting
- Lab configuration management

#### 2.2 Lab Operations (`/lab`)
**Purpose**: Day-to-day laboratory operations

**Key Components**:
- `PatientController.java` - Patient management
- `VisitController.java` - Visit management
- `TestController.java` - Test management
- `BillingController.java` - Billing operations

### 3. Patient Management Module

#### 3.1 Patient Operations
**Purpose**: Complete patient lifecycle management

**Key Components**:
- `PatientService.java` - Patient business logic
- `UpdatePatientService.java` - Patient update operations

**Business Logic**:
```java
// Patient Registration Flow
1. Patient demographic information collection
2. Phone number validation and uniqueness check
3. Patient record creation with lab association
4. Guardian relationship establishment (if applicable)
5. Patient search and retrieval by phone number
```

**Key Features**:
- Patient registration with demographics
- Phone-based patient search
- Patient information updates
- Guardian relationship management
- Patient visit history tracking

#### 3.2 Visit Management
**Purpose**: Patient visit processing and management

**Key Components**:
- `VisitService.java` - Visit business logic
- `PatientVisitSample.java` - Sample association

**Business Logic**:
```java
// Visit Creation Process
1. Patient selection/registration
2. Test selection and assignment
3. Visit creation with test associations
4. Sample collection planning
5. Visit status tracking (scheduled, in-progress, completed)
```

### 4. Test Management Module

#### 4.1 Test Operations
**Purpose**: Laboratory test catalog and management

**Key Components**:
- `TestServices.java` - Test business logic
- `TestReferenceServices.java` - Test reference management
- `AdminTestReferanceandTestServices.java` - Admin test operations

**Business Logic**:
```java
// Test Management Flow
1. Test catalog maintenance
2. Test pricing and categorization
3. Test reference ranges
4. Test package creation
5. CSV import/export for bulk operations
```

**Key Features**:
- Test catalog with pricing
- Test categories and organization
- Reference ranges and normal values
- Test package management
- Bulk test operations via CSV

#### 4.2 Test Reference Management
**Purpose**: Test reference ranges and normal values

**Key Components**:
- `TestReferenceController.java` - Reference operations
- `TestReferenceServices.java` - Reference business logic

**Business Logic**:
```java
// Reference Range Management
1. Test reference range definition
2. Gender-specific ranges
3. Age-specific ranges
4. Reference value validation
5. Test result interpretation
```

### 5. Billing & Payment Module

#### 5.1 Billing Operations
**Purpose**: Financial management and billing

**Key Components**:
- `BillingService.java` - Billing business logic
- `BillingManagementService.java` - Billing management

**Business Logic**:
```java
// Billing Process
1. Test cost calculation
2. GST calculation (CGST, SGST, IGST)
3. Discount application
4. Payment processing
5. Transaction recording
6. Receipt generation
```

**Key Features**:
- Automatic GST calculations
- Discount management
- Payment tracking
- Transaction history
- Financial reporting

### 6. Report Generation Module

#### 6.1 Report Operations
**Purpose**: Test results and report generation

**Key Components**:
- `ReportService.java` - Report business logic
- `ReportGeneration.java` - Report generation
- `SampleAssociation.java` - Sample processing

**Business Logic**:
```java
// Report Generation Flow
1. Test result entry
2. Sample processing completion
3. Report template selection
4. Report generation and formatting
5. Report delivery (print/email)
```

### 7. Super Admin Module

#### 7.1 System Administration
**Purpose**: System-wide administration and configuration

**Key Components**:
- `LabSuperAdminController.java` - Super admin operations
- `ReferanceAndTestController.java` - Reference and test management
- `LabSuperAdminService.java` - Super admin business logic

**Business Logic**:
```java
// Super Admin Operations
1. System-wide configuration
2. Global test reference management
3. Multi-lab oversight
4. System security management
5. Global reporting and analytics
```

---

## Users

### User Role Hierarchy

#### 1. SUPERADMIN
**Purpose**: System-wide administration and control

**Capabilities**:
- Full system access across all laboratories
- Lab creation and management
- User management across all labs
- System configuration and security
- Global test reference management
- Multi-lab reporting and analytics

**Access Scope**:
- All endpoints in the system
- Cross-lab data access
- System configuration endpoints
- Security management endpoints

#### 2. ADMIN
**Purpose**: Laboratory-specific administration

**Capabilities**:
- Lab-specific management
- User management within assigned labs
- Test and package management
- Billing oversight and reporting
- Lab configuration management
- Member management

**Access Scope**:
- Lab-specific endpoints (`/lab/{labId}/**`)
- Admin endpoints (`/admin/**`)
- User management within lab scope
- Test and billing management

#### 3. TECHNICIAN
**Purpose**: Test processing and result entry

**Capabilities**:
- Test result entry and management
- Sample collection and processing
- Visit completion
- Test reference management
- Sample association
- Test result validation

**Access Scope**:
- Test-related endpoints
- Sample management endpoints
- Visit completion endpoints
- Test reference endpoints

#### 4. DESKROLE
**Purpose**: Front-desk operations and patient management

**Capabilities**:
- Patient registration and management
- Visit scheduling and management
- Basic billing operations
- Patient search and lookup
- Visit status updates
- Basic reporting

**Access Scope**:
- Patient management endpoints
- Visit management endpoints
- Basic billing endpoints
- Patient search endpoints

### User Creation Rules

#### Hierarchical User Creation
```java
SUPERADMIN → Can create: SUPERADMIN, ADMIN, TECHNICIAN, DESKROLE
ADMIN → Can create: ADMIN, TECHNICIAN, DESKROLE
TECHNICIAN → Cannot create users
DESKROLE → Cannot create users
```

#### User Validation Rules
1. **Username Uniqueness**: Must be unique across the system
2. **Email Uniqueness**: Must be unique across the system
3. **Role Assignment**: Based on creator's role hierarchy
4. **Lab Association**: Users must be associated with at least one lab
5. **Permission Inheritance**: Users inherit permissions from their roles

---

## Access Control

### Authentication Mechanisms

#### 1. JWT Authentication
**Implementation**: Stateless JWT tokens with role-based claims

**Token Structure**:
```json
{
  "sub": "username",
  "roles": ["ADMIN", "TECHNICIAN"],
  "labs": [1, 2, 3],
  "exp": 1640995200,
  "iat": 1640952000,
  "version": 1
}
```

**Token Lifecycle**:
- **Expiration**: 10 hours
- **Versioning**: Prevents token reuse after logout
- **Refresh**: Manual re-authentication required

#### 2. Role-Based Access Control (RBAC)
**Implementation**: Spring Security with method-level security

**Access Control Matrix**:

| Role | Lab Management | User Management | Test Management | Billing | Reports |
|------|----------------|-----------------|-----------------|---------|---------|
| SUPERADMIN | ✅ All Labs | ✅ All Users | ✅ All Tests | ✅ All Billing | ✅ All Reports |
| ADMIN | ✅ Assigned Labs | ✅ Lab Users | ✅ Lab Tests | ✅ Lab Billing | ✅ Lab Reports |
| TECHNICIAN | ❌ | ❌ | ✅ Lab Tests | ❌ | ✅ Test Results |
| DESKROLE | ❌ | ❌ | ❌ | ✅ Basic Billing | ✅ Basic Reports |

#### 3. Endpoint Security Configuration

**Public Endpoints** (No Authentication Required):
```java
- /auth/login
- /auth/register
- /public/**
- /v3/api-docs/**
- /doc/**
- /swagger-ui/**
```

**Role-Specific Endpoints**:

**SUPERADMIN Only**:
```java
- /lab-super-admin/**
- /super-admin/**
```

**ADMIN and SUPERADMIN**:
```java
- /admin/**
- /lab/admin/**
- /user-management/**
```

**TECHNICIAN, DESKROLE, ADMIN, SUPERADMIN**:
```java
- /lab/{labId}/visits
- /lab/{labId}/patients
- /lab/{labId}/add-patient
```

#### 4. Lab-Scoped Access Control
**Implementation**: Lab-specific data isolation

**Access Rules**:
1. **Lab Association**: Users can only access data from their associated labs
2. **Cross-Lab Access**: Only SUPERADMIN can access cross-lab data
3. **Lab Validation**: All lab-specific endpoints validate lab membership
4. **Data Filtering**: Queries automatically filter by user's lab associations

#### 5. Rate Limiting
**Implementation**: Bucket4j with Redis backend

**Rate Limits**:
- **IP-based**: 10 attempts per 10 minutes
- **User-based**: 5 attempts per 10 minutes
- **Scope**: Login and authentication endpoints only

**Configuration**:
```yaml
rate:
  limit:
    login:
      attempts: 5
      window: 10
    ip:
      attempts: 10
      window: 10
```

#### 6. IP Whitelisting
**Implementation**: Configurable IP-based access control

**Features**:
- **Configurable**: Can be enabled/disabled via API
- **Dynamic Management**: Add/remove IPs via API endpoints
- **Scope**: Login endpoints only
- **Fallback**: Disabled by default for development

**API Endpoints**:
```java
GET    /api/v1/admin/security/ip-whitelist/status
POST   /api/v1/admin/security/ip-whitelist/enable
POST   /api/v1/admin/security/ip-whitelist/disable
POST   /api/v1/admin/security/ip-whitelist/add
DELETE /api/v1/admin/security/ip-whitelist/remove
```

### Security Filters

#### 1. JWT Filter
**Purpose**: Token validation and user authentication
**Implementation**: Custom filter in Spring Security chain
**Features**:
- Token extraction from Authorization header
- Token validation and signature verification
- User context establishment
- Role and permission extraction

#### 2. Rate Limit Filter
**Purpose**: Request rate limiting
**Implementation**: Bucket4j with in-memory storage
**Features**:
- IP-based rate limiting
- User-based rate limiting
- Configurable limits and windows
- Automatic cleanup of expired entries

#### 3. IP Whitelist Filter
**Purpose**: IP-based access control
**Implementation**: Custom filter for login endpoints
**Features**:
- Configurable IP whitelist
- Dynamic IP management
- Login endpoint protection
- Fallback to disabled state

### CORS Configuration

#### Allowed Origins
```java
- https://lab-test-env.tiameds.ai
- http://localhost:3000
```

#### Allowed Methods
```java
- GET, POST, PUT, DELETE, OPTIONS, PATCH
```

#### Security Features
- **Credentials**: Enabled for authenticated requests
- **Max Age**: 3600 seconds (1 hour)
- **Exposed Headers**: Content-Disposition, Access-Control-Allow-Origin

---

## System Architecture

### High-Level Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   API Gateway   │    │   Load Balancer │
│   (React/Vue)   │◄──►│   (Spring Boot) │◄──►│   (Optional)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   Database      │
                       │   (PostgreSQL)  │
                       └─────────────────┘
```

### Application Layers

#### 1. Controller Layer
**Purpose**: REST API endpoints with role-based access control
**Components**:
- Authentication controllers
- Lab management controllers
- Patient management controllers
- Test management controllers
- Billing controllers
- Report controllers

#### 2. Service Layer
**Purpose**: Business logic and transaction management
**Components**:
- User services
- Lab services
- Patient services
- Test services
- Billing services
- Report services

#### 3. Repository Layer
**Purpose**: Data access with JPA/Hibernate
**Components**:
- Entity repositories
- Custom query implementations
- Data access optimization

#### 4. Entity Layer
**Purpose**: Domain models and relationships
**Components**:
- User entities
- Lab entities
- Patient entities
- Test entities
- Billing entities

#### 5. Security Layer
**Purpose**: Authentication, authorization, and filtering
**Components**:
- JWT authentication
- Role-based access control
- Rate limiting
- IP whitelisting

---

## API Endpoints by Module

### Authentication Module (`/auth`)
```
POST   /auth/login                    - User authentication
POST   /auth/register                 - User registration
POST   /auth/logout                   - User logout
GET    /auth/user-profile             - Get user profile
PUT    /auth/update-profile           - Update user profile
```

### Lab Management Module (`/lab/admin`)
```
GET    /lab/admin/get-labs            - Get user's labs
POST   /lab/admin/add-lab             - Create new lab
PUT    /lab/admin/update-lab/{id}     - Update lab
DELETE /lab/admin/delete-lab/{id}     - Delete lab
GET    /lab/admin/get-members/{labId} - Get lab members
POST   /lab/admin/add-member          - Add lab member
```

### Patient Management Module (`/lab/{labId}`)
```
GET    /lab/{labId}/patients         - Get lab patients
GET    /lab/{labId}/patient/{id}     - Get specific patient
POST   /lab/{labId}/add-patient       - Add new patient
PUT    /lab/{labId}/update-patient/{id} - Update patient
DELETE /lab/{labId}/delete-patient/{id} - Delete patient
GET    /lab/{labId}/search-patient    - Search patients
```

### Visit Management Module (`/lab/{labId}`)
```
POST   /lab/{labId}/add-visit/{patientId} - Create visit
GET    /lab/{labId}/visits            - Get lab visits
PUT    /lab/{labId}/update-visit/{id} - Update visit
DELETE /lab/{labId}/delete-visit/{id} - Delete visit
GET    /lab/{labId}/visit/{id}       - Get specific visit
GET    /lab/{labId}/visitsdatewise   - Get visits by date
```

### Test Management Module (`/admin/lab/{labId}`)
```
GET    /admin/lab/{labId}/tests      - Get lab tests
POST   /admin/lab/{labId}/add        - Add new test
PUT    /admin/lab/{labId}/update/{id} - Update test
DELETE /admin/lab/{labId}/remove/{id} - Remove test
GET    /admin/lab/{labId}/test/{id}  - Get specific test
POST   /admin/lab/test/{labId}/csv/upload - Upload test CSV
GET    /admin/lab/{labId}/download   - Download test CSV
```

### Billing Module (`/lab/{labId}`)
```
POST   /lab/{labId}/billing/{id}/partial-payment - Process payment
GET    /lab/{labId}/report/{visitId} - Generate visit report
```

### Security Module (`/admin/security`)
```
GET    /admin/security/ip-whitelist/status - Get IP whitelist status
POST   /admin/security/ip-whitelist/enable - Enable IP whitelist
POST   /admin/security/ip-whitelist/disable - Disable IP whitelist
POST   /admin/security/ip-whitelist/add - Add IP to whitelist
DELETE /admin/security/ip-whitelist/remove - Remove IP from whitelist
POST   /admin/security/rate-limit/reset - Reset rate limits
GET    /admin/security/rate-limit/status - Get rate limit status
```

---

## Business Logic Flow

### Patient Registration Flow
```
1. User Authentication
   ↓
2. Lab Association Validation
   ↓
3. Patient Information Collection
   ↓
4. Phone Number Uniqueness Check
   ↓
5. Patient Record Creation
   ↓
6. Lab Association Assignment
   ↓
7. Success Response
```

### Visit Creation Flow
```
1. Patient Selection/Registration
   ↓
2. Test Selection and Assignment
   ↓
3. Visit Creation with Test Associations
   ↓
4. Sample Collection Planning
   ↓
5. Visit Status Tracking
   ↓
6. Billing Calculation
   ↓
7. Visit Confirmation
```

### Test Processing Flow
```
1. Sample Collection
   ↓
2. Sample Association with Tests
   ↓
3. Test Execution
   ↓
4. Result Entry
   ↓
5. Reference Range Validation
   ↓
6. Report Generation
   ↓
7. Result Delivery
```

### Billing Process Flow
```
1. Test Cost Calculation
   ↓
2. Discount Application
   ↓
3. GST Calculation
   ↓
4. Total Amount Calculation
   ↓
5. Payment Processing
   ↓
6. Transaction Recording
   ↓
7. Receipt Generation
```

---

## Data Flow Diagrams

### User Authentication Flow
```
Client → API Gateway → JWT Filter → Rate Limit Filter → Controller → Service → Repository → Database
  ↓
Response ← JSON ← Controller ← Service ← Repository ← Database
```

### Lab Data Access Flow
```
Client → Lab Controller → Lab Service → Lab Repository → Database
  ↓
Response ← Lab Data ← Controller ← Service ← Repository ← Database
```

### Patient Data Flow
```
Client → Patient Controller → Patient Service → Patient Repository → Database
  ↓
Response ← Patient Data ← Controller ← Service ← Repository ← Database
```

### Test Processing Flow
```
Client → Test Controller → Test Service → Test Repository → Database
  ↓
Response ← Test Data ← Controller ← Service ← Repository ← Database
```

---

## Conclusion

The Tiameds Lab Automation System provides a comprehensive, secure, and scalable solution for laboratory management. The modular architecture, role-based access control, and extensive API coverage make it suitable for various laboratory environments and use cases.

The system's functional specifications ensure:
- **Security**: Multi-layered security with JWT authentication, RBAC, and rate limiting
- **Scalability**: Modular design supporting multiple laboratories
- **Flexibility**: Role-based access control with hierarchical permissions
- **Maintainability**: Clear separation of concerns and well-defined interfaces
- **Extensibility**: Modular architecture supporting future enhancements

For additional technical details, please refer to the API documentation at `/api/v1/doc` or the complete system documentation in `TIAMEDS_LAB_AUTOMATION_DOCUMENTATION.md`.

---

*Document Version: 1.0*  
*Last Updated: January 2024*  
*Maintained by: Tiameds Development Team*
