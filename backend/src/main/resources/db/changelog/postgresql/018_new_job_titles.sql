--liquibase formatted sql
--changeset luis.passos:1.0.0.18 dbms:postgresql

INSERT INTO job_titles (name, rate, start_date, end_date) VALUES
    ('HR Assistant', 6.00, CURRENT_TIMESTAMP, null),
    ('Outsourcer', 6.00, CURRENT_TIMESTAMP, null);
