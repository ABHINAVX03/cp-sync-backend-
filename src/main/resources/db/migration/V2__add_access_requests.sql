CREATE TABLE access_requests (
                                 id          BIGSERIAL PRIMARY KEY,
                                 email       VARCHAR(255) NOT NULL,
                                 created_at  TIMESTAMP NOT NULL DEFAULT now()
);