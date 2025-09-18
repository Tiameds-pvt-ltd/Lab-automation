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

/**
 * Production-ready billing management service that handles all billing operations
 * including payments, refunds, cancellations, and status updates.
 * 
 * Business Rules:
 * - A patient books tests → initial bill created with totalAmount
 * - Payments are recorded as TransactionEntity with receivedAmount > 0
 * - If tests are cancelled, the totalAmount in BillingEntity decreases
 * - After recalculation:
 *   * If total paid < new totalAmount → update dueAmount
 *   * If total paid = new totalAmount → dueAmount = 0, paymentStatus = PAID
 *   * If total paid > new totalAmount → create a refund transaction with refundAmount = overpaid
 * - Never overwrite old transactions, always append new ones (immutability is critical)
 * - paymentStatus values: UNPAID, PARTIALLY_PAID, PAID
 */
@Service
public class BillingManagementService {

    private static final Logger logger = LoggerFactory.getLogger(BillingManagementService.class);
    
    // Payment status constants
    private static final String UNPAID = "UNPAID";
    private static final String PARTIALLY_PAID = "PARTIALLY_PAID";
    private static final String PAID = "PAID";
    
    // Payment method constants
    private static final String CASH = "CASH";
    private static final String CARD = "CARD";
    private static final String UPI = "UPI";
    
    private final BillingRepository billingRepository;
    private final TransactionRepository transactionRepository;

