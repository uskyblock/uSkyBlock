CREATE TABLE usb_players (
    `uuid`          VARCHAR(36)     NOT NULL,
    `username`      VARCHAR(16)     NOT NULL,
    `display_name`  VARCHAR(64),
    CONSTRAINT usb_players_pk PRIMARY KEY (`uuid`)
);

CREATE INDEX ON usb_players (`username`);
