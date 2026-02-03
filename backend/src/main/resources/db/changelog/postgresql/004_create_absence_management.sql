--liquibase formatted sql
--changeset eduardo.novo:1.0.0.4 dbms:postgresql

CREATE TABLE IF NOT EXISTS absence_types (
    id SERIAL,
    name VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE absence_types ADD CONSTRAINT absence_type_id_pk
    PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS absences (
    id SERIAL,
    type_id BIGINT,
    user_id BIGINT,
    name VARCHAR(100),
    approved_date TIMESTAMP WITH TIME ZONE,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    approver BIGINT,
    approved_by BIGINT,
    status_id BIGINT,
    reason VARCHAR(100),
    work_days BIGINT,
    business_year VARCHAR(10),
    observations VARCHAR(200),
    created_at TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT false
);

ALTER TABLE absences ADD CONSTRAINT absences_id_pk
    PRIMARY KEY (id);

ALTER TABLE absences ADD CONSTRAINT absences_type_id_fk
    FOREIGN KEY (type_id) REFERENCES absence_types (id);

ALTER TABLE absences ADD CONSTRAINT absences_status_fk
    FOREIGN KEY (status_id) REFERENCES status (id);

ALTER TABLE absences ADD CONSTRAINT absences_user_id_fk
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE absences ADD CONSTRAINT absences_approver_fk
    FOREIGN KEY (approver) REFERENCES users (id);

ALTER TABLE absences ADD CONSTRAINT absences_approved_by_fk
    FOREIGN KEY (approved_by) REFERENCES users (id);

ALTER TABLE absences ADD CONSTRAINT absences_created_by_fk
    FOREIGN KEY (created_by) REFERENCES users (id);
    
ALTER TABLE absences ADD CONSTRAINT absences_updated_by_fk
    FOREIGN KEY (updated_by) REFERENCES users (id);

