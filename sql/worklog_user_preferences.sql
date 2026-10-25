-- Run once against the srghit PostgreSQL database on the current primary.
CREATE TABLE IF NOT EXISTS public.worklog_user_preferences (
    username varchar(50) PRIMARY KEY REFERENCES public.account(username) ON DELETE CASCADE,
    dark_mode boolean NOT NULL,
    legacy_mode boolean NOT NULL,
    font_scale integer NOT NULL CHECK (font_scale BETWEEN 100 AND 140),
    floating_x integer,
    floating_y integer,
    CHECK ((floating_x IS NULL) = (floating_y IS NULL))
);
ALTER TABLE public.worklog_user_preferences OWNER TO semin5;
