-- Optional note the reviewing admin can leave when approving a request (rejections keep using rejection_reason).
ALTER TABLE public.notification ADD COLUMN IF NOT EXISTS review_remark TEXT;
