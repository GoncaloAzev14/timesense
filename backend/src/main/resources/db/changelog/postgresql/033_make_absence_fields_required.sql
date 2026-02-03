--liquibase formatted sql
--changeset andre.vale:1.0.0.33 dbms:postgresql

-- Make name and reason fields required (NOT NULL) in absences table
ALTER TABLE absences
    ALTER COLUMN name SET NOT NULL;
