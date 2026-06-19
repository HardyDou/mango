-- Flowable 7.0.0 official MySQL schema managed by Mango Flyway.
-- Source jars: flowable-engine-common, identitylink/entitylink/eventsubscription/task/variable/job/batch services, flowable-engine.

create table ACT_GE_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_GE_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    DEPLOYMENT_ID_ varchar(64),
    BYTES_ LONGBLOB,
    GENERATED_ TINYINT,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

insert into ACT_GE_PROPERTY
values ('common.schema.version', '7.0.0.0', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);

create table ACT_RU_IDENTITYLINK (
    ID_ varchar(64),
    REV_ integer,
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);
create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);
create index ACT_IDX_IDENT_LNK_SCOPE on ACT_RU_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_IDENT_LNK_SUB_SCOPE on ACT_RU_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_IDENT_LNK_SCOPE_DEF on ACT_RU_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('identitylink.schema.version', '7.0.0.0', 1);

create table ACT_RU_ENTITYLINK (
    ID_ varchar(64),
    REV_ integer,
    CREATE_TIME_ datetime(3),
    LINK_TYPE_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    PARENT_ELEMENT_ID_ varchar(255),
    REF_SCOPE_ID_ varchar(255),
    REF_SCOPE_TYPE_ varchar(255),
    REF_SCOPE_DEFINITION_ID_ varchar(255),
    ROOT_SCOPE_ID_ varchar(255),
    ROOT_SCOPE_TYPE_ varchar(255),
    HIERARCHY_TYPE_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_ENT_LNK_SCOPE on ACT_RU_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_ENT_LNK_REF_SCOPE on ACT_RU_ENTITYLINK(REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_ENT_LNK_ROOT_SCOPE on ACT_RU_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_ENT_LNK_SCOPE_DEF on ACT_RU_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

insert into ACT_GE_PROPERTY values ('entitylink.schema.version', '7.0.0.0', 1);

create table ACT_RU_EVENT_SUBSCR (
    ID_ varchar(64) not null,
    REV_ integer,
    EVENT_TYPE_ varchar(255) not null,
    EVENT_NAME_ varchar(255),
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTIVITY_ID_ varchar(64),
    CONFIGURATION_ varchar(255),
    CREATED_ timestamp(3) not null DEFAULT CURRENT_TIMESTAMP(3),
    PROC_DEF_ID_ varchar(64),
    SUB_SCOPE_ID_ varchar(64),
    SCOPE_ID_ varchar(64),
    SCOPE_DEFINITION_ID_ varchar(64),
    SCOPE_TYPE_ varchar(64),
    LOCK_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_EVENT_SUBSCR_CONFIG_ on ACT_RU_EVENT_SUBSCR(CONFIGURATION_);
create index ACT_IDX_EVENT_SUBSCR_SCOPEREF_ on ACT_RU_EVENT_SUBSCR(SCOPE_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('eventsubscription.schema.version', '7.0.0.0', 1);

create table ACT_RU_TASK (
    ID_ varchar(64),
    REV_ integer,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    TASK_DEF_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    PROPAGATED_STAGE_INST_ID_ varchar(255),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    TASK_DEF_KEY_ varchar(255),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    DELEGATION_ varchar(64),
    PRIORITY_ integer,
    CREATE_TIME_ timestamp(3) NULL,
    DUE_DATE_ datetime(3),
    CATEGORY_ varchar(255),
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    FORM_KEY_ varchar(255),
    CLAIM_TIME_ datetime(3),
    IS_COUNT_ENABLED_ TINYINT,
    VAR_COUNT_ integer,
    ID_LINK_COUNT_ integer,
    SUB_TASK_COUNT_ integer,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_TASK_SCOPE on ACT_RU_TASK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SUB_SCOPE on ACT_RU_TASK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SCOPE_DEF on ACT_RU_TASK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('task.schema.version', '7.0.0.0', 1);

create table ACT_RU_VARIABLE (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(255) not null,
    NAME_ varchar(255) not null,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    TASK_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    META_INFO_ varchar(4000),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_RU_VAR_SCOPE_ID_TYPE on ACT_RU_VARIABLE(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_RU_VAR_SUB_ID_TYPE on ACT_RU_VARIABLE(SUB_SCOPE_ID_, SCOPE_TYPE_);

alter table ACT_RU_VARIABLE 
    add constraint ACT_FK_VAR_BYTEARRAY 
    foreign key (BYTEARRAY_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

insert into ACT_GE_PROPERTY values ('variable.schema.version', '7.0.0.0', 1);

create table ACT_RU_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_TIMER_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_SUSPENDED_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_DEADLETTER_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_HISTORY_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    ADV_HANDLER_CFG_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    SCOPE_TYPE_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_EXTERNAL_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_JOB_EXCEPTION_STACK_ID on ACT_RU_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_JOB_CUSTOM_VALUES_ID on ACT_RU_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_JOB_CORRELATION_ID on ACT_RU_JOB(CORRELATION_ID_);

create index ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID on ACT_RU_TIMER_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID on ACT_RU_TIMER_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_TIMER_JOB_CORRELATION_ID on ACT_RU_TIMER_JOB(CORRELATION_ID_);
create index ACT_IDX_TIMER_JOB_DUEDATE on ACT_RU_TIMER_JOB(DUEDATE_); 

create index ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID on ACT_RU_SUSPENDED_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID on ACT_RU_SUSPENDED_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_SUSPENDED_JOB_CORRELATION_ID on ACT_RU_SUSPENDED_JOB(CORRELATION_ID_);

create index ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID on ACT_RU_DEADLETTER_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID on ACT_RU_DEADLETTER_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_DEADLETTER_JOB_CORRELATION_ID on ACT_RU_DEADLETTER_JOB(CORRELATION_ID_);

create index ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID on ACT_RU_EXTERNAL_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID on ACT_RU_EXTERNAL_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_EXTERNAL_JOB_CORRELATION_ID on ACT_RU_EXTERNAL_JOB(CORRELATION_ID_);

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_SUSPENDED_JOB
    add constraint ACT_FK_SUSPENDED_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_SUSPENDED_JOB
    add constraint ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_DEADLETTER_JOB
    add constraint ACT_FK_DEADLETTER_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_DEADLETTER_JOB
    add constraint ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_EXTERNAL_JOB
    add constraint ACT_FK_EXTERNAL_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_EXTERNAL_JOB
    add constraint ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_JOB_SCOPE on ACT_RU_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_JOB_SUB_SCOPE on ACT_RU_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_JOB_SCOPE_DEF on ACT_RU_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_TJOB_SCOPE on ACT_RU_TIMER_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TJOB_SUB_SCOPE on ACT_RU_TIMER_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TJOB_SCOPE_DEF on ACT_RU_TIMER_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_); 

create index ACT_IDX_SJOB_SCOPE on ACT_RU_SUSPENDED_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_SJOB_SUB_SCOPE on ACT_RU_SUSPENDED_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_SJOB_SCOPE_DEF on ACT_RU_SUSPENDED_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);   

create index ACT_IDX_DJOB_SCOPE on ACT_RU_DEADLETTER_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_DJOB_SUB_SCOPE on ACT_RU_DEADLETTER_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_DJOB_SCOPE_DEF on ACT_RU_DEADLETTER_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_); 

create index ACT_IDX_EJOB_SCOPE on ACT_RU_EXTERNAL_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_EJOB_SUB_SCOPE on ACT_RU_EXTERNAL_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_EJOB_SCOPE_DEF on ACT_RU_EXTERNAL_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('job.schema.version', '7.0.0.0', 1);

create table FLW_RU_BATCH (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(64) not null,
    SEARCH_KEY_ varchar(255),
    SEARCH_KEY2_ varchar(255),
    CREATE_TIME_ datetime(3) not null,
    COMPLETE_TIME_ datetime(3),
    STATUS_ varchar(255),
    BATCH_DOC_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table FLW_RU_BATCH_PART (
    ID_ varchar(64) not null,
    REV_ integer,
    BATCH_ID_ varchar(64),
    TYPE_ varchar(64) not null,
    SCOPE_ID_ varchar(64),
    SUB_SCOPE_ID_ varchar(64),
    SCOPE_TYPE_ varchar(64),
    SEARCH_KEY_ varchar(255),
    SEARCH_KEY2_ varchar(255),
    CREATE_TIME_ datetime(3) not null,
    COMPLETE_TIME_ datetime(3),
    STATUS_ varchar(255),
    RESULT_DOC_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index FLW_IDX_BATCH_PART on FLW_RU_BATCH_PART(BATCH_ID_);

alter table FLW_RU_BATCH_PART
    add constraint FLW_FK_BATCH_PART_PARENT
    foreign key (BATCH_ID_)
    references FLW_RU_BATCH (ID_);

insert into ACT_GE_PROPERTY values ('batch.schema.version', '7.0.0.0', 1);

create table ACT_RE_DEPLOYMENT (
    ID_ varchar(64),
    NAME_ varchar(255),
    CATEGORY_ varchar(255),
    KEY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    DEPLOY_TIME_ timestamp(3) NULL,
    DERIVED_FROM_ varchar(64),
    DERIVED_FROM_ROOT_ varchar(64),
    PARENT_DEPLOYMENT_ID_ varchar(255),
    ENGINE_VERSION_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RE_MODEL (
    ID_ varchar(64) not null,
    REV_ integer,
    NAME_ varchar(255),
    KEY_ varchar(255),
    CATEGORY_ varchar(255),
    CREATE_TIME_ timestamp(3) null,
    LAST_UPDATE_TIME_ timestamp(3) null,
    VERSION_ integer,
    META_INFO_ varchar(4000),
    DEPLOYMENT_ID_ varchar(64),
    EDITOR_SOURCE_VALUE_ID_ varchar(64),
    EDITOR_SOURCE_EXTRA_VALUE_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_EXECUTION (
    ID_ varchar(64),
    REV_ integer,
    PROC_INST_ID_ varchar(64),
    BUSINESS_KEY_ varchar(255),
    PARENT_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SUPER_EXEC_ varchar(64),
    ROOT_PROC_INST_ID_ varchar(64),
    ACT_ID_ varchar(255),
    IS_ACTIVE_ TINYINT,
    IS_CONCURRENT_ TINYINT,
    IS_SCOPE_ TINYINT,
    IS_EVENT_SCOPE_ TINYINT,
    IS_MI_ROOT_ TINYINT,
    SUSPENSION_STATE_ integer,
    CACHED_ENT_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    START_ACT_ID_ varchar(255),
    START_TIME_ datetime(3),
    START_USER_ID_ varchar(255),
    LOCK_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    IS_COUNT_ENABLED_ TINYINT,
    EVT_SUBSCR_COUNT_ integer, 
    TASK_COUNT_ integer, 
    JOB_COUNT_ integer, 
    TIMER_JOB_COUNT_ integer,
    SUSP_JOB_COUNT_ integer,
    DEADLETTER_JOB_COUNT_ integer,
    EXTERNAL_WORKER_JOB_COUNT_ integer,
    VAR_COUNT_ integer, 
    ID_LINK_COUNT_ integer,
    CALLBACK_ID_ varchar(255),
    CALLBACK_TYPE_ varchar(255),
    REFERENCE_ID_ varchar(255),
    REFERENCE_TYPE_ varchar(255),
    PROPAGATED_STAGE_INST_ID_ varchar(255),
    BUSINESS_STATUS_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RE_PROCDEF (
    ID_ varchar(64) not null,
    REV_ integer,
    CATEGORY_ varchar(255),
    NAME_ varchar(255),
    KEY_ varchar(255) not null,
    VERSION_ integer not null,
    DEPLOYMENT_ID_ varchar(64),
    RESOURCE_NAME_ varchar(4000),
    DGRM_RESOURCE_NAME_ varchar(4000),
    DESCRIPTION_ varchar(4000),
    HAS_START_FORM_KEY_ TINYINT,
    HAS_GRAPHICAL_NOTATION_ TINYINT,
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    ENGINE_VERSION_ varchar(255),
    DERIVED_FROM_ varchar(64),
    DERIVED_FROM_ROOT_ varchar(64),
    DERIVED_VERSION_ integer not null default 0,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_EVT_LOG (
    LOG_NR_ bigint auto_increment,
    TYPE_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    TIME_STAMP_ timestamp(3) not null DEFAULT CURRENT_TIMESTAMP(3),
    USER_ID_ varchar(255),
    DATA_ LONGBLOB,
    LOCK_OWNER_ varchar(255),
    LOCK_TIME_ timestamp(3) null,
    IS_PROCESSED_ tinyint default 0,
    primary key (LOG_NR_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_PROCDEF_INFO (
	ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64) not null,
    REV_ integer,
    INFO_JSON_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_ACTINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_DEF_ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    EXECUTION_ID_ varchar(64) not null,
    ACT_ID_ varchar(255) not null,
    TASK_ID_ varchar(64),
    CALL_PROC_INST_ID_ varchar(64),
    ACT_NAME_ varchar(255),
    ACT_TYPE_ varchar(255) not null,
    ASSIGNEE_ varchar(255),
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    DURATION_ bigint,
    TRANSACTION_ORDER_ integer,
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_EXEC_BUSKEY on ACT_RU_EXECUTION(BUSINESS_KEY_);
create index ACT_IDC_EXEC_ROOT on ACT_RU_EXECUTION(ROOT_PROC_INST_ID_);
create index ACT_IDX_EXEC_REF_ID_ on ACT_RU_EXECUTION(REFERENCE_ID_);
create index ACT_IDX_VARIABLE_TASK_ID on ACT_RU_VARIABLE(TASK_ID_);
create index ACT_IDX_ATHRZ_PROCEDEF on ACT_RU_IDENTITYLINK(PROC_DEF_ID_);
create index ACT_IDX_INFO_PROCDEF on ACT_PROCDEF_INFO(PROC_DEF_ID_);

create index ACT_IDX_RU_ACTI_START on ACT_RU_ACTINST(START_TIME_);
create index ACT_IDX_RU_ACTI_END on ACT_RU_ACTINST(END_TIME_);
create index ACT_IDX_RU_ACTI_PROC on ACT_RU_ACTINST(PROC_INST_ID_);
create index ACT_IDX_RU_ACTI_PROC_ACT on ACT_RU_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_RU_ACTI_EXEC on ACT_RU_ACTINST(EXECUTION_ID_);
create index ACT_IDX_RU_ACTI_EXEC_ACT on ACT_RU_ACTINST(EXECUTION_ID_, ACT_ID_);
create index ACT_IDX_RU_ACTI_TASK on ACT_RU_ACTINST(TASK_ID_);

alter table ACT_GE_BYTEARRAY
    add constraint ACT_FK_BYTEARR_DEPL 
    foreign key (DEPLOYMENT_ID_) 
    references ACT_RE_DEPLOYMENT (ID_);

alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, DERIVED_VERSION_, TENANT_ID_);
    
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCINST 
    foreign key (PROC_INST_ID_) 
    references ACT_RU_EXECUTION (ID_) on delete cascade on update cascade;

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PARENT 
    foreign key (PARENT_ID_) 
    references ACT_RU_EXECUTION (ID_) on delete cascade;
    
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_SUPER 
    foreign key (SUPER_EXEC_) 
    references ACT_RU_EXECUTION (ID_) on delete cascade;
    
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCDEF 
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_TSKASS_TASK 
    foreign key (TASK_ID_) 
    references ACT_RU_TASK (ID_);
    
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_ATHRZ_PROCEDEF 
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF(ID_);
    
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_IDL_PROCINST
    foreign key (PROC_INST_ID_) 
    references ACT_RU_EXECUTION (ID_);       
    
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_TASK
  	add constraint ACT_FK_TASK_PROCDEF
  	foreign key (PROC_DEF_ID_)
  	references ACT_RE_PROCDEF (ID_);
  
alter table ACT_RU_VARIABLE 
    add constraint ACT_FK_VAR_EXE 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION(ID_);

alter table ACT_RU_JOB 
    add constraint ACT_FK_JOB_EXECUTION 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_JOB 
    add constraint ACT_FK_JOB_PROCESS_INSTANCE 
    foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_JOB 
    add constraint ACT_FK_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_TIMER_JOB 
    add constraint ACT_FK_TIMER_JOB_EXECUTION 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_TIMER_JOB 
    add constraint ACT_FK_TIMER_JOB_PROCESS_INSTANCE 
    foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_TIMER_JOB 
    add constraint ACT_FK_TIMER_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_RU_SUSPENDED_JOB 
    add constraint ACT_FK_SUSPENDED_JOB_EXECUTION 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_SUSPENDED_JOB 
    add constraint ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE 
    foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_SUSPENDED_JOB 
    add constraint ACT_FK_SUSPENDED_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_RU_DEADLETTER_JOB 
    add constraint ACT_FK_DEADLETTER_JOB_EXECUTION 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_DEADLETTER_JOB 
    add constraint ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE 
    foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_DEADLETTER_JOB 
    add constraint ACT_FK_DEADLETTER_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_RU_EVENT_SUBSCR
    add constraint ACT_FK_EVENT_EXEC
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION(ID_);
    
alter table ACT_RE_MODEL 
    add constraint ACT_FK_MODEL_SOURCE 
    foreign key (EDITOR_SOURCE_VALUE_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RE_MODEL 
    add constraint ACT_FK_MODEL_SOURCE_EXTRA 
    foreign key (EDITOR_SOURCE_EXTRA_VALUE_ID_) 
    references ACT_GE_BYTEARRAY (ID_);
    
alter table ACT_RE_MODEL 
    add constraint ACT_FK_MODEL_DEPLOYMENT 
    foreign key (DEPLOYMENT_ID_) 
    references ACT_RE_DEPLOYMENT (ID_);        

alter table ACT_PROCDEF_INFO 
    add constraint ACT_FK_INFO_JSON_BA 
    foreign key (INFO_JSON_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_PROCDEF_INFO 
    add constraint ACT_FK_INFO_PROCDEF 
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_PROCDEF_INFO
    add constraint ACT_UNIQ_INFO_PROCDEF
    unique (PROC_DEF_ID_);

insert into ACT_GE_PROPERTY
values ('schema.version', '7.0.0.0', 1);

insert into ACT_GE_PROPERTY
values ('schema.history', 'create(7.0.0.0)', 1);

create table ACT_HI_IDENTITYLINK (
    ID_ varchar(64),
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    CREATE_TIME_ datetime(3),
    PROC_INST_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);
create index ACT_IDX_HI_IDENT_LNK_SCOPE on ACT_HI_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_IDENT_LNK_SUB_SCOPE on ACT_HI_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_IDENT_LNK_SCOPE_DEF on ACT_HI_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create table ACT_HI_ENTITYLINK (
    ID_ varchar(64),
    LINK_TYPE_ varchar(255),
    CREATE_TIME_ datetime(3),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    PARENT_ELEMENT_ID_ varchar(255),
    REF_SCOPE_ID_ varchar(255),
    REF_SCOPE_TYPE_ varchar(255),
    REF_SCOPE_DEFINITION_ID_ varchar(255),
    ROOT_SCOPE_ID_ varchar(255),
    ROOT_SCOPE_TYPE_ varchar(255),
    HIERARCHY_TYPE_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_HI_ENT_LNK_SCOPE on ACT_HI_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_HI_ENT_LNK_REF_SCOPE on ACT_HI_ENTITYLINK(REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_HI_ENT_LNK_ROOT_SCOPE on ACT_HI_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_HI_ENT_LNK_SCOPE_DEF on ACT_HI_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

create table ACT_HI_TASKINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_DEF_ID_ varchar(64),
    TASK_DEF_ID_ varchar(64),
    TASK_DEF_KEY_ varchar(255),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    PROPAGATED_STAGE_INST_ID_ varchar(255),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    START_TIME_ datetime(3) not null,
    CLAIM_TIME_ datetime(3),
    END_TIME_ datetime(3),
    DURATION_ bigint,
    DELETE_REASON_ varchar(4000),
    PRIORITY_ integer,
    DUE_DATE_ datetime(3),
    FORM_KEY_ varchar(255),
    CATEGORY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    LAST_UPDATED_TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_TSK_LOG (
    ID_ bigint auto_increment,
    TYPE_ varchar(64),
    TASK_ID_ varchar(64) not null,
    TIME_STAMP_ timestamp(3) not null,
    USER_ID_ varchar(255),
    DATA_ varchar(4000),
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_HI_TASK_SCOPE on ACT_HI_TASKINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SUB_SCOPE on ACT_HI_TASKINST(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SCOPE_DEF on ACT_HI_TASKINST(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create table ACT_HI_VARINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(100),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    META_INFO_ varchar(4000),
    CREATE_TIME_ datetime(3),
    LAST_UPDATED_TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_HI_PROCVAR_NAME_TYPE on ACT_HI_VARINST(NAME_, VAR_TYPE_);
create index ACT_IDX_HI_VAR_SCOPE_ID_TYPE on ACT_HI_VARINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_VAR_SUB_ID_TYPE on ACT_HI_VARINST(SUB_SCOPE_ID_, SCOPE_TYPE_);

create table ACT_HI_PROCINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_INST_ID_ varchar(64) not null,
    BUSINESS_KEY_ varchar(255),
    PROC_DEF_ID_ varchar(64) not null,
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    DURATION_ bigint,
    START_USER_ID_ varchar(255),
    START_ACT_ID_ varchar(255),
    END_ACT_ID_ varchar(255),
    SUPER_PROCESS_INSTANCE_ID_ varchar(64),
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    CALLBACK_ID_ varchar(255),
    CALLBACK_TYPE_ varchar(255),
    REFERENCE_ID_ varchar(255),
    REFERENCE_TYPE_ varchar(255),
    PROPAGATED_STAGE_INST_ID_ varchar(255),
    BUSINESS_STATUS_ varchar(255),
    primary key (ID_),
    unique (PROC_INST_ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_ACTINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_DEF_ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    EXECUTION_ID_ varchar(64) not null,
    ACT_ID_ varchar(255) not null,
    TASK_ID_ varchar(64),
    CALL_PROC_INST_ID_ varchar(64),
    ACT_NAME_ varchar(255),
    ACT_TYPE_ varchar(255) not null,
    ASSIGNEE_ varchar(255),
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    TRANSACTION_ORDER_ integer,
    DURATION_ bigint,
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_DETAIL (
    ID_ varchar(64) not null,
    TYPE_ varchar(255) not null,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    ACT_INST_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(255),
    REV_ integer,
    TIME_ datetime(3) not null,
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_COMMENT (
    ID_ varchar(64) not null,
    TYPE_ varchar(255),
    TIME_ datetime(3) not null,
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTION_ varchar(255),
    MESSAGE_ varchar(4000),
    FULL_MSG_ LONGBLOB,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_ATTACHMENT (
    ID_ varchar(64) not null,
    REV_ integer,
    USER_ID_ varchar(255),
    NAME_ varchar(255),
    DESCRIPTION_ varchar(4000),
    TYPE_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    URL_ varchar(4000),
    CONTENT_ID_ varchar(64),
    TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_HI_PRO_INST_END on ACT_HI_PROCINST(END_TIME_);
create index ACT_IDX_HI_PRO_I_BUSKEY on ACT_HI_PROCINST(BUSINESS_KEY_);
create index ACT_IDX_HI_PRO_SUPER_PROCINST on ACT_HI_PROCINST(SUPER_PROCESS_INSTANCE_ID_);
create index ACT_IDX_HI_ACT_INST_START on ACT_HI_ACTINST(START_TIME_);
create index ACT_IDX_HI_ACT_INST_END on ACT_HI_ACTINST(END_TIME_);
create index ACT_IDX_HI_DETAIL_PROC_INST on ACT_HI_DETAIL(PROC_INST_ID_);
create index ACT_IDX_HI_DETAIL_ACT_INST on ACT_HI_DETAIL(ACT_INST_ID_);
create index ACT_IDX_HI_DETAIL_TIME on ACT_HI_DETAIL(TIME_);
create index ACT_IDX_HI_DETAIL_NAME on ACT_HI_DETAIL(NAME_);
create index ACT_IDX_HI_DETAIL_TASK_ID on ACT_HI_DETAIL(TASK_ID_);
create index ACT_IDX_HI_PROCVAR_PROC_INST on ACT_HI_VARINST(PROC_INST_ID_);
create index ACT_IDX_HI_PROCVAR_TASK_ID on ACT_HI_VARINST(TASK_ID_);
create index ACT_IDX_HI_PROCVAR_EXE on ACT_HI_VARINST(EXECUTION_ID_);
create index ACT_IDX_HI_ACT_INST_PROCINST on ACT_HI_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_HI_ACT_INST_EXEC on ACT_HI_ACTINST(EXECUTION_ID_, ACT_ID_);
create index ACT_IDX_HI_IDENT_LNK_TASK on ACT_HI_IDENTITYLINK(TASK_ID_);
create index ACT_IDX_HI_IDENT_LNK_PROCINST on ACT_HI_IDENTITYLINK(PROC_INST_ID_);
create index ACT_IDX_HI_TASK_INST_PROCINST on ACT_HI_TASKINST(PROC_INST_ID_);

-- Mango workflow configuration schema.
CREATE TABLE IF NOT EXISTS `workflow_category` (
  `id` bigint NOT NULL COMMENT '流程分类ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '机构隔离ID',
  `category_name` varchar(64) NOT NULL COMMENT '分类名称',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用 1-启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_category_code` (`tenant_id`,`category_code`),
  KEY `idx_workflow_category_status` (`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程分类表';

CREATE TABLE IF NOT EXISTS `workflow_definition` (
  `id` bigint NOT NULL COMMENT '流程定义ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '机构隔离ID',
  `category_id` bigint NOT NULL COMMENT '流程分类ID',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  `admin_users` varchar(1000) DEFAULT NULL COMMENT '流程管理员用户名JSON数组',
  `icon` varchar(512) DEFAULT NULL COMMENT '流程图标',
  `definition_name` varchar(128) NOT NULL COMMENT '流程名称',
  `definition_key` varchar(128) NOT NULL COMMENT '流程编码，对应 Flowable process id',
  `deployment_id` varchar(128) DEFAULT NULL COMMENT 'Flowable 部署ID',
  `process_definition_id` varchar(128) DEFAULT NULL COMMENT 'Flowable 流程定义ID',
  `process_definition_version` int DEFAULT NULL COMMENT 'Flowable 流程定义版本',
  `published_version_no` int DEFAULT NULL COMMENT 'Mango最近发布版本号',
  `source_template_id` bigint DEFAULT NULL COMMENT '来源流程模板ID',
  `source_template_code` varchar(128) DEFAULT NULL COMMENT '来源流程模板编码',
  `source_template_version` int DEFAULT NULL COMMENT '来源流程模板版本',
  `designer_json` longtext NOT NULL COMMENT '设计器JSON内容',
  `bpmn_xml` longtext DEFAULT NULL COMMENT '最近一次发布生成的BPMN XML内容',
  `form_code` varchar(128) DEFAULT NULL COMMENT '表单编码',
  `form_json` longtext DEFAULT NULL COMMENT '动态表单JSON配置',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '流程状态: DRAFT-草稿 PUBLISHED-已发布 DISABLED-停用',
  `last_deploy_time` datetime DEFAULT NULL COMMENT '最后发布时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_definition_key` (`tenant_id`,`definition_key`),
  KEY `idx_workflow_definition_category` (`category_id`),
  KEY `idx_workflow_definition_org` (`org_id`),
  KEY `idx_workflow_definition_status` (`status`),
  KEY `idx_workflow_definition_source_template` (`source_template_id`),
  KEY `idx_workflow_definition_deployment` (`deployment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程定义配置表';

CREATE TABLE IF NOT EXISTS `workflow_template_category` (
  `id` bigint NOT NULL COMMENT '流程模板分类ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '机构隔离ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父级分类ID',
  `category_name` varchar(64) NOT NULL COMMENT '分类名称',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `icon` varchar(128) DEFAULT NULL COMMENT '分类图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用 1-启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_template_category_code` (`tenant_id`,`category_code`),
  KEY `idx_workflow_template_category_parent` (`parent_id`),
  KEY `idx_workflow_template_category_status` (`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模板分类表';

CREATE TABLE IF NOT EXISTS `workflow_template` (
  `id` bigint NOT NULL COMMENT '流程模板ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '机构隔离ID',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `template_code` varchar(128) NOT NULL COMMENT '模板编码',
  `template_category_id` bigint DEFAULT NULL COMMENT '流程模板分类ID',
  `category_code` varchar(64) DEFAULT NULL COMMENT '业务场景编码',
  `category_name` varchar(64) DEFAULT NULL COMMENT '业务场景名称',
  `icon` varchar(512) DEFAULT NULL COMMENT '流程图标',
  `admin_users` varchar(1000) DEFAULT NULL COMMENT '流程管理员用户名JSON数组',
  `designer_json` longtext NOT NULL COMMENT '设计器JSON内容快照',
  `form_code` varchar(128) DEFAULT NULL COMMENT '表单编码',
  `form_json` longtext DEFAULT NULL COMMENT '动态表单JSON配置快照',
  `version_no` int NOT NULL DEFAULT '1' COMMENT '模板版本号',
  `latest_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否当前版本',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '模板状态: ENABLED-可导入 DISABLED-停用 ARCHIVED-归档',
  `source_definition_id` bigint DEFAULT NULL COMMENT '来源流程定义ID',
  `source_definition_key` varchar(128) DEFAULT NULL COMMENT '来源流程编码',
  `source_definition_name` varchar(128) DEFAULT NULL COMMENT '来源流程名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_template_version` (`tenant_id`,`template_code`,`version_no`),
  KEY `idx_workflow_template_category` (`template_category_id`),
  KEY `idx_workflow_template_code` (`tenant_id`,`template_code`),
  KEY `idx_workflow_template_latest` (`tenant_id`,`template_code`,`latest_flag`),
  KEY `idx_workflow_template_status` (`status`),
  KEY `idx_workflow_template_source_definition` (`source_definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模板快照表';

CREATE TABLE IF NOT EXISTS `workflow_node_definition` (
  `id` bigint NOT NULL COMMENT '节点定义ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '机构隔离ID',
  `node_definition_code` varchar(64) NOT NULL COMMENT '节点定义编码',
  `node_type` varchar(64) NOT NULL COMMENT '节点类型',
  `node_name` varchar(64) NOT NULL COMMENT '节点名称',
  `category_code` varchar(64) NOT NULL COMMENT '节点分类编码',
  `category_name` varchar(64) NOT NULL COMMENT '节点分类名称',
  `description` varchar(255) DEFAULT NULL COMMENT '节点说明',
  `bpmn_type` varchar(64) NOT NULL COMMENT '底层BPMN类型',
  `execution_type` varchar(64) NOT NULL COMMENT '执行类型',
  `color` varchar(32) DEFAULT NULL COMMENT '节点颜色',
  `icon` varchar(64) DEFAULT NULL COMMENT '节点图标',
  `property_schema` json DEFAULT NULL COMMENT '属性配置JSON Schema',
  `default_properties` json DEFAULT NULL COMMENT '默认属性JSON',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用 1-启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_node_definition_code` (`tenant_id`,`node_definition_code`),
  KEY `idx_workflow_node_definition_category` (`category_code`,`sort`),
  KEY `idx_workflow_node_definition_status` (`status`,`sort`),
  KEY `idx_workflow_node_definition_execution` (`execution_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程节点定义表';

CREATE TABLE IF NOT EXISTS `workflow_definition_version` (
  `id` bigint NOT NULL COMMENT '发布版本ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '机构隔离ID',
  `definition_id` bigint NOT NULL COMMENT '流程定义ID',
  `version_no` int NOT NULL COMMENT 'Mango发布版本号',
  `category_id` bigint DEFAULT NULL COMMENT '发布时流程分类ID快照',
  `org_id` bigint DEFAULT NULL COMMENT '发布时所属组织ID快照',
  `admin_users` varchar(1000) DEFAULT NULL COMMENT '发布时流程管理员用户名JSON数组快照',
  `icon` varchar(512) DEFAULT NULL COMMENT '发布时流程图标快照',
  `definition_name` varchar(128) DEFAULT NULL COMMENT '发布时流程名称快照',
  `definition_key` varchar(128) DEFAULT NULL COMMENT '发布时流程编码快照',
  `remark` varchar(255) DEFAULT NULL COMMENT '发布时备注快照',
  `form_code` varchar(128) DEFAULT NULL COMMENT '发布时表单编码快照',
  `designer_json` longtext NOT NULL COMMENT '发布时设计器JSON快照',
  `form_json` longtext DEFAULT NULL COMMENT '发布时动态表单JSON快照',
  `bpmn_xml` longtext NOT NULL COMMENT '发布时BPMN XML快照',
  `deployment_id` varchar(128) DEFAULT NULL COMMENT 'Flowable 部署ID',
  `process_definition_id` varchar(128) DEFAULT NULL COMMENT 'Flowable 流程定义ID',
  `process_definition_version` int DEFAULT NULL COMMENT 'Flowable 流程定义版本',
  `publish_status` varchar(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '发布状态: PUBLISHING-发布中 SUCCESS-成功 FAILED-失败',
  `publish_message` varchar(512) DEFAULT NULL COMMENT '发布说明或失败原因',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_definition_version` (`tenant_id`,`definition_id`,`version_no`),
  KEY `idx_workflow_version_definition` (`definition_id`,`version_no`),
  KEY `idx_workflow_version_deployment` (`deployment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程定义发布版本表';

-- Workflow built-in categories and node definitions are declared through
-- META-INF/mango/resources/workflow-common-definition.yml and synced by
-- mango-resource. Flyway keeps the table structure only.

-- -----------------------------------------------------------------------------
-- Folded from V2__workflow_process_start_permission.sql
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- Folded from V3__workflow_runtime_tables.sql
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `workflow_form_instance` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `process_instance_id` varchar(128) NOT NULL COMMENT '流程实例ID',
  `business_key` varchar(128) DEFAULT NULL COMMENT '业务主键',
  `definition_id` bigint DEFAULT NULL COMMENT 'Mango流程定义ID',
  `definition_key` varchar(128) DEFAULT NULL COMMENT '流程定义编码',
  `definition_name` varchar(128) DEFAULT NULL COMMENT '流程定义名称',
  `process_definition_id` varchar(128) DEFAULT NULL COMMENT 'Flowable流程定义ID',
  `process_definition_version` int DEFAULT NULL COMMENT 'Flowable流程版本',
  `form_code` varchar(128) DEFAULT NULL COMMENT '表单编码',
  `form_json` longtext COMMENT '表单JSON快照',
  `variables_json` longtext COMMENT '表单变量JSON快照',
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '状态: RUNNING/COMPLETED/REJECTED',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_form_instance_proc` (`process_instance_id`),
  KEY `idx_workflow_form_instance_definition` (`definition_id`),
  KEY `idx_workflow_form_instance_status` (`status`),
  KEY `idx_workflow_form_instance_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程实例表单快照';

CREATE TABLE IF NOT EXISTS `workflow_task_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `process_instance_id` varchar(128) NOT NULL COMMENT '流程实例ID',
  `task_id` varchar(128) DEFAULT NULL COMMENT '任务ID',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `task_definition_key` varchar(128) DEFAULT NULL COMMENT '任务定义Key',
  `action` varchar(32) NOT NULL COMMENT '动作: START/COMPLETE/REJECT',
  `action_name` varchar(64) NOT NULL COMMENT '动作名称',
  `operator_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '处理人',
  `comment` varchar(1000) DEFAULT NULL COMMENT '处理意见',
  `variables_json` longtext COMMENT '处理变量JSON',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_task_record_proc` (`process_instance_id`),
  KEY `idx_workflow_task_record_task` (`task_id`),
  KEY `idx_workflow_task_record_action` (`action`),
  KEY `idx_workflow_task_record_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流任务处理记录';

CREATE TABLE IF NOT EXISTS `workflow_copied_task` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `process_instance_id` varchar(128) NOT NULL COMMENT '流程实例ID',
  `process_definition_id` varchar(128) DEFAULT NULL COMMENT 'Flowable流程定义ID',
  `process_name` varchar(255) DEFAULT NULL COMMENT '流程名称',
  `process_key` varchar(128) DEFAULT NULL COMMENT '流程编码',
  `business_key` varchar(128) DEFAULT NULL COMMENT '业务主键',
  `node_definition_key` varchar(128) DEFAULT NULL COMMENT '抄送节点定义Key',
  `node_name` varchar(255) DEFAULT NULL COMMENT '抄送节点名称',
  `copied_user_id` varchar(128) NOT NULL COMMENT '抄送用户ID或用户名',
  `copied_user_name` varchar(128) DEFAULT NULL COMMENT '抄送用户名称',
  `message` varchar(1000) DEFAULT NULL COMMENT '抄送消息',
  `read_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已阅',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_copied_task_user` (`copied_user_id`, `read_flag`),
  KEY `idx_workflow_copied_task_proc` (`process_instance_id`),
  KEY `idx_workflow_copied_task_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流抄送待阅记录';

-- -----------------------------------------------------------------------------
-- Folded from V8__workflow_business_apply_center.sql
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `workflow_business_apply` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `apply_code` varchar(128) NOT NULL COMMENT '申请编号',
  `business_type` varchar(128) NOT NULL COMMENT '业务类型',
  `business_key` varchar(128) NOT NULL COMMENT '业务主键',
  `apply_title` varchar(255) NOT NULL COMMENT '申请标题',
  `apply_summary` varchar(1000) DEFAULT NULL COMMENT '申请摘要',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(128) DEFAULT NULL COMMENT '申请人名称',
  `applicant_dept_id` bigint DEFAULT NULL COMMENT '申请部门ID',
  `applicant_dept_name` varchar(128) DEFAULT NULL COMMENT '申请部门名称',
  `process_definition_id` bigint DEFAULT NULL COMMENT 'Mango流程定义ID',
  `process_definition_key` varchar(128) DEFAULT NULL COMMENT '流程定义编码',
  `engine_process_definition_id` varchar(128) DEFAULT NULL COMMENT 'Flowable流程定义ID',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `process_name` varchar(128) DEFAULT NULL COMMENT '流程名称',
  `apply_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '申请状态',
  `current_task_names` varchar(1000) DEFAULT NULL COMMENT '当前节点名称，多个逗号分隔',
  `current_task_definition_keys` varchar(1000) DEFAULT NULL COMMENT '当前节点定义Key，多个逗号分隔',
  `current_assignee_names` varchar(1000) DEFAULT NULL COMMENT '当前处理人名称，多个逗号分隔',
  `render_mode` varchar(32) NOT NULL DEFAULT 'DYNAMIC_FORM' COMMENT '渲染模式',
  `apply_page_key` varchar(128) DEFAULT NULL COMMENT '自定义申请页Key',
  `approve_page_key` varchar(128) DEFAULT NULL COMMENT '自定义审批页Key',
  `form_key` varchar(128) DEFAULT NULL COMMENT '表单Key',
  `form_version` int DEFAULT NULL COMMENT '表单版本',
  `form_json_snapshot` longtext COMMENT '动态表单JSON快照',
  `form_data_snapshot` longtext COMMENT '动态表单数据快照',
  `snapshot_ref` varchar(255) DEFAULT NULL COMMENT '业务快照引用',
  `snapshot_digest` varchar(128) DEFAULT NULL COMMENT '业务快照摘要',
  `variables_json` longtext COMMENT '流程变量JSON',
  `extension_json` longtext COMMENT '扩展配置JSON',
  `reapply_from_apply_id` bigint DEFAULT NULL COMMENT '重新申请来源ID',
  `latest_flag` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否最新申请',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_business_apply_code` (`apply_code`),
  KEY `idx_workflow_business_apply_biz_latest` (`tenant_id`, `business_type`, `business_key`, `latest_flag`),
  KEY `idx_workflow_business_apply_status` (`tenant_id`, `business_type`, `apply_status`),
  KEY `idx_workflow_business_apply_process` (`process_instance_id`),
  KEY `idx_workflow_business_apply_definition` (`process_definition_id`),
  KEY `idx_workflow_business_apply_reapply` (`reapply_from_apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务工作流申请中心';

CREATE TABLE IF NOT EXISTS `workflow_business_apply_current_task` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `apply_id` bigint NOT NULL COMMENT '申请ID',
  `business_type` varchar(128) NOT NULL COMMENT '业务类型',
  `business_key` varchar(128) NOT NULL COMMENT '业务主键',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `task_id` varchar(128) DEFAULT NULL COMMENT '任务ID',
  `task_definition_key` varchar(128) DEFAULT NULL COMMENT '任务定义Key',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `assignee_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `assignee_name` varchar(128) DEFAULT NULL COMMENT '处理人名称',
  `arrived_at` datetime DEFAULT NULL COMMENT '到达时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_business_apply_current_apply` (`apply_id`),
  KEY `idx_workflow_business_apply_current_biz` (`tenant_id`, `business_type`, `business_key`),
  KEY `idx_workflow_business_apply_current_task` (`task_id`),
  KEY `idx_workflow_business_apply_current_node` (`task_definition_key`),
  KEY `idx_workflow_business_apply_current_assignee` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务工作流申请当前任务';

CREATE TABLE IF NOT EXISTS `workflow_business_apply_status_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `apply_id` bigint NOT NULL COMMENT '申请ID',
  `from_status` varchar(32) DEFAULT NULL COMMENT '变更前状态',
  `to_status` varchar(32) NOT NULL COMMENT '变更后状态',
  `action` varchar(32) NOT NULL COMMENT '动作',
  `action_name` varchar(64) NOT NULL COMMENT '动作名称',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人名称',
  `comment` varchar(1000) DEFAULT NULL COMMENT '备注',
  `task_id` varchar(128) DEFAULT NULL COMMENT '任务ID',
  `task_definition_key` varchar(128) DEFAULT NULL COMMENT '任务定义Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_business_apply_log_apply` (`apply_id`),
  KEY `idx_workflow_business_apply_log_process` (`process_instance_id`),
  KEY `idx_workflow_business_apply_log_action` (`action`),
  KEY `idx_workflow_business_apply_log_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务工作流申请状态流水';
