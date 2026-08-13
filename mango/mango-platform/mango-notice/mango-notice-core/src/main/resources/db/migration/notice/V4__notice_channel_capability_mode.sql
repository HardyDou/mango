ALTER TABLE notice_channel_config
    ADD COLUMN capability_mode VARCHAR(16) NOT NULL DEFAULT 'SEND' COMMENT '渠道用途：SEND/RECEIVE/BOTH' AFTER channel_type;
