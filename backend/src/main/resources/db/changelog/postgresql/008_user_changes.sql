
--liquibase formatted sql
--changeset luis.passos:1.0.0.8 dbms:postgresql

ALTER TABLE users
    ADD COLUMN job_title VARCHAR(100);
