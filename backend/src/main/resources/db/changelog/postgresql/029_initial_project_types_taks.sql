--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.29 dbms:postgresql

INSERT INTO project_type_tasks (project_task_id, project_type_id)
SELECT t.id AS project_task_id,
       pt.id AS project_type_id
FROM tasks t
CROSS JOIN project_types pt
WHERE NOT EXISTS (
    SELECT 1
    FROM project_type_tasks ptt
    WHERE ptt.project_task_id = t.id
      AND ptt.project_type_id = pt.id
);