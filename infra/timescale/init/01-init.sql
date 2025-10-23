-- выполняется при первом старте контейнера в БД smartreport
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- (опционально) создадим схему под будущие таблицы телеметрии
CREATE SCHEMA IF NOT EXISTS telemetry;