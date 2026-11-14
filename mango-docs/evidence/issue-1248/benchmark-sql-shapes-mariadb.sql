USE mango_issue_1248_perf;
SET profiling = 1;
-- C1 三条件：每个条件一个 IN 子查询（语义相同，交给优化器做半连接/哈希）
SELECT COUNT(1) FROM act_hi_taskinst t JOIN workflow_business_apply a ON a.process_instance_id = t.PROC_INST_ID_
WHERE a.tenant_id = '1' AND a.business_type = 'BAOHAN_RC_PROJECT_REVIEW'
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id='1' AND fa.business_type='BAOHAN_RC_PROJECT_REVIEW' AND fa.attr_key='guaranteeTypeCode' AND fa.attr_value IN ('PERF'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id='1' AND fa.business_type='BAOHAN_RC_PROJECT_REVIEW' AND fa.attr_key='auditType' AND fa.attr_value IN ('RISK','LEGAL'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id='1' AND fa.business_type='BAOHAN_RC_PROJECT_REVIEW' AND fa.attr_key='beneficiary' AND fa.attr_value IN ('BENE7'));
-- C2 三条件取页
SELECT t.ID_ FROM act_hi_taskinst t JOIN workflow_business_apply a ON a.process_instance_id = t.PROC_INST_ID_
WHERE a.tenant_id = '1' AND a.business_type = 'BAOHAN_RC_PROJECT_REVIEW'
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id='1' AND fa.business_type='BAOHAN_RC_PROJECT_REVIEW' AND fa.attr_key='guaranteeTypeCode' AND fa.attr_value IN ('PERF'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id='1' AND fa.business_type='BAOHAN_RC_PROJECT_REVIEW' AND fa.attr_key='auditType' AND fa.attr_value IN ('RISK','LEGAL'))
  AND a.id IN (SELECT fa.apply_id FROM workflow_business_apply_attribute fa WHERE fa.tenant_id='1' AND fa.business_type='BAOHAN_RC_PROJECT_REVIEW' AND fa.attr_key='beneficiary' AND fa.attr_value IN ('BENE7'))
ORDER BY t.START_TIME_ DESC, t.ID_ ASC LIMIT 20 OFFSET 0;
-- J1 三条件：内连接（每条件一个 JOIN，索引 (apply_id, attr_key)）
SELECT COUNT(1) FROM act_hi_taskinst t
JOIN workflow_business_apply a ON a.process_instance_id = t.PROC_INST_ID_
JOIN workflow_business_apply_attribute f1 ON f1.apply_id = a.id AND f1.attr_key='guaranteeTypeCode' AND f1.attr_value IN ('PERF')
JOIN workflow_business_apply_attribute f2 ON f2.apply_id = a.id AND f2.attr_key='auditType' AND f2.attr_value IN ('RISK','LEGAL')
JOIN workflow_business_apply_attribute f3 ON f3.apply_id = a.id AND f3.attr_key='beneficiary' AND f3.attr_value IN ('BENE7')
WHERE a.tenant_id = '1' AND a.business_type = 'BAOHAN_RC_PROJECT_REVIEW';
-- J2 三条件取页
SELECT t.ID_ FROM act_hi_taskinst t
JOIN workflow_business_apply a ON a.process_instance_id = t.PROC_INST_ID_
JOIN workflow_business_apply_attribute f1 ON f1.apply_id = a.id AND f1.attr_key='guaranteeTypeCode' AND f1.attr_value IN ('PERF')
JOIN workflow_business_apply_attribute f2 ON f2.apply_id = a.id AND f2.attr_key='auditType' AND f2.attr_value IN ('RISK','LEGAL')
JOIN workflow_business_apply_attribute f3 ON f3.apply_id = a.id AND f3.attr_key='beneficiary' AND f3.attr_value IN ('BENE7')
WHERE a.tenant_id = '1' AND a.business_type = 'BAOHAN_RC_PROJECT_REVIEW'
ORDER BY t.START_TIME_ DESC, t.ID_ ASC LIMIT 20 OFFSET 0;
SHOW PROFILES;
