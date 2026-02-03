--liquibase formatted sql
--changeset eduardo.novo:1.0.0.5 dbms:postgresql

CREATE TABLE IF NOT EXISTS resource_types (
    resource_type   VARCHAR(30),
    main_table_name VARCHAR(128)
);

ALTER TABLE resource_types ADD CONSTRAINT resource_types_pk PRIMARY KEY (resource_type);

CREATE TABLE IF NOT EXISTS resource_permissions (
    id BIGSERIAL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id   BIGINT NOT NULL,
    access_type   VARCHAR(30) NOT NULL,
    subject_type  VARCHAR(10) NOT NULL,
    subject       BIGINT NOT NULL,
    created_by    BIGINT,
    created_at    TIMESTAMP,
    updated_by    BIGINT,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE resource_permissions ADD CONSTRAINT resource_permissions_pk
    PRIMARY KEY (resource_type, resource_id, access_type, subject_type, subject);

ALTER TABLE resource_permissions ADD CONSTRAINT resource_permissions_type_fk
    FOREIGN KEY (resource_type) REFERENCES resource_types;

ALTER TABLE resource_permissions ADD CONSTRAINT resource_permissions_created_fk
    FOREIGN KEY (created_by) REFERENCES users;

ALTER TABLE resource_permissions ADD CONSTRAINT resource_permissions_updated_fk
    FOREIGN KEY (updated_by) REFERENCES users;


CREATE TABLE IF NOT EXISTS user_groups (
    id   BIGSERIAL,
    token_id VARCHAR(128),
    name VARCHAR(128),
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

ALTER TABLE user_groups ADD CONSTRAINT groups_pk PRIMARY KEY (id);

ALTER TABLE user_groups ADD CONSTRAINT user_groups_created_fk
    FOREIGN KEY (created_by) REFERENCES users;

ALTER TABLE user_groups ADD CONSTRAINT user_groups_updated_fk
    FOREIGN KEY (updated_by) REFERENCES users;

CREATE TABLE IF NOT EXISTS roles (
    id    BIGSERIAL,
    name  VARCHAR(128),
    parent_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

ALTER TABLE roles ADD CONSTRAINT roles_pk PRIMARY KEY (id);

ALTER TABLE roles ADD CONSTRAINT roles_created_fk
    FOREIGN KEY (created_by) REFERENCES users;

ALTER TABLE roles ADD CONSTRAINT roles_updated_fk
    FOREIGN KEY (updated_by) REFERENCES users;

ALTER TABLE roles ADD CONSTRAINT roles_parent_fk
    FOREIGN KEY (parent_id) REFERENCES roles;
