
--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.9 dbms:postgresql

CREATE TABLE IF NOT EXISTS job_titles (
    id SERIAL, 
    name VARCHAR(100) NOT NULL,
    rate DECIMAL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);
ALTER TABLE job_titles ADD CONSTRAINT job_titles_pk PRIMARY KEY (id);

ALTER TABLE users 
ALTER COLUMN job_title TYPE BIGINT
USING job_title::BIGINT;

ALTER TABLE users ADD CONSTRAINT user_job_title_fk FOREIGN KEY (job_title) 
    REFERENCES job_titles (id);


INSERT INTO job_titles (name, rate, start_date, end_date) VALUES
    ('Trainee', 6.00, CURRENT_TIMESTAMP, null),
    ('Internship', 7.00, CURRENT_TIMESTAMP, null),
    ('Junior Developer I', 8.00, CURRENT_TIMESTAMP, null),
    ('Junior Developer II', 8.50, CURRENT_TIMESTAMP, null),
    ('Technician I', 9.00, CURRENT_TIMESTAMP, null),
    ('Technician II', 9.25, CURRENT_TIMESTAMP, null),
    ('Technician III', 9.50, CURRENT_TIMESTAMP, null),
    ('Technical Lead I', 10.00, CURRENT_TIMESTAMP, null),
    ('Technical Lead II', 11.00, CURRENT_TIMESTAMP, null),
    ('Senior Consultant I', 12.00, CURRENT_TIMESTAMP, null),
    ('Senior Manager', 13.00, CURRENT_TIMESTAMP, null),
    ('Partner', 14.00, CURRENT_TIMESTAMP, null);
    
