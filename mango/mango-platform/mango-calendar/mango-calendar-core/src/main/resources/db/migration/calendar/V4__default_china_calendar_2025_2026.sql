SET @tenant_id = 1;
SET @calendar_code = 'CN_STANDARD';
SET @calendar_name = '中国标准工作日历';
SET @calendar_id = (
  SELECT id
  FROM `calendar`
  WHERE tenant_id = @tenant_id
    AND calendar_code = @calendar_code
  LIMIT 1
);
SET @calendar_id = IFNULL(@calendar_id, (SELECT IFNULL(MAX(id), 0) + 1 FROM `calendar`));

INSERT INTO `calendar` (
  `id`,
  `tenant_id`,
  `calendar_code`,
  `calendar_name`,
  `status`,
  `create_time`,
  `update_time`
) VALUES (
  @calendar_id,
  @tenant_id,
  @calendar_code,
  @calendar_name,
  1,
  NOW(),
  NOW()
) ON DUPLICATE KEY UPDATE
  `calendar_name` = VALUES(`calendar_name`),
  `status` = 1,
  `update_time` = NOW();

INSERT INTO `calendar_day` (
  `id`,
  `tenant_id`,
  `calendar_id`,
  `calendar_year`,
  `calendar_date`,
  `day_of_week`,
  `day_type`,
  `workday`,
  `day_name`,
  `lunar_year`,
  `lunar_month`,
  `lunar_day`,
  `lunar_leap_month`,
  `lunar_text`,
  `ganzhi_year`,
  `zodiac`,
  `solar_term`,
  `source`,
  `remark`,
  `enabled`,
  `create_time`,
  `update_time`
)
SELECT
  202500000000 + CAST(DATE_FORMAT(d.calendar_date, '%Y%m%d') AS UNSIGNED) AS id,
  @tenant_id AS tenant_id,
  @calendar_id AS calendar_id,
  YEAR(d.calendar_date) AS calendar_year,
  d.calendar_date AS calendar_date,
  WEEKDAY(d.calendar_date) + 1 AS day_of_week,
  COALESCE(h.day_type, IF(WEEKDAY(d.calendar_date) IN (5, 6), 'RESTDAY', 'WORKDAY')) AS day_type,
  CASE
    WHEN COALESCE(h.day_type, IF(WEEKDAY(d.calendar_date) IN (5, 6), 'RESTDAY', 'WORKDAY'))
      IN ('WORKDAY', 'ADJUSTED_WORKDAY', 'TEMP_OPEN_DAY', 'CUSTOM_OPEN') THEN 1
    ELSE 0
  END AS workday,
  h.day_name AS day_name,
  NULL AS lunar_year,
  NULL AS lunar_month,
  NULL AS lunar_day,
  0 AS lunar_leap_month,
  NULL AS lunar_text,
  NULL AS ganzhi_year,
  NULL AS zodiac,
  NULL AS solar_term,
  '国务院办公厅' AS source,
  COALESCE(h.remark, '按周末双休规则生成') AS remark,
  1 AS enabled,
  NOW() AS create_time,
  NOW() AS update_time
