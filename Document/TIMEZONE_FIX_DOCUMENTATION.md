# Timezone Fix Documentation


## Overview

This document describes the timezone handling fix implemented for the Lab Automation system to resolve timestamp display issues across different environments (local development, testing, and production/AWS).

## Problem Statement

### Issue
The application was displaying timestamps **5.5 hours earlier** than the actual creation time in the UI, particularly noticeable in AWS/production environments.

### Example
- **User creates record at**: 9:07 PM IST (21:07 IST)
- **UI displays**: 3:37 PM (15:37) - **5.5 hours earlier**
- **API returns**: `"2026-01-07T15:37:07.630130Z"` (UTC, which is correct)

### Root Cause

The issue occurred due to a combination of factors:

1. **Hibernate Configuration**: The application uses `hibernate.jdbc.time_zone: UTC` to store all timestamps in UTC
2. **Entity Type Mismatch**: Entities were using `LocalDateTime` (timezone-naive) instead of `Instant` (timezone-aware)
3. **Incorrect Conversion**: The `formatDateTimeInIST()` method was incorrectly treating UTC values as if they were already in IST, then adding `+05:30` offset

### The Buggy Flow (Before Fix)

```
User creates record at 7:12 PM IST (19:12 IST)
    ↓
Hibernate converts to UTC: 19:12 IST → 13:42 UTC
    ↓
Database stores: 13:42 UTC
    ↓
Hibernate reads back: 13:42 UTC → LocalDateTime(13:42) [NO TIMEZONE INFO]
    ↓
formatDateTimeInIST() treats LocalDateTime(13:42) as IST
    ↓
Adds +05:30 offset → "2026-01-07T13:42:26+05:30" ❌ WRONG!
    ↓
UI displays: 01:42 PM (5.5 hours earlier than actual)
```

### Why It Worked Locally But Failed in AWS

| Environment | OS Timezone | JVM Timezone | Behavior |
|------------|-------------|--------------|----------|
| **Local** | IST | IST | Appeared to work (timezone conversion happened invisibly) |
| **AWS** | UTC | UTC | Timezone mismatch became visible (5.5-hour shift) |

## Solution Implemented

### Changes Made

#### 1. Entity Update: `ReportEntity.java`

**Before:**
```java
@CreationTimestamp
@Column(updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
private LocalDateTime updatedAt;
```

**After:**
```java
@CreationTimestamp
@Column(updatable = false)
private Instant createdAt;

@UpdateTimestamp
private Instant updatedAt;
```

**Why `Instant`?**
- `Instant` is always timezone-aware (UTC)
- No ambiguity about timezone
- Works consistently across all environments
- Industry standard for timestamp storage

#### 2. Service Update: `ReportService.java`

**Removed:**
```java
private String formatDateTimeInIST(LocalDateTime dateTime) {
    if (dateTime == null) return null;
    ZonedDateTime istZonedDateTime = dateTime.atZone(ZoneId.of("Asia/Kolkata"));
    return istZonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
}
```

**Added:**
```java
// Format createdDateTime in IST for display (createdAt remains UTC)
if (report.getCreatedAt() != null) {
    ZonedDateTime istDateTime = report.getCreatedAt().atZone(ZoneId.of("Asia/Kolkata"));
    report.setCreatedDateTime(istDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
}
```

**Key Difference:**
- **Before**: Treated `LocalDateTime` (which was actually UTC) as IST → Wrong
- **After**: Converts `Instant` (UTC) to IST → Correct

### The Correct Flow (After Fix)

```
User creates record at 9:07 PM IST (21:07 IST)
    ↓
Hibernate converts to UTC: 21:07 IST → 15:37 UTC
    ↓
Database stores: 15:37 UTC
    ↓
Hibernate reads back: 15:37 UTC → Instant(15:37 UTC) ✅ TIMEZONE AWARE
    ↓
Convert Instant to IST: Instant(15:37 UTC) → ZonedDateTime(21:07 IST)
    ↓
Format with offset → "2026-01-07T21:07:07+05:30" ✅ CORRECT!
    ↓
UI displays: 9:07 PM (correct time)
```

## API Response Format

### Current Response Structure

```json
{
    "status": "success",
    "message": "Report fetched successfully",
    "data": [
        {
            "reportId": 91,
            "createdAt": "2026-01-07T15:37:07.630130Z",           // UTC (for backend/storage)
            "createdDateTime": "2026-01-07T21:07:07.630130+05:30", // IST (for display)
            ...
        }
    ]
}
```

### Field Descriptions

| Field | Type | Format | Purpose |
|-------|------|--------|---------|
| `createdAt` | `Instant` | ISO-8601 UTC (`Z` suffix) | Backend storage, calculations, comparisons |
| `createdDateTime` | `String` | ISO-8601 with offset (`+05:30`) | Frontend display in IST |

