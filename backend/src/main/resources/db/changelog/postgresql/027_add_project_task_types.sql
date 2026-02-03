--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.27 dbms:postgresql

-- Rename project_tasks -> tasks
ALTER TABLE project_tasks RENAME TO tasks;
ALTER TABLE tasks RENAME CONSTRAINT project_tasks_pk TO tasks_pk;

-- Create JOIN TABLE for many-to-many relation between tasks and types
CREATE TABLE IF NOT EXISTS project_type_tasks (
    project_task_id BIGINT NOT NULL,
    project_type_id BIGINT NOT NULL,

    CONSTRAINT project_type_tasks_pk PRIMARY KEY (project_task_id, project_type_id),

    CONSTRAINT project_type_tasks_task_fk
        FOREIGN KEY (project_task_id) REFERENCES tasks(id),

    CONSTRAINT project_type_tasks_type_fk
        FOREIGN KEY (project_type_id) REFERENCES project_types(id)
);

