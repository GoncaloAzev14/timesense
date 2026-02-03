--liquibase formatted sql
--changeset luis.passos:1.0.0.11 dbms:postgresql

ALTER TABLE time_records ADD COLUMN reason VARCHAR(100);
