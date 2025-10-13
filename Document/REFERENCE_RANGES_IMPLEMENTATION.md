# Reference Ranges JSON Array Implementation

## Overview
This implementation adds support for storing multiple reference ranges as JSON arrays in the test reference system. This allows for more granular and flexible reference range definitions based on different age groups and gender specifications.

## Database Changes

### New Column Added
- **`reference_ranges`** - TEXT - JSON array containing multiple reference ranges

### Database Migration
Run the migration script: `database_migration_add_reference_ranges.sql`

```sql
-- Add the new column
ALTER TABLE super_admin_test_referance 
ADD COLUMN IF NOT EXISTS reference_ranges TEXT;

-- Create index for better search performance
CREATE INDEX IF NOT EXISTS idx_super_admin_test_referance_reference_ranges_text 
ON super_admin_test_referance (reference_ranges);
```

## Entity Changes

### SuperAdminReferanceEntity
Added new field:
```java
@Column(columnDefinition = "text")
private String referenceRanges;
```

## JSON Structure

### Reference Ranges Format
The `referenceRanges` field stores a JSON array with the following structure:

```json
[
  {
    "Gender": "MF",
    "AgeMin": 0,
    "AgeMinUnit": "MONTHS",
    "AgeMax": 1,
    "AgeMaxUnit": "MONTHS",
    "ReferenceRange": "100 - 120 fl"
  },
  {
    "Gender": "MF",
    "AgeMin": 1,
    "AgeMinUnit": "MONTHS",
    "AgeMax": 1,
    "AgeMinUnit": "YEARS",
    "ReferenceRange": "90 - 100 fl"
  }
]
```

### Field Descriptions
- **Gender**: "M", "F", or "MF" (Male, Female, or Both)
- **AgeMin**: Minimum age value
- **AgeMinUnit**: Unit for minimum age ("MONTHS", "YEARS", "DAYS")
- **AgeMax**: Maximum age value
- **AgeMaxUnit**: Unit for maximum age ("MONTHS", "YEARS", "DAYS")
- **ReferenceRange**: The actual reference range value (e.g., "100 - 120 fl")

## API Endpoints

### 1. Upload CSV with Reference Ranges
**POST** `/super-admin/referance-and-test/upload-test-referance`

**CSV Format:**
```csv
Category,Test Name,Test Description,Units,Gender,Min Reference Range,Max Reference Range,Age Min,Min Age Unit,Age Max,Max Age Unit,Remarks,ReportJson,ReferenceRanges
LABORATORY,MCV,Mean Corpuscular Volume,fl,M/F,80,100,0,YEARS,100,YEARS,Red blood cell index,"{""method"": ""Cyanmethemoglobin""}","[{""Gender"": ""MF"", ""AgeMin"": 0, ""AgeMinUnit"": ""MONTHS"", ""AgeMax"": 1, ""AgeMaxUnit"": ""MONTHS"", ""ReferenceRange"": ""100 - 120 fl""}]"
```

### 2. Update Reference Ranges
**PUT** `/super-admin/referance-and-test/test-referance/{id}/reference-ranges`

**Request Body:**
```json
[
  {
    "Gender": "M",
    "AgeMin": 18,
    "AgeMinUnit": "YEARS",
    "AgeMax": 65,
    "AgeMaxUnit": "YEARS",
    "ReferenceRange": "13.8 - 17.2 g/dL"
  },
  {
    "Gender": "F",
    "AgeMin": 18,
    "AgeMinUnit": "YEARS",
    "AgeMax": 65,
    "AgeMaxUnit": "YEARS",
    "ReferenceRange": "12.1 - 15.1 g/dL"
  }
]
```

### 3. Search by Reference Ranges
**GET** `/super-admin/referance-and-test/test-referance/search-by-reference-ranges?gender=MF&ageMin=0&ageMax=1`

### 4. Get Test Reference by ID
**GET** `/super-admin/referance-and-test/test-referance/{id}`

### 5. Download CSV with Reference Ranges
**GET** `/super-admin/referance-and-test/test-referance/download`

## Features

### 1. JSON Array Validation
- Validates that the input is a proper JSON array (starts with `[` and ends with `]`)
- Invalid JSON arrays are logged and stored as null
- Can be extended with more sophisticated validation using Jackson ObjectMapper

### 2. CSV Support
- Upload CSV files with ReferenceRanges column
- Download CSV files including ReferenceRanges data
- Proper escaping of JSON array data in CSV format

