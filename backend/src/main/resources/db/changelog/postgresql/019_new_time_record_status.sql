--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.19 dbms:postgresql

INSERT INTO status(name, type) VALUES
    ('DRAFT', 'Other');