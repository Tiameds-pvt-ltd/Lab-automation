# Report Settings – Documentation

## Overview

The **Report Settings** feature allows lab administrators to configure how lab reports are generated and displayed. Each lab has its own settings that control template layout, header, typography, signatures, and disclaimer. Settings are stored per lab and applied when generating patient reports (PDF/print).

---

## 1. What Gets Stored in the Database

### 1.1 Table: `report_settings`

One row per lab. If a lab has no row, use frontend defaults.

| Column                | Type           | Nullable | Default            | Description                                              |
|-----------------------|----------------|----------|--------------------|----------------------------------------------------------|
| `id`                  | BIGINT (PK)    | No       | Auto-increment     | Primary key                                              |
| `lab_id`              | BIGINT (FK)    | No       | –                  | References `lab.id` (unique constraint — one per lab)    |
| `template_id`         | VARCHAR(30)    | No       | `'templateA'`      | Selected template: `templateA`, `templateB`, `templateC` |
| `header_enabled`      | BOOLEAN        | No       | `true`             | Show header on the report                                |
| `header_required`     | BOOLEAN        | No       | `false`            | Mark header as mandatory (cannot be hidden at print)     |
| `font_size`           | INT            | No       | `12`               | Base font size in pixels (range: 10–18)                  |
| `text_size`           | VARCHAR(10)    | No       | `'Medium'`         | Preset text size: `Small`, `Medium`, `Large`             |
| `text_color`          | VARCHAR(10)    | No       | `'#111827'`        | Hex color code for report text                           |
| `signature_placement` | VARCHAR(20)    | No       | `'bottom-right'`   | Where signatures appear: `top-right`, `top-left`, `bottom-right`, `bottom-left` |
| `signature_columns`   | INT            | No       | `2`                | Number of signature columns per row (2, 3, or 4)         |
| `disclaimer_enabled`  | BOOLEAN        | No       | `true`             | Show disclaimer at the bottom of the report              |
| `disclaimer_text`     | TEXT           | No       | *(default text)*   | Configurable disclaimer content                          |
| `created_at`          | TIMESTAMP      | No       | `CURRENT_TIMESTAMP`| Row creation time                                        |
| `updated_at`          | TIMESTAMP      | No       | `CURRENT_TIMESTAMP`| Last update time (auto-updated)                          |

**Constraints:**
- `UNIQUE(lab_id)` — each lab can have only one settings row.
- `FOREIGN KEY (lab_id) REFERENCES lab(id)`.

### 1.2 Table: `report_role_settings`

Multiple rows per lab — one per signature role.

| Column           | Type          | Nullable | Default            | Description                                          |
|------------------|---------------|----------|--------------------|------------------------------------------------------|
| `id`             | BIGINT (PK)   | No       | Auto-increment     | Primary key                                          |
| `report_setting_id` | BIGINT (FK) | No       | –                  | References `report_settings.id`                      |
| `role`           | VARCHAR(100)  | No       | –                  | Role label (e.g., `Doctor`, `Technician`)            |
| `display_name`   | VARCHAR(200)  | Yes      | `''`               | Display name shown on report (e.g., `Dr. Arjun`)     |
| `designation`    | VARCHAR(200)  | Yes      | `''`               | Designation (e.g., `Consultant Pathologist`)          |
| `signature_url`  | TEXT          | Yes      | `''`               | S3/CDN URL of the uploaded signature image            |
| `enabled`        | BOOLEAN       | No       | `true`             | Whether this role appears on the report               |
| `sort_order`     | INT           | No       | `0`                | Display order (ascending)                             |
| `created_at`     | TIMESTAMP     | No       | `CURRENT_TIMESTAMP`| Row creation time                                    |
| `updated_at`     | TIMESTAMP     | No       | `CURRENT_TIMESTAMP`| Last update time                                     |

**Constraints:**
- `FOREIGN KEY (report_setting_id) REFERENCES report_settings(id) ON DELETE CASCADE`.
- Roles are ordered by `sort_order ASC` when returned.

### 1.3 SQL Schema (Reference)

