--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.16 dbms:postgresql

ALTER TABLE time_records DROP CONSTRAINT unique_user_project_start;

ALTER TABLE time_records ADD CONSTRAINT unique_user_project_start_task
    UNIQUE (user_id, project_id, start_date, task_id);