//package tiameds.com.tiameds.services.lab;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import tiameds.com.tiameds.entity.BillingEntity;
//import tiameds.com.tiameds.entity.TransactionEntity;
//import tiameds.com.tiameds.repository.BillingRepository;
//import tiameds.com.tiameds.repository.TransactionRepository;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.ZoneId;
//
///**
// * Production-ready billing management service that handles all billing operations
// * including payments, refunds, cancellations, and status updates.
// *
// * Business Rules:
// * - A patient books tests → initial bill created with totalAmount
// * - Payments are recorded as TransactionEntity with receivedAmount > 0
// * - If tests are cancelled, the totalAmount in BillingEntity decreases
// * - After recalculation:
// *   * If total paid < new totalAmount → update dueAmount
// *   * If total paid = new totalAmount → dueAmount = 0, paymentStatus = PAID
// *   * If total paid > new totalAmount → create a refund transaction with refundAmount = overpaid
// * - Never overwrite old transactions, always append new ones (immutability is critical)
// * - paymentStatus values: UNPAID, PARTIALLY_PAID, PAID
// */
//@Service
//public class BillingManagementService {
//
//    private static final Logger logger = LoggerFactory.getLogger(BillingManagementService.class);
//
//    // Payment status constants
//    private static final String UNPAID = "UNPAID";
//    private static final String PARTIALLY_PAID = "PARTIALLY_PAID";
//    private static final String PAID = "PAID";
//
//    // Payment method constants
//    private static final String CASH = "CASH";
//    private static final String CARD = "CARD";
//    private static final String UPI = "UPI";
//
//    private final BillingRepository billingRepository;
//    private final TransactionRepository transactionRepository;
//
//    public BillingManagementService(BillingRepository billingRepository, TransactionRepository transactionRepository) {
//        this.billingRepository = billingRepository;
//        this.transactionRepository = transactionRepository;
//    }
//
//    /**
//     * Updates billing after test cancellation with comprehensive business logic
//     *
//     * @param billingId The ID of the billing entity to update
//     * @param newNetAmount The new net amount after test cancellation
//     * @param username The username performing the operation
//     * @return Updated BillingEntity
//     */
//    @Transactional
//    public BillingEntity updateBillingAfterCancellation(Long billingId, BigDecimal newNetAmount, String username) {
//        logger.info("Starting billing update for cancellation - BillingId: {}, NewNetAmount: {}, User: {}",
//                   billingId, newNetAmount, username);
//
//        // Find billing entity with optimistic locking
//        BillingEntity billing = billingRepository.findById(billingId)
//                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
//
//        // Validate input
//        if (newNetAmount == null || newNetAmount.compareTo(BigDecimal.ZERO) < 0) {
//            throw new IllegalArgumentException("New net amount must be non-negative");
//        }
//
//        // Get current values safely (treat null as zero)
//        BigDecimal currentNetAmount = safeGetAmount(billing.getNetAmount());
//        BigDecimal currentReceivedAmount = safeGetAmount(billing.getReceivedAmount());
//        BigDecimal currentActualReceivedAmount = safeGetAmount(billing.getActualReceivedAmount());
//        BigDecimal currentDueAmount = safeGetAmount(billing.getDueAmount());
//
//        logger.info("Current billing state - NetAmount: {}, ReceivedAmount: {}, ActualReceivedAmount: {}, DueAmount: {}",
//                   currentNetAmount, currentReceivedAmount, currentActualReceivedAmount, currentDueAmount);
//
//        // Update net amount
//        billing.setNetAmount(newNetAmount);
//
//        // Recalculate billing based on business rules, considering existing refunds
//        BillingRecalculationResult result = recalculateBillingWithExistingRefunds(billing, currentReceivedAmount, newNetAmount);
//
//        // Calculate actual received amount (total received - total refunded)
//        BigDecimal totalRefundedAmount = BigDecimal.ZERO;
//        if (billing.getTransactions() != null) {
//            totalRefundedAmount = billing.getTransactions().stream()
//                    .map(transaction -> transaction.getRefundAmount() != null ? transaction.getRefundAmount() : BigDecimal.ZERO)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//        }
//        BigDecimal newActualReceivedAmount = currentReceivedAmount.subtract(totalRefundedAmount);
//
//        // Update billing fields with null safety
//        billing.setActualReceivedAmount(safeGetAmount(newActualReceivedAmount));
//        billing.setDueAmount(safeGetAmount(result.getNewDueAmount()));
//        billing.setPaymentStatus(result.getNewPaymentStatus() != null ? result.getNewPaymentStatus() : "UNPAID");
//        billing.setUpdatedBy(username != null ? username : "SYSTEM");
//
//        // Ensure billing time and date are set
//        if (billing.getBillingTime() == null) {
//            billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
//        }
//        if (billing.getBillingDate() == null) {
//            billing.setBillingDate(LocalDate.now().toString());
//        }
//
//        // Save billing entity
//        billing = billingRepository.save(billing);
//        logger.info("Updated billing - NewDueAmount: {}, NewPaymentStatus: {}",
//                   result.getNewDueAmount(), result.getNewPaymentStatus());
//
//        // Create refund transaction if needed
//        if (result.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
//            createRefundTransaction(billing, result.getRefundAmount(), username);
//        }
//
//        // Update due amounts in existing transactions to maintain data consistency
//        updateDueAmountsInTransactions(billing, result.getNewDueAmount(), username);
//
//        logger.info("Billing update completed successfully for BillingId: {}", billingId);
//        return billing;
//    }
//
//    /**
//     * Adds a payment to existing billing
//     *
//     * @param billingId The ID of the billing entity
//     * @param paymentAmount The payment amount
//     * @param paymentMethod The payment method (CASH, CARD, UPI)
//     * @param username The username performing the operation
//     * @return Updated BillingEntity
//     */
//    @Transactional
//    public BillingEntity addPayment(Long billingId, BigDecimal paymentAmount, String paymentMethod, String username) {
//        return addPayment(billingId, paymentAmount, paymentMethod, null, null, null, null, username);
//    }
//
//    /**
//     * Adds a payment to existing billing with detailed payment method amounts
//     *
//     * @param billingId The ID of the billing entity
//     * @param paymentAmount The total payment amount
//     * @param paymentMethod The primary payment method (CASH, CARD, UPI)
//     * @param upiId The UPI ID (if applicable)
//     * @param upiAmount The UPI amount (if applicable)
//     * @param cardAmount The card amount (if applicable)
//     * @param cashAmount The cash amount (if applicable)
//     * @param username The username performing the operation
//     * @return Updated BillingEntity
//     */
//    @Transactional
//    public BillingEntity addPayment(Long billingId, BigDecimal paymentAmount, String paymentMethod,
//                                   String upiId, BigDecimal upiAmount, BigDecimal cardAmount,
//                                   BigDecimal cashAmount, String username) {
//        logger.info("Adding payment - BillingId: {}, Amount: {}, Method: {}, User: {}",
//                   billingId, paymentAmount, paymentMethod, username);
//
//        // Validate input
//        validatePaymentInput(paymentAmount, paymentMethod);
//
//        // Find billing entity
//        BillingEntity billing = billingRepository.findById(billingId)
//                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
//
//        // Get current values safely
//        BigDecimal currentReceivedAmount = safeGetAmount(billing.getReceivedAmount());
//        BigDecimal currentActualReceivedAmount = safeGetAmount(billing.getActualReceivedAmount());
//        BigDecimal currentNetAmount = safeGetAmount(billing.getNetAmount());
//
//        // Calculate new received amount
//        BigDecimal newReceivedAmount = currentReceivedAmount.add(paymentAmount);
//
//        // Recalculate billing considering existing refunds
//        BillingRecalculationResult result = recalculateBillingWithExistingRefunds(billing, newReceivedAmount, currentNetAmount);
//
//        // Calculate actual received amount (total received - total refunded)
//        BigDecimal totalRefundedAmount = BigDecimal.ZERO;
//        if (billing.getTransactions() != null) {
//            totalRefundedAmount = billing.getTransactions().stream()
//                    .map(transaction -> transaction.getRefundAmount() != null ? transaction.getRefundAmount() : BigDecimal.ZERO)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//        }
//        BigDecimal newActualReceivedAmount = newReceivedAmount.subtract(totalRefundedAmount);
//
//        // Update billing with null safety
//        billing.setReceivedAmount(safeGetAmount(newReceivedAmount));
//        billing.setActualReceivedAmount(safeGetAmount(newActualReceivedAmount));
//        billing.setDueAmount(safeGetAmount(result.getNewDueAmount()));
//        billing.setPaymentStatus(result.getNewPaymentStatus() != null ? result.getNewPaymentStatus() : "UNPAID");
//        billing.setUpdatedBy(username != null ? username : "SYSTEM");
//
//        // Ensure billing time and date are set
//        if (billing.getBillingTime() == null) {
//            billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
//        }
//        if (billing.getBillingDate() == null) {
//            billing.setBillingDate(LocalDate.now().toString());
//        }
//
//        // Save billing
//        billing = billingRepository.save(billing);
//
//        // Create payment transaction with detailed amounts
//        createPaymentTransactionWithDetails(billing, paymentAmount, paymentMethod, upiId, upiAmount, cardAmount, cashAmount, username);
//
//        // Create refund transaction if needed (overpayment scenario)
//        if (result.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
//            createRefundTransaction(billing, result.getRefundAmount(), username);
//        }
//
//        // Update due amounts in existing transactions to maintain data consistency
//        updateDueAmountsInTransactions(billing, result.getNewDueAmount(), username);
//
//        logger.info("Payment added successfully - NewReceivedAmount: {}, NewDueAmount: {}, NewStatus: {}, RefundAmount: {}",
//                   newReceivedAmount, result.getNewDueAmount(), result.getNewPaymentStatus(), result.getRefundAmount());
//
//        return billing;
//    }
//
//    /**
//     * Recalculates billing based on business rules, considering existing refunds
//     */
//    private BillingRecalculationResult recalculateBillingWithExistingRefunds(BillingEntity billing, BigDecimal receivedAmount, BigDecimal netAmount) {
//        // Calculate total refunded amount from existing transactions
//        BigDecimal totalRefundedAmount = BigDecimal.ZERO;
//        if (billing.getTransactions() != null) {
//            totalRefundedAmount = billing.getTransactions().stream()
//                    .map(transaction -> transaction.getRefundAmount() != null ? transaction.getRefundAmount() : BigDecimal.ZERO)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//        }
//
//        // Calculate ARA = sum of payments - sum of refunds
//        BigDecimal actualReceivedAmount = receivedAmount.subtract(totalRefundedAmount);
//
//        // Cap ARA to not exceed Total Amount (as per specification)
//        if (actualReceivedAmount.compareTo(netAmount) > 0) {
//            actualReceivedAmount = netAmount;
//        }
//
//        // Calculate due amount: Due = Total Amount - ARA
//        BigDecimal newDueAmount = netAmount.subtract(actualReceivedAmount);
//        BigDecimal refundAmount = BigDecimal.ZERO;
//        String newPaymentStatus;
//
//        logger.debug("Recalculating billing - ReceivedAmount: {}, TotalRefunded: {}, ARA: {}, NetAmount: {}, CalculatedDueAmount: {}",
//                   receivedAmount, totalRefundedAmount, actualReceivedAmount, netAmount, newDueAmount);
//
//        if (newDueAmount.compareTo(BigDecimal.ZERO) > 0) {
//            // Still owe money
//            newPaymentStatus = actualReceivedAmount.compareTo(BigDecimal.ZERO) > 0 ? PARTIALLY_PAID : UNPAID;
//            logger.debug("Still owe money - DueAmount: {}, Status: {}", newDueAmount, newPaymentStatus);
//        } else if (newDueAmount.compareTo(BigDecimal.ZERO) == 0) {
//            // Paid in full
//            newPaymentStatus = PAID;
//            logger.debug("Paid in full - Status: {}", newPaymentStatus);
//        } else {
//            // Overpaid - need refund (this shouldn't happen with proper ARA capping, but handle it)
//            refundAmount = newDueAmount.abs(); // Convert negative to positive
//            newDueAmount = BigDecimal.ZERO;
//            newPaymentStatus = PAID;
//            logger.debug("Overpaid - RefundAmount: {}, DueAmount: {}, Status: {}",
//                       refundAmount, newDueAmount, newPaymentStatus);
//        }
//
//        return new BillingRecalculationResult(newDueAmount, newPaymentStatus, refundAmount);
//    }
//
//    /**
//     * Recalculates billing based on business rules
//     */
//    private BillingRecalculationResult recalculateBilling(BigDecimal receivedAmount, BigDecimal netAmount) {
//        BigDecimal newDueAmount = netAmount.subtract(receivedAmount);
//        BigDecimal refundAmount = BigDecimal.ZERO;
//        String newPaymentStatus;
//
//        logger.debug("Recalculating billing - ReceivedAmount: {}, NetAmount: {}, CalculatedDueAmount: {}",
//                   receivedAmount, netAmount, newDueAmount);
//
//        if (newDueAmount.compareTo(BigDecimal.ZERO) > 0) {
//            // Still owe money
//            newPaymentStatus = receivedAmount.compareTo(BigDecimal.ZERO) > 0 ? PARTIALLY_PAID : UNPAID;
//            logger.debug("Still owe money - DueAmount: {}, Status: {}", newDueAmount, newPaymentStatus);
//        } else if (newDueAmount.compareTo(BigDecimal.ZERO) == 0) {
//            // Paid in full
//            newPaymentStatus = PAID;
//            logger.debug("Paid in full - Status: {}", newPaymentStatus);
//        } else {
//            // Overpaid - need refund
//            refundAmount = newDueAmount.abs(); // Convert negative to positive
//            newDueAmount = BigDecimal.ZERO;
//            newPaymentStatus = PAID;
//            logger.debug("Overpaid - RefundAmount: {}, DueAmount: {}, Status: {}",
//                       refundAmount, newDueAmount, newPaymentStatus);
//        }
//
//        return new BillingRecalculationResult(newDueAmount, newPaymentStatus, refundAmount);
//    }
//
//    /**
//     * Creates a refund transaction
//     */
//    private void createRefundTransaction(BillingEntity billing, BigDecimal refundAmount, String username) {
//        logger.warn("Creating refund transaction - BillingId: {}, RefundAmount: {}, User: {}",
//                   billing.getId(), refundAmount, username);
//
//        TransactionEntity refundTransaction = new TransactionEntity();
//        refundTransaction.setBilling(billing);
//        refundTransaction.setPaymentMethod("REFUND");
//        refundTransaction.setRefundAmount(safeGetAmount(refundAmount));
//        refundTransaction.setReceivedAmount(BigDecimal.ZERO);
//        refundTransaction.setDueAmount(BigDecimal.ZERO);
//        refundTransaction.setPaymentDate(LocalDate.now().toString());
//        refundTransaction.setRemarks("Refund for overpayment");
//        refundTransaction.setCreatedBy(username != null ? username : "SYSTEM");
//        refundTransaction.setCreatedAt(LocalDateTime.now());
//
//        // Set payment method amounts to zero for refund
//        refundTransaction.setUpiId("");
//        refundTransaction.setUpiAmount(BigDecimal.ZERO);
//        refundTransaction.setCardAmount(BigDecimal.ZERO);
//        refundTransaction.setCashAmount(BigDecimal.ZERO);
//
//        // Add to billing's transaction list (immutability - append only)
//        billing.getTransactions().add(refundTransaction);
//
//        // Save transaction
//        transactionRepository.save(refundTransaction);
//
//        logger.info("Refund transaction created successfully - TransactionId: {}, RefundAmount: {}",
//                   refundTransaction.getId(), refundAmount);
//    }
//
//    /**
//     * Creates a payment transaction with detailed payment method amounts
//     */
//    private void createPaymentTransactionWithDetails(BillingEntity billing, BigDecimal paymentAmount, String paymentMethod,
//                                                   String upiId, BigDecimal upiAmount, BigDecimal cardAmount,
//                                                   BigDecimal cashAmount, String username) {
//        logger.info("Creating payment transaction with details - BillingId: {}, Amount: {}, Method: {}, User: {}",
//                   billing.getId(), paymentAmount, paymentMethod, username);
//
//        TransactionEntity paymentTransaction = new TransactionEntity();
//        paymentTransaction.setBilling(billing);
//        paymentTransaction.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
//        paymentTransaction.setReceivedAmount(safeGetAmount(paymentAmount));
//        paymentTransaction.setRefundAmount(BigDecimal.ZERO);
//        paymentTransaction.setDueAmount(safeGetAmount(billing.getDueAmount()));
//        paymentTransaction.setPaymentDate(LocalDate.now().toString());
//        paymentTransaction.setRemarks("Payment via " + (paymentMethod != null ? paymentMethod : "CASH"));
//        paymentTransaction.setCreatedBy(username != null ? username : "SYSTEM");
//        paymentTransaction.setCreatedAt(LocalDateTime.now());
//
//        // Set detailed payment method amounts with null safety
//        paymentTransaction.setUpiId(upiId != null ? upiId : "");
//        paymentTransaction.setUpiAmount(safeGetAmount(upiAmount));
//        paymentTransaction.setCardAmount(safeGetAmount(cardAmount));
//        paymentTransaction.setCashAmount(safeGetAmount(cashAmount));
//
//        // Add to billing's transaction list (immutability - append only)
//        billing.getTransactions().add(paymentTransaction);
//
//        // Save transaction
//        transactionRepository.save(paymentTransaction);
//
//        logger.info("Payment transaction created successfully - TransactionId: {}, Amount: {}",
//                   paymentTransaction.getId(), paymentAmount);
//    }
//
//    /**
//     * Creates a payment transaction
//     */
//    private void createPaymentTransaction(BillingEntity billing, BigDecimal paymentAmount, String paymentMethod, String username) {
//        logger.info("Creating payment transaction - BillingId: {}, Amount: {}, Method: {}, User: {}",
//                   billing.getId(), paymentAmount, paymentMethod, username);
//
//        TransactionEntity paymentTransaction = new TransactionEntity();
//        paymentTransaction.setBilling(billing);
//        paymentTransaction.setPaymentMethod(paymentMethod);
//        paymentTransaction.setReceivedAmount(paymentAmount);
//        paymentTransaction.setRefundAmount(BigDecimal.ZERO);
//        paymentTransaction.setDueAmount(safeGetAmount(billing.getDueAmount()));
//        paymentTransaction.setPaymentDate(LocalDate.now().toString());
//        paymentTransaction.setRemarks("Payment via " + paymentMethod);
//        paymentTransaction.setCreatedBy(username);
//        paymentTransaction.setCreatedAt(LocalDateTime.now());
//
//        // Set payment method specific amounts
//        switch (paymentMethod.toUpperCase()) {
//            case CASH:
//                paymentTransaction.setCashAmount(paymentAmount);
//                paymentTransaction.setCardAmount(BigDecimal.ZERO);
//                paymentTransaction.setUpiAmount(BigDecimal.ZERO);
//                break;
//            case CARD:
//                paymentTransaction.setCashAmount(BigDecimal.ZERO);
//                paymentTransaction.setCardAmount(paymentAmount);
//                paymentTransaction.setUpiAmount(BigDecimal.ZERO);
//                break;
//            case UPI:
//                paymentTransaction.setCashAmount(BigDecimal.ZERO);
//                paymentTransaction.setCardAmount(BigDecimal.ZERO);
//                paymentTransaction.setUpiAmount(paymentAmount);
//                break;
//        }
//
//        // Add to billing's transaction list (immutability - append only)
//        billing.getTransactions().add(paymentTransaction);
//
//        // Save transaction
//        transactionRepository.save(paymentTransaction);
//
//        logger.info("Payment transaction created successfully - TransactionId: {}, Amount: {}",
//                   paymentTransaction.getId(), paymentAmount);
//    }
//
//    /**
//     * Safely gets BigDecimal amount, treating null as zero
//     */
//    private BigDecimal safeGetAmount(BigDecimal amount) {
//        return amount != null ? amount : BigDecimal.ZERO;
//    }
//
//    /**
//     * Validates payment input
//     */
//    private void validatePaymentInput(BigDecimal paymentAmount, String paymentMethod) {
//        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("Payment amount must be positive");
//        }
//
//        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
//            throw new IllegalArgumentException("Payment method is required");
//        }
//
//        String upperMethod = paymentMethod.toUpperCase();
//        if (!upperMethod.equals(CASH) && !upperMethod.equals(CARD) && !upperMethod.equals(UPI)) {
//            throw new IllegalArgumentException("Payment method must be CASH, CARD, or UPI");
//        }
//    }
//
//    /**
//     * Updates due amounts in existing transactions to maintain data consistency
//     * This ensures that all transaction records reflect the current due amount
//     * after billing changes (like test cancellations)
//     */
//    private void updateDueAmountsInTransactions(BillingEntity billing, BigDecimal newDueAmount, String username) {
//        logger.info("Updating due amounts in transactions - BillingId: {}, NewDueAmount: {}, User: {}",
//                   billing.getId(), newDueAmount, username);
//
//        // Update due amounts in all existing transactions for this billing
//        boolean hasUpdates = false;
//        for (TransactionEntity transaction : billing.getTransactions()) {
//            // Only update if the due amount has actually changed
//            BigDecimal currentDueAmount = safeGetAmount(transaction.getDueAmount());
//            if (currentDueAmount.compareTo(newDueAmount) != 0) {
//                transaction.setDueAmount(newDueAmount);
//                hasUpdates = true;
//                logger.debug("Updated transaction {} due amount from {} to {}",
//                           transaction.getId(), currentDueAmount, newDueAmount);
//            }
//        }
//
//        // Save updated transactions if any changes were made
//        if (hasUpdates) {
//            transactionRepository.saveAll(billing.getTransactions());
//            logger.info("Successfully updated due amounts in {} transactions for BillingId: {}",
//                       billing.getTransactions().size(), billing.getId());
//        } else {
//            logger.debug("No transaction due amounts needed updating for BillingId: {}", billing.getId());
//        }
//    }
//
//    /**
//     * Updates due amounts in all transactions for a specific billing
//     * This is a public method that can be called from other services
//     * when billing amounts change outside of the standard flow
//     */
//    @Transactional
//    public void updateDueAmountsInAllTransactions(Long billingId, String username) {
//        logger.info("Updating due amounts in all transactions - BillingId: {}, User: {}", billingId, username);
//
//        BillingEntity billing = billingRepository.findById(billingId)
//                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
//
//        BigDecimal currentDueAmount = safeGetAmount(billing.getDueAmount());
//        updateDueAmountsInTransactions(billing, currentDueAmount, username);
//    }
//
//    /**
//     * Gets billing summary for a specific billing ID
//     */
//    public BillingSummary getBillingSummary(Long billingId) {
//        BillingEntity billing = billingRepository.findById(billingId)
//                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
//
//        BigDecimal totalPaid = billing.getTransactions().stream()
//                .map(t -> safeGetAmount(t.getReceivedAmount()))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal totalRefunded = billing.getTransactions().stream()
//                .map(t -> safeGetAmount(t.getRefundAmount()))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        return new BillingSummary(
//                billing.getId(),
//                safeGetAmount(billing.getNetAmount()),
//                totalPaid,
//                totalRefunded,
//                safeGetAmount(billing.getDueAmount()),
//                billing.getPaymentStatus(),
//                billing.getTransactions().size()
//        );
//    }
//
//    /**
//     * Result class for billing recalculation
//     */
//    private static class BillingRecalculationResult {
//        private final BigDecimal newDueAmount;
//        private final String newPaymentStatus;
//        private final BigDecimal refundAmount;
//
//        public BillingRecalculationResult(BigDecimal newDueAmount, String newPaymentStatus, BigDecimal refundAmount) {
//            this.newDueAmount = newDueAmount;
//            this.newPaymentStatus = newPaymentStatus;
//            this.refundAmount = refundAmount;
//        }
//
//        public BigDecimal getNewDueAmount() { return newDueAmount; }
//        public String getNewPaymentStatus() { return newPaymentStatus; }
//        public BigDecimal getRefundAmount() { return refundAmount; }
//    }
//
//    /**
//     * Billing summary class for reporting
//     */
//    public static class BillingSummary {
//        private final Long billingId;
//        private final BigDecimal netAmount;
//        private final BigDecimal totalPaid;
//        private final BigDecimal totalRefunded;
//        private final BigDecimal dueAmount;
//        private final String paymentStatus;
//        private final int transactionCount;
//
//        public BillingSummary(Long billingId, BigDecimal netAmount, BigDecimal totalPaid,
//                            BigDecimal totalRefunded, BigDecimal dueAmount, String paymentStatus, int transactionCount) {
//            this.billingId = billingId;
//            this.netAmount = netAmount;
//            this.totalPaid = totalPaid;
//            this.totalRefunded = totalRefunded;
//            this.dueAmount = dueAmount;
//            this.paymentStatus = paymentStatus;
//            this.transactionCount = transactionCount;
//        }
//
//        // Getters
//        public Long getBillingId() { return billingId; }
//        public BigDecimal getNetAmount() { return netAmount; }
//        public BigDecimal getTotalPaid() { return totalPaid; }
//        public BigDecimal getTotalRefunded() { return totalRefunded; }
//        public BigDecimal getDueAmount() { return dueAmount; }
//        public String getPaymentStatus() { return paymentStatus; }
//        public int getTransactionCount() { return transactionCount; }
//    }
//}

