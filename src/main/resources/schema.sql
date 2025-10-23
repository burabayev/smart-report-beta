-- 1) таблица показаний
CREATE TABLE IF NOT EXISTS telemetry.readings (
    device_id     text        NOT NULL,
    gateway_imei  text        NOT NULL,
    type          text        NOT NULL,
    sensor_ts     timestamptz NOT NULL,
    ingest_ts     timestamptz NOT NULL,
    temperature_c double precision NULL,
    humidity_pct  double precision NULL,
    battery_v     double precision NOT NULL,
    rssi_dbm      smallint    NOT NULL,
    PRIMARY KEY (device_id, sensor_ts)
);

-- 2) сделать её гипертаблицей (без ошибок, если уже сделана)
SELECT create_hypertable(
               'telemetry.readings',
               'sensor_ts',
               chunk_time_interval => INTERVAL '7 day',
               if_not_exists => TRUE
       );

-- 3) индексы
CREATE INDEX IF NOT EXISTS idx_readings_ts
    ON telemetry.readings (sensor_ts DESC);

CREATE INDEX IF NOT EXISTS idx_readings_device_ts
    ON telemetry.readings (device_id, sensor_ts DESC);


-- инвентарь устройств/шлюзов
CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE IF NOT EXISTS inventory.gateways (
    imei          text PRIMARY KEY,
    first_seen    timestamptz NOT NULL DEFAULT now(),
    last_seen     timestamptz NOT NULL,
    firmware      text,
    last_batt_v   double precision,
    last_input_v  double precision
);

CREATE INDEX IF NOT EXISTS idx_gateways_last_seen ON inventory.gateways(last_seen DESC);

CREATE TABLE IF NOT EXISTS inventory.devices (
    device_id         text PRIMARY KEY,
    model             text,                       -- TAG08 / TAG08B (эвристика по наличию влажности)
    first_seen        timestamptz NOT NULL DEFAULT now(),
    last_seen         timestamptz NOT NULL,
    last_gateway_imei text,
    last_rssi         smallint,
    last_batt_v       double precision,
    last_temp_c       double precision,
    last_humidity_pct double precision,
    state             text NOT NULL DEFAULT 'unknown'   -- online|stale|unknown
);

CREATE INDEX IF NOT EXISTS idx_devices_last_seen ON inventory.devices(last_seen DESC);
CREATE INDEX IF NOT EXISTS idx_devices_state    ON inventory.devices(state);
CREATE INDEX IF NOT EXISTS idx_devices_model    ON inventory.devices(model);