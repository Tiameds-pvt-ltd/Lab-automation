package tiameds.com.tiameds.dto.lab;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StaticDto {

    private long numberOfPatients;
    private long numberOfVisits;
    private long collectedSamples;
    private long pendingSamples;
    private long paidVisits;
    private long totalSales;
    private long productsSold;
    private long averageOrderValue;
    private long totalTests;
    private long totalHealthPackages;
    private long totalDoctors;
    private long totalDiscounts;
    private long totalGrossSales;

}
