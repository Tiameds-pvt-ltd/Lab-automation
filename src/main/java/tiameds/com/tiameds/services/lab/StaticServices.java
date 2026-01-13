package tiameds.com.tiameds.services.lab;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.controller.lab.LabStatisticsDTO;
import tiameds.com.tiameds.dto.lab.StaticDto;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class StaticServices {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final BillingRepository billingRepository;
    private final TestRepository testRepository;
    private final HealthPackageRepository healthPackageRepository;
    private final TransactionRepository transactionRepository;


    public StaticServices(
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            VisitRepository visitRepository,
            BillingRepository billingRepository,
            TestRepository testRepository,
            HealthPackageRepository healthPackageRepository, TransactionRepository transactionRepository) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.billingRepository = billingRepository;
        this.testRepository = testRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public StaticDto getStaticData(Long labId, String startDate, String endDate) {
        LocalDate startLocalDate = LocalDate.parse(startDate.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate endLocalDate = LocalDate.parse(endDate.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // Convert to Instant for VisitEntity (which uses Instant)
        Instant startDateTimeInstant = startLocalDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant endDateTimeInstant = endLocalDate.atTime(23, 59, 59).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        // Convert to LocalDateTime for other entities (which still use LocalDateTime)
        LocalDateTime startDateTime = startLocalDate.atStartOfDay();
        LocalDateTime endDateTime = endLocalDate.atTime(23, 59, 59);
        // Fetch aggregated data in one query if possible
        long numberOfPatients = patientRepository.countByLabIdAndCreatedAtBetween(labId, startDateTime, endDateTime);
        long numberOfVisits = visitRepository.countByLabIdAndCreatedAtBetween(labId, startDateTimeInstant, endDateTimeInstant);

        // Use an Enum or Constants for status values
        long collectedSamples = visitRepository.countByLabIdAndStatus(labId, "Collected", startDateTimeInstant, endDateTimeInstant);
        long pendingSamples = visitRepository.countByLabIdAndStatus(labId, "Pending", startDateTimeInstant, endDateTimeInstant);
        long paidVisits = billingRepository.countByLabIdAndStatus(labId, "PAID", startDateTime, endDateTime);

        // Fetch sums safely
        BigDecimal totalSales = Optional.ofNullable(billingRepository.sumTotalByLabId(labId, startDateTime, endDateTime))
                .orElse(BigDecimal.ZERO);
        BigDecimal totalDiscounts = Optional.ofNullable(billingRepository.sumDiscountByLabId(labId, startDateTime, endDateTime))
                .orElse(BigDecimal.ZERO);
        BigDecimal totalGrossSales = Optional.ofNullable(billingRepository.sumGrossByLabId(labId, startDateTime, endDateTime))
                .orElse(BigDecimal.ZERO);

        long productsSold = Optional.ofNullable(billingRepository.countByLabId(labId, startDateTime, endDateTime))
                .orElse(0L);
        long averageOrderValue = (productsSold > 0) ? totalSales.divide(BigDecimal.valueOf(productsSold), RoundingMode.HALF_UP).longValue() : 0;

        long totalTests = testRepository.countByLabId(labId, startDateTime, endDateTime);
        long totalHealthPackages = healthPackageRepository.countByLabId(labId, startDateTime, endDateTime);
        long totalDoctors = doctorRepository.countByLabId(labId, startDateTime, endDateTime);

        return new StaticDto(
                numberOfPatients, numberOfVisits, collectedSamples, pendingSamples,
                paidVisits, totalSales.longValue(), productsSold, averageOrderValue,
                totalTests, totalHealthPackages, totalDoctors,
                totalDiscounts.longValue(), totalGrossSales.longValue()
        );
    }



    @Transactional(readOnly = true)
    public Page<LabStatisticsDTO> getTransactionDatewise(Long labId,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         User user,
                                                         int page,
                                                         int size) {
        LocalDateTime startDateTime = (startDate != null ? startDate.atStartOfDay() : LocalDate.MIN.atStartOfDay());
        LocalDateTime endDateTime = (endDate != null ? endDate.atTime(23, 59, 59) : LocalDate.MAX.atTime(23, 59, 59));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<BillingEntity> billings = billingRepository.findBillingsByLabAndDateRange(
                labId,
                startDateTime,
                endDateTime,
                pageable
        );

        List<LabStatisticsDTO> dtoList = billings.getContent().stream()
                .map(billing -> {
                    try {
                        return mapBillingToLabStatisticsDTO(billing);
                    } catch (Exception ex) {
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .toList();

        return new PageImpl<>(dtoList, pageable, billings.getTotalElements());
    }

    /**
     * Gets payment details by date range (past bills paid on filter date):
     * - Includes ONLY billings created BEFORE the filter date
     * - AND have transactions where transaction date is in the filter date range
     * - Excludes bills created on the filter date
     * - Excludes bills without transactions
     * Results are sorted by most recent transaction date (descending)
     */
    @Transactional(readOnly = true)
    public Page<LabStatisticsDTO> getPaymentDatewise(Long labId,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     User user,
                                                     int page,
                                                     int size) {
        LocalDateTime startDateTime = (startDate != null ? startDate.atStartOfDay() : LocalDate.MIN.atStartOfDay());
        LocalDateTime endDateTime = (endDate != null ? endDate.atTime(23, 59, 59) : LocalDate.MAX.atTime(23, 59, 59));

        // First, get billings without explicit sorting (we'll sort in memory by most recent transaction date)
        Pageable pageable = PageRequest.of(page, size);

        Page<BillingEntity> billings = billingRepository.findBillingsByLabAndPaymentDateRange(
                labId,
                startDateTime,
                endDateTime,
                pageable
        );

        // Sort by most recent transaction date (if exists), else billing creation date
        List<BillingEntity> sortedBillings = billings.getContent().stream()
                .sorted((b1, b2) -> {
                    LocalDateTime date1 = getMostRecentActivityDate(b1);
                    LocalDateTime date2 = getMostRecentActivityDate(b2);
                    return date2.compareTo(date1); // Descending order (newest first)
                })
                .toList();

        List<LabStatisticsDTO> dtoList = sortedBillings.stream()
                .map(billing -> {
                    try {
                        return mapBillingToLabStatisticsDTO(billing);
                    } catch (Exception ex) {
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .toList();

        return new PageImpl<>(dtoList, pageable, billings.getTotalElements());
    }

    /**
     * Gets the most recent activity date for a billing:
     * - Returns the most recent transaction.createdAt if transactions exist
     * - Returns billing.createdAt if no transactions exist
     */
    private LocalDateTime getMostRecentActivityDate(BillingEntity billing) {
        if (billing.getTransactions() != null && !billing.getTransactions().isEmpty()) {
            return billing.getTransactions().stream()
                    .map(TransactionEntity::getCreatedAt)
                    .filter(date -> date != null)
                    .max(LocalDateTime::compareTo)
                    .orElse(billing.getCreatedAt() != null ? billing.getCreatedAt() : LocalDateTime.MIN);
        }
        return billing.getCreatedAt() != null ? billing.getCreatedAt() : LocalDateTime.MIN;
    }

    private LabStatisticsDTO mapBillingToLabStatisticsDTO(BillingEntity billing) {
        VisitEntity visit = billing.getVisit();
        if (visit == null) {
            throw new IllegalStateException("Billing entity must have an associated visit");
        }



        PatientEntity patient = visit.getPatient();

        LabStatisticsDTO dto = new LabStatisticsDTO();
        dto.setId((int) patient.getId());
        dto.setFirstName(patient.getFirstName());
        dto.setPhone(patient.getPhone());
        dto.setCity(patient.getCity());
        dto.setDateOfBirth(patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : null);
        dto.setAge(patient.getAge());
        dto.setGender(patient.getGender());
        dto.setCreatedBy(patient.getCreatedBy());
        dto.setUpdatedBy(patient.getUpdatedBy());

        // Visit
        LabStatisticsDTO.VisitDTO visitDTO = new LabStatisticsDTO.VisitDTO();
        visitDTO.setVisitId(visit.getVisitId().intValue());
        visitDTO.setVisitDate(visit.getVisitDate().toString());
        visitDTO.setVisitType(visit.getVisitType());
        visitDTO.setVisitStatus(visit.getVisitStatus());
        visitDTO.setVisitDescription(visit.getVisitDescription());
        visitDTO.setCreatedBy(visit.getCreatedBy());
        visitDTO.setUpdatedBy(visit.getUpdatedBy());
        visitDTO.setVisitCancellationReason(visit.getVisitCancellationReason());
        visitDTO.setVisitCancellationBy(visit.getVisitCancellationBy());
        visitDTO.setVisitCancellationDate(String.valueOf(visit.getVisitCancellationDate()));
        visitDTO.setVisitCancellationDate(String.valueOf(visit.getVisitCancellationDate()));

        //find test name and package name and doctor name
        if (visit.getTests() != null && !visit.getTests().isEmpty()) {
            List<String> testNames = visit.getTests().stream()
                    .map(Test::getName)
                    .toList();
            visitDTO.setTestNames(testNames);

            List<Long> testIds = visit.getTests().stream()
                    .map(Test::getId)
                    .map(Long::valueOf)
                    .toList();
            visitDTO.setTestIds(testIds);
        } else {
            visitDTO.setTestNames(List.of());
            visitDTO.setTestIds(List.of());
        }

        if (visit.getPackages() != null && !visit.getPackages().isEmpty()) {
            List<Long> packageIds = visit.getPackages().stream()
                    .map(HealthPackage::getId)
                    .map(Long::valueOf)
                    .toList();
            visitDTO.setPackageIds(packageIds);

            List<Object> packageNames = visit.getPackages().stream()
                    .map(HealthPackage::getPackageName)
                    .map(name -> (Object) name)
                    .toList();
            visitDTO.setPackageNames(packageNames);
        } else {
            visitDTO.setPackageIds(List.of());
            visitDTO.setPackageNames(List.of());
        }

        if (visit.getDoctor() != null) {
            visitDTO.setDoctorId((int) visit.getDoctor().getId());
            visitDTO.setDoctorName(visit.getDoctor().getName());
        }

        // Map test results
        if (visit.getTestResults() != null && !visit.getTestResults().isEmpty()) {
            List<LabStatisticsDTO.TestResultDTO> testResultDTOs = visit.getTestResults().stream().map(vtr -> {
                LabStatisticsDTO.TestResultDTO tr = new LabStatisticsDTO.TestResultDTO();
                tr.setId(vtr.getId().intValue());
                tr.setTestId(vtr.getTest() != null ? (int) vtr.getTest().getId() : 0);
                if (vtr.getTest() != null) {
                    tr.setTestName(vtr.getTest().getName());
                    tr.setCategory(vtr.getTest().getCategory());
                }
                tr.setFilled(Boolean.TRUE.equals(vtr.getIsFilled()));
                tr.setReportStatus(vtr.getReportStatus());
                tr.setCreatedBy(vtr.getCreatedBy());
                tr.setUpdatedBy(vtr.getUpdatedBy());
                tr.setCreatedAt(vtr.getCreatedAt() != null ? vtr.getCreatedAt().toString() : null);
                tr.setUpdatedAt(vtr.getUpdatedAt() != null ? vtr.getUpdatedAt().toString() : null);
                return tr;
            }).toList();
            visitDTO.setTestResult(testResultDTOs);
        } else {
            visitDTO.setTestResult(List.of());
        }


        //listofeachtestdiscount


        // Billing
        LabStatisticsDTO.BillingDTO billingDTO = new LabStatisticsDTO.BillingDTO();
        billingDTO.setBillingId(billing.getId().intValue());
        billingDTO.setTotalAmount(billing.getTotalAmount().doubleValue());
        billingDTO.setPaymentStatus(billing.getPaymentStatus());
        billingDTO.setPaymentMethod(billing.getPaymentMethod());
        billingDTO.setPaymentDate(billing.getPaymentDate());
        billingDTO.setDiscount(billing.getDiscount().doubleValue());
        billingDTO.setNetAmount(billing.getNetAmount().doubleValue());

        billingDTO.setCreatedBy(billing.getCreatedBy());
        billingDTO.setUpdatedBy(billing.getUpdatedBy());
        billingDTO.setBillingTime(billing.getCreatedAt() != null ? billing.getCreatedAt().toLocalTime().toString() : null);
        billingDTO.setBillingDate(billing.getCreatedAt() != null ? billing.getCreatedAt().toLocalDate().toString() : null);
        billingDTO.setCreatedAt(billing.getCreatedAt() != null ? billing.getCreatedAt().toString() : null);
        billingDTO.setUpdatedAt(billing.getUpdatedAt() != null ? billing.getUpdatedAt().toString() : null);
        billingDTO.setDiscountReason(billing.getDiscountReason());

        // Calculate received_amount and due_amount from transactions if they exist
        BigDecimal totalReceivedAmount = BigDecimal.ZERO;
        BigDecimal totalDueAmount = BigDecimal.ZERO;
        
        if (billing.getTransactions() != null && !billing.getTransactions().isEmpty()) {
            for (TransactionEntity transaction : billing.getTransactions()) {
                if (transaction.getReceivedAmount() != null) {
                    totalReceivedAmount = totalReceivedAmount.add(transaction.getReceivedAmount());
                }
                if (transaction.getDueAmount() != null) {
                    totalDueAmount = totalDueAmount.add(transaction.getDueAmount());
                }
            }
        } else {
            // If no transactions, use billing's own received_amount and due_amount if available
            totalReceivedAmount = billing.getReceivedAmount() != null ? billing.getReceivedAmount() : BigDecimal.ZERO;
            totalDueAmount = billing.getDueAmount() != null ? billing.getDueAmount() : BigDecimal.ZERO;
        }
        
        billingDTO.setReceived_amount(totalReceivedAmount.doubleValue());
        billingDTO.setDue_amount(totalDueAmount.doubleValue());

        // Map Transactions - handle case where billing has no transactions
        List<LabStatisticsDTO.TransactionDTO> transactionDTOs;
        if (billing.getTransactions() != null && !billing.getTransactions().isEmpty()) {
            transactionDTOs = billing.getTransactions().stream()
                    .map(transaction -> {
                        LabStatisticsDTO.TransactionDTO trDTO = new LabStatisticsDTO.TransactionDTO();
                        trDTO.setId(transaction.getId().intValue());
                        trDTO.setPayment_method(transaction.getPaymentMethod());
                        trDTO.setUpi_id(transaction.getUpiId());
                        trDTO.setUpi_amount(transaction.getUpiAmount() != null ? transaction.getUpiAmount().doubleValue() : 0.0);
                        trDTO.setCard_amount(transaction.getCardAmount() != null ? transaction.getCardAmount().doubleValue() : 0.0);
                        trDTO.setCash_amount(transaction.getCashAmount() != null ? transaction.getCashAmount().doubleValue() : 0.0);
                        trDTO.setReceived_amount(transaction.getReceivedAmount() != null ? transaction.getReceivedAmount().doubleValue() : 0.0);
                        trDTO.setRefund_amount(transaction.getRefundAmount() != null ? transaction.getRefundAmount().doubleValue() : 0.0);
                        trDTO.setDue_amount(transaction.getDueAmount() != null ? transaction.getDueAmount().doubleValue() : 0.0);
                        trDTO.setPayment_date(transaction.getPaymentDate());
                        trDTO.setCreatedBy(transaction.getCreatedBy());
                        trDTO.setCreated_at(transaction.getCreatedAt() != null ? transaction.getCreatedAt().toString() : null);
                        trDTO.setBilling_id(billing.getId().intValue());
                        trDTO.setRemarks(transaction.getRemarks());
                        return trDTO;
                    })
                    .toList();
        } else {
            // No transactions - return empty array to maintain response structure
            transactionDTOs = List.of();
        }
        
        billingDTO.setTransactions(transactionDTOs);

        // Map test discounts
        if (billing.getTestDiscounts() != null && !billing.getTestDiscounts().isEmpty()) {
            List<LabStatisticsDTO.TestDiscountDTO> discountDTOs = billing.getTestDiscounts().stream().map(td -> {
                LabStatisticsDTO.TestDiscountDTO dd = new LabStatisticsDTO.TestDiscountDTO();
                dd.setId(td.getId().intValue());
                dd.setDiscountAmount(td.getDiscountAmount() != null ? td.getDiscountAmount().doubleValue() : 0.0);
                dd.setDiscountPercent(td.getDiscountPercent() != null ? td.getDiscountPercent().doubleValue() : 0.0);
                dd.setFinalPrice(td.getFinalPrice() != null ? td.getFinalPrice().doubleValue() : 0.0);
                if (td.getTestId() != null) {
                    testRepository.findById(td.getTestId()).ifPresent(test -> {
                        dd.setTestName(test.getName());
                        dd.setCategory(test.getCategory());
                    });
                }
                dd.setCreatedBy(td.getCreatedBy());
                dd.setUpdatedBy(td.getUpdatedBy());
                return dd;
            }).toList();
            visitDTO.setListofeachtestdiscount(discountDTOs);
        } else {
            visitDTO.setListofeachtestdiscount(List.of());
        }

        visitDTO.setBilling(billingDTO);
        dto.setVisit(visitDTO);

        return dto;
    }




}
