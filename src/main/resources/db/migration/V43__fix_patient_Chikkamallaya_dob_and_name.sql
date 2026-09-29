UPDATE public.patients
SET
    date_of_birth = '1976-01-01',
    first_name = 'Mr Chikkamallaya'
WHERE patient_id = (
    SELECT DISTINCT pv.patient_id
    FROM public.patient_visits pv
    JOIN public.lab_visit lv ON pv.visit_id = lv.visit_id
    WHERE lv.lab_id = 23
      AND pv.visit_code = 'VIS23-01562'
);
