--liquibase formatted sql
--changeset eduardo.novo:1.0.0.3 dbms:postgresql

CREATE TABLE IF NOT EXISTS project_types (
    id SERIAL, 
    name VARCHAR(100),
    description VARCHAR,
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE project_types ADD CONSTRAINT project_types_id_pk
    PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS projects (
    id SERIAL,
    name VARCHAR(100),
    description VARCHAR(100),
    type_id BIGINT,
    manager BIGINT,
    client_id BIGINT,
    start_date TIMESTAMP,
    expected_due_date TIMESTAMP,
    end_date TIMESTAMP,
    status_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE projects ADD CONSTRAINT projects_type_fk
    FOREIGN KEY (type_id) REFERENCES project_types (id);

ALTER TABLE projects ADD CONSTRAINT projects_client_fk
    FOREIGN KEY (client_id) REFERENCES clients (id);

ALTER TABLE projects ADD CONSTRAINT projects_status_fk
    FOREIGN KEY (status_id) REFERENCES status (id);

ALTER TABLE projects ADD CONSTRAINT projects_id_pk
    PRIMARY KEY (id);

ALTER TABLE projects ADD CONSTRAINT projects_manager_fk
    FOREIGN KEY (manager) REFERENCES users (id);

ALTER TABLE projects ADD CONSTRAINT projects_created_by_fk
    FOREIGN KEY (created_by) REFERENCES users (id);
    
ALTER TABLE projects ADD CONSTRAINT projects_updated_by_fk
    FOREIGN KEY (updated_by) REFERENCES users (id);


CREATE TABLE IF NOT EXISTS project_assignments (
    id SERIAL,
    user_id BIGINT,
    project_id BIGINT,
    allocation DECIMAL,
    description VARCHAR(100),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE project_assignments ADD CONSTRAINT project_assignments_id_pk
    PRIMARY KEY (id);

ALTER TABLE project_assignments ADD CONSTRAINT project_assignments_user_id_fk
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE project_assignments ADD CONSTRAINT project_assignments_project_id_fk
    FOREIGN KEY (project_id) REFERENCES projects (id);

ALTER TABLE project_assignments ADD CONSTRAINT project_assignments_created_by_fk
    FOREIGN KEY (created_by) REFERENCES users (id);
    
ALTER TABLE project_assignments ADD CONSTRAINT project_assignments_updated_by_fk
    FOREIGN KEY (updated_by) REFERENCES users (id);

CREATE TABLE IF NOT EXISTS time_records (
    id SERIAL,
    user_id BIGINT,
    project_id BIGINT,
    hours DECIMAL,
    description VARCHAR(100),
    status_id BIGINT,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE time_records ADD CONSTRAINT time_records_id_pk
    PRIMARY KEY (id);

ALTER TABLE time_records ADD CONSTRAINT time_records_user_id_fk
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE time_records ADD CONSTRAINT time_records_project_id_fk
    FOREIGN KEY (project_id) REFERENCES projects (id);

ALTER TABLE time_records ADD CONSTRAINT time_records_approved_by_fk
    FOREIGN KEY (approved_by) REFERENCES users (id);

ALTER TABLE time_records ADD CONSTRAINT time_records_created_by_fk
    FOREIGN KEY (created_by) REFERENCES users (id);
    
ALTER TABLE time_records ADD CONSTRAINT time_records_updated_by_fk
    FOREIGN KEY (updated_by) REFERENCES users (id);

ALTER TABLE time_records ADD CONSTRAINT time_records_status_id_fk
    FOREIGN KEY (status_id) REFERENCES status (id);

ALTER TABLE time_records ADD CONSTRAINT unique_user_project_start 
    UNIQUE (user_id, project_id, start_date);