```sql
CREATE TABLE report_settings (
    id              BIGSERIAL PRIMARY KEY,
    lab_id          BIGINT NOT NULL UNIQUE REFERENCES lab(id),
    template_id     VARCHAR(30) NOT NULL DEFAULT 'templateA',
    header_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    header_required BOOLEAN NOT NULL DEFAULT FALSE,
    font_size       INT NOT NULL DEFAULT 12,
    text_size       VARCHAR(10) NOT NULL DEFAULT 'Medium',
    text_color      VARCHAR(10) NOT NULL DEFAULT '#111827',
    signature_placement VARCHAR(20) NOT NULL DEFAULT 'bottom-right',
    signature_columns   INT NOT NULL DEFAULT 2,
    disclaimer_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    disclaimer_text     TEXT NOT NULL DEFAULT 'This laboratory report is intended for clinical correlation only. Results should be interpreted by a qualified medical professional.',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE report_role_settings (
    id                  BIGSERIAL PRIMARY KEY,
    report_setting_id   BIGINT NOT NULL REFERENCES report_settings(id) ON DELETE CASCADE,
    role                VARCHAR(100) NOT NULL,
    display_name        VARCHAR(200) DEFAULT '',
    designation         VARCHAR(200) DEFAULT '',
    signature_url       TEXT DEFAULT '',
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 2. Backend API Specification

All endpoints are prefixed with `/lab/{labId}/report-settings`.

### 2.1 GET `/lab/{labId}/report-settings`

Fetch the current report settings for a lab.

**Response (200):**
```json
{
  "status": "success",
  "message": "Report settings fetched successfully",
  "data": {
    "id": 1,
    "labId": 5,
    "templateId": "templateA",
    "headerEnabled": true,
    "headerRequired": false,
    "fontSize": 12,
    "textSize": "Medium",
    "textColor": "#111827",
    "signaturePlacement": "bottom-right",
    "signatureColumns": 2,
    "disclaimerEnabled": true,
    "disclaimerText": "This laboratory report is intended for clinical correlation only...",
    "roles": [
      {
        "id": 1,
        "role": "Doctor",
        "displayName": "Dr. Arjun",
        "designation": "Consultant Pathologist",
        "signatureUrl": "https://s3-bucket.../signatures/sig1.png",
        "enabled": true,
        "sortOrder": 0
      },
      {
        "id": 2,
        "role": "Technician",
        "displayName": "Lab Technician",
        "designation": "Senior Technician",
        "signatureUrl": "",
        "enabled": true,
        "sortOrder": 1
      }
    ],
    "createdAt": "2026-01-20T10:00:00Z",
    "updatedAt": "2026-01-28T14:30:00Z"
  }
}
```

**Response (404) — no settings saved yet:**
```json
{
  "status": "error",
  "message": "Report settings not found for this lab",
  "data": null
}
```
Frontend should use defaults when 404 is returned.

---

### 2.2 POST `/lab/{labId}/report-settings`

Create report settings for a lab (first time save).

**Request Body:**
```json
{
  "templateId": "templateA",
  "headerEnabled": true,
  "headerRequired": false,
  "fontSize": 12,
  "textSize": "Medium",
  "textColor": "#111827",
  "signaturePlacement": "bottom-right",
  "signatureColumns": 2,
  "disclaimerEnabled": true,
  "disclaimerText": "This laboratory report is intended for clinical correlation only...",
  "roles": [
    {
      "role": "Doctor",
      "displayName": "Dr. Arjun",
      "designation": "Consultant Pathologist",
      "signatureUrl": "",
      "enabled": true,
      "sortOrder": 0
    },
    {
      "role": "Technician",
      "displayName": "Lab Technician",
      "designation": "Senior Technician",
      "signatureUrl": "",
      "enabled": true,
      "sortOrder": 1
    }
  ]
}
```

**Response (201):**
```json
{
  "status": "success",
  "message": "Report settings saved successfully",
  "data": { /* full ReportSettingsResponse */ }
}
```

---

### 2.3 PUT `/lab/{labId}/report-settings`

Update existing report settings. Replaces the entire configuration including roles.

**Request Body:** Same as POST.

**Backend Behavior:**
1. Update the `report_settings` row for this lab.
2. Delete all existing `report_role_settings` rows for this setting.
3. Insert the new roles from the payload with correct `sort_order`.
4. Return the updated full settings.

**Response (200):**
```json
{
  "status": "success",
  "message": "Report settings updated successfully",
  "data": { /* full ReportSettingsResponse */ }
}
```

---

### 2.4 POST `/lab/{labId}/report-settings/signature/upload-url`

Get a presigned S3 URL for uploading a signature image. The frontend uploads the file directly to S3; the backend returns the presigned URL and the final `fileUrl` to store in `report_role_settings.signature_url`.

**Request Body:**
```json
{
  "fileName": "signature.png",
  "fileType": "image/png"
}
```

**Response (200):**
```json
{
  "status": "success",
  "message": "Upload URL generated",
  "data": {
    "uploadUrl": "https://bucket.s3.region.amazonaws.com/labs/5/signatures/uuid-signature.png?X-Amz-...",
    "fileUrl": "https://bucket.s3.region.amazonaws.com/labs/5/signatures/uuid-signature.png",
    "headers": { "Content-Type": "image/png" }
  }
}
```

**Backend behavior:**
1. Generate a unique S3 key (e.g. `labs/{labId}/signatures/{uuid}-{fileName}`).
2. Create a presigned PUT URL for the S3 object.
3. Return `uploadUrl` (for frontend PUT) and `fileUrl` (final URL to store).
4. Optionally return `headers` if S3 requires specific headers (e.g. `Content-Type`, `x-amz-acl`).

**Frontend flow:**
1. User selects an image file for a role’s signature.
2. Frontend calls this endpoint with `fileName` and `fileType`.
3. Frontend `PUT`s the file to `uploadUrl` with the same `Content-Type`.
4. On success, frontend stores `fileUrl` in the role’s `signatureUrl` and saves settings.

---

## 3. Frontend Flow

### 3.1 Architecture

```
page.tsx (Report Settings Page)
├── State: all setting values (template, header, typography, signature, disclaimer, roles)
├── On Mount: GET /lab/{labId}/report-settings → populate state (or use defaults if 404)
├── Save Button: POST (if new) or PUT (if exists) /lab/{labId}/report-settings
├── Reset Button: Re-fetch from server or reset to hardcoded defaults
│
├── TemplateSelector component
│   └── Displays template cards, fires onChange(templateId)
│
├── Header section (checkboxes)
├── Typography section (font size slider, text size dropdown, color picker)
├── Signature Placement section (placement buttons, columns dropdown)
├── Disclaimer section (enable toggle + textarea)
├── Roles & Signatures section (add/remove/reorder/edit roles)
│
└── TemplatePreview component (live preview sidebar)
    └── Receives all settings as props, renders mock report
