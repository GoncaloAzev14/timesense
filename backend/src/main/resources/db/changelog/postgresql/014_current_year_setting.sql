--liquibase formatted sql
--changeset luis.passos:1.0.0.14 dbms:postgresql

ALTER TABLE system_settings ADD user_editable BOOLEAN default true;

INSERT INTO system_settings (name, value, updated_by, deleted, user_editable)
VALUES ('current_year', date_part('year', current_date), null, false, false);
