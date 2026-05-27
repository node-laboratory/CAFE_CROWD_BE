CREATE TABLE cafes (
    id              VARCHAR(40)      PRIMARY KEY,
    name            VARCHAR(80)      NOT NULL,
    available       BOOLEAN          NOT NULL DEFAULT true,
    lat             DOUBLE PRECISION NOT NULL,
    lng             DOUBLE PRECISION NOT NULL,
    intro           TEXT             NOT NULL DEFAULT '',
    address         VARCHAR(200)     NOT NULL,
    thumbnail_url   VARCHAR(500)     NOT NULL,
    photos          JSONB            NOT NULL DEFAULT '[]'::jsonb,
    naver_place_url VARCHAR(500)     NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX idx_cafes_available ON cafes (available);
CREATE INDEX idx_cafes_location ON cafes (lat, lng);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cafes_updated_at
    BEFORE UPDATE ON cafes
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
