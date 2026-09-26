-- Portable SQL: runs on PostgreSQL and on H2 (PostgreSQL mode) used by the tests.

CREATE TABLE parking_lot (
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(100) NOT NULL,
    grace_period_minutes  INTEGER      NOT NULL CHECK (grace_period_minutes >= 0),
    first_hour_cents      BIGINT       NOT NULL CHECK (first_hour_cents >= 0),
    fraction_minutes      INTEGER      NOT NULL CHECK (fraction_minutes > 0),
    fraction_cents        BIGINT       NOT NULL CHECK (fraction_cents >= 0),
    daily_cap_cents       BIGINT       NOT NULL CHECK (daily_cap_cents >= first_hour_cents),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE parking_spot (
    id        UUID PRIMARY KEY,
    lot_id    UUID        NOT NULL REFERENCES parking_lot (id),
    code      VARCHAR(20) NOT NULL,
    type      VARCHAR(20) NOT NULL,
    occupied  BOOLEAN     NOT NULL,
    version   BIGINT      NOT NULL,
    CONSTRAINT uk_spot_lot_code UNIQUE (lot_id, code)
);

CREATE TABLE reservation (
    id             UUID PRIMARY KEY,
    spot_id        UUID        NOT NULL REFERENCES parking_spot (id),
    license_plate  VARCHAR(7)  NOT NULL,
    starts_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_reservation_window CHECK (ends_at > starts_at)
);

CREATE INDEX ix_reservation_spot_window ON reservation (spot_id, starts_at, ends_at);
CREATE INDEX ix_reservation_plate ON reservation (license_plate);

CREATE TABLE parking_session (
    id              UUID PRIMARY KEY,
    spot_id         UUID       NOT NULL REFERENCES parking_spot (id),
    reservation_id  UUID       REFERENCES reservation (id),
    license_plate   VARCHAR(7) NOT NULL,
    -- equals license_plate while the session is open, NULL once closed:
    -- the UNIQUE constraint stops one car from holding two open sessions, even under races
    open_plate      VARCHAR(7),
    entry_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    exit_at         TIMESTAMP WITH TIME ZONE,
    amount_cents    BIGINT,
    CONSTRAINT uk_session_open_plate UNIQUE (open_plate)
);

CREATE INDEX ix_session_spot ON parking_session (spot_id);
