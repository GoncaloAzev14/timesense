--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.20 dbms:postgresql

INSERT INTO system_settings (name, value, updated_by, deleted)
    VALUES ('default_time_records_prev_weeks', '5', null, false);