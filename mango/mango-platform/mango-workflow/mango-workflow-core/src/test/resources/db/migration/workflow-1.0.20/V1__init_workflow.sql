-- Affected Workflow table shapes from Mango Maven 1.0.20 plus the source
-- columns required by the later participation backfill migration.
CREATE TABLE `workflow_form_instance` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(64) NOT NULL DEFAULT '1',
  `process_instance_id` varchar(128) NOT NULL,
  `business_key` varchar(128) DEFAULT NULL,
  `definition_key` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_form_instance_proc` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `workflow_task_record` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(64) NOT NULL DEFAULT '1',
  `process_instance_id` varchar(128) NOT NULL,
  `action` varchar(32) NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(128) DEFAULT NULL,
  `variables_json` longtext,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `workflow_copied_task` (
  `id` bigint NOT NULL,
  `read_time` datetime DEFAULT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `workflow_business_apply_current_task` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(64) NOT NULL DEFAULT '1',
  `business_key` varchar(128) DEFAULT NULL,
  `process_instance_id` varchar(128) DEFAULT NULL,
  `assignee_id` bigint DEFAULT NULL,
  `assignee_name` varchar(128) DEFAULT NULL,
  `arrived_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `workflow_business_apply_status_log` (
  `id` bigint NOT NULL,
  `process_instance_id` varchar(128) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
