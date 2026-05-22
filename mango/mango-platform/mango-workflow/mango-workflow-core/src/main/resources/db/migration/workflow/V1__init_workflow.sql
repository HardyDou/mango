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

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_name`, `category_code`, `sort`, `status`, `remark`, `created_by`, `created_time`, `created_at`, `updated_by`, `updated_time`, `updated_at`)
VALUES (1,1,'通用流程','COMMON',1,1,'系统默认通用流程分类',NULL,NOW(),NOW(),NULL,NOW(),NOW())
ON DUPLICATE KEY UPDATE `category_name` = VALUES(`category_name`);

INSERT INTO `workflow_template_category` (`id`, `tenant_id`, `parent_id`, `category_name`, `category_code`, `icon`, `sort`, `status`, `remark`, `created_by`, `created_time`, `created_at`, `updated_by`, `updated_time`, `updated_at`)
VALUES (1,1,NULL,'通用模板','COMMON_TEMPLATE','CollectionTag',1,1,'系统默认通用流程模板分类',NULL,NOW(),NOW(),NULL,NOW(),NOW())
ON DUPLICATE KEY UPDATE `category_name` = VALUES(`category_name`);

INSERT INTO `workflow_node_definition` (`id`, `tenant_id`, `node_definition_code`, `node_type`, `node_name`, `category_code`, `category_name`, `description`, `bpmn_type`, `execution_type`, `color`, `icon`, `property_schema`, `default_properties`, `sort`, `status`, `created_by`, `created_time`, `created_at`, `updated_by`, `updated_time`, `updated_at`)
VALUES
(360000001,1,'ROOT','ROOT','发起人','BASIC','基础节点','流程发起节点，由系统自动创建','startEvent','NONE','#64748b','User',NULL,JSON_OBJECT(),1,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000002,1,'APPROVAL','APPROVAL','审批节点','BASIC','基础节点','人工审批、会签、或签等人工处理节点','userTask','USER_TASK','#2563eb','Stamp',NULL,JSON_OBJECT('assigneeType','USER'),10,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000003,1,'CC','CC','抄送节点','BASIC','基础节点','流程流转到此处时通知相关人员','serviceTask','EVENT_PUBLISH','#7c3aed','Send',NULL,JSON_OBJECT('eventName','workflow.cc'),20,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000004,1,'EXCLUSIVE_GATEWAY','EXCLUSIVE_GATEWAY','条件分支','BASIC','基础节点','按条件选择一个分支继续流转','exclusiveGateway','NONE','#f59e0b','GitBranch',NULL,JSON_OBJECT(),30,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000005,1,'PARALLEL_GATEWAY','PARALLEL_GATEWAY','并行分支','BASIC','基础节点','多个分支同时流转并在结束后合并','parallelGateway','NONE','#0f766e','GitFork',NULL,JSON_OBJECT(),40,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000006,1,'SERVICE_BEAN','SERVICE','Bean服务任务','SERVICE','服务节点','调用白名单 Spring Bean 执行业务动作','serviceTask','SPRING_BEAN','#0891b2','Box',NULL,JSON_OBJECT('beanName','','methodName',''),50,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000007,1,'SERVICE_HTTP','SERVICE','HTTP服务任务','SERVICE','服务节点','调用受控 HTTP URL 执行业务动作','serviceTask','HTTP_URL','#dc2626','Webhook',NULL,JSON_OBJECT('url','','method','POST','timeoutMillis',5000),60,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000008,1,'SERVICE_REMOTE','SERVICE','远程服务任务','SERVICE','服务节点','调用受控远程服务执行微服务动作','serviceTask','REMOTE_SERVICE','#ea580c','Cloud',NULL,JSON_OBJECT('serviceName','','operation',''),70,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000009,1,'EVENT_PUBLISH','SERVICE','事件发布任务','SERVICE','服务节点','发布流程事件，支持单体事件和后续消息总线扩展','serviceTask','EVENT_PUBLISH','#16a34a','Radio',NULL,JSON_OBJECT('eventName',''),80,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000101,1,'GUARANTEE_CUSTOMER_SUBMIT','GUARANTEE_CUSTOMER_SUBMIT','客户提交资料','GUARANTEE','保函节点','客户提交保函申请资料','userTask','USER_TASK','#2563eb','FileText',NULL,JSON_OBJECT('businessStage','CUSTOMER_SUBMIT'),110,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000102,1,'GUARANTEE_SUPPLEMENT','GUARANTEE_SUPPLEMENT','资料补正','GUARANTEE','保函节点','资料缺失或不合规时发起补正','userTask','USER_TASK','#f59e0b','FileWarning',NULL,JSON_OBJECT('businessStage','SUPPLEMENT'),120,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000103,1,'GUARANTEE_RISK_REVIEW','GUARANTEE_RISK_REVIEW','元丰行风控初审','GUARANTEE','保函节点','元丰行进行业务初审和风控判断','userTask','USER_TASK','#7c3aed','ShieldCheck',NULL,JSON_OBJECT('businessStage','RISK_REVIEW'),130,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000104,1,'GUARANTEE_CONTRACT_PREPARE','GUARANTEE_CONTRACT_PREPARE','签约资料整理','GUARANTEE','保函节点','整理签约、授权、反担保等资料','userTask','USER_TASK','#0f766e','ClipboardList',NULL,JSON_OBJECT('businessStage','CONTRACT_PREPARE'),140,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000105,1,'GUARANTEE_GUARANTOR_APPROVE','GUARANTEE_GUARANTOR_APPROVE','担保机构审批','GUARANTEE','保函节点','下游融资性担保机构内部审批','userTask','USER_TASK','#2563eb','Landmark',NULL,JSON_OBJECT('businessStage','GUARANTOR_APPROVE'),150,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000106,1,'GUARANTEE_BANK_SUBMIT','GUARANTEE_BANK_SUBMIT','银行资料提交','GUARANTEE','保函节点','向银行系统或线下流程提交资料','serviceTask','REMOTE_SERVICE','#dc2626','Building2',NULL,JSON_OBJECT('businessStage','BANK_SUBMIT'),160,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000107,1,'GUARANTEE_BANK_FEEDBACK','GUARANTEE_BANK_FEEDBACK','银行反馈补件','GUARANTEE','保函节点','处理银行反馈、补件或退回','userTask','USER_TASK','#f59e0b','MessageSquareWarning',NULL,JSON_OBJECT('businessStage','BANK_FEEDBACK'),170,1,NULL,NOW(),NOW(),NULL,NOW(),NOW()),
(360000108,1,'GUARANTEE_ARCHIVE','GUARANTEE_ARCHIVE','出函归档','GUARANTEE','保函节点','出函后归档资料并同步业务状态','serviceTask','EVENT_PUBLISH','#16a34a','Archive',NULL,JSON_OBJECT('businessStage','ARCHIVE','eventName','guarantee.archived'),180,1,NULL,NOW(),NOW(),NULL,NOW(),NOW())
ON DUPLICATE KEY UPDATE `node_name` = VALUES(`node_name`), `category_code` = VALUES(`category_code`), `category_name` = VALUES(`category_name`), `description` = VALUES(`description`), `bpmn_type` = VALUES(`bpmn_type`), `execution_type` = VALUES(`execution_type`), `color` = VALUES(`color`), `icon` = VALUES(`icon`), `default_properties` = VALUES(`default_properties`), `sort` = VALUES(`sort`), `status` = VALUES(`status`);

-- -----------------------------------------------------------------------------
-- Folded from V2__workflow_process_start_permission.sql
-- -----------------------------------------------------------------------------

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2602000,1,'internal-admin',2602,3,'发起流程','workflow:process:start',NULL,NULL,NULL,1,1,0,0,0,NULL,'workflow:process:start',NULL,NULL,NOW(),NOW(),'发起已发布流程实例',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE `permissions` = VALUES(`permissions`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `del_flag` = VALUES(`del_flag`);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1101,1,1,2602000,101),
(2054,1,2,2602000,54);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(52602000,1,1,2602000,NOW(),NULL,NOW(),NULL,NOW()),
(62602000,2,2,2602000,NOW(),NULL,NOW(),NULL,NOW()),
(72602000,3,3,2602000,NOW(),NULL,NOW(),NULL,NOW()),
(82602000,4,4,2602000,NOW(),NULL,NOW(),NULL,NOW());



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

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2601001,1,'internal-admin',2601,3,'查询任务详情','workflow:task:detail',NULL,NULL,NULL,101,1,0,0,0,NULL,'workflow:task:detail',NULL,NULL,NOW(),NOW(),'查询工作流任务详情、表单与审批记录',0,NULL,NOW(),NULL,NOW()),
(2601002,1,'internal-admin',2601,3,'审批通过','workflow:task:complete',NULL,NULL,NULL,102,1,0,0,0,NULL,'workflow:task:complete',NULL,NULL,NOW(),NOW(),'完成工作流待办任务',0,NULL,NOW(),NULL,NOW()),
(2601003,1,'internal-admin',2601,3,'审批驳回','workflow:task:reject',NULL,NULL,NULL,103,1,0,0,0,NULL,'workflow:task:reject',NULL,NULL,NOW(),NOW(),'驳回并终止当前工作流实例',0,NULL,NOW(),NULL,NOW()),
(2601004,1,'internal-admin',2601,3,'审批暂存','workflow:task:save',NULL,NULL,NULL,104,1,0,0,0,NULL,'workflow:task:save',NULL,NULL,NOW(),NOW(),'暂存工作流待办任务',0,NULL,NOW(),NULL,NOW()),
(2601005,1,'internal-admin',2601,3,'审批转办','workflow:task:transfer',NULL,NULL,NULL,105,1,0,0,0,NULL,'workflow:task:transfer',NULL,NULL,NOW(),NOW(),'转办工作流待办任务',0,NULL,NOW(),NULL,NOW()),
(2601006,1,'internal-admin',2601,3,'审批加签','workflow:task:add-sign',NULL,NULL,NULL,106,1,0,0,0,NULL,'workflow:task:add-sign',NULL,NULL,NOW(),NOW(),'加签工作流待办任务',0,NULL,NOW(),NULL,NOW()),
(2601007,1,'internal-admin',2601,3,'任务认领','workflow:task:claim',NULL,NULL,NULL,107,1,0,0,0,NULL,'workflow:task:claim',NULL,NULL,NOW(),NOW(),'认领候选工作流任务',0,NULL,NOW(),NULL,NOW()),
(2601008,1,'internal-admin',2601,3,'释放任务','workflow:task:unclaim',NULL,NULL,NULL,108,1,0,0,0,NULL,'workflow:task:unclaim',NULL,NULL,NOW(),NOW(),'释放已认领工作流任务',0,NULL,NOW(),NULL,NOW()),
(2601009,1,'internal-admin',2601,3,'抄送已阅','workflow:task:read-copied',NULL,NULL,NULL,109,1,0,0,0,NULL,'workflow:task:read-copied',NULL,NULL,NOW(),NOW(),'标记工作流抄送已阅',0,NULL,NOW(),NULL,NOW()),
(2602001,1,'internal-admin',2602,3,'查询流程详情','workflow:process:detail',NULL,NULL,NULL,104,1,0,0,0,NULL,'workflow:process:detail',NULL,NULL,NOW(),NOW(),'查询工作流实例详情与审批轨迹',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE `permissions` = VALUES(`permissions`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `del_flag` = VALUES(`del_flag`);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1102,1,1,2601001,102),
(1103,1,1,2601002,103),
(1104,1,1,2601003,104),
(1105,1,1,2602001,105),
(1109,1,1,2601004,109),
(1110,1,1,2601005,110),
(1111,1,1,2601006,111),
(1112,1,1,2601007,112),
(1113,1,1,2601008,113),
(1114,1,1,2601009,114),
(2055,1,2,2601001,55),
(2056,1,2,2601002,56),
(2057,1,2,2601003,57),
(2058,1,2,2602001,58),
(2062,1,2,2601004,62),
(2063,1,2,2601005,63),
(2064,1,2,2601006,64),
(2065,1,2,2601007,65),
(2066,1,2,2601008,66),
(2067,1,2,2601009,67);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(52601001,1,1,2601001,NOW(),NULL,NOW(),NULL,NOW()),
(52601002,1,1,2601002,NOW(),NULL,NOW(),NULL,NOW()),
(52601003,1,1,2601003,NOW(),NULL,NOW(),NULL,NOW()),
(52601004,1,1,2601004,NOW(),NULL,NOW(),NULL,NOW()),
(52601005,1,1,2601005,NOW(),NULL,NOW(),NULL,NOW()),
(52601006,1,1,2601006,NOW(),NULL,NOW(),NULL,NOW()),
(52601007,1,1,2601007,NOW(),NULL,NOW(),NULL,NOW()),
(52601008,1,1,2601008,NOW(),NULL,NOW(),NULL,NOW()),
(52601009,1,1,2601009,NOW(),NULL,NOW(),NULL,NOW()),
(52602001,1,1,2602001,NOW(),NULL,NOW(),NULL,NOW()),
(62601001,2,2,2601001,NOW(),NULL,NOW(),NULL,NOW()),
(62601002,2,2,2601002,NOW(),NULL,NOW(),NULL,NOW()),
(62601003,2,2,2601003,NOW(),NULL,NOW(),NULL,NOW()),
(62601004,2,2,2601004,NOW(),NULL,NOW(),NULL,NOW()),
(62601005,2,2,2601005,NOW(),NULL,NOW(),NULL,NOW()),
(62601006,2,2,2601006,NOW(),NULL,NOW(),NULL,NOW()),
(62601007,2,2,2601007,NOW(),NULL,NOW(),NULL,NOW()),
(62601008,2,2,2601008,NOW(),NULL,NOW(),NULL,NOW()),
(62601009,2,2,2601009,NOW(),NULL,NOW(),NULL,NOW()),
(62602001,2,2,2602001,NOW(),NULL,NOW(),NULL,NOW()),
(72601001,3,3,2601001,NOW(),NULL,NOW(),NULL,NOW()),
(72601002,3,3,2601002,NOW(),NULL,NOW(),NULL,NOW()),
(72601003,3,3,2601003,NOW(),NULL,NOW(),NULL,NOW()),
(72601004,3,3,2601004,NOW(),NULL,NOW(),NULL,NOW()),
(72601005,3,3,2601005,NOW(),NULL,NOW(),NULL,NOW()),
(72601006,3,3,2601006,NOW(),NULL,NOW(),NULL,NOW()),
(72601007,3,3,2601007,NOW(),NULL,NOW(),NULL,NOW()),
(72601008,3,3,2601008,NOW(),NULL,NOW(),NULL,NOW()),
(72601009,3,3,2601009,NOW(),NULL,NOW(),NULL,NOW()),
(72602001,3,3,2602001,NOW(),NULL,NOW(),NULL,NOW()),
(82601001,4,4,2601001,NOW(),NULL,NOW(),NULL,NOW()),
(82601002,4,4,2601002,NOW(),NULL,NOW(),NULL,NOW()),
(82601003,4,4,2601003,NOW(),NULL,NOW(),NULL,NOW()),
(82601004,4,4,2601004,NOW(),NULL,NOW(),NULL,NOW()),
(82601005,4,4,2601005,NOW(),NULL,NOW(),NULL,NOW()),
(82601006,4,4,2601006,NOW(),NULL,NOW(),NULL,NOW()),
(82601007,4,4,2601007,NOW(),NULL,NOW(),NULL,NOW()),
(82601008,4,4,2601008,NOW(),NULL,NOW(),NULL,NOW()),
(82601009,4,4,2601009,NOW(),NULL,NOW(),NULL,NOW()),
(82602001,4,4,2602001,NOW(),NULL,NOW(),NULL,NOW());



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

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2602002,1,'internal-admin',2602,3,'创建业务申请','workflow:business-apply:create',NULL,NULL,NULL,106,1,0,0,0,NULL,'workflow:business-apply:create',NULL,NULL,NOW(),NOW(),'创建业务申请与流程实例关联记录',0,NULL,NOW(),NULL,NOW()),
(2602003,1,'internal-admin',2602,3,'查询业务申请','workflow:business-apply:list',NULL,NULL,NULL,107,1,0,0,0,NULL,'workflow:business-apply:list',NULL,NULL,NOW(),NOW(),'查询业务申请列表与最新进度',0,NULL,NOW(),NULL,NOW()),
(2602004,1,'internal-admin',2602,3,'查看业务申请','workflow:business-apply:detail',NULL,NULL,NULL,108,1,0,0,0,NULL,'workflow:business-apply:detail',NULL,NULL,NOW(),NOW(),'查看业务申请详情、历史和进度',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE `permissions` = VALUES(`permissions`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `del_flag` = VALUES(`del_flag`);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1106,1,1,2602002,106),
(1107,1,1,2602003,107),
(1108,1,1,2602004,108),
(2059,1,2,2602002,59),
(2060,1,2,2602003,60),
(2061,1,2,2602004,61);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(52602002,1,1,2602002,NOW(),NULL,NOW(),NULL,NOW()),
(52602003,1,1,2602003,NOW(),NULL,NOW(),NULL,NOW()),
(52602004,1,1,2602004,NOW(),NULL,NOW(),NULL,NOW()),
(62602002,2,2,2602002,NOW(),NULL,NOW(),NULL,NOW()),
(62602003,2,2,2602003,NOW(),NULL,NOW(),NULL,NOW()),
(62602004,2,2,2602004,NOW(),NULL,NOW(),NULL,NOW()),
(72602002,3,3,2602002,NOW(),NULL,NOW(),NULL,NOW()),
(72602003,3,3,2602003,NOW(),NULL,NOW(),NULL,NOW()),
(72602004,3,3,2602004,NOW(),NULL,NOW(),NULL,NOW()),
(82602002,4,4,2602002,NOW(),NULL,NOW(),NULL,NOW()),
(82602003,4,4,2602003,NOW(),NULL,NOW(),NULL,NOW()),
(82602004,4,4,2602004,NOW(),NULL,NOW(),NULL,NOW());



INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2604,1,'internal-admin',26,1,'流程管理','workflow:manage','/workflow/manage','Operation',NULL,2,1,1,0,0,'/workflow/manage/definition',NULL,NULL,NULL,NOW(),NOW(),'流程模板、流程定义和发布配置管理',0,NULL,NOW(),NULL,NOW()),
(260401,1,'internal-admin',2604,2,'流程模板','workflow:template','/workflow/manage/template','CollectionTag','@/views/workflow/template/index.vue',1,1,1,0,0,NULL,'workflow:template:list',NULL,NULL,NOW(),NOW(),'系统内置流程模板与租户初始化模板管理',0,NULL,NOW(),NULL,NOW()),
(260402,1,'internal-admin',2604,2,'流程定义','workflow:definition','/workflow/manage/definition','Files','@/views/workflow/definition/index.vue',2,1,1,0,0,NULL,'workflow:definition:list',NULL,NULL,NOW(),NOW(),'流程分类与流程定义管理',0,NULL,NOW(),NULL,NOW()),
(2604000,1,'internal-admin',260402,3,'查询流程','workflow:definition:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'workflow:definition:list',NULL,NULL,NOW(),NOW(),'流程定义列表查询权限',0,NULL,NOW(),NULL,NOW()),
(2604001,1,'internal-admin',260402,3,'查看流程','workflow:definition:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'workflow:definition:query',NULL,NULL,NOW(),NOW(),'流程定义详情查询权限',0,NULL,NOW(),NULL,NOW()),
(2604002,1,'internal-admin',260402,3,'新增流程','workflow:definition:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'workflow:definition:add',NULL,NULL,NOW(),NOW(),'流程定义新增权限',0,NULL,NOW(),NULL,NOW()),
(2604003,1,'internal-admin',260402,3,'编辑流程','workflow:definition:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'workflow:definition:edit',NULL,NULL,NOW(),NOW(),'流程定义编辑权限',0,NULL,NOW(),NULL,NOW()),
(2604004,1,'internal-admin',260402,3,'删除流程','workflow:definition:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'workflow:definition:delete',NULL,NULL,NOW(),NOW(),'流程定义删除权限',0,NULL,NOW(),NULL,NOW()),
(2604005,1,'internal-admin',260402,3,'调整状态','workflow:definition:status',NULL,NULL,NULL,5,1,0,0,0,NULL,'workflow:definition:status',NULL,NULL,NOW(),NOW(),'流程定义状态调整权限',0,NULL,NOW(),NULL,NOW()),
(2604006,1,'internal-admin',260402,3,'发布流程','workflow:definition:deploy',NULL,NULL,NULL,6,1,0,0,0,NULL,'workflow:definition:deploy',NULL,NULL,NOW(),NOW(),'流程定义发布权限',0,NULL,NOW(),NULL,NOW()),
(2604100,1,'internal-admin',260401,3,'查询模板','workflow:template:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'workflow:template:list',NULL,NULL,NOW(),NOW(),'流程模板列表查询权限',0,NULL,NOW(),NULL,NOW()),
(2604101,1,'internal-admin',260401,3,'查看模板','workflow:template:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'workflow:template:query',NULL,NULL,NOW(),NOW(),'流程模板详情查询权限',0,NULL,NOW(),NULL,NOW()),
(2604102,1,'internal-admin',260401,3,'新增模板','workflow:template:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'workflow:template:add',NULL,NULL,NOW(),NOW(),'流程模板新增权限',0,NULL,NOW(),NULL,NOW()),
(2604103,1,'internal-admin',260401,3,'编辑模板','workflow:template:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'workflow:template:edit',NULL,NULL,NOW(),NOW(),'流程模板编辑权限',0,NULL,NOW(),NULL,NOW()),
(2604104,1,'internal-admin',260401,3,'删除模板','workflow:template:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'workflow:template:delete',NULL,NULL,NOW(),NOW(),'流程模板删除权限',0,NULL,NOW(),NULL,NOW()),
(2604105,1,'internal-admin',260401,3,'由模板创建流程','workflow:template:create-definition',NULL,NULL,NULL,5,1,0,0,0,NULL,'workflow:template:create-definition',NULL,NULL,NOW(),NOW(),'根据流程模板创建租户流程定义权限',0,NULL,NOW(),NULL,NOW()),
(2604106,1,'internal-admin',260401,3,'推送流程','workflow:template:push',NULL,NULL,NULL,6,1,0,0,0,NULL,'workflow:template:push',NULL,NULL,NOW(),NOW(),'将流程模板推送为目标机构流程草稿权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`menu_type` = VALUES(`menu_type`),
`menu_name` = VALUES(`menu_name`),
`menu_code` = VALUES(`menu_code`),
`path` = VALUES(`path`),
`icon` = VALUES(`icon`),
`component` = VALUES(`component`),
`sort` = VALUES(`sort`),
`status` = VALUES(`status`),
`visible` = VALUES(`visible`),
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`),
`update_time` = NOW(),
`updated_at` = NOW();

