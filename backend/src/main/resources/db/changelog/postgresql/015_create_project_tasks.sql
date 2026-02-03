--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.15 dbms:postgresql

CREATE TABLE IF NOT EXISTS project_tasks (
    id SERIAL, 
    name VARCHAR(100) NOT NULL, 
    description VARCHAR(100),
    updated_by BIGINT,
    deleted boolean DEFAULT false
);

ALTER TABLE project_tasks ADD CONSTRAINT project_tasks_pk PRIMARY KEY (id);
ALTER TABLE project_tasks ADD CONSTRAINT project_tasks_updated_by_fk
    FOREIGN KEY (updated_by) REFERENCES users (id);

ALTER TABLE time_records ADD COLUMN task_id BIGINT;
ALTER TABLE time_records ADD CONSTRAINT time_record_tasks_fk 
    FOREIGN KEY (task_id) REFERENCES project_tasks(id);

INSERT INTO project_tasks (name) VALUES 
    ('Analysis');
INSERT INTO project_tasks (name) VALUES 
    ('Development');
INSERT INTO project_tasks (name) VALUES 
    ('Support');
INSERT INTO project_tasks (name) VALUES 
    ('Testing');


INSERT INTO system_settings (name, value, updated_by, deleted)
    VALUES ('default_vacation_days', '23', null, false);
