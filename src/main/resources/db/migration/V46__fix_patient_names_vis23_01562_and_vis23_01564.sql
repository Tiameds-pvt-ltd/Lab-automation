DO $$
DECLARE
    shared_patient_id BIGINT;
    new_patient_id    BIGINT;
    new_patient_code  TEXT;
BEGIN
    SELECT DISTINCT pv.patient_id INTO shared_patient_id
    FROM public.patient_visits pv
    WHERE pv.visit_code = 'VIS23-01564'
    LIMIT 1;

    SELECT COALESCE(patient_code, 'PAT-' || shared_patient_id::TEXT) || '-VIS01564'
    INTO new_patient_code
    FROM public.patients WHERE patient_id = shared_patient_id;

    INSERT INTO public.patients (
        first_name, last_name, email, phone, address, city, state, zip,
        blood_group, date_of_birth, age, gender, created_by, patient_code
    )
    SELECT first_name, last_name, email, phone, address, city, state, zip,
        blood_group, date_of_birth, age, gender, created_by, new_patient_code
    FROM public.patients WHERE patient_id = shared_patient_id
    RETURNING patient_id INTO new_patient_id;

    INSERT INTO public.lab_patients (patient_id, lab_id) VALUES (new_patient_id, 23) ON CONFLICT DO NOTHING;

    UPDATE public.patient_visits SET patient_id = new_patient_id WHERE visit_code = 'VIS23-01564';

    UPDATE public.patients p SET first_name = 'Mr.Chikkamallaya'
    FROM public.patient_visits pv
    WHERE pv.patient_id = p.patient_id
      AND pv.visit_code = 'VIS23-01562';

    UPDATE public.patients p SET first_name = 'Mr rangaswamy'
    FROM public.patient_visits pv
    WHERE pv.patient_id = p.patient_id
      AND pv.visit_code = 'VIS23-01564';

END $$;
