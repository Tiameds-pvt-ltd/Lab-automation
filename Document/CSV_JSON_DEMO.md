# CSV JSON Processing Demo

## How to Test the Implementation

### 1. Using the API Endpoint
You can test the CSV upload functionality using the existing API endpoint:

```bash
POST /api/lab/{labId}/csv/upload
Content-Type: multipart/form-data
Authorization: Bearer {your-jwt-token}

Form Data:
- file: sample_test_references_with_reference_ranges.csv
```

### 2. Sample CSV Data
The implementation now supports CSV files with JSON columns. Here's an example of the expected format:

```csv
Category,Test Name,Test Description,Units,Gender,Min Reference Range,Max Reference Range,Age Min,Min Age Unit,Age Max,Max Age Unit,Remarks,ReportJson,ReferenceRanges
RADIOLOGY,Chest X-ray,Chest X-ray examination,N/A,M/F,,,,0,YEARS,100,YEARS,Routine screening,"{""observations"": ""Lung opacity noted"", ""size"": ""3cm"", ""images"": [""image1.png"", ""image2.png""], ""notes"": ""Follow-up in 2 weeks""}",""
LABORATORY,Complete Blood Count,Full blood count analysis,count/μL,M/F,4.5,11.0,0,YEARS,100,YEARS,Standard lab test,"{""parameters"":{""hemoglobin"":{""value"":"""",""unit"":""g/dL"",""normal_range"":""12-16""},""white_blood_cells"":{""value"":"""",""unit"":""cells/μL"",""normal_range"":""4000-11000""},""platelets"":{""value"":"""",""unit"":""cells/μL"",""normal_range"":""150000-450000""}},""interpretation"":""Normal range""}","[{""Gender"": ""M"", ""AgeMin"": 0, ""AgeMinUnit"": ""YEARS"", ""AgeMax"": 18, ""AgeMaxUnit"": ""YEARS"", ""ReferenceRange"": ""4.5 - 11.0 x 10³/μL""}, {""Gender"": ""F"", ""AgeMin"": 0, ""AgeMinUnit"": ""YEARS"", ""AgeMax"": 18, ""AgeMaxUnit"": ""YEARS"", ""ReferenceRange"": ""4.5 - 11.0 x 10³/μL""}, {""Gender"": ""MF"", ""AgeMin"": 18, ""AgeMinUnit"": ""YEARS"", ""AgeMax"": 100, ""AgeMaxUnit"": ""YEARS"", ""ReferenceRange"": ""4.5 - 11.0 x 10³/μL""}]"
```

### 3. JSON Column Validation
The system validates both JSON columns:

- **ReportJson**: Must be a valid JSON object (starts with `{`, ends with `}`)
- **ReferenceRanges**: Must be a valid JSON array (starts with `[`, ends with `]`)

### 4. Error Handling
If invalid JSON is found:
- A warning is logged with the record number and invalid content
- The field is set to `null` in the database
- Processing continues with other records

### 5. Database Storage
The JSON data is stored in PostgreSQL as `jsonb` columns:
- `report_json` (jsonb)
- `reference_ranges` (jsonb)

### 6. Testing the Implementation
1. Start the Spring Boot application
2. Use the provided sample CSV file: `Document/sample_test_references_with_reference_ranges.csv`
3. Upload via the API endpoint
4. Check the database to verify JSON data is stored correctly
5. Verify that invalid JSON is handled gracefully

## Expected Behavior
- ✅ Valid JSON in both columns is stored successfully
- ✅ Invalid JSON is logged as warnings and stored as null
- ✅ Empty JSON columns are stored as null
- ✅ All other CSV processing continues normally
- ✅ Database constraints are maintained
