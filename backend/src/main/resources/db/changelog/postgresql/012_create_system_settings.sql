--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.12 dbms:postgresql

CREATE TABLE IF NOT EXISTS system_settings (
    id SERIAL,
    name VARCHAR(100) NOT NULL,
    value VARCHAR(100) NOT NULL,
    updated_by BIGINT,
    deleted boolean
);

ALTER TABLE system_settings ADD CONSTRAINT system_settings_pk PRIMARY KEY (id);
ALTER TABLE system_settings ADD CONSTRAINT system_settings_updated_by_fk
    FOREIGN KEY (updated_by) REFERENCES users (id);

INSERT INTO system_settings (name, value, updated_by, deleted)
VALUES ('project_max_hours_per_day', '8', null, false);
