--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.22 dbms:postgresql

ALTER TABLE time_records 
ALTER COLUMN description TYPE TEXT;