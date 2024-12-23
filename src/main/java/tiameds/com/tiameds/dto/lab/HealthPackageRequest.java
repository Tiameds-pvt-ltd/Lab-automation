package tiameds.com.tiameds.dto.lab;

import java.util.Set;

public class HealthPackageRequest {

    private String packageName;
    private double price;
    private double discount;
    private Set<Long> testIds; // List of test IDs to be associated with the package

    // Getters and Setters
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Set<Long> getTestIds() {
        return testIds;
    }

    public void setTestIds(Set<Long> testIds) {
        this.testIds = testIds;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }
}

