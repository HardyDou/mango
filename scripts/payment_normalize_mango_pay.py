#!/usr/bin/env python3
"""Normalize local Mango Pay seed data.

This script intentionally keeps application code untouched. It only updates the
current workspace database so legacy built-in payment seed data becomes the
MANGO_PAY built-in virtual channel.
"""

from __future__ import annotations

import argparse
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = REPO_ROOT / ".mango" / "dev-workspace.env"


def parse_env(path: Path) -> dict[str, str]:
    if not path.exists():
        raise SystemExit(f"Workspace env not found: {path}")
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == "'" and value[-1] == "'":
            value = value[1:-1]
        values[key] = value
    return values


def mysql_args(env: dict[str, str]) -> list[str]:
    db_name = env.get("MANGO_DB_NAME", "")
    if not db_name.startswith("mango_dev_"):
        raise SystemExit(f"Refuse to normalize non-workspace database: {db_name}")
    args = [
        "mysql",
        f"-h{env.get('MANGO_DB_HOST', '127.0.0.1')}",
        f"-P{env.get('MANGO_DB_PORT', '3306')}",
        f"-u{env.get('MANGO_DB_USERNAME', 'root')}",
        "--batch",
        "--raw",
        "--skip-column-names",
        db_name,
    ]
    password = env.get("MANGO_DB_PASSWORD", "")
    if password:
        args.insert(4, f"-p{password}")
    return args


def run_mysql(env: dict[str, str], sql: str) -> str:
    result = subprocess.run(
        [*mysql_args(env), "-e", sql],
        cwd=REPO_ROOT,
        check=True,
        text=True,
        capture_output=True,
    )
    return result.stdout.strip()


def count_sql() -> str:
    return """
SELECT 'payment_channel_legacy_primary', COUNT(*) FROM payment_channel
 WHERE tenant_id = 1 AND channel_code = 'SANDBOX' AND del_flag = 0
UNION ALL
SELECT 'payment_channel_legacy_secondary', COUNT(*) FROM payment_channel
 WHERE tenant_id = 1 AND channel_code = 'SPECIAL' AND del_flag = 0
UNION ALL
SELECT 'payment_channel_MANGO_PAY', COUNT(*) FROM payment_channel
 WHERE tenant_id = 1 AND channel_code = 'MANGO_PAY' AND del_flag = 0
UNION ALL
SELECT 'payment_channel_legacy_adapter', COUNT(*) FROM payment_channel
 WHERE tenant_id = 1 AND adapter_type IN ('SANDBOX', 'SPECIAL', 'SELF_BUILT_SANDBOX') AND del_flag = 0
UNION ALL
SELECT 'contract_environment_legacy', COUNT(*) FROM payment_channel_contract
 WHERE tenant_id = 1 AND environment IN ('SANDBOX', 'SPECIAL') AND del_flag = 0
UNION ALL
SELECT 'capability_environment_legacy', COUNT(*) FROM payment_channel_capability
 WHERE tenant_id = 1 AND environment IN ('SANDBOX', 'SPECIAL') AND del_flag = 0
UNION ALL
SELECT 'route_environment_legacy', COUNT(*) FROM payment_method_route_rule
 WHERE tenant_id = 1 AND environment IN ('SANDBOX', 'SPECIAL') AND del_flag = 0
UNION ALL
SELECT 'operation_audit_legacy_primary_channel', COUNT(*) FROM payment_operation_audit
 WHERE tenant_id = 1 AND resource_type = 'PAYMENT_CHANNEL' AND resource_id = 'SANDBOX' AND del_flag = 0
UNION ALL
SELECT 'operation_audit_legacy_secondary_channel', COUNT(*) FROM payment_operation_audit
 WHERE tenant_id = 1 AND resource_type = 'PAYMENT_CHANNEL' AND resource_id = 'SPECIAL' AND del_flag = 0;
"""


