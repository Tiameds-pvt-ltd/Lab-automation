#!/bin/bash

# Test script for Reference Ranges functionality in Test Reference API
# Make sure to replace YOUR_TOKEN with actual JWT token

BASE_URL="http://localhost:8080/super-admin/referance-and-test"
TOKEN="YOUR_TOKEN"

echo "=== Testing Reference Ranges Functionality for Test References ==="
echo

# Test 1: Create a test reference with reference ranges JSON array
echo "1. Creating test reference with reference ranges JSON array..."
curl -X POST "$BASE_URL/test-referance/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "LABORATORY",
    "testName": "MCV",
    "testDescription": "Mean Corpuscular Volume",
    "units": "fl",
    "gender": "M/F",
    "ageMin": 0,
    "minAgeUnit": "YEARS",
    "ageMax": 100,
    "maxAgeUnit": "YEARS",
    "remarks": "Red blood cell index",
    "reportJson": "{\"method\": \"Cyanmethemoglobin\", \"significance\": \"Size of red blood cells\"}",
    "referenceRanges": "[{\"Gender\": \"MF\", \"AgeMin\": 0, \"AgeMinUnit\": \"MONTHS\", \"AgeMax\": 1, \"AgeMaxUnit\": \"MONTHS\", \"ReferenceRange\": \"100 - 120 fl\"}, {\"Gender\": \"MF\", \"AgeMin\": 1, \"AgeMinUnit\": \"MONTHS\", \"AgeMax\": 1, \"AgeMinUnit\": \"YEARS\", \"ReferenceRange\": \"90 - 100 fl\"}, {\"Gender\": \"MF\", \"AgeMin\": 1, \"AgeMinUnit\": \"YEARS\", \"AgeMax\": 100, \"AgeMaxUnit\": \"YEARS\", \"ReferenceRange\": \"80 - 100 fl\"}]"
  }' | jq '.'

echo -e "\n"

# Test 2: Create another test reference with complex reference ranges
echo "2. Creating test reference with complex reference ranges (Hemoglobin)..."
curl -X POST "$BASE_URL/test-referance/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "LABORATORY",
    "testName": "Hemoglobin",
    "testDescription": "Blood hemoglobin level",
    "units": "g/dL",
    "gender": "M/F",
    "ageMin": 0,
    "minAgeUnit": "YEARS",
    "ageMax": 100,
    "maxAgeUnit": "YEARS",
    "remarks": "Blood test",
    "reportJson": "{\"method\": \"Cyanmethemoglobin\", \"specimen\": \"Whole blood\"}",
    "referenceRanges": "[{\"Gender\": \"M\", \"AgeMin\": 0, \"AgeMinUnit\": \"MONTHS\", \"AgeMax\": 2, \"AgeMinUnit\": \"YEARS\", \"ReferenceRange\": \"9.0 - 14.0 g/dL\"}, {\"Gender\": \"F\", \"AgeMin\": 0, \"AgeMinUnit\": \"MONTHS\", \"AgeMax\": 2, \"AgeMinUnit\": \"YEARS\", \"ReferenceRange\": \"9.0 - 14.0 g/dL\"}, {\"Gender\": \"M\", \"AgeMin\": 18, \"AgeMinUnit\": \"YEARS\", \"AgeMax\": 100, \"AgeMaxUnit\": \"YEARS\", \"ReferenceRange\": \"13.8 - 17.2 g/dL\"}, {\"Gender\": \"F\", \"AgeMin\": 18, \"AgeMinUnit\": \"YEARS\", \"AgeMax\": 100, \"AgeMaxUnit\": \"YEARS\", \"ReferenceRange\": \"12.1 - 15.1 g/dL\"}]"
  }' | jq '.'

echo -e "\n"

# Test 3: Get all test references
echo "3. Getting all test references..."
curl -X GET "$BASE_URL/test-referance" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n"

# Test 4: Search by reference ranges content (gender)
echo "4. Searching test references by reference ranges content (gender: MF)..."
curl -X GET "$BASE_URL/test-referance/search-by-reference-ranges?gender=MF" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n"

# Test 5: Search by reference ranges content (age)
echo "5. Searching test references by reference ranges content (ageMin: 0)..."
curl -X GET "$BASE_URL/test-referance/search-by-reference-ranges?ageMin=0" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n"

# Test 6: Search by reference ranges content (combined)
echo "6. Searching test references by reference ranges content (gender: M, ageMin: 18)..."
curl -X GET "$BASE_URL/test-referance/search-by-reference-ranges?gender=M&ageMin=18" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n"

# Test 7: Update reference ranges (assuming ID 1 exists)
echo "7. Updating reference ranges for test reference ID 1..."
curl -X PUT "$BASE_URL/test-referance/1/reference-ranges" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "Gender": "MF",
      "AgeMin": 0,
      "AgeMinUnit": "DAYS",
      "AgeMax": 30,
      "AgeMaxUnit": "DAYS",
      "ReferenceRange": "95 - 125 fl"
    },
    {
      "Gender": "MF",
      "AgeMin": 1,
      "AgeMinUnit": "MONTHS",
      "AgeMax": 12,
      "AgeMaxUnit": "MONTHS",
      "ReferenceRange": "85 - 105 fl"
    }
  ]' | jq '.'

echo -e "\n"

# Test 8: Get specific test reference by ID
echo "8. Getting test reference by ID 1..."
curl -X GET "$BASE_URL/test-referance/1" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n"

# Test 9: Download CSV with reference ranges data
echo "9. Downloading CSV with reference ranges data..."
curl -X GET "$BASE_URL/test-referance/download" \
  -H "Authorization: Bearer $TOKEN" \
  -o test_references_with_reference_ranges.csv

echo "CSV downloaded as test_references_with_reference_ranges.csv"
echo "Content preview:"
head -5 test_references_with_reference_ranges.csv

echo -e "\n"

# Test 10: Upload CSV with reference ranges
echo "10. Uploading CSV with reference ranges..."
curl -X POST "$BASE_URL/upload-test-referance" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@sample_test_references_with_reference_ranges.csv" | jq '.'

echo -e "\n"
echo "=== Reference Ranges Functionality Test Complete ==="

