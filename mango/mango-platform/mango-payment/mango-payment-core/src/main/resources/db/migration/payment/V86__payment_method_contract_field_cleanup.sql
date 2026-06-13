ALTER TABLE `payment_method`
  DROP COLUMN `visible_scope`,
  DROP COLUMN `route_strategy`,
  DROP COLUMN `min_amount`,
  DROP COLUMN `max_amount`;