```

### 3.2 Data Flow

```
┌─────────────┐      GET /report-settings       ┌──────────┐
│  Frontend   │ ──────────────────────────────►  │ Backend  │
│  Page Load  │ ◄──────────────────────────────  │          │
│             │      200: settings JSON          │          │
│             │      404: use defaults           │          │
└──────┬──────┘                                  └──────────┘
       │
       │ User edits settings (local state)
       ▼
┌─────────────┐     POST or PUT /report-settings  ┌──────────┐
│  Save Click │ ──────────────────────────────►   │ Backend  │
│             │ ◄──────────────────────────────   │          │
│             │      200/201: saved settings      │          │
└─────────────┘                                   └──────────┘
```

### 3.3 Service Functions (Frontend)

Located in `services/reportServices.ts`:

| Function                     | HTTP Method | Endpoint                                        | Purpose                                |
|------------------------------|-------------|-------------------------------------------------|----------------------------------------|
| `getReportSettings`          | GET         | `/lab/{labId}/report-settings`                  | Fetch saved settings on page load      |
| `saveReportSettings`         | POST        | `/lab/{labId}/report-settings`                  | First-time save                        |
| `updateReportSettings`       | PUT         | `/lab/{labId}/report-settings`                  | Update existing settings               |
| `getReportSignatureUploadUrl`| POST        | `/lab/{labId}/report-settings/signature/upload-url` | Get presigned S3 URL for signature upload |

### 3.4 TypeScript Types (Frontend)

Located in `src/types/reportSettings.ts`:

| Type                      | Purpose                                           |
|---------------------------|---------------------------------------------------|
| `TemplateId`              | Union type: `'templateA' \| 'templateB' \| 'templateC'` |
| `SignaturePlacement`      | Union type: `'top-right' \| 'top-left' \| 'bottom-right' \| 'bottom-left'` |
| `TextSizePreset`          | Union type: `'Small' \| 'Medium' \| 'Large'`     |
| `SignatureColumns`        | Union type: `2 \| 3 \| 4`                        |
| `ReportRoleSetting`       | Individual role/signature entry                   |
| `ReportSettingsPayload`   | Request body sent to POST/PUT                     |
| `ReportSettingsResponse`  | Full response from GET/POST/PUT                   |

---

## 4. Field Reference

| Setting              | UI Control       | DB Column              | Type        | Allowed Values                                    | Default           |
|----------------------|------------------|------------------------|-------------|---------------------------------------------------|--------------------|
| Template             | Card selector    | `template_id`          | VARCHAR(30) | `templateA`, `templateB`, `templateC`             | `templateA`        |
| Header Enabled       | Checkbox         | `header_enabled`       | BOOLEAN     | `true`, `false`                                   | `true`             |
| Header Required      | Checkbox         | `header_required`      | BOOLEAN     | `true`, `false`                                   | `false`            |
| Font Size            | Range slider     | `font_size`            | INT         | `10` – `18`                                       | `12`               |
| Text Size            | Dropdown         | `text_size`            | VARCHAR(10) | `Small`, `Medium`, `Large`                        | `Medium`           |
| Text Color           | Color picker     | `text_color`           | VARCHAR(10) | Any valid hex code (e.g., `#111827`)              | `#111827`          |
| Signature Placement  | Button group     | `signature_placement`  | VARCHAR(20) | `top-right`, `top-left`, `bottom-right`, `bottom-left` | `bottom-right` |
| Signatures Per Row   | Dropdown         | `signature_columns`    | INT         | `2`, `3`, `4`                                     | `2`                |
| Disclaimer Enabled   | Checkbox         | `disclaimer_enabled`   | BOOLEAN     | `true`, `false`                                   | `true`             |
| Disclaimer Text      | Textarea         | `disclaimer_text`      | TEXT        | Free text                                         | *(see default)*    |

