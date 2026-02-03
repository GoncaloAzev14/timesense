--liquibase formatted sql
--changeset andre.vale:1.0.0.32 dbms:postgresql

-- Create indexes to optimize absence queries
CREATE INDEX IF NOT EXISTS idx_absence_start_end_date 
ON absences (start_date, end_date) 
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_absence_user_id 
ON absences (user_id) 
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_absence_status_id 
ON absences (status_id) 
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_absence_type_id 
ON absences (type_id) 
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_absence_business_year 
ON absences (business_year) 
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_absence_composite 
ON absences (start_date, end_date, user_id, status_id, type_id) 
WHERE deleted = false;
