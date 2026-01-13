# Frontend Guide: Dropdown and Impression JSON Fields

## Overview

The `dropdown` and `impression` fields are **JSONB fields** that store structured JSON data. They must be sent to the API as **JSON strings** (stringified JSON).

---

## Field Types

| Field | Type | Format | Validation |
|-------|------|--------|------------|
| `dropdown` | JSONB (String) | JSON Array `[{},{}]` | `isValidJson()` - Must be valid JSON array of objects |
| `impression` | JSONB (String) | JSON Object `{}` | `isValidJson()` - Must be valid JSON object |

---

## API Request Format

### ✅ **Correct Format: Send as JSON String**

Both fields must be sent as **stringified JSON** in the request body:

```json
{
  "category": "LABORATORY",
  "testName": "Complete Blood Count",
  "testDescription": "Full blood count analysis",
  "dropdown": "[{\"value\": \"A+\", \"label\": \"A Positive\"}, {\"value\": \"A-\", \"label\": \"A Negative\"}, {\"value\": \"B+\", \"label\": \"B Positive\"}]",
  "impression": "{\"text\": \"Normal findings\", \"code\": \"NORMAL\", \"severity\": \"low\"}"
}
```

### ❌ **Incorrect Format: Don't Send as Object**

```json
{
  "dropdown": [
    {"value": "A+", "label": "A Positive"},
    {"value": "A-", "label": "A Negative"}
  ],
  "impression": {
    "text": "Normal findings",
    "code": "NORMAL"
  }
}
```
**This will NOT work** - The backend expects strings, not objects/arrays!

---

## Frontend Implementation Examples

### **JavaScript/TypeScript**

#### **1. Creating/Updating Test Reference**

```javascript
// Example: Create test reference with dropdown and impression
const createTestReference = async (labId, testData) => {
  const requestBody = {
    labId: labId, // ✅ Required: labId must be in request body
    category: testData.category,
    testName: testData.testName,
    testDescription: testData.testDescription,
    units: testData.units,
    gender: testData.gender,
    minReferenceRange: testData.minReferenceRange,
    maxReferenceRange: testData.maxReferenceRange,
    ageMin: testData.ageMin,
    ageMax: testData.ageMax,
    minAgeUnit: testData.minAgeUnit,
    maxAgeUnit: testData.maxAgeUnit,
    
    // ✅ Convert JSON objects/arrays to strings
    dropdown: JSON.stringify(testData.dropdown), // Array of objects
    impression: JSON.stringify(testData.impression), // Object
    
    // Other JSON fields (if needed)
    reportJson: testData.reportJson ? JSON.stringify(testData.reportJson) : null,
    referenceRanges: testData.referenceRanges ? JSON.stringify(testData.referenceRanges) : null
  };
  
  const response = await fetch(`/api/v1/lab/test-reference/add`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(requestBody)
  });
  
  return response.json();
};
```

#### **2. Handling Form Data**

```javascript
// Example: Form with dropdown and impression inputs
const handleSubmit = async (formData) => {
  // Build dropdown as array of objects
  const dropdownData = formData.dropdownValues.split(',').map(value => ({
    value: value.trim(),
    label: value.trim() // You can customize labels as needed
  }));
  
  // Example with blood types:
  // const dropdownData = [
  //   {"value": "A+", "label": "A Positive"},
  //   {"value": "A-", "label": "A Negative"},
  //   {"value": "B+", "label": "B Positive"},
  //   {"value": "B-", "label": "B Negative"},
  //   {"value": "AB+", "label": "AB Positive"},
  //   {"value": "AB-", "label": "AB Negative"},
  //   {"value": "O+", "label": "O Positive"},
  //   {"value": "O-", "label": "O Negative"}
  // ];
  
  // Build impression JSON object
  const impressionData = {
    text: formData.impressionText,
    code: formData.impressionCode,
    severity: formData.severity, // "low", "medium", "high"
    category: formData.category,
    notes: formData.notes || null
  };
  
  const requestBody = {
    ...formData,
    // ✅ Stringify before sending
    dropdown: JSON.stringify(dropdownData), // Array of objects
    impression: JSON.stringify(impressionData) // Object
  };
  
  // Send to API
  await updateTestReference(labId, testReferenceId, requestBody);
};
```

