DELETE FROM `payment_operation_audit`
WHERE `id` IN (450001, 450002)
   OR (`resource_id` IN ('MANGO_PAY', 'DF202605250002')
       AND `operation_action` IN ('CREATE_CHANNEL', 'PROCESS_DIFFERENCE'));

DELETE FROM `payment_settlement_summary`
WHERE `id` IN (440001, 440002)
   OR (`channel_code` IN ('MANGO_PAY', 'ALLINPAY')
       AND `settlement_date` = CURDATE()
       AND `trade_amount` IN (29800, 5900));

DELETE FROM `payment_difference`
WHERE `id` IN (430001, 430002)
   OR `difference_no` IN ('DF202605250001', 'DF202605250002')
   OR `related_order_no` IN ('PO202605250003', 'NT202605250002');

DELETE FROM `payment_reconciliation`
WHERE `id` IN (420001, 420002)
   OR `reconciliation_no` IN ('RC202605250001', 'RC202605250002');

DELETE FROM `payment_notification_record`
WHERE `id` IN (410001, 410002)
   OR `notification_no` IN ('NT202605250001', 'NT202605250002')
   OR `related_order_no` IN ('PO202605250001', 'PO202605250003');

DELETE FROM `payment_exception_order`
WHERE `id` IN (400001, 400002)
   OR `exception_no` IN ('EX202605250001', 'EX202605250002')
   OR `related_order_no` IN ('PO202605250003', 'BO202605250002');

DELETE FROM `payment_transaction_flow`
WHERE `id` IN (390001, 390002, 390003)
   OR `flow_no` IN ('FLOW202605250001', 'FLOW202605250002', 'FLOW202605250003');

DELETE FROM `payment_refund_order`
WHERE `id` IN (380001, 380002)
   OR `refund_order_no` IN ('RO202605250001', 'RO202605250002')
   OR `biz_refund_no` IN ('BR202605250001', 'BR202605250002');

DELETE FROM `payment_order`
WHERE `id` IN (370001, 370002, 370003)
   OR `pay_order_no` IN ('PO202605250001', 'PO202605250002', 'PO202605250003')
   OR `channel_trade_no` IN ('MANGO_PAY-T202605250001', 'MANGO_PAY-T202605250002', 'ALLINPAY-T202605250003');

DELETE FROM `payment_business_order`
WHERE `id` IN (360001, 360002, 360003)
   OR `biz_order_no` IN ('BO202605250001', 'BO202605250002', 'BO202605250003');
