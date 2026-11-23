-- workflow_process_participant 的主键仍是历史 AUTO_INCREMENT（见 V3），
-- 而实体 WorkflowProcessParticipantEntity 继承 WorkflowBaseEntity 使用雪花 ID（IdType.ASSIGN_ID），
-- 自增属性从未被使用，只会让 schema 校验报「标准主键 id 必须使用雪花算法」。
-- 这里只去掉自增属性，不改列类型、不动已有数据；V3 的 INSERT ... SELECT 依赖自增，
-- 而 V3 先于此迁移执行，全新安装不受影响。
ALTER TABLE `workflow_process_participant`
  MODIFY COLUMN `id` bigint NOT NULL COMMENT '主键';
