--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.16 dbms:postgresql

ALTER TABLE absences ADD COLUMN vacation_type VARCHAR(100) DEFAULT 'Day';
