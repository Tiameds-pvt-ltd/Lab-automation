# Billing Test Scenario

## Test Case: Refund Given, Then New Test Added

### Initial Setup
1. Patient takes 2 tests costing 300 each = 600 total
2. Patient pays 1000
3. System calculates refund: 1000 - 600 = 400
4. Refund transaction is created with refundAmount = 400
5. Billing status: PAID, dueAmount = 0

### State After Initial Payment
```
BillingEntity:
- totalAmount: 600
- netAmount: 600
- receivedAmount: 1000
- dueAmount: 0
- paymentStatus: "PAID"

Transactions:
- Transaction 1: receivedAmount = 1000, refundAmount = 0
- Transaction 2: receivedAmount = 0, refundAmount = 400
```

### Adding New Test
1. New test costing 400 is added
2. New total: 600 + 400 = 1000
3. New net amount: 1000

### Expected Calculation in `recalculateBillingForTestModifications()`
```java
// Get current received amount (total payments made)
BigDecimal currentReceivedAmount = 1000; // from billing.getReceivedAmount()

// Get total refunded amount from existing transactions
BigDecimal totalRefunded = 400; // from Transaction 2

// Calculate effective received amount (payments minus refunds)
BigDecimal effectiveReceivedAmount = 1000 - 400 = 600;

// Calculate new due amount
BigDecimal newDueAmount = 1000 - 600 = 400;

// Determine payment status
String newPaymentStatus = "PARTIALLY_PAID"; // because effectiveReceivedAmount > 0 but newDueAmount > 0
```

### Expected Final State
```
BillingEntity:
- totalAmount: 1000
- netAmount: 1000
- receivedAmount: 1000 (unchanged)
- dueAmount: 400 (NEW - patient owes this)
- paymentStatus: "PARTIALLY_PAID"

Transactions:
- Transaction 1: receivedAmount = 1000, refundAmount = 0, dueAmount = 400 (updated)
- Transaction 2: receivedAmount = 0, refundAmount = 400, dueAmount = 400 (updated)
```

## Key Points
1. **No new refund is created** - the 400 refund was already given
2. **Due amount is correctly calculated** - 400 (new test cost)
3. **Payment status is updated** - PARTIALLY_PAID (paid 600, owes 400)
4. **Transaction consistency** - all transactions show current due amount

## Verification
The fix ensures that when a patient has received a refund and then new tests are added, the system correctly:
- Calculates the new due amount based on effective received amount (payments minus refunds)
- Updates payment status appropriately
- Maintains transaction history without creating duplicate refunds
- Preserves data integrity across all related entities
