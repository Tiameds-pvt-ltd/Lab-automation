# Test JSON Fields in API Response

## Summary of Changes Made

### ✅ **Fixed Issues:**

1. **Added JSON fields to TestReferenceDTO:**
   - Added `reportJson` field
   - Added `referenceRanges` field

2. **Updated DTO mapping methods:**
   - Updated `getAllTestReferences()` method to include JSON fields
   - Updated `getTestReferenceByTestName()` method to include JSON fields

### ✅ **What's Now Working:**

The API endpoint `GET /api/lab/{labId}/test-reference/{labId}` will now return:

```json
{
  "success": true,
  "message": "Test references fetched successfully",
  "data": [
    {
      "id": 123,
      "category": "LABORATORY",
      "testName": "Complete Blood Count",
      "testDescription": "Full blood count analysis",
      "units": "count/μL",
      "gender": "MF",
      "minReferenceRange": 4.5,
      "maxReferenceRange": 11.0,
      "ageMin": 0,
      "minAgeUnit": "YEARS",
      "ageMax": 100,
      "maxAgeUnit": "YEARS",
      "createdBy": "admin",
      "updatedBy": "admin",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00",
      "reportJson": "{\"parameters\":{\"hemoglobin\":{\"value\":\"\",\"unit\":\"g/dL\",\"normal_range\":\"12-16\"},\"white_blood_cells\":{\"value\":\"\",\"unit\":\"cells/μL\",\"normal_range\":\"4000-11000\"},\"platelets\":{\"value\":\"\",\"unit\":\"cells/μL\",\"normal_range\":\"150000-450000\"}},\"interpretation\":\"Normal range\"}",
      "referenceRanges": "[{\"Gender\": \"M\", \"AgeMin\": 0, \"AgeMinUnit\": \"YEARS\", \"AgeMax\": 18, \"AgeMaxUnit\": \"YEARS\", \"ReferenceRange\": \"4.5 - 11.0 x 10³/μL\"}, {\"Gender\": \"F\", \"AgeMin\": 0, \"AgeMinUnit\": \"YEARS\", \"AgeMax\": 18, \"AgeMaxUnit\": \"YEARS\", \"ReferenceRange\": \"4.5 - 11.0 x 10³/μL\"}, {\"Gender\": \"MF\", \"AgeMin\": 18, \"AgeMinUnit\": \"YEARS\", \"AgeMax\": 100, \"AgeMaxUnit\": \"YEARS\", \"ReferenceRange\": \"4.5 - 11.0 x 10³/μL\"}]"
    }
  ]
}
```

### ✅ **Testing Steps:**

1. **Upload CSV with JSON data:**
   ```bash
   POST /api/lab/{labId}/csv/upload
   Content-Type: multipart/form-data
   Authorization: Bearer {token}
   
   Form Data:
   - file: sample_test_references_with_reference_ranges.csv
   ```

2. **Retrieve test references:**
   ```bash
   GET /api/lab/{labId}/test-reference/{labId}
   Authorization: Bearer {token}
   ```

3. **Verify JSON fields are present:**
   - Check that `reportJson` field contains valid JSON object
   - Check that `referenceRanges` field contains valid JSON array
   - Verify the data matches what was uploaded in the CSV

### ✅ **Expected Behavior:**

- ✅ JSON fields are now included in API responses
- ✅ Data is properly mapped from entity to DTO
- ✅ Both `getAllTestReferences` and `getTestReferenceByTestName` methods work
- ✅ CSV upload continues to work with JSON validation
- ✅ Invalid JSON is handled gracefully (stored as null)

The issue has been resolved! The `ReportJson` and `ReferenceRanges` fields will now be included in the API response when fetching test references.

