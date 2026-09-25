-- Local development database initialization.
--
-- Generated from the Liquibase changelogs (the single source of truth):
--   * apps/inventory/server/src/main/resources/db/changelog/0001-initial-schema.xml
--   * platform/keystone-admin/src/main/resources/admin-db/changelog/0001-initial-schema.xml
--   * platform/keystone-admin/src/main/resources/admin-db/changelog/0002-tenant-slug-username.xml
--
-- Mounted into the Postgres container at /docker-entrypoint-initdb.d/init.sql (see compose.yaml), so
-- a fresh `docker compose up -d` yields an initialized database. It runs only on the FIRST start
-- (empty data volume).
--
-- The application and `SchemaTool migrate` remain safe to run afterwards: every Liquibase changeset
-- is guarded by a preCondition (onFail="MARK_RAN"), so re-applying over these objects is a no-op.
--
-- Regenerate after editing a changelog:
--   mvn -pl apps/inventory/server,platform/keystone-admin -am generate-sources
-- then copy the CREATE TABLE / ALTER TABLE statements from each module's
-- target/generated-ddl/schema.sql into the matching section below.

CREATE SCHEMA IF NOT EXISTS inventory;

SET search_path TO inventory;

CREATE TABLE items (id UUID NOT NULL, name VARCHAR(255) NOT NULL, quantity INTEGER NOT NULL, version BIGINT DEFAULT 0 NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, CONSTRAINT items_pkey PRIMARY KEY (id));

CREATE SCHEMA IF NOT EXISTS platform;

SET search_path TO platform;

CREATE TABLE tenants (id UUID NOT NULL, name VARCHAR(255) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, CONSTRAINT tenants_pkey PRIMARY KEY (id), UNIQUE (name));

CREATE TABLE users (id UUID NOT NULL, sub VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, tenant_id UUID, must_change_password BOOLEAN DEFAULT FALSE NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, CONSTRAINT users_pkey PRIMARY KEY (id), CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id), UNIQUE (sub));

CREATE TABLE roles (id UUID NOT NULL, code VARCHAR(255) NOT NULL, scope VARCHAR(16) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, CONSTRAINT roles_pkey PRIMARY KEY (id), UNIQUE (code));

CREATE TABLE permissions (id UUID NOT NULL, code VARCHAR(255) NOT NULL, scope VARCHAR(16) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, CONSTRAINT permissions_pkey PRIMARY KEY (id), UNIQUE (code));

CREATE TABLE role_permissions (role_id UUID NOT NULL, permission_id UUID NOT NULL, CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id), CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id));

ALTER TABLE role_permissions ADD PRIMARY KEY (role_id, permission_id);

CREATE TABLE user_roles (user_id UUID NOT NULL, role_id UUID NOT NULL, tenant_id UUID, CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id), CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id), CONSTRAINT fk_user_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id));

ALTER TABLE user_roles ADD PRIMARY KEY (user_id, role_id);

ALTER TABLE roles ADD CONSTRAINT chk_roles_scope CHECK (scope IN ('PLATFORM','TENANT'));

ALTER TABLE permissions ADD CONSTRAINT chk_permissions_scope CHECK (scope IN ('PLATFORM','TENANT'));

ALTER TABLE tenants ADD slug VARCHAR(255) NOT NULL;

ALTER TABLE tenants ADD CONSTRAINT uq_tenants_slug UNIQUE (slug);

ALTER TABLE users ADD username VARCHAR(255) NOT NULL;

ALTER TABLE users ADD CONSTRAINT uq_users_username_tenant UNIQUE (username, tenant_id);