//
//package tiameds.com.tiameds.services.lab;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import tiameds.com.tiameds.entity.BillingEntity;
//import tiameds.com.tiameds.entity.TransactionEntity;
//import tiameds.com.tiameds.repository.BillingRepository;
//import tiameds.com.tiameds.repository.TransactionRepository;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.ZoneId;
//import java.util.Comparator;
//
//@Service
//public class BillingManagementService {
//
//    private static final Logger logger = LoggerFactory.getLogger(BillingManagementService.class);
//
//    // Payment status constants
//    private static final String UNPAID = "UNPAID";
//    private static final String PARTIALLY_PAID = "PARTIALLY_PAID";
//    private static final String PAID = "PAID";
//    private static final String REFUND = "REFUND";
//
//    private final BillingRepository billingRepository;
//    private final TransactionRepository transactionRepository;
//
//    public BillingManagementService(BillingRepository billingRepository, TransactionRepository transactionRepository) {
//        this.billingRepository = billingRepository;
//        this.transactionRepository = transactionRepository;
//    }
//
//    /**
//     * FIXED: Updates billing after test cancellation with proper refund logic
//     */
//    @Transactional
//    public BillingEntity updateBillingAfterCancellation(Long billingId, BigDecimal newNetAmount, String username) {
//        logger.info("FIXED: Starting billing update for cancellation - BillingId: {}, NewNetAmount: {}, User: {}",
//                billingId, newNetAmount, username);
//
//        BillingEntity billing = billingRepository.findById(billingId)
//                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
//
//        if (newNetAmount == null || newNetAmount.compareTo(BigDecimal.ZERO) < 0) {
//            throw new IllegalArgumentException("New net amount must be non-negative");
//        }
//
//        BigDecimal currentNetAmount = safeGetAmount(billing.getNetAmount());
//        BigDecimal totalPayments = getTotalPayments(billing);
//        BigDecimal totalRefunds = getTotalRefunds(billing);
//        BigDecimal currentAra = totalPayments.subtract(totalRefunds);
//
//        logger.info("FIXED: Current state - NetAmount: {}, Payments: {}, Refunds: {}, ARA: {}",
//                currentNetAmount, totalPayments, totalRefunds, currentAra);
//
//        // Calculate net change (negative when tests are cancelled)
//        BigDecimal netChange = newNetAmount.subtract(currentNetAmount);
//
//        // FIXED: Proper refund calculation
//        BigDecimal refundAmount = calculateRefundAmount(currentAra, currentNetAmount, newNetAmount, netChange);
//
//        // FIXED: Calculate new ARA (without problematic capping)
//        BigDecimal newAra = currentAra.subtract(refundAmount);
//
//        // FIXED: Due amount calculation
//        BigDecimal newDueAmount = newNetAmount.subtract(newAra);
//
//        // FIXED: Payment status calculation
//        String newPaymentStatus = calculatePaymentStatus(newAra, newNetAmount, newDueAmount);
//
//        // Update billing
//        billing.setNetAmount(newNetAmount);
//        billing.setActualReceivedAmount(newAra);
//        billing.setDueAmount(newDueAmount);
//        billing.setPaymentStatus(newPaymentStatus);
//        billing.setUpdatedBy(username);
//
//        // Save billing first
//        billing = billingRepository.save(billing);
//
//        // FIXED: Create refund only if needed and proper amount
//        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
//            createRefundTransaction(billing, refundAmount,
//                    "Refund for test cancellation: " + netChange.abs() + " amount reduction", username);
//        }
//
//        logger.info("FIXED: Billing updated - NewNet: {}, NewARA: {}, Due: {}, Status: {}, Refund: {}",
//                newNetAmount, newAra, newDueAmount, newPaymentStatus, refundAmount);
//
//        return billing;
//    }
//
//    /**
//     * FIXED: Proper refund calculation logic
//     */
//    private BigDecimal calculateRefundAmount(BigDecimal currentAra, BigDecimal currentNetAmount,
//                                             BigDecimal newNetAmount, BigDecimal netChange) {
//
//        // If tests were cancelled (net change is negative)
//        if (netChange.compareTo(BigDecimal.ZERO) < 0) {
//            BigDecimal cancelledAmount = netChange.abs();
//
//            // Calculate how much the patient overpaid after cancellation
//            BigDecimal overpaymentAfterCancellation = currentAra.subtract(newNetAmount);
//
//            // Refund should be the minimum of:
//            // 1. The overpaid amount (if any)
//            // 2. The cancelled test amount
//            // 3. But never negative
//            BigDecimal refund = overpaymentAfterCancellation.max(BigDecimal.ZERO).min(cancelledAmount);
//
//            logger.debug("FIXED: Refund calculation - Overpayment: {}, Cancelled: {}, Refund: {}",
//                    overpaymentAfterCancellation, cancelledAmount, refund);
//
//            return refund;
//        }
//
//        // If tests were added, no refund
//        return BigDecimal.ZERO;
//    }
//
//    /**
//     * FIXED: Proper payment status calculation
//     */
//    private String calculatePaymentStatus(BigDecimal ara, BigDecimal netAmount, BigDecimal dueAmount) {
//        if (dueAmount.compareTo(BigDecimal.ZERO) == 0) {
//            return PAID;
//        } else if (ara.compareTo(BigDecimal.ZERO) > 0) {
//            return PARTIALLY_PAID;
//        } else {
//            return UNPAID;
//        }
//    }
//
//    /**
//     * FIXED: Add payment with proper ARA calculation
//     */
//    @Transactional
//    public BillingEntity addPayment(Long billingId, BigDecimal paymentAmount, String paymentMethod,
//                                    String upiId, BigDecimal upiAmount, BigDecimal cardAmount,
//                                    BigDecimal cashAmount, String username) {
//
//        logger.info("FIXED: Adding payment - BillingId: {}, Amount: {}, Method: {}, User: {}",
//                billingId, paymentAmount, paymentMethod, username);
//
//        validatePaymentInput(paymentAmount, paymentMethod);
//
//        BillingEntity billing = billingRepository.findById(billingId)
//                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
//
//        BigDecimal currentNetAmount = safeGetAmount(billing.getNetAmount());
//        BigDecimal totalPayments = getTotalPayments(billing);
//        BigDecimal totalRefunds = getTotalRefunds(billing);
//        BigDecimal currentAra = totalPayments.subtract(totalRefunds);
//
//        // FIXED: Add new payment to total
//        BigDecimal newTotalPayments = totalPayments.add(paymentAmount);
//        BigDecimal newAra = newTotalPayments.subtract(totalRefunds);
//
//        // FIXED: Calculate due amount
//        BigDecimal newDueAmount = currentNetAmount.subtract(newAra);
//
//        // FIXED: Check if overpayment occurred
//        BigDecimal overpayment = newAra.subtract(currentNetAmount).max(BigDecimal.ZERO);
//        BigDecimal refundAmount = BigDecimal.ZERO;
//
//        if (overpayment.compareTo(BigDecimal.ZERO) > 0) {
//            // Cap ARA to net amount and refund overpayment
//            newAra = currentNetAmount;
//            newDueAmount = BigDecimal.ZERO;
//            refundAmount = overpayment;
//        }
//
//        String newPaymentStatus = calculatePaymentStatus(newAra, currentNetAmount, newDueAmount);
//
//        // Update billing
//        billing.setActualReceivedAmount(newAra);
//        billing.setDueAmount(newDueAmount);
//        billing.setPaymentStatus(newPaymentStatus);
//        billing.setUpdatedBy(username);
//
//        billing = billingRepository.save(billing);
//
//        // Create payment transaction
//        createPaymentTransaction(billing, paymentAmount, paymentMethod, upiId, upiAmount, cardAmount, cashAmount, username);
//
//        // Create refund if overpayment occurred
//        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
//            createRefundTransaction(billing, refundAmount, "Refund for overpayment", username);
//        }
//
//        logger.info("FIXED: Payment added - NewARA: {}, Due: {}, Status: {}, Refund: {}",
//                newAra, newDueAmount, newPaymentStatus, refundAmount);
//
//        return billing;
//    }
//
//    /**
//     * FIXED: Create refund transaction
//     */
//    private void createRefundTransaction(BillingEntity billing, BigDecimal refundAmount, String remarks, String username) {
//        logger.info("FIXED: Creating refund - BillingId: {}, Amount: {}, Remarks: {}",
//                billing.getId(), refundAmount, remarks);
//
//        TransactionEntity refundTransaction = new TransactionEntity();
//        refundTransaction.setBilling(billing);
//        refundTransaction.setPaymentMethod(REFUND);
//        refundTransaction.setRefundAmount(refundAmount);
//        refundTransaction.setReceivedAmount(BigDecimal.ZERO);
//        refundTransaction.setDueAmount(safeGetAmount(billing.getDueAmount()));
//        refundTransaction.setPaymentDate(LocalDate.now().toString());
//        refundTransaction.setRemarks(remarks);
//        refundTransaction.setCreatedBy(username);
//        refundTransaction.setCreatedAt(LocalDateTime.now());
//
//        // Set zero amounts for payment methods
//        refundTransaction.setUpiId("");
//        refundTransaction.setUpiAmount(BigDecimal.ZERO);
//        refundTransaction.setCardAmount(BigDecimal.ZERO);
//        refundTransaction.setCashAmount(BigDecimal.ZERO);
//
//        transactionRepository.save(refundTransaction);
//    }
//
//    /**
//     * FIXED: Create payment transaction
//     */
//    private void createPaymentTransaction(BillingEntity billing, BigDecimal paymentAmount, String paymentMethod,
//                                          String upiId, BigDecimal upiAmount, BigDecimal cardAmount,
//                                          BigDecimal cashAmount, String username) {
//
//        TransactionEntity transaction = new TransactionEntity();
//        transaction.setBilling(billing);
//        transaction.setPaymentMethod(paymentMethod);
//        transaction.setReceivedAmount(paymentAmount);
//        transaction.setRefundAmount(BigDecimal.ZERO);
//        transaction.setDueAmount(safeGetAmount(billing.getDueAmount()));
//        transaction.setPaymentDate(LocalDate.now().toString());
//        transaction.setRemarks("Payment via " + paymentMethod);
//        transaction.setCreatedBy(username);
//        transaction.setCreatedAt(LocalDateTime.now());
//
//        // Set payment method amounts
//        transaction.setUpiId(upiId != null ? upiId : "");
//        transaction.setUpiAmount(safeGetAmount(upiAmount));
//        transaction.setCardAmount(safeGetAmount(cardAmount));
//        transaction.setCashAmount(safeGetAmount(cashAmount));
//
//        transactionRepository.save(transaction);
//    }
//
//    /**
//     * FIXED: Get total payments (sum of receivedAmount from all transactions)
//     */
//    private BigDecimal getTotalPayments(BillingEntity billing) {
//        return billing.getTransactions().stream()
//                .map(t -> safeGetAmount(t.getReceivedAmount()))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
//
//    /**
//     * FIXED: Get total refunds (sum of refundAmount from all transactions)
//     */
//    private BigDecimal getTotalRefunds(BillingEntity billing) {
//        return billing.getTransactions().stream()
//                .map(t -> safeGetAmount(t.getRefundAmount()))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
//
//    /**
//     * FIXED: Safe amount getter
//     */
//    private BigDecimal safeGetAmount(BigDecimal amount) {
//        return amount != null ? amount : BigDecimal.ZERO;
//    }
//
//    /**
//     * FIXED: Payment validation
//     */
//    private void validatePaymentInput(BigDecimal paymentAmount, String paymentMethod) {
//        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("Payment amount must be positive");
//        }
//        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
//            throw new IllegalArgumentException("Payment method is required");
//        }
//    }
//
//    /**
//     * FIXED: Remove transaction due amount updates - keep historical data immutable
//     */
//    @Transactional
//    public void updateDueAmountsInAllTransactions(Long billingId, String username) {
//        logger.info("FIXED: Due amount updates are now immutable - only current billing due amount is maintained");
//        // Intentional no-op to maintain backward compatibility
//    }
//
//    /**
//     * FIXED: Get billing summary
//     */
//    public BillingSummary getBillingSummary(Long billingId) {
//        BillingEntity billing = billingRepository.findById(billingId)
//                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
//
//        BigDecimal totalPayments = getTotalPayments(billing);
//        BigDecimal totalRefunds = getTotalRefunds(billing);
//        BigDecimal actualAra = totalPayments.subtract(totalRefunds);
//
//        return new BillingSummary(
//                billing.getId(),
//                safeGetAmount(billing.getNetAmount()),
//                totalPayments,
//                totalRefunds,
//                actualAra,
//                safeGetAmount(billing.getDueAmount()),
//                billing.getPaymentStatus(),
//                billing.getTransactions().size()
//        );
//    }
//
//    public static class BillingSummary {
//        private final Long billingId;
//        private final BigDecimal netAmount;
//        private final BigDecimal totalPaid;
//        private final BigDecimal totalRefunded;
//        private final BigDecimal actualReceivedAmount;
//        private final BigDecimal dueAmount;
//        private final String paymentStatus;
//        private final int transactionCount;
//
//        public BillingSummary(Long billingId, BigDecimal netAmount, BigDecimal totalPaid,
//                              BigDecimal totalRefunded, BigDecimal actualReceivedAmount,
//                              BigDecimal dueAmount, String paymentStatus, int transactionCount) {
//            this.billingId = billingId;
//            this.netAmount = netAmount;
//            this.totalPaid = totalPaid;
//            this.totalRefunded = totalRefunded;
//            this.actualReceivedAmount = actualReceivedAmount;
//            this.dueAmount = dueAmount;
//            this.paymentStatus = paymentStatus;
//            this.transactionCount = transactionCount;
//        }
//
//        // Getters
//        public Long getBillingId() { return billingId; }
//        public BigDecimal getNetAmount() { return netAmount; }
//        public BigDecimal getTotalPaid() { return totalPaid; }
//        public BigDecimal getTotalRefunded() { return totalRefunded; }
//        public BigDecimal getActualReceivedAmount() { return actualReceivedAmount; }
//        public BigDecimal getDueAmount() { return dueAmount; }
//        public String getPaymentStatus() { return paymentStatus; }
//        public int getTransactionCount() { return transactionCount; }
//    }
//}



