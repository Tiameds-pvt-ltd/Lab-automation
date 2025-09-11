package tiameds.com.tiameds.controller.lab;


import lombok.Data;
import java.util.List;

@Data
public class LabStatisticsDTO {
    private int id;
    private String firstName;
    private String phone;
    private String city;
    private String dateOfBirth;
    private String age;
    private String gender;
    private String createdBy;
    private String updatedBy;
    private VisitDTO visit;

    @Data
    public static class VisitDTO {
        private int visitId;
        private String visitDate;
        private String visitType;
        private String visitStatus;
        private String visitDescription;
        private Integer doctorId;
        private List<Long> testIds;
        private List<Long> packageIds;
        private List<String> testNames;
        private List<Object> packageNames;
        private String DoctorName;
        private String createdBy;
        private String updatedBy;
        private String visitCancellationReason;
        private String visitCancellationDate;
        private String visitCancellationBy;
        private String visitCancellationTime;
        private BillingDTO billing;
        private List<TestResultDTO> testResult;
        private List<TestDiscountDTO> listofeachtestdiscount;
    }

    @Data
    public static class BillingDTO {
        private int billingId;
        private double totalAmount;
        private String paymentStatus;
        private String paymentMethod;
        private String paymentDate;
        private double discount;
        private double netAmount;
        private String discountReason;
        private String createdBy;
        private String updatedBy;
        private String billingTime;
        private String billingDate;
        private String createdAt;
        private String updatedAt;
        private double received_amount;
        private double due_amount;
        private List<TransactionDTO> transactions;
    }

    @Data
    public static class TransactionDTO {
        private int id;
        private String createdBy;
        private int billing_id;
        private String payment_method;
        private String upi_id;
        private double upi_amount;
        private double card_amount;
        private double cash_amount;
        private double received_amount;
        private double refund_amount;
        private double due_amount;
        private String payment_date;
        private String remarks;
        private String created_at;
    }

    @Data
    public static class TestResultDTO {
        private int id;
        private int testId;
        private boolean isFilled;
        private String reportStatus;
        private String createdBy;
        private String updatedBy;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    public static class TestDiscountDTO {
        private double discountAmount;
        private double discountPercent;
        private double finalPrice;
        private String createdBy;
        private String updatedBy;
        private int id;
    }
}
