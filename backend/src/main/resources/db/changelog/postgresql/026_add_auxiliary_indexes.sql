--liquibase formatted sql
--changeset luis.passos:1.0.0.26 dbms:postgresql

-- index to support no duplicate project codes\
CREATE INDEX IF NOT EXISTS unique_projects_project_name
    ON projects (name);

-- Indexes to optimize queries on time records
CREATE INDEX IF NOT EXISTS idx_time_records_project
    ON time_records (project_id, start_date);

CREATE INDEX IF NOT EXISTS idx_time_records_status
    ON time_records (status_id, project_id);

-- Indexes to optimize queries on absences
CREATE INDEX IF NOT EXISTS idx_absences_status
    ON absences (status_id, approver);

CREATE INDEX IF NOT EXISTS idx_absences_business_year
    ON absences (business_year);

CREATE INDEX IF NOT EXISTS idx_absences_type
    ON absences (type_id, user_id);

CREATE INDEX IF NOT EXISTS idx_absences_user
    ON absences (user_id, type_id);
