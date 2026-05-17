ALTER TABLE `file_record`
    DROP INDEX `uk_file_object`,
    ADD KEY `idx_file_object` (`bucket_name`,`object_name`);
