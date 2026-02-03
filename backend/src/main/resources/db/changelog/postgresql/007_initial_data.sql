--liquibase formatted sql
--changeset tiago.gouveia:1.0.0.7 dbms:postgresql

--##############################################################
--################### Resource Types ###########################
--##############################################################
INSERT INTO resource_types (resource_type, main_table_name) VALUES
    ('System', '');
INSERT INTO resource_types (resource_type, main_table_name) VALUES
    ('Project', 'projects');

--##############################################################
--################### Default Application Roles#################
--##############################################################
INSERT INTO roles (name) VALUES
    ('User');
INSERT INTO roles (name, parent_id) VALUES
    ('Manager', (select id from roles where name = 'User'));
INSERT INTO roles (name, parent_id) VALUES
    ('Admin', (select id from roles where name = 'Manager'));

--##############################################################
--############## Default Application Permissions################
--##############################################################

INSERT INTO resource_permissions (
    resource_type, resource_id, access_type, subject_type, subject,
    created_by, created_at, updated_by, updated_at)
SELECT 'System', 0, access_type, 'role', roles.id, null, CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP
FROM (VALUES ('CREATE_PROJECTS'), ('MANAGE_SECURITY'), ('MANAGE_TIMEOFF')) AS a(access_type)
JOIN roles ON name = 'Admin';

--##############################################################
--################### STATUS ##################################
--##############################################################
INSERT INTO status (name) VALUES
    ('IN_PROGRESS');
INSERT INTO status (name) VALUES
    ('CANCELLED');
INSERT INTO status (name) VALUES
    ('SCHEDULED');
INSERT INTO status (name) VALUES
    ('FINISHED');
INSERT INTO status (name) VALUES
    ('APPROVED');
INSERT INTO status (name) VALUES
    ('PENDING');
INSERT INTO status (name) VALUES
    ('DENIED');
INSERT INTO status (name) VALUES
    ('DONE');

--##############################################################
--################### Default Absences Types ###################
--##############################################################
INSERT INTO absence_types (name) VALUES
    ('VACATION');
INSERT INTO absence_types (name) VALUES
    ('SICKNESS');
INSERT INTO absence_types (name) VALUES
    ('LICENSE');

--##############################################################
--################### Holidays ################################
--##############################################################
insert into holidays (holiday_date, name) values
    ('2025-01-01','Feriado - Dia de ano novo'),
    ('2025-04-18','Feriado - Sexta feira santa'),
    ('2025-04-20','Feriado - Páscoa'),
    ('2025-04-25','Feriado - Dia da liberdade'),
    ('2025-05-01','Feriado - Dia do trabalhador'),
    ('2025-06-10','Feriado - Dia de Portugal'),
    ('2025-06-19','Feriado - Corpo de Deus'),
    ('2025-08-15','Feriado - Assunção de Nossa Senhora'),
    ('2025-10-05','Feriado - Implantação da república'),
    ('2025-11-01','Feriado - Dia de todos os santos'),
    ('2025-12-01','Feriado - Restauração da independência'),
    ('2025-12-08','Feriado - Dia da Imaculada Conceição'),
    ('2025-12-25','Feriado - Natal');

--#############################################################
--################### Users ###################################
--##############################################################
INSERT INTO users
(id, name, email, birthdate, line_manager, current_year_vacation_days, prev_year_vacation_days, created_at, updated_at, created_by, updated_by, deleted)
VALUES(nextval('users_id_seq'::regclass), 'O Chefe', 'manager@email.com', null, null, 23, 23, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, null, false);

INSERT INTO user_roles
(user_id, role_id, created_at, created_by, updated_at, updated_by)
VALUES(1, 2, null, null, null, null);

--##############################################################
--################### Clients ##################################
--##############################################################
INSERT INTO clients (name)
    VALUES('DataCentric');
INSERT INTO clients (name)
    VALUES('Sonae');
INSERT INTO clients (name)
    VALUES('Lusiadas');

--##############################################################
--################### Project Types ############################
--##############################################################
INSERT INTO project_types (name)
    VALUES('Product');
INSERT INTO project_types (name)
    VALUES('Major');
INSERT INTO project_types (name)
    VALUES('Minor');
