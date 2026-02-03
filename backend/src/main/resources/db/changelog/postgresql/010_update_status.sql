--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.10 dbms:postgresql

ALTER TABLE status ADD COLUMN type VARCHAR(100);

UPDATE status
SET type = 'General'
WHERE name = 'PENDING';

UPDATE status
SET type = 'Other'
WHERE name IN ('CANCELLED', 'APPROVED', 'DENIED', 'DONE');

INSERT INTO status(name, type) VALUES
    ('OPEN', 'Project');
INSERT INTO status(name, type) VALUES
    ('FINISHED', 'Project');