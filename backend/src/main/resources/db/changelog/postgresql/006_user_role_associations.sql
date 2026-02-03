--liquibase formatted sql
--changeset eduardo.novo:1.0.0.6 dbms:postgresql

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

ALTER TABLE user_roles ADD CONSTRAINT user_roles_pk PRIMARY KEY (user_id, role_id);
ALTER TABLE user_roles ADD CONSTRAINT user_roles_user_fk FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_roles ADD CONSTRAINT user_roles_role_fk FOREIGN KEY (role_id) REFERENCES roles (id);

CREATE TABLE IF NOT EXISTS user_user_groups (
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

ALTER TABLE user_user_groups ADD CONSTRAINT user_user_groups_pk PRIMARY KEY (user_id, group_id);
ALTER TABLE user_user_groups ADD CONSTRAINT user_user_groups_user_fk FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_user_groups ADD CONSTRAINT user_user_groups_group_fk FOREIGN KEY (group_id) REFERENCES user_groups (id);

CREATE TABLE IF NOT EXISTS user_group_roles (
    user_group_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

ALTER TABLE user_group_roles ADD CONSTRAINT user_group_roles_pk PRIMARY KEY (user_group_id, role_id);
ALTER TABLE user_group_roles ADD CONSTRAINT user_group_roles_group_fk FOREIGN KEY (user_group_id) REFERENCES user_groups (id);
ALTER TABLE user_group_roles ADD CONSTRAINT user_group_roles_role_fk FOREIGN KEY (role_id) REFERENCES roles (id);
