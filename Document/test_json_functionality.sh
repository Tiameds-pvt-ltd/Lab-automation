#!/bin/bash

# Test script for JSON functionality in Test Reference API
# Make sure to replace YOUR_TOKEN with actual JWT token

BASE_URL="http://localhost:8080/super-admin/referance-and-test"
TOKEN="YOUR_TOKEN"

echo "=== Testing JSON Functionality for Test References ==="
echo

# Test 1: Create a test reference with JSON data
echo "1. Creating test reference with JSON data..."
curl -X POST "$BASE_URL/test-referance/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "RADIOLOGY",
    "testName": "Chest X-ray",
    "testDescription": "Chest X-ray examination",
    "units": "N/A",
    "gender": "M/F",
    "ageMin": 0,
    "minAgeUnit": "YEARS",
    "ageMax": 100,
    "maxAgeUnit": "YEARS",
    "remarks": "Routine screening test",
    "reportJson": "{\"observations\": \"Lung opacity noted\", \"size\": \"3cm\", \"images\": [\"image1.png\", \"image2.png\"], \"notes\": \"Follow-up in 2 weeks\"}"
  }' | jq '.'

echo -e "\n"

# Test 2: Create another test reference with complex JSON
echo "2. Creating test reference with complex JSON (USG Carotid Doppler)..."
curl -X POST "$BASE_URL/test-referance/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "RADIOLOGY",
    "testName": "USG Carotid Doppler",
    "testDescription": "Ultrasound examination of carotid arteries",
    "units": "N/A",
    "gender": "M/F",
    "ageMin": 0,
    "minAgeUnit": "YEARS",
    "ageMax": 100,
    "maxAgeUnit": "YEARS",
    "remarks": "Vascular screening",
    "reportJson": "{\"USG_Carotid_Doppler\":[{\"Artery_Segment\":\"Common Carotid – Proximal\",\"Peak_Systolic_cm_s\":\"\",\"End_Diastolic_cm_s\":\"\",\"Spectrum\":\"\",\"Stenosis_Percent\":\"\"},{\"Artery_Segment\":\"Common Carotid – Mid\",\"Peak_Systolic_cm_s\":\"\",\"End_Diastolic_cm_s\":\"\",\"Spectrum\":\"\",\"Stenosis_Percent\":\"\"}]}"
  }' | jq '.'

echo -e "\n"

# Test 3: Get all test references
echo "3. Getting all test references..."
curl -X GET "$BASE_URL/test-referance" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n"

# Test 4: Search by JSON content
echo "4. Searching test references by JSON content (key: observations)..."
curl -X GET "$BASE_URL/test-referance/search-by-json?jsonKey=observations" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n"

# Test 5: Search by JSON value
echo "5. Searching test references by JSON value (value: opacity)..."
curl -X GET "$BASE_URL/test-referance/search-by-json?jsonValue=opacity" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n"

# Test 6: Update JSON data (assuming ID 1 exists)
echo "6. Updating JSON data for test reference ID 1..."
curl -X PUT "$BASE_URL/test-referance/1/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '"{\"observations\": \"Updated findings\", \"size\": \"2cm\", \"images\": [\"updated_image.png\"], \"notes\": \"Updated notes\"}"' | jq '.'

echo -e "\n"

# Test 7: Get specific test reference by ID
echo "7. Getting test reference by ID 1..."
curl -X GET "$BASE_URL/test-referance/1" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n"

# Test 8: Download CSV with JSON data
echo "8. Downloading CSV with JSON data..."
curl -X GET "$BASE_URL/test-referance/download" \
  -H "Authorization: Bearer $TOKEN" \
  -o test_references_with_json.csv

echo "CSV downloaded as test_references_with_json.csv"
echo "Content preview:"
head -5 test_references_with_json.csv

echo -e "\n"
echo "=== JSON Functionality Test Complete ==="

