# Sample Association Lab Isolation Documentation

## Overview

This document describes the changes made to the Sample Association system to ensure that each lab has its own isolated set of samples, rather than sharing samples globally across all labs.

## Table of Contents

1. [Changes Summary](#changes-summary)
2. [Database Schema Changes](#database-schema-changes)
3. [API Endpoints](#api-endpoints)
4. [Migration Guide](#migration-guide)
5. [Usage Examples](#usage-examples)
6. [Breaking Changes](#breaking-changes)
7. [Security Considerations](#security-considerations)

---

## Changes Summary

### Previous Behavior
- Samples were shared globally across all labs
- All labs could see and use the same set of samples
- Sample codes were generated using lab ID `0` (global)

### New Behavior
- Each lab has its own isolated set of samples
- Labs can only see and manage their own samples
- Sample codes are generated per lab
- All operations require lab-specific access validation

### Key Changes

1. **Entity Changes**
   - Added `labId` field to `SampleEntity`
   - Samples are now associated with a specific lab

2. **Repository Changes**
   - Added methods to query samples by lab ID
   - Added method to check for duplicate names within a lab

3. **Service Changes**
   - All service methods now require `labId` parameter
   - Added validation to ensure samples belong to the correct lab
   - Duplicate name checking is now lab-specific

4. **Controller Changes**
   - All endpoints now require `labId` as a path variable
   - Added lab access validation using `LabAccessableFilter`
   - Updated audit logging to use actual lab ID

---

## Database Schema Changes

### Table: `sample_entity`

#### New Column

```sql
ALTER TABLE sample_entity 
ADD COLUMN lab_id BIGINT NOT NULL;
```

#### Index Recommendation

```sql
CREATE INDEX idx_sample_entity_lab_id ON sample_entity(lab_id);
```

#### Updated Unique Constraint

The `sample_code` column remains unique globally, but sample names are now unique per lab (enforced at the application level).

**Note:** Consider adding a composite unique constraint if you want database-level enforcement:

```sql
ALTER TABLE sample_entity 
ADD CONSTRAINT uk_sample_name_lab UNIQUE (name, lab_id);
```

---

## API Endpoints

### Base URL
All endpoints are under `/lab/{labId}/`

### 1. Get Sample List

**Endpoint:** `GET /lab/{labId}/sample-list`

**Description:** Retrieves all samples for a specific lab.

**Path Parameters:**
- `labId` (Long, required): The ID of the lab

**Response:**
```json
{
  "status": "success",
  "message": "Sample associations retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Blood Sample",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": 2,
      "name": "Urine Sample",
      "createdAt": "2024-01-15T11:00:00",
      "updatedAt": "2024-01-15T11:00:00"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized`: User not found or lab is not accessible
- `403 Forbidden`: User does not have access to the specified lab

---

### 2. Create Sample

**Endpoint:** `POST /lab/{labId}/sample`

**Description:** Creates a new sample for a specific lab.

**Path Parameters:**
- `labId` (Long, required): The ID of the lab

**Request Body:**
```json
{
  "name": "Blood Sample"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Sample created successfully",
  "data": {
    "id": 1,
    "name": "Blood Sample",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Error Responses:**
- `400 Bad Request`: Sample already exists for this lab
- `401 Unauthorized`: User not found or lab is not accessible

---

### 3. Update Sample

**Endpoint:** `PUT /lab/{labId}/sample/{sampleId}`

**Description:** Updates an existing sample for a specific lab.

**Path Parameters:**
- `labId` (Long, required): The ID of the lab
- `sampleId` (Long, required): The ID of the sample to update

**Request Body:**
```json
{
  "name": "Updated Blood Sample"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Sample updated successfully",
  "data": {
    "id": 1,
    "name": "Updated Blood Sample",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T12:00:00"
  }
}
```

**Error Responses:**
- `400 Bad Request`: 
  - Sample not found
  - Sample does not belong to this lab
  - A sample with this name already exists for this lab
- `401 Unauthorized`: User not found or lab is not accessible
- `404 Not Found`: Sample not found

---

### 4. Delete Sample

**Endpoint:** `DELETE /lab/{labId}/sample/{sampleId}`

**Description:** Deletes a sample for a specific lab.

**Path Parameters:**
- `labId` (Long, required): The ID of the lab
- `sampleId` (Long, required): The ID of the sample to delete

**Response:**
```json
{
  "status": "success",
  "message": "Sample deleted successfully",
  "data": null
}
```

**Error Responses:**
- `400 Bad Request`: 
  - Sample not found
  - Sample does not belong to this lab
- `401 Unauthorized`: User not found or lab is not accessible
- `404 Not Found`: Sample not found

---

## Migration Guide

### Step 1: Database Migration

Create and run the following migration script:

```sql
-- Add lab_id column to sample_entity table
ALTER TABLE sample_entity 
ADD COLUMN lab_id BIGINT;

-- Create index for better query performance
CREATE INDEX idx_sample_entity_lab_id ON sample_entity(lab_id);

-- Update existing samples
-- Option 1: Assign all existing samples to a default lab (e.g., lab ID 1)
UPDATE sample_entity 
SET lab_id = 1 
WHERE lab_id IS NULL;

-- Option 2: If you have a mapping of samples to labs, update accordingly
-- UPDATE sample_entity 
-- SET lab_id = <appropriate_lab_id> 
-- WHERE <your_condition>;

-- Make lab_id NOT NULL after backfilling
ALTER TABLE sample_entity 
ALTER COLUMN lab_id SET NOT NULL;

-- Optional: Add composite unique constraint for name + lab_id
ALTER TABLE sample_entity 
ADD CONSTRAINT uk_sample_name_lab UNIQUE (name, lab_id);
```

### Step 2: Update Frontend/Client Code

Update all API calls to include `labId` in the path:

**Before:**
```javascript
// GET samples
GET /lab/sample-list

// CREATE sample
POST /lab/sample

// UPDATE sample
PUT /lab/sample/{sampleId}

// DELETE sample
DELETE /lab/sample/{sampleId}
```

**After:**
```javascript
// GET samples
GET /lab/{labId}/sample-list

// CREATE sample
POST /lab/{labId}/sample

// UPDATE sample
PUT /lab/{labId}/sample/{sampleId}

// DELETE sample
DELETE /lab/{labId}/sample/{sampleId}
```

### Step 3: Testing Checklist

- [ ] Verify existing samples are assigned to correct labs
- [ ] Test creating a new sample for a lab
- [ ] Test updating a sample (verify it belongs to the lab)
- [ ] Test deleting a sample (verify it belongs to the lab)
- [ ] Test that users can only access samples from their accessible labs
- [ ] Test duplicate name validation within a lab
- [ ] Verify audit logs contain correct lab IDs
- [ ] Test that samples from one lab are not visible to other labs

---

## Usage Examples

### Example 1: Create a Sample for Lab 1

```bash
curl -X POST "http://localhost:8080/api/v1/lab/1/sample" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Blood Sample"
  }'
```

### Example 2: Get All Samples for Lab 1

```bash
curl -X GET "http://localhost:8080/api/v1/lab/1/sample-list" \
  -H "Authorization: Bearer <token>"
```

### Example 3: Update a Sample

```bash
curl -X PUT "http://localhost:8080/api/v1/lab/1/sample/5" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Blood Sample"
  }'
```

### Example 4: Delete a Sample

```bash
curl -X DELETE "http://localhost:8080/api/v1/lab/1/sample/5" \
  -H "Authorization: Bearer <token>"
```

### Example 5: JavaScript/TypeScript Client

```typescript
// Sample service class
class SampleService {
  private baseUrl = 'http://localhost:8080/api/v1/lab';

  async getSamples(labId: number): Promise<SampleDto[]> {
    const response = await fetch(`${this.baseUrl}/${labId}/sample-list`, {
      headers: {
        'Authorization': `Bearer ${this.getToken()}`
      }
    });
    const result = await response.json();
    return result.data;
  }

  async createSample(labId: number, name: string): Promise<SampleDto> {
    const response = await fetch(`${this.baseUrl}/${labId}/sample`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.getToken()}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name })
    });
    const result = await response.json();
    return result.data;
  }

  async updateSample(labId: number, sampleId: number, name: string): Promise<SampleDto> {
    const response = await fetch(`${this.baseUrl}/${labId}/sample/${sampleId}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${this.getToken()}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name })
    });
    const result = await response.json();
    return result.data;
  }

  async deleteSample(labId: number, sampleId: number): Promise<void> {
    await fetch(`${this.baseUrl}/${labId}/sample/${sampleId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${this.getToken()}`
      }
    });
  }
}
```

---

## Breaking Changes

### API Endpoint Changes

All sample endpoints now require `labId` as a path parameter:

| Old Endpoint | New Endpoint | Change Type |
|-------------|--------------|-------------|
| `GET /lab/sample-list` | `GET /lab/{labId}/sample-list` | **Breaking** |
| `POST /lab/sample` | `POST /lab/{labId}/sample` | **Breaking** |
| `PUT /lab/sample/{sampleId}` | `PUT /lab/{labId}/sample/{sampleId}` | **Breaking** |
| `DELETE /lab/sample/{sampleId}` | `DELETE /lab/{labId}/sample/{sampleId}` | **Breaking** |

### Database Changes

- New required column: `lab_id` in `sample_entity` table
- Existing samples must be migrated to have a `lab_id` value

### Service Layer Changes

All service methods now require `labId` parameter:

- `getSampleAssociationList(Long labId)` - was `getSampleAssociationList()`
- `createSample(SampleDto sampleDto, Long labId)` - was `createSample(SampleDto sampleDto)`
- `updateSample(Long sampleId, SampleDto sampleDto, Long labId)` - was `updateSample(Long sampleId, SampleDto sampleDto)`
- `deleteSample(Long sampleId, Long labId)` - was `deleteSample(Long sampleId)`
- `getSampleSnapshot(Long sampleId, Long labId)` - was `getSampleSnapshot(Long sampleId)`

---

## Security Considerations

### Lab Access Validation

All endpoints now validate that the authenticated user has access to the specified lab using `LabAccessableFilter`. This ensures:

1. **Authorization**: Users can only access samples from labs they are members of
2. **Data Isolation**: Samples from one lab are completely isolated from other labs
3. **Audit Trail**: All operations are logged with the correct lab ID

### Validation Rules

1. **Lab Access**: Users must be members of the lab to perform any operations
2. **Sample Ownership**: Samples can only be updated/deleted if they belong to the specified lab
3. **Duplicate Names**: Sample names must be unique within a lab (but can be duplicated across different labs)

### Error Handling

The system returns appropriate error messages for:
- Unauthorized access attempts
- Attempts to modify samples from other labs
- Duplicate name violations within a lab

---

## Architecture Details

### Entity Relationship

```
Lab (1) ──────< (Many) SampleEntity
```

Each `SampleEntity` is now associated with exactly one `Lab` through the `labId` field.

### Service Layer Flow

1. **Controller** receives request with `labId`
2. **LabAccessableFilter** validates user has access to the lab
3. **Service** performs operation with lab-specific filtering
4. **Repository** queries/updates with lab ID constraint
5. **Audit Log** records operation with correct lab ID

### Data Isolation

- Samples are filtered by `labId` at the repository level
- Service methods verify sample ownership before operations
- Controller validates lab access before delegating to service

---

## Troubleshooting

### Issue: "Lab is not accessible" Error

**Cause:** User is not a member of the specified lab or lab doesn't exist.

**Solution:** 
- Verify the user is a member of the lab
- Check that the `labId` is correct
- Ensure the lab exists and is active

### Issue: "Sample does not belong to this lab" Error

**Cause:** Attempting to update/delete a sample that belongs to a different lab.

**Solution:**
- Verify the `sampleId` and `labId` match
- Check that the sample exists and belongs to the specified lab

### Issue: "Sample already exists for this lab" Error

**Cause:** Attempting to create a sample with a name that already exists in the lab.

**Solution:**
- Use a different name for the sample
- Or update the existing sample instead of creating a new one

### Issue: Migration Fails - "lab_id cannot be null"

**Cause:** Existing samples don't have `lab_id` values.

**Solution:**
- Run the migration script to backfill `lab_id` values before making the column NOT NULL

---

## Related Documentation

- [Entity Schema Documentation](./ENTITY_SCHEMA.md)
- [Lab Management Documentation](./LAB_MANAGEMENT.md)
- [API Documentation](./API_DOCUMENTATION.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2024-01-15 | Initial implementation of lab-specific sample isolation |

---

## Support

For questions or issues related to this feature, please contact the development team or refer to the main project documentation.