FROM (
  SELECT DATE_ADD('2025-01-01', INTERVAL n.number DAY) AS calendar_date
  FROM (
    SELECT ones.n + tens.n * 10 + hundreds.n * 100 AS number
    FROM (
      SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) ones
    CROSS JOIN (
      SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) tens
    CROSS JOIN (
      SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) hundreds
  ) n
  WHERE DATE_ADD('2025-01-01', INTERVAL n.number DAY) <= '2026-12-31'
) d
LEFT JOIN (
  SELECT DATE('2025-01-01') AS calendar_date, 'LEGAL_HOLIDAY' AS day_type, '元旦' AS day_name, '国务院办公厅关于2025年部分节假日安排的通知' AS remark
  UNION ALL SELECT DATE('2025-01-28'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-01-29'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-01-30'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-01-31'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-02-01'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-02-02'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-02-03'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-02-04'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-01-26'), 'ADJUSTED_WORKDAY', '春节调休上班', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-02-08'), 'ADJUSTED_WORKDAY', '春节调休上班', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-04-04'), 'LEGAL_HOLIDAY', '清明节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-04-05'), 'LEGAL_HOLIDAY', '清明节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-04-06'), 'LEGAL_HOLIDAY', '清明节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-05-01'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-05-02'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-05-03'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-05-04'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-05-05'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-04-27'), 'ADJUSTED_WORKDAY', '劳动节调休上班', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-05-31'), 'LEGAL_HOLIDAY', '端午节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-06-01'), 'LEGAL_HOLIDAY', '端午节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-06-02'), 'LEGAL_HOLIDAY', '端午节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-10-01'), 'LEGAL_HOLIDAY', '国庆节、中秋节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-10-02'), 'LEGAL_HOLIDAY', '国庆节、中秋节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-10-03'), 'LEGAL_HOLIDAY', '国庆节、中秋节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-10-04'), 'LEGAL_HOLIDAY', '国庆节、中秋节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-10-05'), 'LEGAL_HOLIDAY', '国庆节、中秋节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-10-06'), 'LEGAL_HOLIDAY', '国庆节、中秋节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-10-07'), 'LEGAL_HOLIDAY', '国庆节、中秋节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-10-08'), 'LEGAL_HOLIDAY', '国庆节、中秋节', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-09-28'), 'ADJUSTED_WORKDAY', '国庆节、中秋节调休上班', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2025-10-11'), 'ADJUSTED_WORKDAY', '国庆节、中秋节调休上班', '国务院办公厅关于2025年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-01-01'), 'LEGAL_HOLIDAY', '元旦', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-01-02'), 'LEGAL_HOLIDAY', '元旦', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-01-03'), 'LEGAL_HOLIDAY', '元旦', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-15'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-16'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-17'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-18'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-19'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-20'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-21'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-22'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-23'), 'LEGAL_HOLIDAY', '春节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-14'), 'ADJUSTED_WORKDAY', '春节调休上班', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-02-28'), 'ADJUSTED_WORKDAY', '春节调休上班', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-04-04'), 'LEGAL_HOLIDAY', '清明节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-04-05'), 'LEGAL_HOLIDAY', '清明节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-04-06'), 'LEGAL_HOLIDAY', '清明节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-05-01'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-05-02'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-05-03'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-05-04'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-05-05'), 'LEGAL_HOLIDAY', '劳动节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-05-09'), 'ADJUSTED_WORKDAY', '劳动节调休上班', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-06-19'), 'LEGAL_HOLIDAY', '端午节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-06-20'), 'LEGAL_HOLIDAY', '端午节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-06-21'), 'LEGAL_HOLIDAY', '端午节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-09-25'), 'LEGAL_HOLIDAY', '中秋节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-09-26'), 'LEGAL_HOLIDAY', '中秋节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-09-27'), 'LEGAL_HOLIDAY', '中秋节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-10-01'), 'LEGAL_HOLIDAY', '国庆节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-10-02'), 'LEGAL_HOLIDAY', '国庆节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-10-03'), 'LEGAL_HOLIDAY', '国庆节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-10-04'), 'LEGAL_HOLIDAY', '国庆节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-10-05'), 'LEGAL_HOLIDAY', '国庆节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-10-06'), 'LEGAL_HOLIDAY', '国庆节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-10-07'), 'LEGAL_HOLIDAY', '国庆节', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-09-20'), 'ADJUSTED_WORKDAY', '国庆节调休上班', '国务院办公厅关于2026年部分节假日安排的通知'
  UNION ALL SELECT DATE('2026-10-10'), 'ADJUSTED_WORKDAY', '国庆节调休上班', '国务院办公厅关于2026年部分节假日安排的通知'
) h ON h.calendar_date = d.calendar_date
ON DUPLICATE KEY UPDATE
  `day_of_week` = VALUES(`day_of_week`),
  `day_type` = IF(`calendar_day`.`source` IS NULL OR `calendar_day`.`source` IN ('系统默认', '国务院办公厅'), VALUES(`day_type`), `calendar_day`.`day_type`),
  `workday` = IF(`calendar_day`.`source` IS NULL OR `calendar_day`.`source` IN ('系统默认', '国务院办公厅'), VALUES(`workday`), `calendar_day`.`workday`),
  `day_name` = IF(`calendar_day`.`source` IS NULL OR `calendar_day`.`source` IN ('系统默认', '国务院办公厅'), VALUES(`day_name`), `calendar_day`.`day_name`),
  `source` = IF(`calendar_day`.`source` IS NULL OR `calendar_day`.`source` IN ('系统默认', '国务院办公厅'), VALUES(`source`), `calendar_day`.`source`),
  `remark` = IF(`calendar_day`.`source` IS NULL OR `calendar_day`.`source` IN ('系统默认', '国务院办公厅'), VALUES(`remark`), `calendar_day`.`remark`),
  `enabled` = 1,
  `update_time` = NOW();
