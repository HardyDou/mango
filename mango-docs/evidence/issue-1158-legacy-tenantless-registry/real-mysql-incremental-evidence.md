# 真实 MySQL 增量升级验证日志摘要（mango dev start backend, DB=mango_dev_mango_issue_1158_017）

## 启动结果
```
--- 2026-11-07T08:50:18.533Z mango-backend: applying cold bootstrap generation 1 ---
--- 2026-11-07T08:54:09.596Z mango-backend: applying cold bootstrap generation 1 ---
--- 2026-11-07T08:57:59.847Z mango-backend: applying cold bootstrap generation 1 ---
```

## 历史无租户 Registry 行处理（warn-only）
```
2026-11-07 17:00:12.836 [main] WARN  i.m.r.c.s.i.ResourceRegistryService - [traceId=] Mango resource missing disable skipped: tenant scope is unavailable, resourceType=AUTH_SUBJECT_ROLE, resourceId=2026062919100001401
2026-11-07 17:00:20.004 [main] WARN  i.m.r.c.s.i.ResourceRegistryService - [traceId=] Mango resource missing disable skipped: tenant scope is unavailable, resourceType=IDENTITY_USER, resourceId=2026062919100001301
```

## 异常检查
```
IllegalStateException 次数: 0
资源缺少租户范围 次数: 0
```

## 行状态（验证后）
```
resource_type	resource_id	tenant_id	status	sync_mode	ownership	last_sync_time
IDENTITY_USER	2026062919100001301	NULL	ACTIVE	SYNC_ALWAYS	RESOURCE_MANAGED	NULL
AUTH_SUBJECT_ROLE	2026062919100001401	NULL	ACTIVE	SYNC_ALWAYS	RESOURCE_MANAGED	NULL
```
