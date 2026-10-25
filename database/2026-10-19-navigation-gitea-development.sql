BEGIN;

INSERT INTO navigation_site (site_code, name, url, icon_key, sort_order, external)
SELECT 'gitea', 'Gitea', '/gitea', 'cloud',
       COALESCE((SELECT MAX(sort_order) FROM navigation_site), 0) + 1, false
WHERE NOT EXISTS (SELECT 1 FROM navigation_site WHERE site_code = 'gitea');

INSERT INTO navigation_site (site_code, name, url, icon_key, sort_order, external)
SELECT 'development', '개발·배포', '/development', 'tools',
       COALESCE((SELECT MAX(sort_order) FROM navigation_site), 0) + 1, true
WHERE NOT EXISTS (SELECT 1 FROM navigation_site WHERE site_code = 'development');

COMMIT;
