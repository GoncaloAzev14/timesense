--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.24 dbms:postgresql

ALTER TABLE absences 
RENAME COLUMN vacation_type TO record_type;

ALTER TABLE absences 
ADD COLUMN absence_hours double precision;