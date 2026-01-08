# API Documentation: Datewise Transaction Details

## Endpoint Overview

**Endpoint:** `GET /lab/statistics/{labId}/datewise-transactionsdetails`

**Description:** Retrieves paginated transaction details for a specific lab within an optional date range. Returns all billings (both with and without transactions) including patient information, visit details, billing information, and associated transactions.

**Base URL:** `/lab/statistics`

---

## Authentication

This endpoint requires authentication. Include a valid JWT token in the request headers:

```
Authorization: Bearer <your-jwt-token>
```

**Required Roles:** User must have access to the specified lab.

---

## Request Parameters

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `labId` | Long | Yes | The unique identifier of the lab |

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `startDate` | String (ISO Date) | No | `null` | Start date for filtering (format: `YYYY-MM-DD`). If not provided, searches from the earliest date. |
| `endDate` | String (ISO Date) | No | `null` | End date for filtering (format: `YYYY-MM-DD`). If not provided, searches until the latest date. |
| `page` | Integer | No | `0` | Page number (0-indexed). Minimum value: 0 |
| `size` | Integer | No | `50` | Number of items per page. Range: 10-200 (automatically sanitized) |

### Request Example

```http
GET /lab/statistics/1/datewise-transactionsdetails?startDate=2024-01-01&endDate=2024-01-31&page=0&size=50
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Response Structure

### Success Response (200 OK)

#### Response Headers

| Header | Type | Description |
|--------|------|-------------|
| `X-Total-Elements` | String | Total number of billings matching the criteria |
| `X-Total-Pages` | String | Total number of pages |
| `X-Page-Number` | String | Current page number (0-indexed) |
| `X-Page-Size` | String | Number of items per page |

#### Response Body

```json
{
  "status": "success",
  "message": "Transaction details fetched successfully",
  "data": [
    {
      "id": 123,
      "firstName": "John",
      "phone": "9876543210",
      "city": "Mumbai",
      "dateOfBirth": "1990-05-15",
      "age": "34",
      "gender": "M",
      "createdBy": "admin",
      "updatedBy": "admin",
      "visit": {
        "visitId": 456,
        "visitDate": "2024-01-15",
        "visitType": "Walk-in",
        "visitStatus": "Completed",
        "visitDescription": "Regular checkup",
        "doctorId": 10,
        "DoctorName": "Dr. Smith",
        "testNames": ["CBC", "Blood Sugar"],
        "testIds": [1, 2],
        "packageIds": [],
        "packageNames": [],
        "createdBy": "admin",
        "updatedBy": "admin",
        "visitCancellationReason": null,
        "visitCancellationDate": null,
        "visitCancellationBy": null,
        "visitCancellationTime": null,
        "billing": {
          "billingId": 789,
          "totalAmount": 1500.00,
          "paymentStatus": "PAID",
          "paymentMethod": "CASH",
          "paymentDate": "2024-01-15",
          "discount": 100.00,
          "netAmount": 1400.00,
          "discountReason": "Senior citizen discount",
          "createdBy": "admin",
          "updatedBy": "admin",
          "billingTime": "14:30:00",
          "billingDate": "2024-01-15",
          "createdAt": "2024-01-15T14:30:00",
          "updatedAt": "2024-01-15T14:30:00",
          "received_amount": 1400.00,
          "due_amount": 0.00,
          "transactions": [
            {
              "id": 101,
              "billing_id": 789,
              "payment_method": "CASH",
              "upi_id": null,
              "upi_amount": 0.0,
              "card_amount": 0.0,
              "cash_amount": 1400.0,
              "received_amount": 1400.0,
              "refund_amount": 0.0,
              "due_amount": 0.0,
              "payment_date": "2024-01-15",
              "remarks": "Full payment received",
              "createdBy": "admin",
              "created_at": "2024-01-15T14:30:00"
            }
          ]
        },
        "testResult": [
          {
            "id": 201,
            "testId": 1,
            "isFilled": true,
            "testName": "CBC",
            "category": "Hematology",
            "reportStatus": "Completed",
            "createdBy": "admin",
            "updatedBy": "admin",
            "createdAt": "2024-01-15T14:30:00",
            "updatedAt": "2024-01-15T16:00:00"
          }
        ],
        "listofeachtestdiscount": [
          {
            "id": 301,
            "discountAmount": 50.00,
            "discountPercent": 5.0,
            "finalPrice": 950.00,
            "testName": "CBC",
            "category": "Hematology",
            "createdBy": "admin",
            "updatedBy": "admin"
          }
        ]
      }
    }
  ]
}
```

### Response for Billing Without Transactions

When a billing exists but has no associated transactions (e.g., unpaid billings), the `transactions` array will be empty:

```json
{
  "status": "success",
  "message": "Transaction details fetched successfully",
  "data": [
    {
      "id": 124,
      "firstName": "Jane",
      "phone": "9876543211",
      "city": "Delhi",
      "visit": {
        "billing": {
          "billingId": 790,
          "totalAmount": 2000.00,
          "paymentStatus": "PENDING",
          "discount": 0.00,
          "netAmount": 2000.00,
          "received_amount": 0.00,
          "due_amount": 2000.00,
          "transactions": []
        }
      }
    }
  ]
}
```

### Error Responses

#### 401 Unauthorized - User Not Found

```json
{
  "status": "success",
  "message": "User not found",
  "data": null
}
```

#### 401 Unauthorized - Lab Not Accessible

```json
{
  "status": "success",
  "message": "Lab is not accessible",
  "data": null
}
```

#### 200 OK - No Transactions Found

```json
{
  "status": "success",
  "message": "No transactions found",
  "data": []
}
```

---

## Response Field Descriptions

### Root Level

| Field | Type | Description |
|-------|------|-------------|
| `status` | String | Response status (always "success") |
| `message` | String | Human-readable message |
| `data` | Array | Array of transaction detail objects |

### Patient Information (Root of each data item)

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Patient ID |
| `firstName` | String | Patient's first name |
| `phone` | String | Patient's phone number |
| `city` | String | Patient's city |
| `dateOfBirth` | String | Patient's date of birth (ISO format) |
| `age` | String | Patient's age |
| `gender` | String | Patient's gender (M/F) |
| `createdBy` | String | User who created the patient record |
| `updatedBy` | String | User who last updated the patient record |
| `visit` | Object | Visit details (see VisitDTO below) |

### VisitDTO

| Field | Type | Description |
|-------|------|-------------|
| `visitId` | Integer | Unique visit identifier |
| `visitDate` | String | Date of the visit (ISO format) |
| `visitType` | String | Type of visit (e.g., "Walk-in", "Appointment") |
| `visitStatus` | String | Status of the visit (e.g., "Completed", "Pending", "Cancelled") |
| `visitDescription` | String | Description of the visit |
| `doctorId` | Integer | ID of the associated doctor (nullable) |
| `DoctorName` | String | Name of the associated doctor (nullable) |
| `testNames` | Array[String] | List of test names ordered for this visit |
| `testIds` | Array[Long] | List of test IDs ordered for this visit |
| `packageIds` | Array[Long] | List of health package IDs ordered |
| `packageNames` | Array[Object] | List of health package names ordered |
| `createdBy` | String | User who created the visit |
| `updatedBy` | String | User who last updated the visit |
| `visitCancellationReason` | String | Reason for cancellation (if cancelled) |
| `visitCancellationDate` | String | Date of cancellation (if cancelled) |
| `visitCancellationBy` | String | User who cancelled the visit (if cancelled) |
| `visitCancellationTime` | String | Time of cancellation (if cancelled) |
| `billing` | Object | Billing information (see BillingDTO below) |
| `testResult` | Array | List of test results (see TestResultDTO below) |
| `listofeachtestdiscount` | Array | List of test-level discounts (see TestDiscountDTO below) |

### BillingDTO

| Field | Type | Description |
|-------|------|-------------|
| `billingId` | Integer | Unique billing identifier |
| `totalAmount` | Double | Total amount before discount and tax |
| `paymentStatus` | String | Payment status (e.g., "PAID", "PENDING", "PARTIAL") |
| `paymentMethod` | String | Payment method (e.g., "CASH", "CARD", "UPI") |
| `paymentDate` | String | Date of payment (ISO format) |
| `discount` | Double | Discount amount applied |
| `netAmount` | Double | Final amount after discount and tax |
| `discountReason` | String | Reason for discount (nullable) |
| `createdBy` | String | User who created the billing |
| `updatedBy` | String | User who last updated the billing |
| `billingTime` | String | Time when billing was created (HH:mm:ss format) |
| `billingDate` | String | Date when billing was created (YYYY-MM-DD format) |
| `createdAt` | String | Timestamp when billing was created (ISO format) |
| `updatedAt` | String | Timestamp when billing was last updated (ISO format) |
| `received_amount` | Double | Total amount received (sum of all transactions or billing's own value) |
| `due_amount` | Double | Total amount due (sum of all transactions or billing's own value) |
| `transactions` | Array | List of transactions (see TransactionDTO below). **Empty array if no transactions exist.** |

### TransactionDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Unique transaction identifier |
| `billing_id` | Integer | ID of the associated billing |
| `payment_method` | String | Payment method used (e.g., "CASH", "CARD", "UPI") |
| `upi_id` | String | UPI ID if payment method is UPI (nullable) |
| `upi_amount` | Double | Amount paid via UPI |
| `card_amount` | Double | Amount paid via card |
| `cash_amount` | Double | Amount paid via cash |
| `received_amount` | Double | Total amount received in this transaction |
| `refund_amount` | Double | Refund amount (if any) |
| `due_amount` | Double | Due amount after this transaction |
| `payment_date` | String | Date of payment (ISO format) |
| `remarks` | String | Additional remarks (nullable) |
| `createdBy` | String | User who created the transaction |
| `created_at` | String | Timestamp when transaction was created (ISO format) |

### TestResultDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Unique test result identifier |
| `testId` | Integer | ID of the associated test |
| `isFilled` | Boolean | Whether the test result has been filled |
| `testName` | String | Name of the test |
| `category` | String | Category of the test (e.g., "Hematology", "Biochemistry") |
| `reportStatus` | String | Status of the report (e.g., "Completed", "Pending") |
| `createdBy` | String | User who created the test result |
| `updatedBy` | String | User who last updated the test result |
| `createdAt` | String | Timestamp when test result was created (ISO format) |
| `updatedAt` | String | Timestamp when test result was last updated (ISO format) |

### TestDiscountDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Unique test discount identifier |
| `discountAmount` | Double | Discount amount applied to the test |
| `discountPercent` | Double | Discount percentage applied |
| `finalPrice` | Double | Final price after discount |
| `testName` | String | Name of the test |
| `category` | String | Category of the test |
| `createdBy` | String | User who created the discount |
| `updatedBy` | String | User who last updated the discount |

---

## Important Notes

### 1. **Billings Without Transactions**
   - The API returns **all billings** for the specified lab and date range, including those without transactions
   - Billings without transactions will have an **empty `transactions` array** (`[]`)
   - This allows visibility into unpaid or pending billings

### 2. **Pagination**
   - Results are sorted by billing creation date in descending order (newest first)
   - Page numbers are 0-indexed (first page is 0)
   - Page size is automatically sanitized:
     - Minimum: 10 items per page
     - Maximum: 200 items per page
     - Default: 50 items per page

### 3. **Date Filtering**
   - If `startDate` is not provided, searches from the earliest date
   - If `endDate` is not provided, searches until the latest date
   - Date format must be ISO 8601: `YYYY-MM-DD`
   - The date range is inclusive

### 4. **Received Amount and Due Amount Calculation**
   - If billing has transactions: `received_amount` and `due_amount` are calculated by summing all transactions
   - If billing has no transactions: Uses the billing entity's own `receivedAmount` and `dueAmount` fields
   - If billing has no transactions and no values in entity: Both default to `0.0`

### 5. **Response Structure Consistency**
   - The response structure remains the same whether billing has transactions or not
   - Frontend can check `transactions.length === 0` to identify unpaid billings
   - No breaking changes to existing frontend implementations

---

## Example Requests

### Example 1: Get all transactions for a lab (no date filter)

```http
GET /lab/statistics/1/datewise-transactionsdetails
Authorization: Bearer <token>
```

### Example 2: Get transactions for a specific date range

```http
GET /lab/statistics/1/datewise-transactionsdetails?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer <token>
```

### Example 3: Get transactions with pagination

```http
GET /lab/statistics/1/datewise-transactionsdetails?startDate=2024-01-01&endDate=2024-01-31&page=0&size=20
Authorization: Bearer <token>
```

### Example 4: Get second page of results

```http
GET /lab/statistics/1/datewise-transactionsdetails?page=1&size=50
Authorization: Bearer <token>
```

---

## Pagination Headers Example

```
X-Total-Elements: 150
X-Total-Pages: 3
X-Page-Number: 0
X-Page-Size: 50
```

This indicates:
- Total of 150 billings match the criteria
- 3 pages total (150 ÷ 50 = 3)
- Currently on page 0 (first page)
- 50 items per page

---

## Error Handling

### Common Error Scenarios

1. **Invalid Lab ID**: Returns 401 if user doesn't have access to the lab
2. **Invalid Date Format**: Spring will return 400 Bad Request if date format is incorrect
3. **No Data**: Returns 200 OK with empty `data` array
4. **Unauthorized**: Returns 401 if user is not authenticated or doesn't have access

### Best Practices

- Always check the `status` field in the response
- Check pagination headers to determine if more pages are available
- Handle empty `transactions` array for unpaid billings
- Validate date formats before sending requests
- Implement proper error handling for 401 responses

---

## Implementation Details

### Database Query
- Queries `BillingEntity` directly (not `TransactionEntity`)
- Joins with `Lab`, `Visit`, and `Patient` entities
- Filters by lab ID and billing creation date range
- Results sorted by billing creation date (DESC)

### Performance Considerations
- Uses pagination to limit result set size
- Lazy loading for related entities
- Efficient JOIN queries to minimize database round trips

### Transaction Handling
- All billings are returned, regardless of transaction status
- Transactions are included when available
- Empty transaction arrays for billings without payments

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-12-01 | Initial implementation - Query from BillingEntity to include all billings (with and without transactions) |

---

## Support

For issues or questions regarding this API endpoint, please contact the development team.

