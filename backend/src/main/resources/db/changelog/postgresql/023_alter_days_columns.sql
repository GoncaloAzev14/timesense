--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.23 dbms:postgresql

ALTER TABLE users 
ALTER COLUMN current_year_vacation_days TYPE double precision;

ALTER TABLE users 
ALTER COLUMN prev_year_vacation_days TYPE double precision;

ALTER TABLE absences
ALTER COLUMN work_days TYPE double precision;