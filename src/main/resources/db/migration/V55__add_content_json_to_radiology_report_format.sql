-- Text and tables of the uploaded Word format, parsed to JSON on upload.
ALTER TABLE radiology_report_format ADD COLUMN IF NOT EXISTS content_json JSONB;
