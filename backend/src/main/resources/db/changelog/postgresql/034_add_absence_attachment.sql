--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.30 dbms:postgresql

CREATE TABLE IF NOT EXISTS absence_attachments (
    id SERIAL,
    absence_id BIGINT,
    original_file_name VARCHAR(100),
    storage_object_id VARCHAR(100)
);

ALTER TABLE absence_attachments ADD CONSTRAINT absence_attachments_pk PRIMARY KEY (id);
ALTER TABLE absence_attachments ADD CONSTRAINT absence_attachments_absence_id_fk
    FOREIGN KEY (absence_id) REFERENCES absences (id);

CREATE INDEX IF NOT EXISTS absence_attachments_absence_id_idx
    ON absence_attachments (absence_id);

ALTER TABLE absences
ADD COLUMN has_attachments BOOLEAN DEFAULT FALSE;