    public BillingManagementService(BillingRepository billingRepository, TransactionRepository transactionRepository) {
        this.billingRepository = billingRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Updates billing after test cancellation with comprehensive business logic
     * 
     * @param billingId The ID of the billing entity to update
     * @param newNetAmount The new net amount after test cancellation
     * @param username The username performing the operation
     * @return Updated BillingEntity
     */
    @Transactional
    public BillingEntity updateBillingAfterCancellation(Long billingId, BigDecimal newNetAmount, String username) {
        logger.info("Starting billing update for cancellation - BillingId: {}, NewNetAmount: {}, User: {}", 
                   billingId, newNetAmount, username);
        
        // Find billing entity with optimistic locking
        BillingEntity billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
        
        // Validate input
        if (newNetAmount == null || newNetAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("New net amount must be non-negative");
        }
        
        // Get current values safely (treat null as zero)
        BigDecimal currentNetAmount = safeGetAmount(billing.getNetAmount());
        BigDecimal currentReceivedAmount = safeGetAmount(billing.getReceivedAmount());
        BigDecimal currentDueAmount = safeGetAmount(billing.getDueAmount());
        
        logger.info("Current billing state - NetAmount: {}, ReceivedAmount: {}, DueAmount: {}", 
                   currentNetAmount, currentReceivedAmount, currentDueAmount);
        
        // Update net amount
        billing.setNetAmount(newNetAmount);
        
        // Recalculate billing based on business rules
        BillingRecalculationResult result = recalculateBilling(currentReceivedAmount, newNetAmount);
        
        // Update billing fields
        billing.setDueAmount(result.getNewDueAmount());
        billing.setPaymentStatus(result.getNewPaymentStatus());
        billing.setUpdatedBy(username);
        
        // Save billing entity
        billing = billingRepository.save(billing);
        logger.info("Updated billing - NewDueAmount: {}, NewPaymentStatus: {}", 
                   result.getNewDueAmount(), result.getNewPaymentStatus());
        
        // Create refund transaction if needed
        if (result.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            createRefundTransaction(billing, result.getRefundAmount(), username);
        }
        
        logger.info("Billing update completed successfully for BillingId: {}", billingId);
        return billing;
    }

    /**
     * Adds a payment to existing billing
     * 
     * @param billingId The ID of the billing entity
     * @param paymentAmount The payment amount
     * @param paymentMethod The payment method (CASH, CARD, UPI)
     * @param username The username performing the operation
     * @return Updated BillingEntity
     */
    @Transactional
    public BillingEntity addPayment(Long billingId, BigDecimal paymentAmount, String paymentMethod, String username) {
        logger.info("Adding payment - BillingId: {}, Amount: {}, Method: {}, User: {}", 
                   billingId, paymentAmount, paymentMethod, username);
        
        // Validate input
        validatePaymentInput(paymentAmount, paymentMethod);
        
        // Find billing entity
        BillingEntity billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
        
        // Get current values safely
        BigDecimal currentReceivedAmount = safeGetAmount(billing.getReceivedAmount());
        BigDecimal currentNetAmount = safeGetAmount(billing.getNetAmount());
        
        // Calculate new received amount
        BigDecimal newReceivedAmount = currentReceivedAmount.add(paymentAmount);
        
        // Recalculate billing
        BillingRecalculationResult result = recalculateBilling(newReceivedAmount, currentNetAmount);
        
        // Update billing
        billing.setReceivedAmount(newReceivedAmount);
        billing.setDueAmount(result.getNewDueAmount());
        billing.setPaymentStatus(result.getNewPaymentStatus());
        billing.setUpdatedBy(username);
        
        // Save billing
        billing = billingRepository.save(billing);
        
        // Create payment transaction
        createPaymentTransaction(billing, paymentAmount, paymentMethod, username);
        
        logger.info("Payment added successfully - NewReceivedAmount: {}, NewDueAmount: {}, NewStatus: {}", 
                   newReceivedAmount, result.getNewDueAmount(), result.getNewPaymentStatus());
        
        return billing;
    }

    /**
     * Recalculates billing based on business rules
     */
    private BillingRecalculationResult recalculateBilling(BigDecimal receivedAmount, BigDecimal netAmount) {
        BigDecimal newDueAmount = netAmount.subtract(receivedAmount);
        BigDecimal refundAmount = BigDecimal.ZERO;
        String newPaymentStatus;
        
        if (newDueAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Still owe money
            newPaymentStatus = receivedAmount.compareTo(BigDecimal.ZERO) > 0 ? PARTIALLY_PAID : UNPAID;
        } else if (newDueAmount.compareTo(BigDecimal.ZERO) == 0) {
            // Paid in full
            newPaymentStatus = PAID;
        } else {
            // Overpaid - need refund
            refundAmount = newDueAmount.abs(); // Convert negative to positive
            newDueAmount = BigDecimal.ZERO;
            newPaymentStatus = PAID;
        }
        
        return new BillingRecalculationResult(newDueAmount, newPaymentStatus, refundAmount);
    }

    /**
     * Creates a refund transaction
     */
    private void createRefundTransaction(BillingEntity billing, BigDecimal refundAmount, String username) {
        logger.warn("Creating refund transaction - BillingId: {}, RefundAmount: {}, User: {}", 
                   billing.getId(), refundAmount, username);
        
        TransactionEntity refundTransaction = new TransactionEntity();
        refundTransaction.setBilling(billing);
        refundTransaction.setPaymentMethod("REFUND");
        refundTransaction.setRefundAmount(refundAmount);
        refundTransaction.setReceivedAmount(BigDecimal.ZERO);
        refundTransaction.setDueAmount(BigDecimal.ZERO);
        refundTransaction.setPaymentDate(LocalDate.now().toString());
        refundTransaction.setRemarks("Refund for test cancellation - overpayment");
        refundTransaction.setCreatedBy(username);
        refundTransaction.setCreatedAt(LocalDateTime.now());
        
        // Add to billing's transaction list (immutability - append only)
        billing.getTransactions().add(refundTransaction);
        
        // Save transaction
        transactionRepository.save(refundTransaction);
        
        logger.info("Refund transaction created successfully - TransactionId: {}, RefundAmount: {}", 
                   refundTransaction.getId(), refundAmount);
    }

    /**
     * Creates a payment transaction
     */
    private void createPaymentTransaction(BillingEntity billing, BigDecimal paymentAmount, String paymentMethod, String username) {
        logger.info("Creating payment transaction - BillingId: {}, Amount: {}, Method: {}, User: {}", 
                   billing.getId(), paymentAmount, paymentMethod, username);
        
        TransactionEntity paymentTransaction = new TransactionEntity();
        paymentTransaction.setBilling(billing);
        paymentTransaction.setPaymentMethod(paymentMethod);
        paymentTransaction.setReceivedAmount(paymentAmount);
        paymentTransaction.setRefundAmount(BigDecimal.ZERO);
        paymentTransaction.setDueAmount(safeGetAmount(billing.getDueAmount()));
        paymentTransaction.setPaymentDate(LocalDate.now().toString());
        paymentTransaction.setRemarks("Payment via " + paymentMethod);
        paymentTransaction.setCreatedBy(username);
        paymentTransaction.setCreatedAt(LocalDateTime.now());
        
        // Set payment method specific amounts
        switch (paymentMethod.toUpperCase()) {
            case CASH:
                paymentTransaction.setCashAmount(paymentAmount);
                paymentTransaction.setCardAmount(BigDecimal.ZERO);
                paymentTransaction.setUpiAmount(BigDecimal.ZERO);
                break;
            case CARD:
                paymentTransaction.setCashAmount(BigDecimal.ZERO);
                paymentTransaction.setCardAmount(paymentAmount);
                paymentTransaction.setUpiAmount(BigDecimal.ZERO);
                break;
            case UPI:
                paymentTransaction.setCashAmount(BigDecimal.ZERO);
                paymentTransaction.setCardAmount(BigDecimal.ZERO);
                paymentTransaction.setUpiAmount(paymentAmount);
                break;
        }
        
        // Add to billing's transaction list (immutability - append only)
        billing.getTransactions().add(paymentTransaction);
        
        // Save transaction
        transactionRepository.save(paymentTransaction);
        
        logger.info("Payment transaction created successfully - TransactionId: {}, Amount: {}", 
                   paymentTransaction.getId(), paymentAmount);
    }

    /**
     * Safely gets BigDecimal amount, treating null as zero
     */
    private BigDecimal safeGetAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    /**
     * Validates payment input
     */
    private void validatePaymentInput(BigDecimal paymentAmount, String paymentMethod) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        
        String upperMethod = paymentMethod.toUpperCase();
        if (!upperMethod.equals(CASH) && !upperMethod.equals(CARD) && !upperMethod.equals(UPI)) {
            throw new IllegalArgumentException("Payment method must be CASH, CARD, or UPI");
        }
    }

