# S3 Logo Upload Flow (Lab Edit)

This document describes the full end-to-end flow for uploading a lab logo to S3 from the frontend, and updating the lab record in the backend.

## Overview

- Frontend uploads the image directly to S3 using a presigned URL.
- Backend generates the presigned URL and returns the final public file URL.
- Frontend submits the file URL in the update payload.
- Backend updates the lab record with the new logo URL.

## Frontend Flow

1. User clicks **Edit** in the Lab View modal.
2. User selects a logo file.
3. Frontend requests a presigned URL:
   - `POST /lab/admin/lab-logo/upload-url`
4. Backend returns:
   - `uploadUrl` (presigned S3 URL)
   - `fileUrl` (public URL to store in DB)
5. Frontend uploads the file directly to S3 via `PUT uploadUrl`.
6. On success, frontend stores `fileUrl` in form state.
7. Save is enabled after upload completes.
8. On Save, frontend sends update payload including `labLogo: fileUrl`.

## Backend Flow

### 1) Presigned URL Endpoint

**Route**  
`POST /lab/admin/lab-logo/upload-url`

**Request**  
```json
{
  "labId": 123,
  "fileName": "logo.png",
  "fileType": "image/png"
}
```

**Response**  
```json
{
  "uploadUrl": "https://s3...presigned-url",
  "fileUrl": "https://cdn-or-s3/public/path/logo.png"
}
```

**Backend responsibilities**

- Build an S3 key like `labs/{labId}/{uuid}-{fileName}`.
- Generate a presigned **PUT** URL with short expiry (5–10 minutes).
- Return both `uploadUrl` and the final `fileUrl`.

### 2) Update Lab Endpoint

**Route**  
`PUT /lab/admin/update-lab-by-id/{labId}`

**Payload example**  
```json
{
  "name": "LAB NAME",
  "labLogo": "https://cdn-or-s3/public/path/logo.png",
  "labType": "RESEARCH",
  "labPhone": "9999999999"
}
```

**Backend rules**

- If `labLogo` is present: update logo field.
- If `labLogo` is missing: keep current logo.
- Update other fields normally.

## Key Rules

- No S3 credentials in the frontend.
- Frontend uploads only via presigned URL.
- Database stores only the public image URL.
- Disable Save during upload to avoid inconsistent state.

## Error Handling

- If upload fails, keep Save disabled and show error.
- If update fails, remain in edit mode and show error.
- On success, refresh the lab data in the UI.
