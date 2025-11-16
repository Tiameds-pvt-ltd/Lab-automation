# Sample Association Lab Isolation - Quick Reference

## Quick Summary

Samples are now **lab-specific** instead of shared globally. Each lab has its own isolated set of samples.

---

## API Endpoints (Updated)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/lab/{labId}/sample-list` | Get all samples for a lab |
| `POST` | `/lab/{labId}/sample` | Create a new sample for a lab |
| `PUT` | `/lab/{labId}/sample/{sampleId}` | Update a sample for a lab |
| `DELETE` | `/lab/{labId}/sample/{sampleId}` | Delete a sample for a lab |

**Note:** All endpoints require `labId` as a path parameter.

---

## Database Changes

```sql
-- Add lab_id column
ALTER TABLE sample_entity ADD COLUMN lab_id BIGINT NOT NULL;

-- Create index
CREATE INDEX idx_sample_entity_lab_id ON sample_entity(lab_id);

-- Backfill existing data (choose appropriate lab_id)
UPDATE sample_entity SET lab_id = 1 WHERE lab_id IS NULL;
```

---

## Key Changes

### Before
- ❌ Samples shared across all labs
- ❌ Global sample list
- ❌ No lab isolation

### After
- ✅ Each lab has its own samples
- ✅ Lab-specific sample lists
- ✅ Complete data isolation
- ✅ Lab access validation

---

## Migration Steps

1. **Add `lab_id` column** to `sample_entity` table
2. **Backfill** existing samples with appropriate `lab_id`
3. **Update frontend** to include `labId` in all API calls
4. **Test** all endpoints with lab-specific access

---

## Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| "Lab is not accessible" | User not a member of lab | Verify lab membership |
| "Sample does not belong to this lab" | Wrong labId for sample | Use correct labId |
| "Sample already exists for this lab" | Duplicate name in lab | Use different name or update existing |

---

## Code Examples

### JavaScript/TypeScript

```typescript
// Get samples for lab 1
GET /lab/1/sample-list

// Create sample for lab 1
POST /lab/1/sample
Body: { "name": "Blood Sample" }

// Update sample 5 in lab 1
PUT /lab/1/sample/5
Body: { "name": "Updated Name" }

// Delete sample 5 in lab 1
DELETE /lab/1/sample/5
```

### cURL

```bash
# Get samples
curl -X GET "http://localhost:8080/api/v1/lab/1/sample-list" \
  -H "Authorization: Bearer <token>"

# Create sample
curl -X POST "http://localhost:8080/api/v1/lab/1/sample" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Blood Sample"}'
```

---

## Validation Rules

1. ✅ User must be a member of the lab
2. ✅ Sample must belong to the specified lab
3. ✅ Sample names must be unique within a lab
4. ✅ All operations are logged with correct lab ID

---

## Breaking Changes

⚠️ **All endpoints now require `labId` in the path**

- Old: `GET /lab/sample-list`
- New: `GET /lab/{labId}/sample-list`

Update all client code to include `labId` parameter.

---

For detailed documentation, see [SAMPLE_ASSOCIATION_LAB_ISOLATION.md](./SAMPLE_ASSOCIATION_LAB_ISOLATION.md)

