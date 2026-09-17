ALTER TABLE sys_user
  ADD COLUMN token_version INT NOT NULL DEFAULT 1 AFTER status;
