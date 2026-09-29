UPDATE public.patients p
SET first_name = 'Mr.Chikkamallaya'
FROM public.patient_visits pv
WHERE pv.patient_id = p.patient_id
  AND pv.visit_code = 'VIS23-01562';
