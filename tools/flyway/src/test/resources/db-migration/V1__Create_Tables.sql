CREATE TABLE `thread` (
  `id`            VARCHAR(32)                 NOT NULL,
  `deleted`       BOOLEAN                     NOT NULL,
  `sequence_nr`   BIGINT                      NOT NULL,
  `creator_id`    VARCHAR(32)                 NOT NULL,
  `parent_id`     VARCHAR(32),
  `created_at`    DATETIME(6)                 NOT NULL,
  `updated_at`    DATETIME(6)                 NOT NULL,
  `removed_at`    DATETIME(6),
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE `thread_administrator_ids` (
  `id`            VARCHAR(32)                 NOT NULL,
  `thread_id`     VARCHAR(32)                 NOT NULL,
  `account_id`    VARCHAR(32)                 NOT NULL,
  `adder_id`      VARCHAR(32)                 NOT NULL,
  `created_at`    DATETIME(6)                 NOT NULL,
  `updated_at`    DATETIME(6)                 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `thread_id_account_id_uk` (`thread_id`, `account_id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE `thread_member_ids` (
  `id`            VARCHAR(32)                 NOT NULL,
  `thread_id`     VARCHAR(32)                 NOT NULL,
  `account_id`    VARCHAR(32)                 NOT NULL,
  `adder_id`      VARCHAR(32)                 NOT NULL,
  `created_at`    DATETIME(6)                 NOT NULL,
  `updated_at`    DATETIME(6)                 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `thread_id_account_id_uk` (`thread_id`, `account_id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE `thread_message` (
  `id`            VARCHAR(32)                 NOT NULL,
  `deleted`       BOOLEAN                     NOT NULL,
  `thread_id`     VARCHAR(32)                 NOT NULL,
  `sender_id`     VARCHAR(32)                 NOT NULL,
  `type`          VARCHAR(32)                 NOT NULL,
  `body`          VARCHAR(1024)               NOT NULL,
  `created_at`    DATETIME(6)                 NOT NULL,
  `updated_at`    DATETIME(6)                 NOT NULL,
  PRIMARY KEY (`id`),
  KEY `thread_message_thread_id_idx` (`thread_id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

