# Mango Job 本地调度稳定性证据

- jobCode: mango_job_stability_chromium_1780829147528
- jobName: 稳定性 每分钟任务 chromium
- jobId: 2063573128854683650
- schedule: 0 */1 * * * ?
- observeStartedAt: 2026-06-07T10:45:47.528Z
- observeEndedAt: 2026-06-07T10:56:02.856Z
- observeMinutes: 10
- expectedMinimumCompletedInstances: 8
- actualCompletedInstances: 10
- totalInstances: 10
- duplicateScheduledFireTimes: 0
- nonSuccessInstances: 0
- sampleInstanceId: 2063575698226593793
- sampleWorkerAddress: in-memory://MacBookPro.local/38485@MacBookPro.local
- sampleLogContainsSystemOut: true
- sampleLogContainsLogger: true

## 观察快照

| time | completed | total |
|---|---:|---:|
| 2026-06-07T10:45:48.209Z | 0 | 0 |
| 2026-06-07T10:46:03.330Z | 1 | 1 |
| 2026-06-07T10:46:18.433Z | 1 | 1 |
| 2026-06-07T10:46:33.530Z | 1 | 1 |
| 2026-06-07T10:46:48.632Z | 1 | 1 |
| 2026-06-07T10:47:03.751Z | 2 | 2 |
| 2026-06-07T10:47:18.864Z | 2 | 2 |
| 2026-06-07T10:47:33.967Z | 2 | 2 |
| 2026-06-07T10:47:49.073Z | 2 | 2 |
| 2026-06-07T10:48:04.188Z | 3 | 3 |
| 2026-06-07T10:48:19.291Z | 3 | 3 |
| 2026-06-07T10:48:34.397Z | 3 | 3 |
| 2026-06-07T10:48:49.502Z | 3 | 3 |
| 2026-06-07T10:49:04.609Z | 4 | 4 |
| 2026-06-07T10:49:19.722Z | 4 | 4 |
| 2026-06-07T10:49:34.828Z | 4 | 4 |
| 2026-06-07T10:49:49.941Z | 4 | 4 |
| 2026-06-07T10:50:05.056Z | 5 | 5 |
| 2026-06-07T10:50:20.157Z | 5 | 5 |
| 2026-06-07T10:50:35.266Z | 5 | 5 |
| 2026-06-07T10:50:50.379Z | 5 | 5 |
| 2026-06-07T10:51:05.483Z | 6 | 6 |
| 2026-06-07T10:51:20.593Z | 6 | 6 |
| 2026-06-07T10:52:46.076Z | 6 | 6 |
| 2026-06-07T10:53:01.202Z | 7 | 7 |
| 2026-06-07T10:53:16.335Z | 8 | 8 |
| 2026-06-07T10:53:31.455Z | 8 | 8 |
| 2026-06-07T10:53:46.563Z | 8 | 8 |
| 2026-06-07T10:54:01.676Z | 8 | 8 |
| 2026-06-07T10:54:16.797Z | 9 | 9 |
| 2026-06-07T10:54:31.913Z | 9 | 9 |
| 2026-06-07T10:54:47.040Z | 9 | 9 |
| 2026-06-07T10:55:02.151Z | 9 | 9 |
| 2026-06-07T10:55:17.274Z | 10 | 10 |
| 2026-06-07T10:55:32.397Z | 10 | 10 |
| 2026-06-07T10:55:47.514Z | 10 | 10 |

## 完成实例窗口

| instanceId | scheduledFireTime | triggerTime | status | workerAddress |
|---|---|---|---|---|
| 2063573435642855426 | 2026-06-07 18:47:00 | 2026-06-07 18:47:01 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063573689960284162 | 2026-06-07 18:48:00 | 2026-06-07 18:48:01 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063573944495816706 | 2026-06-07 18:49:00 | 2026-06-07 18:49:02 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063574199966679041 | 2026-06-07 18:50:00 | 2026-06-07 18:50:03 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063574454716121090 | 2026-06-07 18:51:00 | 2026-06-07 18:51:04 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063574890172956673 | 2026-06-07 18:52:00 | 2026-06-07 18:52:48 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063574955432132610 | 2026-06-07 18:53:00 | 2026-06-07 18:53:03 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063575209623732226 | 2026-06-07 18:54:00 | 2026-06-07 18:54:04 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063575464847130625 | 2026-06-07 18:55:00 | 2026-06-07 18:55:04 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063575698226593793 | 2026-06-07 18:56:00 | 2026-06-07 18:56:00 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
