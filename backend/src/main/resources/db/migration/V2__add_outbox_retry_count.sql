-- Tracks how many times the outbox relay has attempted to publish each event.
-- Used by the relay to promote persistently-failing events to FAILED (dead-letter)
-- after outbox.relay.max-retries attempts, preventing infinite retry loops.
ALTER TABLE outbox_events
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;
