CREATE TABLE usb_challenge_completion (
    `uuid`                          VARCHAR(36)                     NOT NULL,
    `sharing_type`                  ENUM('ISLAND', 'PLAYER')        NOT NULL,
    `challenge`                     VARCHAR(128)                    NOT NULL,
    `first_completed`               TIMESTAMP,
    `times_completed`               INTEGER,
    `times_completed_since_timer`   INTEGER,
    CONSTRAINT usb_challenge_completion_pk PRIMARY KEY (`uuid`, `sharing_type`, `challenge`)
);

CREATE INDEX ON usb_challenge_completion (`uuid`, `sharing_type`);

CREATE TABLE usb_islands (
    `uuid`              VARCHAR(36)     NOT NULL,
    `name`              VARCHAR(128)    NOT NULL,
    `center_x`          INTEGER         NOT NULL,
    `center_z`          INTEGER         NOT NULL,
    `owner`             VARCHAR(36),
    `ignore`            BOOLEAN         DEFAULT FALSE,
    `locked`            BOOLEAN         DEFAULT FALSE,
    `warpActive`        BOOLEAN         DEFAULT FALSE,
    `regionVersion`     VARCHAR(128),
    `schematicName`     VARCHAR(128),
    `level`             DOUBLE PRECISION,
    `scoreMultiplier`   DOUBLE PRECISION,
    `scoreOffset`       DOUBLE PRECISION,
    `biome`             VARCHAR(128),
    `leaf_breaks`        INTEGER,
    `hopper_count`       INTEGER,
    CONSTRAINT usb_islands_pk PRIMARY KEY (`uuid`)
);

CREATE INDEX ON usb_islands (`name`);
CREATE INDEX ON usb_islands (`owner`);
CREATE INDEX ON usb_islands (`center_x`, `center_z`);

CREATE TABLE usb_island_access (
    `player_uuid`       VARCHAR(36)                     NOT NULL,
    `island_uuid`       VARCHAR(36)                     NOT NULL,
    `access_type`       ENUM('BANNED', 'TRUSTED')       NOT NULL,
    CONSTRAINT usb_island_access_pk PRIMARY KEY (`player_uuid`, `island_uuid`)
);

CREATE INDEX ON usb_island_access (`player_uuid`);
CREATE INDEX ON usb_island_access (`island_uuid`);

CREATE TABLE usb_island_locations (
    `island_uuid`       VARCHAR(36)             NOT NULL,
    `location_type`     ENUM('WARP')            NOT NULL,
    `location_world`    VARCHAR(64)             NOT NULL,
    `location_x`        DOUBLE PRECISION        NOT NULL,
    `location_y`        DOUBLE PRECISION        NOT NULL,
    `location_z`        DOUBLE PRECISION        NOT NULL,
    `location_pitch`    DOUBLE PRECISION,
    `location_yaw`      DOUBLE PRECISION,
    CONSTRAINT usb_island_locations_pk PRIMARY KEY (`island_uuid`, `location_type`)
);

CREATE TABLE usb_island_log (
    `log_uuid`          VARCHAR(36)     NOT NULL,
    `island_uuid`       VARCHAR(36)     NOT NULL,
    `timestamp`         TIMESTAMP       NOT NULL,
    `log_line`          VARCHAR(2048)   NOT NULL,
    `variables`         VARCHAR(2048),
    CONSTRAINT usb_island_log_pk PRIMARY KEY (`log_uuid`)
);

CREATE INDEX ON usb_island_log (`island_uuid`);

CREATE TABLE usb_island_members (
    `player_uuid`       VARCHAR(36)     NOT NULL,
    `island_uuid`       VARCHAR(36)     NOT NULL,
    `role`              VARCHAR(64)     NOT NULL,
    `can_change_biome`  BOOLEAN,
    `can_toggle_lock`   BOOLEAN,
    `can_change_warp`   BOOLEAN,
    `can_toggle_warp`   BOOLEAN,
    `can_invite_others` BOOLEAN,
    `can_kick_others`   BOOLEAN,
    `can_ban_others`    BOOLEAN,
    `max_animals`       INTEGER,
    `max_monsters`      INTEGER,
    `max_villagers`     INTEGER,
    `max_golems`        INTEGER,
    CONSTRAINT usb_island_members_pk PRIMARY KEY (`player_uuid`)
);

CREATE INDEX ON usb_island_members (`island_uuid`);

CREATE TABLE usb_players (
    `uuid`              VARCHAR(36)     NOT NULL,
    `username`          VARCHAR(16)     NOT NULL,
    `display_name`      VARCHAR(64),
    `clear_inventory`   BOOLEAN,
    CONSTRAINT usb_players_pk PRIMARY KEY (`uuid`)
);

CREATE INDEX ON usb_players (`username`);

CREATE TABLE usb_player_pending (
    `uuid`          VARCHAR(36)                     NOT NULL,
    `type`          ENUM('COMMAND', 'PERMISSION')   NOT NULL,
    `value`         VARCHAR(2048)                   NOT NULL,
    CONSTRAINT usb_player_pending_pk PRIMARY KEY (`uuid`)
);
