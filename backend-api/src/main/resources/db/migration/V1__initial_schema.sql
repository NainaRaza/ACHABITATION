CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    session_token_hash VARCHAR(128),
    session_token_issued_at TIMESTAMP WITH TIME ZONE,
    living_rest NUMERIC(12,2),
    weight_mode VARCHAR(30),
    advanced_living_rest BOOLEAN,
    net_income_after_tax NUMERIC(12,2),
    rent NUMERIC(12,2),
    credits NUMERIC(12,2),
    fixed_charges NUMERIC(12,2),
    transport NUMERIC(12,2),
    insurance NUMERIC(12,2),
    other_mandatory_expenses NUMERIC(12,2),
    menstrual_protection NUMERIC(12,2),
    vegetarian BOOLEAN,
    no_alcohol BOOLEAN,
    living_rest_public BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE trip (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    start_date DATE,
    end_date DATE,
    reference_currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE trip_member (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trip(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    role VARCHAR(30),
    joined_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_trip_member UNIQUE (trip_id, user_id)
);

CREATE TABLE trip_invitation (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trip(id),
    created_by_user_id UUID NOT NULL REFERENCES app_user(id),
    code VARCHAR(64) NOT NULL UNIQUE,
    role_to_grant VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE trip_custom_constraint (
    trip_id UUID NOT NULL REFERENCES trip(id),
    constraint_name VARCHAR(120) NOT NULL
);

CREATE TABLE user_known_custom_constraint (
    user_id UUID NOT NULL REFERENCES app_user(id),
    constraint_name VARCHAR(120) NOT NULL
);

CREATE TABLE user_custom_constraint (
    user_id UUID NOT NULL REFERENCES app_user(id),
    constraint_name VARCHAR(120) NOT NULL
);

CREATE TABLE person (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trip(id),
    name VARCHAR(120) NOT NULL,
    linked_user_id UUID REFERENCES app_user(id),
    normalized_name VARCHAR(120) NOT NULL,
    living_rest NUMERIC(12,2) NOT NULL,
    weight_mode VARCHAR(30) NOT NULL,
    advanced_living_rest BOOLEAN NOT NULL,
    net_income_after_tax NUMERIC(12,2) NOT NULL,
    rent NUMERIC(12,2) NOT NULL,
    credits NUMERIC(12,2) NOT NULL,
    fixed_charges NUMERIC(12,2) NOT NULL,
    transport NUMERIC(12,2) NOT NULL,
    insurance NUMERIC(12,2) NOT NULL,
    other_mandatory_expenses NUMERIC(12,2) NOT NULL,
    menstrual_protection NUMERIC(12,2) NOT NULL DEFAULT 0,
    living_rest_public BOOLEAN NOT NULL DEFAULT FALSE,
    vegetarian BOOLEAN NOT NULL,
    no_alcohol BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT uk_person_name_per_trip UNIQUE (trip_id, normalized_name)
);

CREATE TABLE presence_period (
    id UUID PRIMARY KEY,
    person_id UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    start_date DATE,
    end_date DATE
);

CREATE TABLE person_custom_constraint (
    person_id UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    constraint_name VARCHAR(120) NOT NULL
);

CREATE TABLE expense (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trip(id),
    title VARCHAR(180) NOT NULL,
    date DATE NOT NULL,
    payer_person_id UUID NOT NULL REFERENCES person(id),
    total_amount NUMERIC(12,2) NOT NULL,
    meat_amount NUMERIC(12,2) NOT NULL,
    alcohol_amount NUMERIC(12,2) NOT NULL,
    type VARCHAR(30) NOT NULL,
    advanced_mode BOOLEAN NOT NULL,
    currency VARCHAR(3) NOT NULL,
    exchange_rate_to_trip_currency NUMERIC(16,8) NOT NULL
);

CREATE TABLE expense_custom_constraint_amount (
    expense_id UUID NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
    constraint_name VARCHAR(120) NOT NULL,
    amount NUMERIC(12,2) NOT NULL
);

CREATE TABLE expense_manual_participant (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES person(id),
    CONSTRAINT uk_expense_manual_participant UNIQUE (expense_id, person_id)
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trip(id),
    actor_user_id UUID REFERENCES app_user(id),
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    description VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_trip_member_user ON trip_member(user_id);
CREATE INDEX idx_person_trip ON person(trip_id);
CREATE INDEX idx_person_linked_user ON person(linked_user_id);
CREATE INDEX idx_expense_trip ON expense(trip_id);
CREATE INDEX idx_audit_trip ON audit_log(trip_id);