UPDATE `authorization_menu`
SET `module_code` = 'mango-workflow',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (2604,260401,260402,2604000,2604001,2604002,2604003,2604004,2604005,2604006,2604100,2604101,2604102,2604103,2604104,2604105,2604106);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(12604,1,1,2604,24),
(12600401,1,1,260401,86),
(12600402,1,1,260402,87),
(126040000,1,1,2604000,88),
(126040001,1,1,2604001,89),
(126040002,1,1,2604002,90),
(126040003,1,1,2604003,91),
(126040004,1,1,2604004,92),
(126040005,1,1,2604005,93),
(126040006,1,1,2604006,94),
(126041000,1,1,2604100,95),
(126041001,1,1,2604101,96),
(126041002,1,1,2604102,97),
(126041003,1,1,2604103,98),
(126041004,1,1,2604104,99),
(126041005,1,1,2604105,100),
(126041006,1,1,2604106,101),
(22604,1,2,2604,17),
(22600401,1,2,260401,18),
(22600402,1,2,260402,19),
(226040000,1,2,2604000,20),
(226040001,1,2,2604001,21),
(226040002,1,2,2604002,22),
(226040003,1,2,2604003,23),
(226040004,1,2,2604004,24),
(226040005,1,2,2604005,25),
(226040006,1,2,2604006,26),
(226041000,1,2,2604100,27),
(226041001,1,2,2604101,28),
(226041002,1,2,2604102,29),
(226041003,1,2,2604103,30),
(226041004,1,2,2604104,31),
(226041005,1,2,2604105,32),
(226041006,1,2,2604106,33);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(52604,1,1,2604,NOW(),NULL,NOW(),NULL,NOW()),
(526401,1,1,260401,NOW(),NULL,NOW(),NULL,NOW()),
(526402,1,1,260402,NOW(),NULL,NOW(),NULL,NOW()),
(5260400,1,1,2604000,NOW(),NULL,NOW(),NULL,NOW()),
(5260403,1,1,2604003,NOW(),NULL,NOW(),NULL,NOW()),
(5260404,1,1,2604004,NOW(),NULL,NOW(),NULL,NOW()),
(5260405,1,1,2604005,NOW(),NULL,NOW(),NULL,NOW()),
(5260406,1,1,2604006,NOW(),NULL,NOW(),NULL,NOW()),
(5260407,1,1,2604001,NOW(),NULL,NOW(),NULL,NOW()),
(5260408,1,1,2604002,NOW(),NULL,NOW(),NULL,NOW()),
(5264100,1,1,2604100,NOW(),NULL,NOW(),NULL,NOW()),
(5264101,1,1,2604101,NOW(),NULL,NOW(),NULL,NOW()),
(5264102,1,1,2604102,NOW(),NULL,NOW(),NULL,NOW()),
(5264103,1,1,2604103,NOW(),NULL,NOW(),NULL,NOW()),
(5264104,1,1,2604104,NOW(),NULL,NOW(),NULL,NOW()),
(5264105,1,1,2604105,NOW(),NULL,NOW(),NULL,NOW()),
(5264106,1,1,2604106,NOW(),NULL,NOW(),NULL,NOW()),
(62604,1,2,2604,NOW(),NULL,NOW(),NULL,NOW()),
(626401,1,2,260401,NOW(),NULL,NOW(),NULL,NOW()),
(626402,1,2,260402,NOW(),NULL,NOW(),NULL,NOW()),
(6260400,1,2,2604000,NOW(),NULL,NOW(),NULL,NOW()),
(6260403,1,2,2604003,NOW(),NULL,NOW(),NULL,NOW()),
(6260404,1,2,2604004,NOW(),NULL,NOW(),NULL,NOW()),
(6260405,1,2,2604005,NOW(),NULL,NOW(),NULL,NOW()),
(6260406,1,2,2604006,NOW(),NULL,NOW(),NULL,NOW()),
(6260407,1,2,2604001,NOW(),NULL,NOW(),NULL,NOW()),
(6260408,1,2,2604002,NOW(),NULL,NOW(),NULL,NOW()),
(6264100,1,2,2604100,NOW(),NULL,NOW(),NULL,NOW()),
(6264101,1,2,2604101,NOW(),NULL,NOW(),NULL,NOW()),
(6264102,1,2,2604102,NOW(),NULL,NOW(),NULL,NOW()),
(6264103,1,2,2604103,NOW(),NULL,NOW(),NULL,NOW()),
(6264104,1,2,2604104,NOW(),NULL,NOW(),NULL,NOW()),
(6264105,1,2,2604105,NOW(),NULL,NOW(),NULL,NOW()),
(6264106,1,2,2604106,NOW(),NULL,NOW(),NULL,NOW()),
(72604,1,3,2604,NOW(),NULL,NOW(),NULL,NOW()),
(726401,1,3,260401,NOW(),NULL,NOW(),NULL,NOW()),
(726402,1,3,260402,NOW(),NULL,NOW(),NULL,NOW()),
(7260400,1,3,2604000,NOW(),NULL,NOW(),NULL,NOW()),
(7260403,1,3,2604003,NOW(),NULL,NOW(),NULL,NOW()),
(7260404,1,3,2604004,NOW(),NULL,NOW(),NULL,NOW()),
(7260405,1,3,2604005,NOW(),NULL,NOW(),NULL,NOW()),
(7260406,1,3,2604006,NOW(),NULL,NOW(),NULL,NOW()),
(7260407,1,3,2604001,NOW(),NULL,NOW(),NULL,NOW()),
(7260408,1,3,2604002,NOW(),NULL,NOW(),NULL,NOW()),
(7264100,1,3,2604100,NOW(),NULL,NOW(),NULL,NOW()),
(7264101,1,3,2604101,NOW(),NULL,NOW(),NULL,NOW()),
(7264102,1,3,2604102,NOW(),NULL,NOW(),NULL,NOW()),
(7264103,1,3,2604103,NOW(),NULL,NOW(),NULL,NOW()),
(7264104,1,3,2604104,NOW(),NULL,NOW(),NULL,NOW()),
(7264105,1,3,2604105,NOW(),NULL,NOW(),NULL,NOW()),
(7264106,1,3,2604106,NOW(),NULL,NOW(),NULL,NOW()),
(82604,1,4,2604,NOW(),NULL,NOW(),NULL,NOW()),
(826401,1,4,260401,NOW(),NULL,NOW(),NULL,NOW()),
(826402,1,4,260402,NOW(),NULL,NOW(),NULL,NOW()),
(8260400,1,4,2604000,NOW(),NULL,NOW(),NULL,NOW()),
(8260403,1,4,2604003,NOW(),NULL,NOW(),NULL,NOW()),
(8260404,1,4,2604004,NOW(),NULL,NOW(),NULL,NOW()),
(8260405,1,4,2604005,NOW(),NULL,NOW(),NULL,NOW()),
(8260406,1,4,2604006,NOW(),NULL,NOW(),NULL,NOW()),
(8260407,1,4,2604001,NOW(),NULL,NOW(),NULL,NOW()),
(8260408,1,4,2604002,NOW(),NULL,NOW(),NULL,NOW()),
(8264100,1,4,2604100,NOW(),NULL,NOW(),NULL,NOW()),
(8264101,1,4,2604101,NOW(),NULL,NOW(),NULL,NOW()),
(8264102,1,4,2604102,NOW(),NULL,NOW(),NULL,NOW()),
(8264103,1,4,2604103,NOW(),NULL,NOW(),NULL,NOW()),
(8264104,1,4,2604104,NOW(),NULL,NOW(),NULL,NOW()),
(8264105,1,4,2604105,NOW(),NULL,NOW(),NULL,NOW()),
(8264106,1,4,2604106,NOW(),NULL,NOW(),NULL,NOW());
