package tiameds.com.tiameds.entity;

public enum Gender {
    M("M"),
    F("F"),
    MF("M/F");

    private final String displayValue;

    Gender(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}