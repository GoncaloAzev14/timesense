--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.21 dbms:postgresql

ALTER TABLE projects ADD COLUMN real_budget DECIMAL DEFAULT 0.00;