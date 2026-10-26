-- Normalize persisted legacy sync modes introduced before Mango 1.1.0.
-- This changes Registry metadata only; target resource tables and ownership remain unchanged.
UPDATE `resource_registry`
SET `sync_mode` = CASE `sync_mode`
  WHEN 'AUTO' THEN 'SYNC_ALWAYS'
  WHEN 'INIT_ONLY' THEN 'SYNC_ONCE'
  WHEN 'MANUAL' THEN 'SYNC_ONCE'
  WHEN 'LOCKED' THEN 'SYNC_ONCE'
END
WHERE `sync_mode` IN ('AUTO', 'INIT_ONLY', 'MANUAL', 'LOCKED');