### 3. Search Capabilities
- Search by gender within reference ranges
- Search by age minimum within reference ranges
- Search by age maximum within reference ranges
- Case-insensitive search within JSON array content

### 4. Database Optimization
- TEXT column for compatibility with Hibernate
- Index on reference_ranges column for efficient searching
- Can be converted to JSONB later for better performance

## Usage Examples

### 1. Upload CSV with Reference Ranges
```bash
curl -X POST "http://localhost:8080/super-admin/referance-and-test/upload-test-referance" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@sample_test_references_with_reference_ranges.csv"
```

### 2. Update Reference Ranges
```bash
curl -X PUT "http://localhost:8080/super-admin/referance-and-test/test-referance/1/reference-ranges" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "Gender": "MF",
      "AgeMin": 0,
      "AgeMinUnit": "MONTHS",
      "AgeMax": 1,
      "AgeMaxUnit": "MONTHS",
      "ReferenceRange": "100 - 120 fl"
    }
  ]'
```

### 3. Search by Reference Ranges
```bash
curl -X GET "http://localhost:8080/super-admin/referance-and-test/test-referance/search-by-reference-ranges?gender=MF&ageMin=0" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Real-World Examples

### Hemoglobin Reference Ranges
```json
[
  {
    "Gender": "M",
    "AgeMin": 0,
    "AgeMinUnit": "MONTHS",
    "AgeMax": 2,
    "AgeMinUnit": "YEARS",
    "ReferenceRange": "9.0 - 14.0 g/dL"
  },
  {
    "Gender": "F",
    "AgeMin": 0,
    "AgeMinUnit": "MONTHS",
    "AgeMax": 2,
    "AgeMinUnit": "YEARS",
    "ReferenceRange": "9.0 - 14.0 g/dL"
  },
  {
    "Gender": "M",
    "AgeMin": 18,
    "AgeMinUnit": "YEARS",
    "AgeMax": 100,
    "AgeMaxUnit": "YEARS",
    "ReferenceRange": "13.8 - 17.2 g/dL"
  },
  {
    "Gender": "F",
    "AgeMin": 18,
    "AgeMinUnit": "YEARS",
    "AgeMax": 100,
    "AgeMaxUnit": "YEARS",
    "ReferenceRange": "12.1 - 15.1 g/dL"
  }
]
```

### MCV (Mean Corpuscular Volume) Reference Ranges
```json
[
  {
    "Gender": "MF",
    "AgeMin": 0,
    "AgeMinUnit": "MONTHS",
    "AgeMax": 1,
    "AgeMaxUnit": "MONTHS",
    "ReferenceRange": "100 - 120 fl"
  },
  {
    "Gender": "MF",
    "AgeMin": 1,
    "AgeMinUnit": "MONTHS",
    "AgeMax": 1,
    "AgeMinUnit": "YEARS",
    "ReferenceRange": "90 - 100 fl"
  },
  {
    "Gender": "MF",
    "AgeMin": 1,
    "AgeMinUnit": "YEARS",
    "AgeMax": 100,
    "AgeMaxUnit": "YEARS",
    "ReferenceRange": "80 - 100 fl"
  }
]
```

## Benefits

1. **Flexible Reference Ranges**: Store multiple reference ranges for different age groups and genders
2. **Granular Control**: Define specific ranges for different demographics
3. **Easy Querying**: Search and filter by gender, age ranges, and reference values
4. **CSV Integration**: Seamless upload/download of reference ranges data
5. **Backward Compatibility**: Existing functionality remains unchanged
6. **Extensible**: Easy to add new fields or modify the structure

## Future Enhancements

1. **Advanced JSON Validation**: Use Jackson ObjectMapper for comprehensive validation
2. **Reference Range Templates**: Predefined templates for common test types
3. **Age Range Validation**: Validate that age ranges don't overlap
4. **Unit Conversion**: Automatic conversion between different age units
5. **Reference Range Calculator**: Calculate appropriate ranges based on demographics
6. **Audit Trail**: Track changes to reference ranges over time

## Migration Notes

1. **Run the migration script** to add the new column
2. **Update existing data** if needed to populate reference ranges
3. **Test CSV upload/download** with the new column
4. **Verify search functionality** works as expected
5. **Consider converting to JSONB** for better performance if needed

The implementation provides a robust foundation for managing complex reference ranges while maintaining compatibility with existing systems.

