-- Apply to srghit on the PostgreSQL 5432 primary before deploying the new application.
-- Preserve the existing designated device and its active session token.
BEGIN;
ALTER TABLE public.designated_device ADD COLUMN IF NOT EXISTS id uuid;
UPDATE public.designated_device SET id = gen_random_uuid() WHERE id IS NULL;
ALTER TABLE public.designated_device ALTER COLUMN id SET NOT NULL;
ALTER TABLE public.designated_device ALTER COLUMN username SET NOT NULL;
ALTER TABLE public.designated_device DROP CONSTRAINT IF EXISTS designated_device_pkey;
ALTER TABLE public.designated_device ADD CONSTRAINT designated_device_pkey PRIMARY KEY (id);
CREATE UNIQUE INDEX IF NOT EXISTS designated_device_token_hash_key ON public.designated_device (token_hash);
CREATE INDEX IF NOT EXISTS designated_device_username_idx ON public.designated_device (username);
COMMIT;
