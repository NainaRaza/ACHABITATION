ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verification_requested_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE email_verification_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_email_verification_token_hash ON email_verification_token(token_hash);
CREATE INDEX idx_email_verification_token_user ON email_verification_token(user_id);
