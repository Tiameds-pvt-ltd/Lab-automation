package tiameds.com.tiameds.repository;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tiameds.com.tiameds.entity.Gender;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        return (gender == null) ? null : gender.getDisplayValue();
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        for (Gender g : Gender.values()) {
            if (g.getDisplayValue().equals(dbData)) {
                return g;
            }
        }
        return null; // or throw exception if invalid
    }
}
