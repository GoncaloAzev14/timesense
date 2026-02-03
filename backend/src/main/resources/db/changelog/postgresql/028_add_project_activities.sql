--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.28 dbms:postgresql

-- Create project activities table
CREATE TABLE IF NOT EXISTS project_tasks (
    project_id BIGINT NOT NULL,
    project_task_id BIGINT NOT NULL,

    CONSTRAINT project_tasks_project_fk
        FOREIGN KEY (project_id) REFERENCES projects(id),

    CONSTRAINT project_tasks_task_fk
        FOREIGN KEY (project_task_id) REFERENCES tasks(id),
    
    CONSTRAINT project_tasks_pk PRIMARY KEY (project_id, project_task_id)
);