## Configuration

### Hibernate Configuration (`application.yml`)

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC  # ✅ Keep this - stores all timestamps in UTC
```

**Important**: This configuration should remain as `UTC`. Do not change it.

### Why UTC Storage?

1. **Consistency**: All timestamps stored in one timezone
2. **No Ambiguity**: UTC has no daylight saving time
3. **Global Compatibility**: Works for users in any timezone
4. **Database Best Practice**: Industry standard

## Frontend Integration

### Recommended Approach

The frontend should convert UTC timestamps to the user's local timezone:

```javascript
// Using dayjs with timezone plugin
import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';

dayjs.extend(utc);
dayjs.extend(timezone);

// Convert UTC to IST for display
const displayTime = dayjs(apiResponse.createdAt)
    .tz("Asia/Kolkata")
    .format("DD/MM/YYYY [at] hh:mm A");

// Or use createdDateTime directly (already in IST)
const displayTime = dayjs(apiResponse.createdDateTime)
    .format("DD/MM/YYYY [at] hh:mm A");
```

### Alternative: Use `createdDateTime` Directly

Since `createdDateTime` is already formatted in IST, the frontend can use it directly:

```javascript
// Simple approach - use createdDateTime as-is
const displayTime = apiResponse.createdDateTime;
// Format: "2026-01-07T21:07:07.630130+05:30"
```

## Best Practices

### ✅ DO

1. **Use `Instant` for all timestamp fields** in entities
2. **Store timestamps in UTC** in the database
3. **Convert to local timezone** only for display purposes
4. **Keep `hibernate.jdbc.time_zone: UTC`** configuration
5. **Use `@CreationTimestamp` and `@UpdateTimestamp`** with `Instant`

### ❌ DON'T

1. **Don't use `LocalDateTime`** for timestamps that need timezone awareness
2. **Don't mix UTC storage with IST serialization** without proper conversion
3. **Don't attach timezone offsets to UTC values** without converting first
4. **Don't let the UI guess timezone** - always provide explicit timezone info
5. **Don't change `hibernate.jdbc.time_zone`** from UTC

## Testing

### Test Scenarios

1. **Create a new record** and verify timestamps are correct
2. **Check API response** - `createdAt` should be UTC, `createdDateTime` should be IST
3. **Verify in different environments** - local, testing, production
4. **Test timezone conversion** - ensure 5.5-hour offset is applied correctly

### Example Test Case

```
Input: User creates report at 9:07 PM IST
Expected API Response:
  - createdAt: "2026-01-07T15:37:07Z" (UTC)
  - createdDateTime: "2026-01-07T21:07:07+05:30" (IST)
Expected UI Display: 9:07 PM
```

## Migration Notes

### For Existing Records

Existing records created before this fix may have been stored with `LocalDateTime`. Hibernate will automatically handle the conversion when reading these records as `Instant`, but you should verify:

1. **Check existing data**: Query database to verify timestamp values
2. **Test old records**: Ensure they display correctly after the fix
3. **Monitor**: Watch for any timestamp-related issues in production

### Database Schema

No database migration is required. The existing `TIMESTAMP` columns work with both `LocalDateTime` and `Instant`. Hibernate handles the conversion automatically.

## Troubleshooting

### Issue: Timestamps still showing wrong time

**Check:**
1. Verify entity uses `Instant`, not `LocalDateTime`
2. Confirm `hibernate.jdbc.time_zone: UTC` is set
3. Check if `createdDateTime` is being set correctly in service
4. Verify frontend is using the correct field

### Issue: Null values in `createdDateTime`

**Check:**
1. Ensure `report.getCreatedAt()` is not null
2. Verify the service method is being called
3. Check for any exceptions in logs

### Issue: Timezone offset incorrect

**Check:**
1. Verify `ZoneId.of("Asia/Kolkata")` is correct
2. Check if system timezone is interfering
3. Verify Hibernate timezone configuration

## Related Files

- `src/main/java/tiameds/com/tiameds/entity/ReportEntity.java`
- `src/main/java/tiameds/com/tiameds/services/lab/ReportService.java`
- `src/main/resources/application.yml`

## References

- [Java Time API Documentation](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html)
- [Hibernate Timezone Configuration](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#basic-datetime-jdbc)
- [ISO-8601 Date Format](https://en.wikipedia.org/wiki/ISO_8601)

## Summary

This fix ensures:
- ✅ Timestamps are stored consistently in UTC
- ✅ No timezone ambiguity in the backend
- ✅ Correct timezone conversion for display
- ✅ Works across all environments (local, testing, production)
- ✅ Industry-standard approach

The solution maintains UTC storage while providing IST-formatted timestamps for display, ensuring accuracy and consistency across the application.

---

**Last Updated**: January 2026  
**Status**: ✅ Implemented and Tested  
**Environments**: Local, Testing, Production

