BEGIN;

ALTER TABLE navigation_site
    ADD COLUMN IF NOT EXISTS external boolean NOT NULL DEFAULT false;

UPDATE navigation_site SET external = true WHERE site_code = 'nms';

INSERT INTO navigation_site (site_code, name, url, icon_key, sort_order, external)
SELECT 'monitoring', '모니터링',
       '/monitoring/d/server-monitoring/server-monitoring?orgId=1&from=now-1h&to=now&timezone=browser&refresh=auto',
       'monitor', (SELECT COALESCE(MAX(sort_order), 0) + 1 FROM navigation_site), true
WHERE NOT EXISTS (SELECT 1 FROM navigation_site WHERE site_code = 'monitoring');

COMMIT;