def normalize_sql() -> str:
    return """
START TRANSACTION;

SELECT COUNT(*) INTO @mango_pay_channel_count
  FROM payment_channel
 WHERE tenant_id = 1
   AND channel_code = 'MANGO_PAY'
   AND del_flag = 0;

UPDATE payment_channel
   SET channel_code = 'MANGO_PAY',
       channel_name = '芒果支付',
       channel_type = 'BUILTIN_VIRTUAL',
       adapter_type = 'MANGO_PAY',
       capability_summary = '芒果支付内置虚拟通道，支持全部标准支付方式、支付下单、回调、查单、关单、退款、退款查询、账单、对账、返回码映射和异常场景控制',
       updated_at = NOW()
 WHERE tenant_id = 1
   AND channel_code IN ('SANDBOX', 'SPECIAL')
   AND del_flag = 0
   AND @mango_pay_channel_count = 0;

UPDATE payment_channel
   SET channel_name = '芒果支付',
       channel_type = 'BUILTIN_VIRTUAL',
       adapter_type = 'MANGO_PAY',
       capability_summary = '芒果支付内置虚拟通道，支持全部标准支付方式、支付下单、回调、查单、关单、退款、退款查询、账单、对账、返回码映射和异常场景控制',
       updated_at = NOW()
 WHERE tenant_id = 1
   AND channel_code = 'MANGO_PAY'
   AND del_flag = 0;

UPDATE payment_channel
   SET adapter_type = CASE channel_code
         WHEN 'ALLINPAY' THEN 'ALLINPAY'
         WHEN 'HUAXIA_BANK' THEN 'HUAXIA_BANK'
         ELSE adapter_type
       END,
       updated_at = NOW()
 WHERE tenant_id = 1
   AND channel_code IN ('ALLINPAY', 'HUAXIA_BANK')
   AND adapter_type IN ('SANDBOX', 'SPECIAL', 'SELF_BUILT_SANDBOX')
   AND del_flag = 0;

UPDATE payment_channel
   SET del_flag = 1,
       updated_at = NOW()
 WHERE tenant_id = 1
   AND channel_code IN ('SANDBOX', 'SPECIAL')
   AND del_flag = 0
   AND @mango_pay_channel_count > 0;

UPDATE payment_channel_contract
   SET environment = 'MANGO_PAY',
       merchant_no = CASE
         WHEN merchant_no IN ('SANDBOX_MERCHANT_001', 'SPECIAL_MERCHANT_001') THEN 'MANGO_PAY_MERCHANT_001'
         ELSE merchant_no
       END,
       contract_name = REPLACE(REPLACE(REPLACE(contract_name, '沙箱', '芒果支付'), '特殊通道', '芒果支付'), '自建特殊', '芒果支付'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (environment IN ('SANDBOX', 'SPECIAL')
        OR merchant_no IN ('SANDBOX_MERCHANT_001', 'SPECIAL_MERCHANT_001')
        OR contract_name LIKE '%沙箱%'
        OR contract_name LIKE '%特殊通道%'
        OR contract_name LIKE '%自建特殊%');

UPDATE payment_channel_contract
   SET config_values_json = JSON_REMOVE(
         JSON_SET(
           COALESCE(NULLIF(config_values_json, ''), '{}'),
           '$.mangoPayScenario',
           JSON_UNQUOTE(JSON_EXTRACT(COALESCE(NULLIF(config_values_json, ''), '{}'), '$.sandboxScenario'))
         ),
         '$.sandboxScenario'
       ),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND environment = 'MANGO_PAY'
   AND JSON_CONTAINS_PATH(COALESCE(NULLIF(config_values_json, ''), '{}'), 'one', '$.sandboxScenario')
   AND NOT JSON_CONTAINS_PATH(COALESCE(NULLIF(config_values_json, ''), '{}'), 'one', '$.mangoPayScenario');

UPDATE payment_channel_contract
   SET config_values_json = JSON_REMOVE(config_values_json, '$.sandboxScenario'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND environment = 'MANGO_PAY'
   AND JSON_CONTAINS_PATH(COALESCE(NULLIF(config_values_json, ''), '{}'), 'one', '$.sandboxScenario');

UPDATE payment_channel_contract
   SET config_values_json = JSON_REMOVE(
         JSON_SET(
           COALESCE(NULLIF(config_values_json, ''), '{}'),
           '$.mangoPayRefundScenario',
           JSON_UNQUOTE(JSON_EXTRACT(COALESCE(NULLIF(config_values_json, ''), '{}'), '$.sandboxRefundScenario'))
         ),
         '$.sandboxRefundScenario'
       ),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND environment = 'MANGO_PAY'
   AND JSON_CONTAINS_PATH(COALESCE(NULLIF(config_values_json, ''), '{}'), 'one', '$.sandboxRefundScenario')
   AND NOT JSON_CONTAINS_PATH(COALESCE(NULLIF(config_values_json, ''), '{}'), 'one', '$.mangoPayRefundScenario');

UPDATE payment_channel_contract
   SET config_values_json = JSON_REMOVE(config_values_json, '$.sandboxRefundScenario'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND environment = 'MANGO_PAY'
   AND JSON_CONTAINS_PATH(COALESCE(NULLIF(config_values_json, ''), '{}'), 'one', '$.sandboxRefundScenario');

UPDATE payment_channel_contract_value
   SET field_code = 'mangoPayScenario',
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND field_code = 'sandboxScenario'
   AND NOT EXISTS (
       SELECT 1
         FROM (
           SELECT contract_id
             FROM payment_channel_contract_value
            WHERE tenant_id = 1
              AND del_flag = 0
              AND field_code = 'mangoPayScenario'
         ) existing
        WHERE existing.contract_id = payment_channel_contract_value.contract_id
   );

UPDATE payment_channel_contract_value
   SET del_flag = 1,
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND field_code = 'sandboxScenario';

UPDATE payment_channel_contract_value
   SET field_code = 'mangoPayRefundScenario',
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND field_code = 'sandboxRefundScenario'
   AND NOT EXISTS (
       SELECT 1
         FROM (
           SELECT contract_id
             FROM payment_channel_contract_value
            WHERE tenant_id = 1
              AND del_flag = 0
              AND field_code = 'mangoPayRefundScenario'
         ) existing
        WHERE existing.contract_id = payment_channel_contract_value.contract_id
   );

UPDATE payment_channel_contract_value
   SET del_flag = 1,
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND field_code = 'sandboxRefundScenario';

UPDATE payment_channel_contract_value
   SET value_text = 'MANGO_PAY_MERCHANT_001',
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND field_code = 'merchantNo'
   AND value_text = 'SANDBOX_MERCHANT_001';

UPDATE payment_channel_capability
   SET environment = 'MANGO_PAY',
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND environment IN ('SANDBOX', 'SPECIAL');

UPDATE payment_method_route_rule
   SET environment = 'MANGO_PAY',
       rule_code = REPLACE(REPLACE(rule_code, '_SANDBOX', '_MANGO_PAY'), '_SPECIAL', '_MANGO_PAY'),
       rule_name = REPLACE(REPLACE(REPLACE(rule_name, '沙箱', '芒果支付'), '特殊通道', '芒果支付'), '自建特殊', '芒果支付'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (environment IN ('SANDBOX', 'SPECIAL')
        OR rule_code LIKE '%SANDBOX%'
        OR rule_code LIKE '%SPECIAL%'
        OR rule_name LIKE '%沙箱%'
        OR rule_name LIKE '%特殊通道%'
        OR rule_name LIKE '%自建特殊%');

UPDATE payment_cashier_config
   SET display_config = REPLACE(REPLACE(REPLACE(display_config, '沙箱', '芒果支付'), '特殊通道', '芒果支付'), '自建特殊', '芒果支付'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (display_config LIKE '%沙箱%'
        OR display_config LIKE '%特殊通道%'
        OR display_config LIKE '%自建特殊%');

UPDATE payment_operation_audit
   SET resource_id = 'MANGO_PAY',
       updated_at = NOW()
 WHERE tenant_id = 1
   AND resource_type = 'PAYMENT_CHANNEL'
   AND resource_id IN ('SANDBOX', 'SPECIAL')
   AND del_flag = 0;

UPDATE payment_mango_pay_scenario_control
   SET channel_code = 'MANGO_PAY',
       remark = REPLACE(REPLACE(REPLACE(COALESCE(remark, ''), '沙箱', '芒果支付'), '特殊通道', '芒果支付'), '自建特殊', '芒果支付'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (channel_code IN ('SANDBOX', 'SPECIAL') OR remark LIKE '%沙箱%' OR remark LIKE '%特殊通道%' OR remark LIKE '%自建特殊%');

UPDATE payment_channel
   SET channel_name = '芒果支付',
       capability_summary = REPLACE(REPLACE(REPLACE(REPLACE(capability_summary,
         CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
         '芒果支付芒果支付', '芒果支付'),
         '自建芒果支付通道', '芒果支付'),
         '自建芒果支付', '芒果支付'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND channel_code = 'MANGO_PAY'
   AND del_flag = 0
   AND (channel_name <> '芒果支付'
        OR capability_summary LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR capability_summary LIKE '%芒果支付芒果支付%'
        OR capability_summary LIKE '%自建芒果支付%');

UPDATE payment_channel_contract
   SET contract_name = REPLACE(REPLACE(REPLACE(REPLACE(contract_name,
         CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
         '芒果支付芒果支付', '芒果支付'),
         '自建芒果支付通道', '芒果支付'),
         '自建芒果支付', '芒果支付'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (contract_name LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR contract_name LIKE '%芒果支付芒果支付%'
        OR contract_name LIKE '%自建芒果支付%');

UPDATE payment_method_route_rule
   SET rule_name = REPLACE(REPLACE(REPLACE(REPLACE(rule_name,
         CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
         '芒果支付芒果支付', '芒果支付'),
         '自建芒果支付通道', '芒果支付'),
         '自建芒果支付', '芒果支付'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (rule_name LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR rule_name LIKE '%芒果支付芒果支付%'
        OR rule_name LIKE '%自建芒果支付%');

UPDATE payment_cashier_config
   SET cashier_name = REPLACE(REPLACE(REPLACE(REPLACE(cashier_name,
         CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
         '芒果支付芒果支付', '芒果支付'),
         '自建芒果支付通道', '芒果支付'),
         '自建芒果支付', '芒果支付'),
       display_config = REPLACE(REPLACE(REPLACE(REPLACE(display_config,
         CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
         '芒果支付芒果支付', '芒果支付'),
         '自建芒果支付通道', '芒果支付'),
         '自建芒果支付', '芒果支付'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (cashier_name LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR cashier_name LIKE '%芒果支付芒果支付%'
        OR cashier_name LIKE '%自建芒果支付%'
        OR display_config LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR display_config LIKE '%芒果支付芒果支付%'
        OR display_config LIKE '%自建芒果支付%');

UPDATE payment_notification_record
   SET target_url = REPLACE(target_url, '/payment/sandbox', '/payment/mango-pay/virtual'),
       updated_at = NOW()
 WHERE tenant_id = 1
   AND del_flag = 0
   AND target_url LIKE '%/payment/sandbox%';

DROP TABLE IF EXISTS payment_special_channel_scenario_control;

COMMIT;
"""