#### **3. Parsing Response Data**

```javascript
// Example: Get all test references and parse JSON fields
const getTestReferences = async (labId) => {
  const response = await fetch(`/api/v1/lab/test-reference?labId=${labId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const data = await response.json();
  const testReferences = data.data.content || data.data; // Handle paginated or non-paginated response
  
  // ✅ Parse JSON strings back to objects for each test reference
  return testReferences.map(testRef => {
    let dropdown = null;
    let impression = null;
    
    try {
      dropdown = testRef.dropdown ? JSON.parse(testRef.dropdown) : null;
    } catch (e) {
      console.error('Error parsing dropdown:', e);
    }
    
    try {
      impression = testRef.impression ? JSON.parse(testRef.impression) : null;
    } catch (e) {
      console.error('Error parsing impression:', e);
    }
    
    return {
      ...testRef,
      dropdown,  // Now it's an array of objects, not a string
      impression // Now it's an object, not a string
    };
  });
};

// Example: Get test reference by test name
const getTestReferenceByName = async (labId, testName) => {
  const response = await fetch(`/api/v1/lab/test-reference/${labId}/test/${encodeURIComponent(testName)}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const data = await response.json();
  const testReference = data.data[0] || data.data; // Handle array or single object response
  
  // ✅ Parse JSON strings back to objects
  let dropdown = null;
  let impression = null;
  
  try {
    dropdown = testReference.dropdown ? JSON.parse(testReference.dropdown) : null;
  } catch (e) {
    console.error('Error parsing dropdown:', e);
  }
  
  try {
    impression = testReference.impression ? JSON.parse(testReference.impression) : null;
  } catch (e) {
    console.error('Error parsing impression:', e);
  }
  
  return {
    ...testReference,
    dropdown,  // Now it's an array of objects, not a string
    impression // Now it's an object, not a string
  };
};
```

---

## Recommended JSON Structures

### **Dropdown Field Structure**

**Format: Array of Objects `[{},{}]`**

```json
[
  {"value": "A+", "label": "A Positive"},
  {"value": "A-", "label": "A Negative"},
  {"value": "B+", "label": "B Positive"},
  {"value": "B-", "label": "B Negative"},
  {"value": "AB+", "label": "AB Positive"},
  {"value": "AB-", "label": "AB Negative"},
  {"value": "O+", "label": "O Positive"},
  {"value": "O-", "label": "O Negative"}
]
```

**With Additional Properties:**

```json
[
  {
    "value": "A+",
    "label": "A Positive",
    "description": "A positive blood type",
    "disabled": false
  },
  {
    "value": "A-",
    "label": "A Negative",
    "description": "A negative blood type",
    "disabled": false
  },
  {
    "value": "B+",
    "label": "B Positive",
    "description": "B positive blood type",
    "disabled": false
  }
]
```

**Minimal Structure (just value and label):**

```json
[
  {"value": "option1", "label": "Option 1"},
  {"value": "option2", "label": "Option 2"},
  {"value": "option3", "label": "Option 3"}
]
```

### **Impression Field Structure**

```json
{
  "text": "Normal findings. No abnormalities detected.",
  "code": "NORMAL",
  "severity": "low",
  "category": "GENERAL",
  "notes": "Patient shows normal test results",
  "recommendations": ["Continue current treatment", "Follow-up in 3 months"]
}
```

**Alternative Structure (for detailed impressions):**

```json
{
  "summary": "Normal findings",
  "details": [
    {
      "finding": "Hemoglobin levels",
      "status": "normal",
      "value": "14.5 g/dL"
    },
    {
      "finding": "White blood cell count",
      "status": "normal",
      "value": "7.2 x 10³/μL"
    }
  ],
  "code": "NORMAL",
  "severity": "low",
  "timestamp": "2026-01-07T10:30:00Z"
}
```

---

## React Example

```jsx
import React, { useState, useEffect } from 'react';

const TestReferenceForm = ({ labId, testReferenceId, onSave }) => {
  const [formData, setFormData] = useState({
    testName: '',
    category: '',
    dropdown: [], // Array of objects: [{value: "", label: ""}, ...]
    impression: {
      text: '',
      code: '',
      severity: 'low'
    }
  });
  
  // Load existing data
  useEffect(() => {
    if (testReferenceId) {
      loadTestReference();
    }
  }, [testReferenceId]);
  
  const loadTestReference = async () => {
    const response = await fetch(`/api/v1/lab/test-reference?labId=${labId}`);
    const data = await response.json();
    const testRef = data.data;
    
    // Parse JSON strings
    setFormData({
      ...testRef,
      dropdown: testRef.dropdown ? JSON.parse(testRef.dropdown) : [], // Array of objects
      impression: testRef.impression ? JSON.parse(testRef.impression) : { text: '', code: '', severity: 'low' }
    });
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const requestBody = {
      ...formData,
      // ✅ Stringify before sending
      dropdown: JSON.stringify(formData.dropdown),
      impression: JSON.stringify(formData.impression)
    };
    
    // Add labId for create endpoint
    if (!testReferenceId) {
      requestBody.labId = labId;
    }
    
    const url = testReferenceId 
      ? `/api/v1/lab/test-reference/${labId}/${testReferenceId}`
      : `/api/v1/lab/test-reference/add`;
    
    const method = testReferenceId ? 'PUT' : 'POST';
    
    const response = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(requestBody)
    });
    
    const result = await response.json();
    onSave(result);
  };
  
  return (
    <form onSubmit={handleSubmit}>
      {/* Form fields */}
      <input
        type="text"
        value={formData.testName}
        onChange={(e) => setFormData({ ...formData, testName: e.target.value })}
        placeholder="Test Name"
      />
      
      {/* Dropdown values input */}
      <textarea
        value={formData.dropdown.map(item => `${item.value}:${item.label}`).join(', ')}
        onChange={(e) => {
          const items = e.target.value.split(',').map(v => {
            const parts = v.trim().split(':');
            return {
              value: parts[0]?.trim() || '',
              label: parts[1]?.trim() || parts[0]?.trim() || ''
            };
          }).filter(item => item.value);
          setFormData({ ...formData, dropdown: items });
        }}
        placeholder="Enter dropdown values (format: value:label, value:label)"
      />
      
      {/* Or use a more structured input for dropdown items */}
      {formData.dropdown.map((item, index) => (
        <div key={index}>
          <input
            value={item.value}
            onChange={(e) => {
              const newDropdown = [...formData.dropdown];
              newDropdown[index].value = e.target.value;
              setFormData({ ...formData, dropdown: newDropdown });
            }}
            placeholder="Value"
          />
          <input
            value={item.label}
            onChange={(e) => {
              const newDropdown = [...formData.dropdown];
              newDropdown[index].label = e.target.value;
              setFormData({ ...formData, dropdown: newDropdown });
            }}
            placeholder="Label"
          />
        </div>
      ))}
      
      {/* Impression text input */}
      <textarea
        value={formData.impression.text}
        onChange={(e) => setFormData({
          ...formData,
          impression: {
            ...formData.impression,
            text: e.target.value
          }
        })}
        placeholder="Enter impression"
      />
      
      <button type="submit">Save</button>
    </form>
  );
};

export default TestReferenceForm;
```

---

## Vue.js Example

```vue
<template>
  <form @submit.prevent="handleSubmit">
    <input v-model="formData.testName" placeholder="Test Name" />
    
    <textarea
      v-model="dropdownValuesText"
      @input="updateDropdown"
      placeholder="Enter dropdown values (comma-separated)"
    />
    
    <textarea
      v-model="formData.impression.text"
      @input="updateImpression"
      placeholder="Enter impression"
    />
    
    <button type="submit">Save</button>
  </form>
</template>

<script>
export default {
  data() {
    return {
      formData: {
        testName: '',
        category: '',
        dropdown: [], // Array of objects: [{value: "", label: ""}, ...]
        impression: {
          text: '',
          code: '',
          severity: 'low'
        }
      },
      dropdownValuesText: ''
    };
  },
  methods: {
    updateDropdown() {
      // Convert comma-separated values to array of objects
      this.formData.dropdown = this.dropdownValuesText
        .split(',')
        .map(v => {
          const parts = v.trim().split(':');
          return {
            value: parts[0]?.trim() || '',
            label: parts[1]?.trim() || parts[0]?.trim() || ''
          };
        })
        .filter(item => item.value);
    },
    updateImpression() {
      // Impression is already bound to formData.impression.text
    },
    async handleSubmit() {
      const requestBody = {
        ...this.formData,
        // ✅ Stringify before sending
        dropdown: JSON.stringify(this.formData.dropdown),
        impression: JSON.stringify(this.formData.impression)
      };
      
      const response = await fetch(`/api/v1/lab/test-reference/add`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.token}`
        },
        body: JSON.stringify(requestBody)
      });
      
      const result = await response.json();
      this.$emit('saved', result);
    }
  }
};
</script>
```

---

## Angular Example

```typescript
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface DropdownItem {
  value: string;
  label: string;
  description?: string;
  disabled?: boolean;
}

