--liquibase formatted sql
--changeset luis.passos:1.0.0.30 dbms:postgresql

ALTER TABLE users ADD COLUMN admission_date DATE;
ALTER TABLE users ADD COLUMN exit_date DATE;