def residual_sql() -> str:
    return """
SELECT 'payment_channel_legacy', COUNT(*) FROM payment_channel
 WHERE tenant_id = 1 AND channel_code IN ('SANDBOX', 'SPECIAL') AND del_flag = 0
UNION ALL
SELECT 'payment_channel_legacy_adapter', COUNT(*) FROM payment_channel
 WHERE tenant_id = 1 AND adapter_type IN ('SANDBOX', 'SPECIAL', 'SELF_BUILT_SANDBOX') AND del_flag = 0
UNION ALL
SELECT 'contract_legacy_environment', COUNT(*) FROM payment_channel_contract
 WHERE tenant_id = 1 AND environment IN ('SANDBOX', 'SPECIAL') AND del_flag = 0
UNION ALL
SELECT 'capability_legacy_environment', COUNT(*) FROM payment_channel_capability
 WHERE tenant_id = 1 AND environment IN ('SANDBOX', 'SPECIAL') AND del_flag = 0
UNION ALL
SELECT 'route_legacy_environment', COUNT(*) FROM payment_method_route_rule
 WHERE tenant_id = 1 AND environment IN ('SANDBOX', 'SPECIAL') AND del_flag = 0
UNION ALL
SELECT 'audit_legacy_channel', COUNT(*) FROM payment_operation_audit
 WHERE tenant_id = 1 AND resource_type = 'PAYMENT_CHANNEL' AND resource_id IN ('SANDBOX', 'SPECIAL') AND del_flag = 0
UNION ALL
SELECT 'contract_value_legacy_field', COUNT(*) FROM payment_channel_contract_value
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (field_code IN ('sandboxScenario', 'sandboxRefundScenario')
        OR value_text IN ('SANDBOX_MERCHANT_001', 'SPECIAL_MERCHANT_001'));
"""