type DropdownData = DropdownItem[]; // Array of objects

interface ImpressionData {
  text: string;
  code?: string;
  severity?: string;
}

@Component({
  selector: 'app-test-reference-form',
  template: `
    <form (ngSubmit)="onSubmit()">
      <input [(ngModel)]="formData.testName" name="testName" />
      
      <textarea
        [(ngModel)]="dropdownValuesText"
        (input)="updateDropdown()"
        name="dropdown"
        placeholder="Enter dropdown values (comma-separated)"
      ></textarea>
      
      <textarea
        [(ngModel)]="formData.impression.text"
        name="impression"
        placeholder="Enter impression"
      ></textarea>
      
      <button type="submit">Save</button>
    </form>
  `
})
export class TestReferenceFormComponent implements OnInit {
  formData = {
    testName: '',
    category: '',
    dropdown: [] as DropdownData, // Array of objects
    impression: {
      text: '',
      code: '',
      severity: 'low'
    } as ImpressionData
  };
  
  dropdownValuesText = '';
  
  constructor(private http: HttpClient) {}
  
  updateDropdown() {
    // Convert comma-separated values to array of objects
    this.formData.dropdown = this.dropdownValuesText
      .split(',')
      .map(v => {
        const parts = v.trim().split(':');
        return {
          value: parts[0]?.trim() || '',
          label: parts[1]?.trim() || parts[0]?.trim() || ''
        };
      })
      .filter(item => item.value);
  }
  
