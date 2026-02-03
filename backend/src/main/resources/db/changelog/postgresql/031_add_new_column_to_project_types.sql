--liquibase formatted sql
--changeset andre.vale:1.0.0.31 dbms:postgresql

ALTER TABLE project_types ADD COLUMN line_manager BOOLEAN default false;