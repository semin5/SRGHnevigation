BEGIN;
UPDATE navigation_site SET url = '/worklog' WHERE site_code = 'work';
UPDATE user_settings SET work_path = '/worklog' WHERE work_path IS DISTINCT FROM '/worklog';
COMMIT;
