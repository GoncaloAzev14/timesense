--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.2 dbms:postgresql

CREATE TABLE IF NOT EXISTS clients (
    id SERIAL, 
    name VARCHAR(100),
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE clients ADD CONSTRAINT clients_id_pk
    PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS status (
    id SERIAL, 
    name VARCHAR(100),
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE status ADD CONSTRAINT status_id_pk
    PRIMARY KEY (id);


CREATE TABLE IF NOT EXISTS holidays (
    id SERIAL,
    holiday_date DATE,
    name VARCHAR(100),
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE holidays ADD CONSTRAINT holidays_id_pk
    PRIMARY KEY (id);  
