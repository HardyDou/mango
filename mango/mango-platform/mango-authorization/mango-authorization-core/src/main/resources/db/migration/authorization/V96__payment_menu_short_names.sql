UPDATE `authorization_menu`
SET `menu_name` = CASE `id`
  WHEN 2802 THEN '企业主体'
  WHEN 2803 THEN '支付通道'
  WHEN 2804 THEN '支付方式'
  WHEN 2805 THEN '收银台'
  WHEN 2816 THEN '签约通道'
  ELSE `menu_name`
END,
`update_time` = NOW(),
`updated_at` = NOW()
WHERE `id` IN (2802, 2803, 2804, 2805, 2816);
