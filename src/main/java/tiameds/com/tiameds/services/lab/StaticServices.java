package tiameds.com.tiameds.services.lab;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.lab.StaticDto;
import tiameds.com.tiameds.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class StaticServices {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final BillingRepository billingRepository;
    private final TestRepository testRepository;
    private final HealthPackageRepository healthPackageRepository;

    public StaticServices(
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            VisitRepository visitRepository,
            BillingRepository billingRepository,
            TestRepository testRepository,
            HealthPackageRepository healthPackageRepository) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.billingRepository = billingRepository;
        this.testRepository = testRepository;
        this.healthPackageRepository = healthPackageRepository;
    }

    @Transactional(readOnly = true)
    public StaticDto getStaticData(Long labId, String startDate, String endDate) {
        LocalDateTime startDateTime = LocalDate.parse(startDate.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        LocalDateTime endDateTime = LocalDate.parse(endDate.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atTime(23, 59, 59);
        // Fetch aggregated data in one query if possible
        long numberOfPatients = patientRepository.countByLabIdAndCreatedAtBetween(labId, startDateTime, endDateTime);
        long numberOfVisits = visitRepository.countByLabIdAndCreatedAtBetween(labId, startDateTime, endDateTime);

        // Use an Enum or Constants for status values
        long collectedSamples = visitRepository.countByLabIdAndStatus(labId, "Collected", startDateTime, endDateTime);
        long pendingSamples = visitRepository.countByLabIdAndStatus(labId, "Pending", startDateTime, endDateTime);
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


}
