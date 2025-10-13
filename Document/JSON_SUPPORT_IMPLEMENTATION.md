# JSON Support Implementation for Test References

## Overview
This implementation adds JSON support to the test reference system, allowing storage of structured report data in PostgreSQL using the `jsonb` column type. This enables efficient storage and querying of complex medical report data.

## Database Changes

### New Columns Added
1. **`remarks`** - VARCHAR(500) - Additional remarks or notes
2. **`report_json`** - JSONB - Structured JSON data for report information

### Database Migration
Run the migration script: `database_migration_add_json_support.sql`

```sql
-- Add the new columns
ALTER TABLE super_admin_test_referance 
ADD COLUMN IF NOT EXISTS remarks VARCHAR(500);

ALTER TABLE super_admin_test_referance 
ADD COLUMN IF NOT EXISTS report_json JSONB;

-- Create index for better JSON query performance
CREATE INDEX IF NOT EXISTS idx_super_admin_test_referance_report_json 
ON super_admin_test_referance USING GIN (report_json);
```

## Entity Changes

### SuperAdminReferanceEntity
Added two new fields:
```java
@Column(nullable = true)
private String remarks;

@Column(columnDefinition = "jsonb")
private String reportJson;
```

## API Endpoints

### 1. Upload CSV with JSON Support
**POST** `/super-admin/referance-and-test/upload-test-referance`

**CSV Format:**
```csv
Category,Test Name,Test Description,Units,Gender,Min Reference Range,Max Reference Range,Age Min,Min Age Unit,Age Max,Max Age Unit,Remarks,ReportJson
RADIOLOGY,Chest X-ray,Chest X-ray Description,N/A,M/F,,,,0,YEARS,100,YEARS,,"{""observations"": ""Lung opacity noted"", ""size"": ""3cm"", ""images"": [""image1.png"", ""image2.png""], ""notes"": ""Follow-up in 2 weeks""}"
```

### 2. Create Test Reference with JSON
**POST** `/super-admin/referance-and-test/test-referance/create`

**Request Body:**
```json
{
  "category": "RADIOLOGY",
  "testName": "USG Carotid Doppler",
  "testDescription": "Ultrasound examination of carotid arteries",
  "units": "N/A",
  "gender": "M/F",
  "minReferenceRange": null,
  "maxReferenceRange": null,
  "ageMin": 0,
  "minAgeUnit": "YEARS",
  "ageMax": 100,
  "maxAgeUnit": "YEARS",
  "remarks": "Routine screening",
  "reportJson": "{\"USG_Carotid_Doppler\":[{\"Artery_Segment\":\"Common Carotid – Proximal\",\"Peak_Systolic_cm_s\":\"\",\"End_Diastolic_cm_s\":\"\",\"Spectrum\":\"\",\"Stenosis_Percent\":\"\"},{\"Artery_Segment\":\"Common Carotid – Mid\",\"Peak_Systolic_cm_s\":\"\",\"End_Diastolic_cm_s\":\"\",\"Spectrum\":\"\",\"Stenosis_Percent\":\"\"}]}"
}
```

### 3. Update Test Reference JSON
**PUT** `/super-admin/referance-and-test/test-referance/{id}/json`

**Request Body:**
```json
"{\"observations\": \"Updated findings\", \"size\": \"2cm\", \"images\": [\"new_image.png\"], \"notes\": \"Updated notes\"}"
```

### 4. Search by JSON Content
**GET** `/super-admin/referance-and-test/test-referance/search-by-json?jsonKey=observations&jsonValue=opacity`

### 5. Get Test Reference by ID
**GET** `/super-admin/referance-and-test/test-referance/{id}`

### 6. Download CSV with JSON
**GET** `/super-admin/referance-and-test/test-referance/download`

## JSON Data Examples

### Radiology Report Example
```json
{
  "observations": "Lung opacity noted",
  "size": "3cm",
  "images": ["image1.png", "image2.png"],
  "notes": "Follow-up in 2 weeks"
}
```

### USG Carotid Doppler Example
```json
{
  "USG_Carotid_Doppler": [
    {
      "Artery_Segment": "Common Carotid – Proximal",
      "Peak_Systolic_cm_s": "",
      "End_Diastolic_cm_s": "",
      "Spectrum": "",
      "Stenosis_Percent": ""
    },
    {
      "Artery_Segment": "Common Carotid – Mid",
      "Peak_Systolic_cm_s": "",
      "End_Diastolic_cm_s": "",
      "Spectrum": "",
      "Stenosis_Percent": ""
    }
  ]
}
```

## Features

### 1. JSON Validation
- Basic JSON structure validation (starts with `{` and ends with `}`)
- Invalid JSON is logged and stored as null
- Can be extended with more sophisticated validation using Jackson ObjectMapper

### 2. CSV Support
- Upload CSV files with ReportJson column
- Download CSV files including ReportJson data
- Proper escaping of JSON data in CSV format

### 3. Search Capabilities
- Search by JSON key
- Search by JSON value
- Case-insensitive search within JSON content

### 4. Database Optimization
- GIN index on JSONB column for efficient querying
- PostgreSQL JSONB type for optimal storage and performance

## Usage Examples

### 1. Upload CSV with JSON Data
```bash
curl -X POST "http://localhost:8080/super-admin/referance-and-test/upload-test-referance" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test_references_with_json.csv"
```

### 2. Create Test Reference with JSON
```bash
curl -X POST "http://localhost:8080/super-admin/referance-and-test/test-referance/create" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "RADIOLOGY",
    "testName": "Chest X-ray",
    "testDescription": "Chest X-ray Description",
    "units": "N/A",
    "gender": "M/F",
    "ageMin": 0,
    "minAgeUnit": "YEARS",
    "ageMax": 100,
    "maxAgeUnit": "YEARS",
    "remarks": "Routine screening",
    "reportJson": "{\"observations\": \"Lung opacity noted\", \"size\": \"3cm\"}"
  }'
```

### 3. Search by JSON Content
```bash
curl -X GET "http://localhost:8080/super-admin/referance-and-test/test-referance/search-by-json?jsonKey=observations&jsonValue=opacity" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Benefits

1. **Flexible Data Storage**: Store complex, structured report data without schema changes
2. **Efficient Querying**: PostgreSQL JSONB provides fast JSON operations
3. **Backward Compatibility**: Existing functionality remains unchanged
4. **CSV Integration**: Seamless upload/download of JSON data via CSV
5. **Search Capabilities**: Find records based on JSON content
6. **Validation**: Basic JSON validation prevents invalid data storage

## Future Enhancements

1. **Advanced JSON Validation**: Use Jackson ObjectMapper for comprehensive validation
2. **JSON Schema Validation**: Implement schema-based validation for specific report types
3. **Advanced Search**: Full-text search within JSON content
4. **JSON Query API**: Direct PostgreSQL JSON query support
5. **Report Templates**: Predefined JSON templates for common report types

