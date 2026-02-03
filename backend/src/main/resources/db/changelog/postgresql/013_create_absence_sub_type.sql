--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.13 dbms:postgresql

CREATE TABLE IF NOT EXISTS absence_sub_types (
    id SERIAL, 
    name VARCHAR(100) NOT NULL, 
    description VARCHAR(100),
    updated_by BIGINT,
    deleted boolean DEFAULT false
);

ALTER TABLE absence_sub_types ADD CONSTRAINT absence_sub_types_pk PRIMARY KEY (id);
ALTER TABLE absence_sub_types ADD CONSTRAINT absence_sub_types_updated_by_fk
    FOREIGN KEY (updated_by) REFERENCES users (id);

ALTER TABLE absences ADD COLUMN sub_type_id BIGINT;
ALTER TABLE absences ADD CONSTRAINT absences_sub_types_fk
    FOREIGN KEY (sub_type_id) REFERENCES absence_sub_types(id);

DELETE FROM absence_types WHERE name IN ('SICKNESS','LICENSE');
INSERT INTO absence_types (name) VALUES
    ('ABSENCES');

INSERT INTO absence_sub_types (name) VALUES
    ('Maternity Leave');
INSERT INTO absence_sub_types (name) VALUES
    ('Paternity Leave');
INSERT INTO absence_sub_types (name) VALUES
    ('Bereavement Leave');
INSERT INTO absence_sub_types (name) VALUES
    ('Birthday Leave');

DELETE FROM status WHERE name IN ('IN_PROGRESS', 'SCHEDULED', 'FINISHED')
    AND type is null;