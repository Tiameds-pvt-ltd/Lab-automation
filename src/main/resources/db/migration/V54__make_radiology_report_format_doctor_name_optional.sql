-- Doctor name is optional when uploading a radiology report format.
ALTER TABLE radiology_report_format ALTER COLUMN doctor_name DROP NOT NULL;
