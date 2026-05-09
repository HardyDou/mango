ALTER TABLE sys_operation_log
    ADD COLUMN handler_method VARCHAR(200) DEFAULT NULL COMMENT '处理器方法' AFTER method;

UPDATE sys_operation_log
SET handler_method = method,
    method = NULL
WHERE method IS NOT NULL
  AND method NOT IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS', 'HEAD');
