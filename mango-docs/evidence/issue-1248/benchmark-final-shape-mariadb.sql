USE mango_issue_1248_perf;
SET profiling = 1;
SET @t='1'; SET @bt='BAOHAN_RC_PROJECT_REVIEW';
-- 1 单条件低选择性（guaranteeTypeCode=PERF，10 万申请）
SELECT COUNT(1) FROM act_hi_taskinst t JOIN workflow_business_apply a ON a.process_instance_id = t.PROC_INST_ID_
WHERE a.tenant_id=@t AND a.business_type=@bt
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='guaranteeTypeCode' AND fa.attr_value IN ('PERF'));
-- 2 六条件计数
SELECT COUNT(1) FROM act_hi_taskinst t JOIN workflow_business_apply a ON a.process_instance_id = t.PROC_INST_ID_
WHERE a.tenant_id=@t AND a.business_type=@bt
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='guaranteeTypeCode' AND fa.attr_value IN ('PERF'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='auditType' AND fa.attr_value IN ('RISK','LEGAL'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='versionStatus' AND fa.attr_value IN ('PENDING','APPROVED'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='beneficiary' AND fa.attr_value IN ('BENE7'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='guaranteedParty' AND fa.attr_value IN ('GP7'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='projectType' AND fa.attr_value IN ('BUILDING'));
-- 3 三条件深翻页 offset 5000
SELECT COUNT(1) FROM (SELECT t.ID_ FROM act_hi_taskinst t JOIN workflow_business_apply a ON a.process_instance_id = t.PROC_INST_ID_
WHERE a.tenant_id=@t AND a.business_type=@bt
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='guaranteeTypeCode' AND fa.attr_value IN ('PERF'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='auditType' AND fa.attr_value IN ('RISK','LEGAL'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='projectType' AND fa.attr_value IN ('BUILDING','WATER'))
ORDER BY t.START_TIME_ DESC, t.ID_ ASC LIMIT 20 OFFSET 5000) p;
-- 4 两条件按申请时间倒序取页
SELECT COUNT(1) FROM (SELECT t.ID_ FROM act_hi_taskinst t JOIN workflow_business_apply a ON a.process_instance_id = t.PROC_INST_ID_
WHERE a.tenant_id=@t AND a.business_type=@bt
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='guaranteeTypeCode' AND fa.attr_value IN ('PERF'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id=@t AND fa.business_type=@bt AND fa.attr_key='projectType' AND fa.attr_value IN ('BUILDING','WATER'))
ORDER BY a.created_time DESC, t.ID_ ASC LIMIT 20) p;
SHOW PROFILES;
