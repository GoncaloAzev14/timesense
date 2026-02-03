--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.1 dbms:postgresql

CREATE TABLE IF NOT EXISTS users (
    id SERIAL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    birthdate DATE,
    line_manager BIGINT,
    current_year_vacation_days BIGINT,
    prev_year_vacation_days BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE users ADD CONSTRAINT users_pk PRIMARY KEY (id);
ALTER TABLE users ADD CONSTRAINT user_line_manager_fk FOREIGN KEY (line_manager) REFERENCES users (id);
ALTER TABLE users ADD CONSTRAINT users_created_by_fk
    FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE users ADD CONSTRAINT users_updated_by_fk
    FOREIGN KEY (updated_by) REFERENCES users (id);
 