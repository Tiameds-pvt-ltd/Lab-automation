# Tiameds Lab Automation System - Complete Documentation

## Table of Contents
1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Technology Stack](#technology-stack)
4. [Database Schema](#database-schema)
5. [API Documentation](#api-documentation)
6. [Security Features](#security-features)
7. [CORS Configuration](#cors-configuration)
8. [User Roles and Permissions](#user-roles-and-permissions)
9. [Core Features](#core-features)
10. [Configuration Management](#configuration-management)
11. [Deployment Guide](#deployment-guide)
12. [API Endpoints Reference](#api-endpoints-reference)
13. [Error Handling](#error-handling)
14. [Best Practices](#best-practices)

---

## Overview

The Tiameds Lab Automation System is a comprehensive laboratory management platform built with Spring Boot 3.3.4 and Java 17. It provides end-to-end functionality for managing laboratory operations, patient visits, test management, billing, and reporting in a multi-lab environment.

### Key Capabilities
- **Multi-lab Management**: Support for multiple laboratories with role-based access control
- **Patient Management**: Complete patient lifecycle management with visit tracking
- **Test Management**: Comprehensive test catalog with pricing and categorization
- **Billing System**: Advanced billing with GST calculations, discounts, and payment tracking
- **Report Generation**: Automated report generation and test result management
- **Security**: Enterprise-grade security with JWT authentication, rate limiting, and IP whitelisting

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
1. **Controller Layer**: REST API endpoints with role-based access control
2. **Service Layer**: Business logic and transaction management
3. **Repository Layer**: Data access with JPA/Hibernate
4. **Entity Layer**: Domain models and relationships
5. **Security Layer**: Authentication, authorization, and filtering

---

## Technology Stack

### Backend Technologies
- **Framework**: Spring Boot 3.3.4
- **Java Version**: Java 17
- **Database**: PostgreSQL
- **ORM**: JPA/Hibernate
- **Security**: Spring Security with JWT
- **Documentation**: OpenAPI 3 (Swagger)
- **Rate Limiting**: Bucket4j
- **Build Tool**: Maven

### Key Dependencies
```xml
- Spring Boot Starter Web
- Spring Boot Starter Security
- Spring Boot Starter Data JPA
- PostgreSQL Driver
- JWT (jjwt)
- SpringDoc OpenAPI
- Bucket4j (Rate Limiting)
- AWS SDK (Secrets Manager)
- Apache Commons CSV
```

---

## Database Schema

### Core Entities

#### 1. User Management
- **User**: System users with roles and lab associations
- **Role**: User roles (ADMIN, SUPERADMIN, TECHNICIAN, DESKROLE)
- **ModuleEntity**: Feature modules for role-based access

#### 2. Laboratory Management
- **Lab**: Laboratory information with comprehensive details
- **Doctors**: Doctor profiles associated with labs
- **InsuranceEntity**: Insurance providers

#### 3. Patient Management
- **PatientEntity**: Patient information and demographics
- **VisitEntity**: Patient visits with test associations
- **SampleEntity**: Sample collection and management

#### 4. Test Management
- **Test**: Laboratory tests with pricing
- **TestReferenceEntity**: Test reference ranges
- **HealthPackage**: Test packages and bundles
- **TestDiscountEntity**: Test-specific discounts

#### 5. Billing System
- **BillingEntity**: Billing information with GST calculations
- **TransactionEntity**: Payment transactions
- **VisitTestResult**: Test results and reporting

### Entity Relationships
```
Lab (1) ── (M) User (Lab Members)
Lab (1) ── (M) PatientEntity (Lab Patients)
Lab (1) ── (M) Test (Lab Tests)
Lab (1) ── (M) HealthPackage (Lab Packages)

PatientEntity (1) ── (M) VisitEntity (Patient Visits)
VisitEntity (1) ── (1) BillingEntity (Visit Billing)
VisitEntity (M) ── (M) Test (Visit Tests)
VisitEntity (M) ── (M) SampleEntity (Visit Samples)

BillingEntity (1) ── (M) TransactionEntity (Payment Transactions)
BillingEntity (1) ── (M) TestDiscountEntity (Test Discounts)
```

---

## API Documentation

### Base URL
- **Development**: `http://localhost:8080/api/v1`
- **Production**: `https://tiameds.com/api/v1`

### API Documentation Access
- **Swagger UI**: `/api/v1/doc`
- **OpenAPI JSON**: `/api/v1/v3/api-docs`

### Authentication
All API endpoints (except public ones) require JWT authentication:
```
Authorization: Bearer <jwt_token>
```

---

## Security Features

### 1. JWT Authentication
- **Token Expiration**: 10 hours
- **Token Versioning**: Prevents token reuse after logout
- **Secret Key**: Configurable via environment variables

### 2. Role-Based Access Control (RBAC)
- **SUPERADMIN**: Full system access
- **ADMIN**: Lab management and user administration
- **TECHNICIAN**: Test management and result entry
- **DESKROLE**: Patient management and billing

### 3. Rate Limiting
- **IP-based**: 10 attempts per 10 minutes
- **User-based**: 5 attempts per 10 minutes
- **Scope**: Login and authentication endpoints

### 4. IP Whitelisting
- **Configurable**: Can be enabled/disabled
- **Management**: Dynamic IP addition/removal via API
- **Scope**: Login endpoints only

### 5. Security Filters
- **JwtFilter**: Token validation and user authentication
- **RateLimitFilter**: Request rate limiting
- **IpWhitelistFilter**: IP-based access control

---

## CORS Configuration

### Allowed Origins
```java
- https://lab-test-env.tiameds.ai
- http://localhost:3000
```

### Allowed Methods
- GET, POST, PUT, DELETE, OPTIONS, PATCH

### Allowed Headers
- Authorization, Content-Type, Accept
- X-Requested-With, X-Forwarded-For, X-Real-IP
- Origin, Access-Control-Request-Method, Access-Control-Request-Headers

### Security Features
- **Credentials**: Enabled for authenticated requests
- **Max Age**: 3600 seconds (1 hour)
- **Exposed Headers**: Content-Disposition, Access-Control-Allow-Origin

---

## User Roles and Permissions

### SUPERADMIN
- Full system access
- Lab creation and management
- User management across all labs
- System configuration
- Security management

### ADMIN
- Lab-specific management
- User management within assigned labs
- Test and package management
- Billing oversight
- Report generation

### TECHNICIAN
- Test result entry and management
- Sample collection and processing
- Visit completion
- Test reference management

### DESKROLE
- Patient registration and management
- Visit scheduling
- Basic billing operations
- Patient search and lookup

---

## Core Features

### 1. Patient Management
- **Patient Registration**: Complete demographic information
- **Patient Search**: Phone-based search functionality
- **Patient Updates**: Comprehensive patient information updates
- **Guardian Association**: Support for guardian relationships

### 2. Visit Management
- **Visit Creation**: New patient visits with test assignments
- **Visit Updates**: Modify visit details and test assignments
- **Visit Cancellation**: Cancellation with reason tracking
- **Date-wise Filtering**: Visit filtering by date ranges

### 3. Test Management
- **Test Catalog**: Comprehensive test database
- **Test Categories**: Organized test categorization
- **Pricing Management**: Dynamic pricing with discounts
- **CSV Import/Export**: Bulk test management
- **Test References**: Reference ranges and normal values

### 4. Billing System
- **GST Calculations**: Automatic GST, CGST, SGST, IGST calculations
- **Discount Management**: Test-specific and general discounts
- **Payment Tracking**: Multiple payment methods support
- **Partial Payments**: Support for installment payments
- **Transaction History**: Complete payment audit trail

### 5. Report Generation
- **Test Results**: Automated test result reports
- **Visit Reports**: Comprehensive visit summaries
- **Billing Reports**: Financial reports and analytics
- **Lab Statistics**: Performance metrics and KPIs

### 6. Sample Management
- **Sample Collection**: Sample tracking and management
- **Sample Association**: Link samples to visits and tests
- **Sample Status**: Track sample processing status

---

## Configuration Management

### Environment Profiles
- **Development**: `application-dev.yml`
- **Production**: `application-prod.yml`
- **Test**: `application-test.yml`

### Key Configuration Properties

#### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_URL}:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 40
      minimum-idle: 0
      max-lifetime: 900000
```

#### Security Configuration
```yaml
spring:
  jwt:
    secret: ${JWT_SECRET}

rate:
  limit:
    login:
      attempts: 5
      window: 10
    ip:
      attempts: 10
      window: 10

security:
  ip:
    whitelist:
      enabled: false
      ips: ""
```

#### CORS Configuration
```yaml
cors:
  allowed-origins:
    - https://lab-test-env.tiameds.ai
    - http://localhost:3000
  allowed-methods:
    - GET, POST, PUT, DELETE, OPTIONS, PATCH
  allow-credentials: true
  max-age: 3600
```

---

## Deployment Guide

### Prerequisites
- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher

### Environment Variables
```bash
# Database Configuration
DB_URL=your-database-host
DB_NAME=your-database-name
DB_USERNAME=your-database-username
DB_PASSWORD=your-database-password

# Security
JWT_SECRET=your-jwt-secret-key

# Optional: AWS Secrets Manager
AWS_REGION=your-aws-region
```

### Build and Run
```bash
# Build the application
mvn clean package

# Run with specific profile
java -jar target/app.jar --spring.profiles.active=prod

# Docker deployment
docker build -t tiameds-lab-automation .
docker run -p 8080:8080 tiameds-lab-automation
```

### Database Setup
1. Create PostgreSQL database
2. Run database migrations (if any)
3. Configure connection properties
4. Initialize default roles and modules

---

## API Endpoints Reference

### Authentication Endpoints
```
POST /api/v1/auth/login
POST /api/v1/auth/register
POST /api/v1/auth/logout
```

### Patient Management
```
GET    /api/v1/lab/{labId}/patients
GET    /api/v1/lab/{labId}/patient/{patientId}
POST   /api/v1/lab/{labId}/add-patient
PUT    /api/v1/lab/{labId}/update-patient/{patientId}
DELETE /api/v1/lab/{labId}/delete-patient/{patientId}
GET    /api/v1/lab/{labId}/search-patient?phone={phone}
```

### Visit Management
```
POST   /api/v1/lab/{labId}/add-visit/{patientId}
GET    /api/v1/lab/{labId}/visits
PUT    /api/v1/lab/{labId}/update-visit/{visitId}
DELETE /api/v1/lab/{labId}/delete-visit/{visitId}
GET    /api/v1/lab/{labId}/visit/{visitId}
GET    /api/v1/lab/{labId}/visitsdatewise
```

### Test Management
```
GET    /api/v1/admin/lab/{labId}/tests
POST   /api/v1/admin/lab/{labId}/add
PUT    /api/v1/admin/lab/{labId}/update/{testId}
DELETE /api/v1/admin/lab/{labId}/remove/{testId}
GET    /api/v1/admin/lab/{labId}/test/{testId}
POST   /api/v1/admin/lab/test/{labId}/csv/upload
GET    /api/v1/admin/lab/{labId}/download
```

### Billing Management
```
POST   /api/v1/lab/{labId}/billing/{billingId}/partial-payment
GET    /api/v1/lab/{labId}/report/{visitId}
```

### Security Management
```
GET    /api/v1/admin/security/ip-whitelist/status
POST   /api/v1/admin/security/ip-whitelist/enable
POST   /api/v1/admin/security/ip-whitelist/disable
POST   /api/v1/admin/security/ip-whitelist/add
DELETE /api/v1/admin/security/ip-whitelist/remove
POST   /api/v1/admin/security/rate-limit/reset
GET    /api/v1/admin/security/rate-limit/status
```

---

## Error Handling

### Standard Error Response Format
```json
{
  "success": false,
  "message": "Error description",
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400
}
```

### Common HTTP Status Codes
- **200**: Success
- **201**: Created
- **400**: Bad Request
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found
- **429**: Too Many Requests
- **500**: Internal Server Error

### Error Categories
1. **Authentication Errors**: Invalid or expired tokens
2. **Authorization Errors**: Insufficient permissions
3. **Validation Errors**: Invalid input data
4. **Business Logic Errors**: Domain-specific constraints
5. **System Errors**: Database or infrastructure issues

---

## Best Practices

### Security Best Practices
1. **Token Management**: Implement proper token refresh mechanisms
2. **Input Validation**: Validate all input data at controller and service levels
3. **SQL Injection Prevention**: Use parameterized queries
4. **Rate Limiting**: Monitor and adjust rate limits based on usage patterns
5. **Audit Logging**: Log all critical operations for security auditing

### Performance Best Practices
1. **Database Optimization**: Use appropriate indexes and query optimization
2. **Caching**: Implement caching for frequently accessed data
3. **Connection Pooling**: Configure appropriate connection pool settings
4. **Lazy Loading**: Use lazy loading for entity relationships
5. **Batch Operations**: Use batch operations for bulk data processing

### Code Quality Best Practices
1. **Exception Handling**: Implement comprehensive exception handling
2. **Logging**: Use structured logging with appropriate levels
3. **Documentation**: Maintain up-to-date API documentation
4. **Testing**: Implement unit and integration tests
5. **Code Review**: Regular code reviews for quality assurance

### Operational Best Practices
1. **Monitoring**: Implement application monitoring and alerting
2. **Backup**: Regular database backups and disaster recovery planning
3. **Scaling**: Design for horizontal scaling
4. **Maintenance**: Regular security updates and dependency management
5. **Documentation**: Keep documentation updated with system changes

---

## Conclusion

The Tiameds Lab Automation System provides a comprehensive solution for laboratory management with enterprise-grade security, scalability, and maintainability. The system's modular architecture, role-based access control, and extensive API coverage make it suitable for various laboratory environments and use cases.

For additional support or questions, please refer to the API documentation at `/api/v1/doc` or contact the development team.

---

*Document Version: 1.0*  
*Last Updated: January 2024*  
*Maintained by: Tiameds Development Team*