def display_residual_sql() -> str:
    return """
SELECT 'channel_display_residual', COUNT(*) FROM payment_channel
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (channel_name LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR channel_name LIKE '%芒果支付芒果支付%'
        OR channel_name LIKE '%自建芒果支付%'
        OR capability_summary LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR capability_summary LIKE '%芒果支付芒果支付%'
        OR capability_summary LIKE '%自建芒果支付%')
UNION ALL
SELECT 'contract_display_residual', COUNT(*) FROM payment_channel_contract
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (contract_name LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR contract_name LIKE '%芒果支付芒果支付%'
        OR contract_name LIKE '%自建芒果支付%')
UNION ALL
SELECT 'route_display_residual', COUNT(*) FROM payment_method_route_rule
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (rule_name LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR rule_name LIKE '%芒果支付芒果支付%'
        OR rule_name LIKE '%自建芒果支付%')
UNION ALL
SELECT 'cashier_display_residual', COUNT(*) FROM payment_cashier_config
 WHERE tenant_id = 1
   AND del_flag = 0
   AND (cashier_name LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR cashier_name LIKE '%芒果支付芒果支付%'
        OR cashier_name LIKE '%自建芒果支付%'
        OR display_config LIKE CONCAT('%', CHAR(26126), CHAR(26126), '芒果支付%')
        OR display_config LIKE '%芒果支付芒果支付%'
        OR display_config LIKE '%自建芒果支付%');
"""