//------------

package tiameds.com.tiameds.services.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.BillingEntity;
import tiameds.com.tiameds.entity.TransactionEntity;
import tiameds.com.tiameds.repository.BillingRepository;
import tiameds.com.tiameds.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class BillingManagementService {

    private static final Logger logger = LoggerFactory.getLogger(BillingManagementService.class);

    // Payment status constants
    private static final String UNPAID = "UNPAID";
    private static final String PARTIALLY_PAID = "PARTIALLY_PAID";
    private static final String PAID = "PAID";
    private static final String REFUND = "REFUND";

    private final BillingRepository billingRepository;
    private final TransactionRepository transactionRepository;

    public BillingManagementService(BillingRepository billingRepository, TransactionRepository transactionRepository) {
        this.billingRepository = billingRepository;
        this.transactionRepository = transactionRepository;

    }

    /**
     * FIXED: Updates billing after test cancellation with proper refund logic
     */
    @Transactional
    public BillingEntity updateBillingAfterCancellation(Long billingId, BigDecimal newNetAmount, String username) {
        logger.info("FIXED: Starting billing update for cancellation - BillingId: {}, NewNetAmount: {}, User: {}",
                billingId, newNetAmount, username);

        BillingEntity billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));

        if (newNetAmount == null || newNetAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("New net amount must be non-negative");
        }

        BigDecimal currentNetAmount = safeGetAmount(billing.getNetAmount());
        BigDecimal totalPayments = getTotalPayments(billing);
        BigDecimal totalRefunds = getTotalRefunds(billing);
        BigDecimal currentAra = totalPayments.subtract(totalRefunds);

        logger.info("FIXED: Current state - NetAmount: {}, Payments: {}, Refunds: {}, ARA: {}",
                currentNetAmount, totalPayments, totalRefunds, currentAra);

        // Calculate net change (negative when tests are cancelled)
        BigDecimal netChange = newNetAmount.subtract(currentNetAmount);

        // FIXED: Proper refund calculation that handles your specific case
        BigDecimal refundAmount = calculateRefundAmountForCancellation(
                totalPayments, totalRefunds, currentNetAmount, newNetAmount, netChange);

        // FIXED: Calculate new ARA properly (Total Payments - Total Refunds including new refund)
        BigDecimal newTotalRefunds = totalRefunds.add(refundAmount);
        BigDecimal newAra = totalPayments.subtract(newTotalRefunds);

        // FIXED: Due amount calculation (should never be negative)
        BigDecimal newDueAmount = newNetAmount.subtract(newAra);

        // FIXED: Safety check - prevent negative due amounts
        if (newDueAmount.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("FIXED: Correcting negative due amount from {} to 0", newDueAmount);
            // If due is negative, it means we need additional refund
            BigDecimal additionalRefund = newDueAmount.abs();
            refundAmount = refundAmount.add(additionalRefund);
            newTotalRefunds = totalRefunds.add(refundAmount);
            newAra = totalPayments.subtract(newTotalRefunds);
            newDueAmount = BigDecimal.ZERO;
        }

        // FIXED: Payment status calculation
        String newPaymentStatus = calculatePaymentStatus(newAra, newNetAmount, newDueAmount);

        // Update billing
        billing.setNetAmount(newNetAmount);
        billing.setActualReceivedAmount(newAra);
        billing.setDueAmount(newDueAmount);
        billing.setPaymentStatus(newPaymentStatus);
        billing.setUpdatedBy(username);

        // Save billing first
        billing = billingRepository.save(billing);

        // FIXED: Create refund only if needed
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            createRefundTransaction(billing, refundAmount,
                    "Refund for test cancellation: " + netChange.abs() + " amount reduction", username);
        }

        logger.info("FIXED: Billing updated - NewNet: {}, NewARA: {}, Due: {}, Status: {}, Refund: {}",
                newNetAmount, newAra, newDueAmount, newPaymentStatus, refundAmount);

        return billing;
    }

    /**
     * FIXED: Proper refund calculation that handles your specific case
     * Case: Paid 1000, Refunded 400, ARA=600, Cancel test worth 300
     * Expected: Refund 300 more, ARA=300, Due=0
     */
    private BigDecimal calculateRefundAmountForCancellation(BigDecimal totalPayments, BigDecimal existingRefunds,
                                                            BigDecimal currentNetAmount, BigDecimal newNetAmount,
                                                            BigDecimal netChange) {

        // If tests were cancelled (net change is negative)
        if (netChange.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal cancelledAmount = netChange.abs();

            // FIXED: Calculate how much patient has actually paid net of previous refunds
            BigDecimal netAmountPaid = totalPayments.subtract(existingRefunds);

            // FIXED: Calculate how much they should pay for remaining tests
            BigDecimal expectedPayment = newNetAmount;

            // FIXED: The refund needed is the difference between what they paid and what they should pay
            BigDecimal refundNeeded = netAmountPaid.subtract(expectedPayment);

            // FIXED: Refund should be the minimum of refund needed and cancelled amount
            BigDecimal refund = refundNeeded.max(BigDecimal.ZERO).min(cancelledAmount);

            logger.debug("FIXED: Refund calculation - TotalPaid: {}, ExistingRefunds: {}, NetPaid: {}, " +
                            "Expected: {}, RefundNeeded: {}, Cancelled: {}, FinalRefund: {}",
                    totalPayments, existingRefunds, netAmountPaid, expectedPayment,
                    refundNeeded, cancelledAmount, refund);

            return refund;
        }

        // If tests were added, no refund
        return BigDecimal.ZERO;
    }

    /**
     * FIXED: Proper payment status calculation
     */
    private String calculatePaymentStatus(BigDecimal ara, BigDecimal netAmount, BigDecimal dueAmount) {
        if (dueAmount.compareTo(BigDecimal.ZERO) == 0) {
            return PAID;
        } else if (ara.compareTo(BigDecimal.ZERO) > 0) {
            return PARTIALLY_PAID;
        } else {
            return UNPAID;
        }
    }

    /**
     * FIXED: Add payment with proper ARA calculation
     */
    @Transactional
    public BillingEntity addPayment(Long billingId, BigDecimal paymentAmount, String paymentMethod,
                                    String upiId, BigDecimal upiAmount, BigDecimal cardAmount,
                                    BigDecimal cashAmount, String username) {

        logger.info("FIXED: Adding payment - BillingId: {}, Amount: {}, Method: {}, User: {}",
                billingId, paymentAmount, paymentMethod, username);

        validatePaymentInput(paymentAmount, paymentMethod);

        BillingEntity billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));

        BigDecimal currentNetAmount = safeGetAmount(billing.getNetAmount());
        BigDecimal totalPayments = getTotalPayments(billing);
        BigDecimal totalRefunds = getTotalRefunds(billing);
        BigDecimal currentAra = totalPayments.subtract(totalRefunds);

        // FIXED: Add new payment to total
        BigDecimal newTotalPayments = totalPayments.add(paymentAmount);
        BigDecimal newAra = newTotalPayments.subtract(totalRefunds);

        // FIXED: Check if overpayment occurred
        BigDecimal overpayment = newAra.subtract(currentNetAmount).max(BigDecimal.ZERO);
        BigDecimal refundAmount = BigDecimal.ZERO;

        if (overpayment.compareTo(BigDecimal.ZERO) > 0) {
            // Cap ARA to net amount and refund overpayment
            refundAmount = overpayment;
            newAra = currentNetAmount;
        }

        // FIXED: Calculate due amount (never negative)
        BigDecimal newDueAmount = currentNetAmount.subtract(newAra).max(BigDecimal.ZERO);

        String newPaymentStatus = calculatePaymentStatus(newAra, currentNetAmount, newDueAmount);

        // Update billing
        billing.setActualReceivedAmount(newAra);
        billing.setDueAmount(newDueAmount);
        billing.setPaymentStatus(newPaymentStatus);
        billing.setUpdatedBy(username);

        billing = billingRepository.save(billing);

        // Create payment transaction
        createPaymentTransaction(billing, paymentAmount, paymentMethod, upiId, upiAmount, cardAmount, cashAmount, username);

        // Create refund if overpayment occurred
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            createRefundTransaction(billing, refundAmount, "Refund for overpayment", username);
        }

        logger.info("FIXED: Payment added - NewARA: {}, Due: {}, Status: {}, Refund: {}",
                newAra, newDueAmount, newPaymentStatus, refundAmount);

        return billing;
    }

    /**
     * FIXED: Create refund transaction
     */
    private void createRefundTransaction(BillingEntity billing, BigDecimal refundAmount, String remarks, String username) {
        logger.info("FIXED: Creating refund - BillingId: {}, Amount: {}, Remarks: {}",
                billing.getId(), refundAmount, remarks);

        TransactionEntity refundTransaction = new TransactionEntity();
        refundTransaction.setBilling(billing);
        refundTransaction.setPaymentMethod(REFUND);
        refundTransaction.setRefundAmount(refundAmount);
        refundTransaction.setReceivedAmount(BigDecimal.ZERO);
        refundTransaction.setDueAmount(safeGetAmount(billing.getDueAmount()));
        refundTransaction.setPaymentDate(LocalDate.now().toString());
        refundTransaction.setRemarks(remarks);
        refundTransaction.setCreatedBy(username);
        refundTransaction.setCreatedAt(LocalDateTime.now());

        // Set zero amounts for payment methods
        refundTransaction.setUpiId("");
        refundTransaction.setUpiAmount(BigDecimal.ZERO);
        refundTransaction.setCardAmount(BigDecimal.ZERO);
        refundTransaction.setCashAmount(BigDecimal.ZERO);

        transactionRepository.save(refundTransaction);
    }

    /**
     * FIXED: Create payment transaction
     */
    private void createPaymentTransaction(BillingEntity billing, BigDecimal paymentAmount, String paymentMethod,
                                          String upiId, BigDecimal upiAmount, BigDecimal cardAmount,
                                          BigDecimal cashAmount, String username) {

        TransactionEntity transaction = new TransactionEntity();
        transaction.setBilling(billing);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setReceivedAmount(paymentAmount);
        transaction.setRefundAmount(BigDecimal.ZERO);
        transaction.setDueAmount(safeGetAmount(billing.getDueAmount()));
        transaction.setPaymentDate(LocalDate.now().toString());
        transaction.setRemarks("Payment via " + paymentMethod);
        transaction.setCreatedBy(username);
        transaction.setCreatedAt(LocalDateTime.now());

        // Set payment method amounts
        transaction.setUpiId(upiId != null ? upiId : "");
        transaction.setUpiAmount(safeGetAmount(upiAmount));
        transaction.setCardAmount(safeGetAmount(cardAmount));
        transaction.setCashAmount(safeGetAmount(cashAmount));

        transactionRepository.save(transaction);
    }

    /**
     * FIXED: Get total payments (sum of receivedAmount from all transactions)
     */
    private BigDecimal getTotalPayments(BillingEntity billing) {
        return billing.getTransactions().stream()
                .map(t -> safeGetAmount(t.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * FIXED: Get total refunds (sum of refundAmount from all transactions)
     */
    private BigDecimal getTotalRefunds(BillingEntity billing) {
        return billing.getTransactions().stream()
                .map(t -> safeGetAmount(t.getRefundAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * FIXED: Safe amount getter
     */
    private BigDecimal safeGetAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    /**
     * FIXED: Payment validation
     */
    private void validatePaymentInput(BigDecimal paymentAmount, String paymentMethod) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
    }

    /**
     * FIXED: Remove transaction due amount updates - keep historical data immutable
     */
    @Transactional
    public void updateDueAmountsInAllTransactions(Long billingId, String username) {
        logger.info("FIXED: Due amount updates are now immutable - only current billing due amount is maintained");
        // Intentional no-op to maintain backward compatibility
    }

    /**
     * FIXED: Get billing summary
     */
    public BillingSummary getBillingSummary(Long billingId) {
        BillingEntity billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));

        BigDecimal totalPayments = getTotalPayments(billing);
        BigDecimal totalRefunds = getTotalRefunds(billing);
        BigDecimal actualAra = totalPayments.subtract(totalRefunds);

        return new BillingSummary(
                billing.getId(),
                safeGetAmount(billing.getNetAmount()),
                totalPayments,
                totalRefunds,
                actualAra,
                safeGetAmount(billing.getDueAmount()),
                billing.getPaymentStatus(),
                billing.getTransactions().size()
        );
    }

    /**
     * NEW: Method specifically for your test case debugging
     */
    public String debugBillingState(Long billingId) {
        BillingEntity billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));

        BigDecimal totalPayments = getTotalPayments(billing);
        BigDecimal totalRefunds = getTotalRefunds(billing);
        BigDecimal actualAra = totalPayments.subtract(totalRefunds);

        return String.format(
                "DEBUG Billing State - ID: %s\n" +
                        "Net Amount: %s\n" +
                        "Total Payments: %s\n" +
                        "Total Refunds: %s\n" +
                        "Actual Received Amount (ARA): %s\n" +
                        "Due Amount: %s\n" +
                        "Payment Status: %s\n" +
                        "Transactions: %d",
                billing.getId(), billing.getNetAmount(), totalPayments, totalRefunds,
                actualAra, billing.getDueAmount(), billing.getPaymentStatus(),
                billing.getTransactions().size()
        );
    }

    public static class BillingSummary {
        private final Long billingId;
        private final BigDecimal netAmount;
        private final BigDecimal totalPaid;
        private final BigDecimal totalRefunded;
        private final BigDecimal actualReceivedAmount;
        private final BigDecimal dueAmount;
        private final String paymentStatus;
        private final int transactionCount;

        public BillingSummary(Long billingId, BigDecimal netAmount, BigDecimal totalPaid,
                              BigDecimal totalRefunded, BigDecimal actualReceivedAmount,
                              BigDecimal dueAmount, String paymentStatus, int transactionCount) {
            this.billingId = billingId;
            this.netAmount = netAmount;
            this.totalPaid = totalPaid;
            this.totalRefunded = totalRefunded;
            this.actualReceivedAmount = actualReceivedAmount;
            this.dueAmount = dueAmount;
            this.paymentStatus = paymentStatus;
            this.transactionCount = transactionCount;
        }

        // Getters
        public Long getBillingId() { return billingId; }
        public BigDecimal getNetAmount() { return netAmount; }
        public BigDecimal getTotalPaid() { return totalPaid; }
        public BigDecimal getTotalRefunded() { return totalRefunded; }
        public BigDecimal getActualReceivedAmount() { return actualReceivedAmount; }
        public BigDecimal getDueAmount() { return dueAmount; }
        public String getPaymentStatus() { return paymentStatus; }
        public int getTransactionCount() { return transactionCount; }
    }
}





