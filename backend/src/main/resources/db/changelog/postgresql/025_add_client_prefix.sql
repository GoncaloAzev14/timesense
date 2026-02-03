--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.25 dbms:postgresql

ALTER TABLE clients
ADD COLUMN client_ticker VARCHAR(100);
