CREATE TABLE pets (
  id UUID PRIMARY KEY,
  owner_id TEXT NOT NULL,
  name TEXT NOT NULL,
  species TEXT NOT NULL CHECK (species IN ('DOG', 'CAT')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX one_pet_per_owner ON pets(owner_id);
CREATE TABLE pet_state (
  pet_id UUID PRIMARY KEY REFERENCES pets(id) ON DELETE CASCADE,
  hunger INT NOT NULL CHECK (hunger BETWEEN 0 AND 100),
  energy INT NOT NULL CHECK (energy BETWEEN 0 AND 100),
  mood INT NOT NULL CHECK (mood BETWEEN 0 AND 100),
  bond INT NOT NULL CHECK (bond BETWEEN 0 AND 100),
  stress INT NOT NULL CHECK (stress BETWEEN 0 AND 100),
  action TEXT NOT NULL,
  simulated_until TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE pet_events (
  id BIGSERIAL PRIMARY KEY,
  pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
  occurred_at TIMESTAMPTZ NOT NULL,
  event_type TEXT NOT NULL,
  detail TEXT NOT NULL
);
CREATE INDEX pet_events_recent ON pet_events(pet_id, occurred_at DESC, id DESC);
CREATE TABLE action_requests (
  owner_id TEXT NOT NULL,
  idempotency_key TEXT NOT NULL,
  request_hash TEXT NOT NULL,
  response_json TEXT NOT NULL,
  PRIMARY KEY(owner_id, idempotency_key)
);