def legacy_table_residual_sql() -> str:
    return """
SELECT 'legacy_special_channel_table', COUNT(*) FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name = 'payment_special_channel_scenario_control';
"""


def legacy_table_check_sql() -> str:
    return """
SELECT COUNT(*) FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name = 'payment_mango_pay_scenario_control';
"""


def assert_mango_pay_scenario_table_exists(env: dict[str, str]) -> None:
    exists = run_mysql(env, legacy_table_check_sql()).strip()
    if exists != "1":
        raise SystemExit("payment_mango_pay_scenario_control is missing; run Flyway payment migrations first")


def main() -> None:
    parser = argparse.ArgumentParser(description="Normalize local payment MANGO_PAY channel data.")
    parser.add_argument("--apply", action="store_true", help="Apply changes. Without this flag only prints counts.")
    args = parser.parse_args()

    env = parse_env(ENV_FILE)
    print(f"workspace={REPO_ROOT}")
    print(f"database={env.get('MANGO_DB_HOST', '127.0.0.1')}:{env.get('MANGO_DB_PORT', '3306')}/{env.get('MANGO_DB_NAME', '')}")
    print("before:")
    print(run_mysql(env, count_sql()))
    if not args.apply:
        print("dry_run=true")
        return
    assert_mango_pay_scenario_table_exists(env)
    run_mysql(env, normalize_sql())
    print("after:")
    print(run_mysql(env, count_sql()))
    print("residual:")
    print(run_mysql(env, residual_sql()))
    print(run_mysql(env, display_residual_sql()))
    print(run_mysql(env, legacy_table_residual_sql()))


if __name__ == "__main__":
    main()
