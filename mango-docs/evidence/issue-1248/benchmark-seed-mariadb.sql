DROP DATABASE IF EXISTS mango_issue_1248_perf;
CREATE DATABASE mango_issue_1248_perf;
USE mango_issue_1248_perf;
CREATE TABLE workflow_business_apply (
  id bigint NOT NULL,
  tenant_id varchar(64) NOT NULL,
  org_id bigint,
  business_type varchar(128) NOT NULL,
  business_key varchar(128) NOT NULL,
  apply_status varchar(32) NOT NULL,
  process_instance_id varchar(128),
  apply_code varchar(128),
  apply_title varchar(255),
  apply_summary varchar(1000),
  process_name varchar(255),
  process_definition_key varchar(128),
  applicant_dept_id bigint,
  created_time datetime NOT NULL,
  PRIMARY KEY (id),
  KEY idx_workflow_business_apply_biz_latest (tenant_id, business_type, business_key),
  KEY idx_workflow_business_apply_created (tenant_id, business_type, created_time)
) ENGINE=InnoDB;
CREATE TABLE act_hi_taskinst (
  ID_ varchar(64) NOT NULL,
  NAME_ varchar(255),
  TASK_DEF_KEY_ varchar(128),
  PROC_INST_ID_ varchar(64),
  PROC_DEF_ID_ varchar(128),
  ASSIGNEE_ varchar(128),
  START_TIME_ datetime,
  CLAIM_TIME_ datetime,
  END_TIME_ datetime,
  DELETE_REASON_ varchar(255),
  PRIMARY KEY (ID_),
  KEY ACT_IDX_HI_TASK_INST_PROCINST (PROC_INST_ID_)
) ENGINE=InnoDB;
CREATE TABLE workflow_business_apply_attribute (
  id bigint NOT NULL,
  tenant_id varchar(64) NOT NULL,
  org_id bigint,
  apply_id bigint NOT NULL,
  business_type varchar(128) NOT NULL,
  attr_key varchar(64) NOT NULL,
  attr_value varchar(255) NOT NULL,
  created_by bigint,
  created_at datetime,
  updated_by bigint,
  updated_at datetime,
  PRIMARY KEY (id),
  KEY idx_workflow_apply_attr_apply (apply_id, attr_key),
  KEY idx_workflow_apply_attr_lookup (tenant_id, business_type, attr_key, attr_value(48), apply_id)
) ENGINE=InnoDB;
CREATE TABLE workflow_task_record (
  id bigint NOT NULL,
  tenant_id varchar(64),
  process_instance_id varchar(128),
  task_id varchar(128),
  task_definition_key varchar(128),
  action varchar(32),
  variables_json longtext,
  created_time datetime,
  PRIMARY KEY (id),
  KEY idx_task_record_return (tenant_id, process_instance_id, action)
) ENGINE=InnoDB;
INSERT INTO workflow_business_apply
 (id, tenant_id, org_id, business_type, business_key, apply_status, process_instance_id,
  apply_code, apply_title, apply_summary, process_name, process_definition_key, applicant_dept_id, created_time)
SELECT seq, '1', 100, 'BAOHAN_RC_PROJECT_REVIEW', CONCAT('RC', seq), 'IN_APPROVAL', CONCAT('P', seq),
       CONCAT('GR', LPAD(seq, 10, '0')), CONCAT('保函风控审核', seq), '履约保函', '项目审批',
       'rc_project_review', 200, TIMESTAMPADD(MINUTE, -seq, '2026-11-14 00:00:00')
FROM seq_1_to_300000;
INSERT INTO act_hi_taskinst (ID_, NAME_, TASK_DEF_KEY_, PROC_INST_ID_, PROC_DEF_ID_, ASSIGNEE_, START_TIME_)
SELECT CONCAT('T', s.seq, '_', n2.seq), '初审', 'FIRST_REVIEW', CONCAT('P', s.seq), 'PD1', 'alice',
       TIMESTAMPADD(MINUTE, CAST(n2.seq AS SIGNED) - CAST(s.seq AS SIGNED), '2026-11-14 00:00:00')
FROM seq_1_to_300000 s CROSS JOIN seq_1_to_2 n2;
INSERT INTO workflow_business_apply_attribute (id, tenant_id, apply_id, business_type, attr_key, attr_value, created_at)
SELECT seq * 10 + 1, '1', seq, 'BAOHAN_RC_PROJECT_REVIEW', 'guaranteeTypeCode',
       ELT(1 + MOD(seq, 3), 'PERF', 'BID', 'ADV'), NOW() FROM seq_1_to_300000
UNION ALL
SELECT seq * 10 + 2, '1', seq, 'BAOHAN_RC_PROJECT_REVIEW', 'auditType',
       ELT(1 + MOD(seq, 3), 'RISK', 'LEGAL', 'FINANCE'), NOW() FROM seq_1_to_300000
UNION ALL
SELECT seq * 10 + 3, '1', seq, 'BAOHAN_RC_PROJECT_REVIEW', 'versionStatus',
       ELT(1 + MOD(seq, 3), 'PENDING', 'APPROVED', 'REJECTED'), NOW() FROM seq_1_to_300000
UNION ALL
SELECT seq * 10 + 4, '1', seq, 'BAOHAN_RC_PROJECT_REVIEW', 'beneficiary',
       CONCAT('BENE', MOD(seq, 1000)), NOW() FROM seq_1_to_300000
UNION ALL
SELECT seq * 10 + 5, '1', seq, 'BAOHAN_RC_PROJECT_REVIEW', 'guaranteedParty',
       CONCAT('GP', MOD(seq, 500)), NOW() FROM seq_1_to_300000
UNION ALL
SELECT seq * 10 + 6, '1', seq, 'BAOHAN_RC_PROJECT_REVIEW', 'projectType',
       ELT(1 + MOD(seq, 5), 'MUNICIPAL', 'HIGHWAY', 'BUILDING', 'WATER', 'OTHER'), NOW() FROM seq_1_to_300000;
ANALYZE TABLE workflow_business_apply, act_hi_taskinst, workflow_business_apply_attribute;
SELECT (SELECT COUNT(*) FROM workflow_business_apply) AS applies,
       (SELECT COUNT(*) FROM act_hi_taskinst) AS tasks,
       (SELECT COUNT(*) FROM workflow_business_apply_attribute) AS attributes;
