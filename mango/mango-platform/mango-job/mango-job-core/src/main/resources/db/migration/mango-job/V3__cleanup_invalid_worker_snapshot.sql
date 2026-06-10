-- Remove invalid worker snapshots imported from placeholder engine addresses.

DELETE FROM `mango_job_worker_snapshot`
WHERE UPPER(TRIM(`worker_address`)) IN ('N/A', 'UNKNOWN', 'NULL')
   OR `worker_address` = '-';