  async onSubmit() {
    const requestBody = {
      ...this.formData,
      // ✅ Stringify before sending
      dropdown: JSON.stringify(this.formData.dropdown),
      impression: JSON.stringify(this.formData.impression)
    };
    
    const url = `/api/v1/lab/test-reference/add`;
    
    this.http.post(url, requestBody).subscribe(
      (response) => {
        console.log('Success:', response);
      },
      (error) => {
        console.error('Error:', error);
      }
    );
  }
}
```

---

## Important Notes

### ✅ **DO:**

1. **Always stringify JSON objects** before sending to API:
   ```javascript
   dropdown: JSON.stringify(dropdownObject)
   ```

2. **Parse JSON strings** when receiving from API:
   ```javascript
   const dropdown = JSON.parse(response.dropdown)
   ```

3. **Handle null/empty values**:
   ```javascript
   dropdown: formData.dropdown ? JSON.stringify(formData.dropdown) : null
   ```

4. **Validate JSON** before sending (optional but recommended):
   ```javascript
   try {
     JSON.parse(jsonString);
     // Valid JSON
   } catch (e) {
     // Invalid JSON
   }
   ```

### ❌ **DON'T:**

1. **Don't send JSON objects/arrays directly**:
   ```javascript
   // ❌ WRONG
   dropdown: [{value: "A+", label: "A Positive"}]
   ```

2. **Don't forget to parse** when receiving:
   ```javascript
   // ❌ WRONG - dropdown will be a string
   const items = response.dropdown[0]; // This won't work!
   
   // ✅ CORRECT - parse first, then access
   const dropdown = JSON.parse(response.dropdown);
   const firstItem = dropdown[0];
   ```

3. **Don't send empty strings** for null values:
   ```javascript
   // ❌ WRONG
   dropdown: ""
   
   // ✅ CORRECT
   dropdown: null
   ```

---

## API Endpoints

### **Create Test Reference**
```
POST /api/v1/lab/test-reference/add
```
**Note**: `labId` must be provided in the request body, not in the URL path.

### **Update Test Reference**
```
PUT /api/v1/lab/test-reference/{labId}/{testReferenceId}
```
**Alternative endpoint**:
```
PUT /api/v1/lab/test-reference/update
```
**Note**: For alternative endpoint, `labId` and `testReferenceCode` must be in request body.

### **Get All Test References**
```
GET /api/v1/lab/test-reference?labId={labId}&page=0&size=10
```
**Query Parameters**:
- `labId` (required) - Lab ID
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 10) - Page size

### **Get Test Reference by Test Name**
```
GET /api/v1/lab/test-reference/{labId}/test/{testName}
```
**Or with query parameter**:
```
GET /api/v1/lab/test-reference/{labId}/test?testName={testName}
```

### **CSV Upload**
```
POST /api/v1/lab/test-reference/{labId}/csv/upload
```

### **CSV Download**
```
GET /api/v1/lab/test-reference/{labId}/download
```

---

## Example API Request/Response

### **Request (Create):**
```json
{
  "labId": 20,
  "category": "LABORATORY",
  "testName": "Complete Blood Count",
  "testDescription": "Full blood count analysis",
  "units": "count/μL",
  "gender": "MF",
  "minReferenceRange": 4.5,
  "maxReferenceRange": 11.0,
  "ageMin": 0,
  "ageMax": 100,
  "minAgeUnit": "YEARS",
  "maxAgeUnit": "YEARS",
  "dropdown": "[{\"value\": \"Normal\", \"label\": \"Normal\"}, {\"value\": \"Abnormal\", \"label\": \"Abnormal\"}, {\"value\": \"Critical\", \"label\": \"Critical\"}]",
  "impression": "{\"text\": \"Normal findings. No abnormalities detected.\", \"code\": \"NORMAL\", \"severity\": \"low\"}"
}
```

### **Request (Update):**
```json
{
  "testReferenceCode": "TRF20-00001",
  "dropdown": "[{\"value\": \"A+\", \"label\": \"A Positive\"}, {\"value\": \"A-\", \"label\": \"A Negative\"}]",
  "impression": "{\"text\": \"Updated impression\", \"code\": \"UPDATED\"}"
}
```
**Note**: For update, you can send only the fields you want to update. `testReferenceCode` or `id` is required.

### **Response:**
```json
{
  "status": "success",
  "message": "Test reference created successfully",
  "data": {
    "id": 123,
    "testName": "Complete Blood Count",
    "category": "LABORATORY",
    "dropdown": "[{\"value\": \"Normal\", \"label\": \"Normal\"}, {\"value\": \"Abnormal\", \"label\": \"Abnormal\"}, {\"value\": \"Critical\", \"label\": \"Critical\"}]",
    "impression": "{\"text\": \"Normal findings. No abnormalities detected.\", \"code\": \"NORMAL\", \"severity\": \"low\"}",
    "createdAt": "2026-01-07T10:30:00Z",
    "updatedAt": "2026-01-07T10:30:00Z"
  }
}
```

---

## Summary

1. **Send**: JSON objects/arrays must be **stringified** (`JSON.stringify()`)
2. **Receive**: JSON strings must be **parsed** (`JSON.parse()`)
3. **Format**: 
   - `dropdown`: JSON array of objects `[{},{}]` - e.g., `[{"value": "A+", "label": "A Positive"}]`
   - `impression`: JSON object `{}` - e.g., `{"text": "Normal findings", "code": "NORMAL"}`
4. **Validation**: Backend validates JSON format automatically
5. **Null handling**: Send `null` for empty values, not empty strings

---

**Last Updated**: January 2026  
**Status**: ✅ Ready for Frontend Implementation

