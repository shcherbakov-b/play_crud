CREATE TABLE plans (
  id          BIGINT PRIMARY KEY,
  name        VARCHAR(256) NOT NULL,
  description TEXT         NOT NULL,
  status      BOOLEAN      NOT NULL
);

COMMENT ON TABLE plans IS 'Таблица для хранения планов';
COMMENT ON COLUMN plans.id IS 'Идентификатор';
COMMENT ON COLUMN plans.name IS 'Наименование';
COMMENT ON COLUMN plans.description IS 'Описание';
COMMENT ON COLUMN plans.status IS 'Статус';

CREATE TABLE limits (
  id          BIGINT PRIMARY KEY,
  name        VARCHAR(256) NOT NULL,
  description TEXT         NOT NULL
);

COMMENT ON TABLE limits IS 'Таблица для хранения ограничений';
COMMENT ON COLUMN limits.id IS 'Идентификатор';
COMMENT ON COLUMN limits.name IS 'Наименование';
COMMENT ON COLUMN limits.description IS 'Описание';

CREATE TABLE plan_limits (
  limit_id BIGINT NOT NULL
    CONSTRAINT plan_limits_limit_id_fkey
    REFERENCES limits
    ON DELETE CASCADE,
  plan_id  BIGINT NOT NULL
    CONSTRAINT plan_limits_plan_id_fkey
    REFERENCES plans
    ON DELETE CASCADE,
  value    INT    NOT NULL,
  PRIMARY KEY (limit_id, plan_id)
);

COMMENT ON TABLE plan_limits IS 'Таблица для хранения ресурсных планов';
COMMENT ON COLUMN plan_limits.limit_id IS 'Ссылка на описание ресурсов';
COMMENT ON COLUMN plan_limits.plan_id IS 'Ссылка на описание планов';
COMMENT ON COLUMN plan_limits.value IS 'Значение';

CREATE TABLE plan_roles (
  plan_id BIGINT NOT NULL
    CONSTRAINT plan_roles_plan_id_fkey
    REFERENCES plans
    ON DELETE CASCADE,
  role_id BIGINT NOT NULL UNIQUE,
  PRIMARY KEY (plan_id, role_id)
);


COMMENT ON TABLE plan_roles IS 'Таблица для хранения ролей с планами';
COMMENT ON COLUMN plan_roles.plan_id IS 'Ссылка на описание плана';
COMMENT ON COLUMN plan_roles.role_id IS 'Идентификатор роли';