### Roles (per role entry):

| Field            | UI Control   | DB Column        | Type         | Description                              |
|------------------|-------------|------------------|--------------|------------------------------------------|
| Role             | Text input   | `role`           | VARCHAR(100) | Label (e.g., Doctor, Technician)         |
| Display Name     | Text input   | `display_name`   | VARCHAR(200) | Name shown on report                     |
| Designation      | Text input   | `designation`    | VARCHAR(200) | Title/degree (e.g., Consultant Pathologist) |
| Signature        | File upload | `signature_url`  | TEXT         | S3 URL (populated after upload via presigned URL) |
| Enabled          | Checkbox     | `enabled`        | BOOLEAN      | Show/hide this role on report            |
| Sort Order       | Up/Down btns | `sort_order`     | INT          | Display order (0 = first)                |

---

## 5. Backend Implementation Notes

### 5.1 Controller: `ReportSettingsController`

```
@RestController
@RequestMapping("/lab/{labId}/report-settings")
```

| Method | Endpoint | Action                                                     |
|--------|----------|-------------------------------------------------------------|
| GET    | `/`      | Fetch `report_settings` + join `report_role_settings`       |
| POST   | `/`      | Insert `report_settings` + insert roles                     |
| PUT    | `/`      | Update `report_settings` + delete old roles + insert new    |

### 5.2 Service Layer

```java
public class ReportSettingsService {

    public ReportSettingsResponse getByLabId(Long labId);

    public ReportSettingsResponse create(Long labId, ReportSettingsPayload payload);

    public ReportSettingsResponse update(Long labId, ReportSettingsPayload payload);
}
```

### 5.3 Entity Mapping

```
ReportSettings (Entity)
├── id, labId, templateId, headerEnabled, headerRequired
├── fontSize, textSize, textColor
├── signaturePlacement, signatureColumns
├── disclaimerEnabled, disclaimerText
├── createdAt, updatedAt
└── @OneToMany → List<ReportRoleSetting>

ReportRoleSetting (Entity)
├── id, reportSettingId
├── role, displayName, designation, signatureUrl
├── enabled, sortOrder
└── createdAt, updatedAt
```

### 5.4 Key Rules

1. **One setting per lab** — enforce via `UNIQUE(lab_id)` constraint.
2. **Roles are replaced on update** — delete all old roles, insert new ones from payload. This avoids complex diff logic.
3. **Sort order comes from array index** — frontend sends roles in order; backend sets `sort_order = index`.
4. **Validate enum values** — `templateId`, `textSize`, `signaturePlacement`, `signatureColumns` should be validated server-side.
5. **Default fallback** — if GET returns 404, frontend uses hardcoded defaults (no empty-state error).

---

## 6. How Report Settings Are Used at Report Generation

When generating/printing a patient report:

1. **Fetch settings**: `GET /lab/{labId}/report-settings`
2. **Apply template**: Use `templateId` to determine layout structure (header style, spacing, signature layout).
3. **Apply typography**: Set `fontSize`, `textSize`, `textColor` on the report body.
4. **Header**: If `headerEnabled` is true, render the lab header. If `headerRequired` is true, always show it (even in PDF).
5. **Signatures**: Place enabled roles in the `signaturePlacement` position, arranged in `signatureColumns` columns, ordered by `sortOrder`.
6. **Disclaimer**: If `disclaimerEnabled` is true, render `disclaimerText` at the report footer.

---

## 7. Default Values (Frontend Fallback)

When no settings exist in the database (GET returns 404), the frontend uses:

```typescript
const DEFAULTS = {
  templateId: 'templateA',
  headerEnabled: true,
  headerRequired: false,
  fontSize: 12,
  textSize: 'Medium',
  textColor: '#111827',
  signaturePlacement: 'bottom-right',
  signatureColumns: 2,
  disclaimerEnabled: true,
  disclaimerText: 'This laboratory report is intended for clinical correlation only. Results should be interpreted by a qualified medical professional.',
  roles: [
    { role: 'Doctor', displayName: '', designation: '', signatureUrl: '', enabled: true, sortOrder: 0 },
    { role: 'Technician', displayName: '', designation: '', signatureUrl: '', enabled: true, sortOrder: 1 },
  ],
};
```