    /**
     * Gets billing summary for a specific billing ID
     */
    public BillingSummary getBillingSummary(Long billingId) {
        BillingEntity billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found with ID: " + billingId));
        
        BigDecimal totalPaid = billing.getTransactions().stream()
                .map(t -> safeGetAmount(t.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRefunded = billing.getTransactions().stream()
                .map(t -> safeGetAmount(t.getRefundAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new BillingSummary(
                billing.getId(),
                safeGetAmount(billing.getNetAmount()),
                totalPaid,
                totalRefunded,
                safeGetAmount(billing.getDueAmount()),
                billing.getPaymentStatus(),
                billing.getTransactions().size()
        );
    }

    /**
     * Result class for billing recalculation
     */
    private static class BillingRecalculationResult {
        private final BigDecimal newDueAmount;
        private final String newPaymentStatus;
        private final BigDecimal refundAmount;

        public BillingRecalculationResult(BigDecimal newDueAmount, String newPaymentStatus, BigDecimal refundAmount) {
            this.newDueAmount = newDueAmount;
            this.newPaymentStatus = newPaymentStatus;
            this.refundAmount = refundAmount;
        }

        public BigDecimal getNewDueAmount() { return newDueAmount; }
        public String getNewPaymentStatus() { return newPaymentStatus; }
        public BigDecimal getRefundAmount() { return refundAmount; }
    }

    /**
     * Billing summary class for reporting
     */
    public static class BillingSummary {
        private final Long billingId;
        private final BigDecimal netAmount;
        private final BigDecimal totalPaid;
        private final BigDecimal totalRefunded;
        private final BigDecimal dueAmount;
        private final String paymentStatus;
        private final int transactionCount;

        public BillingSummary(Long billingId, BigDecimal netAmount, BigDecimal totalPaid, 
                            BigDecimal totalRefunded, BigDecimal dueAmount, String paymentStatus, int transactionCount) {
            this.billingId = billingId;
            this.netAmount = netAmount;
            this.totalPaid = totalPaid;
            this.totalRefunded = totalRefunded;
            this.dueAmount = dueAmount;
            this.paymentStatus = paymentStatus;
            this.transactionCount = transactionCount;
        }

        // Getters
        public Long getBillingId() { return billingId; }
        public BigDecimal getNetAmount() { return netAmount; }
        public BigDecimal getTotalPaid() { return totalPaid; }
        public BigDecimal getTotalRefunded() { return totalRefunded; }
        public BigDecimal getDueAmount() { return dueAmount; }
        public String getPaymentStatus() { return paymentStatus; }
        public int getTransactionCount() { return transactionCount; }
    }
}
