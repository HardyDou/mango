UPDATE `file_settings`
SET `preview_external_extensions` = 'txt,csv,json,xml,png,jpg,jpeg,gif,webp,svg,pdf,ofd,doc,docx,xls,xlsx,ppt,pptx,odt,ods,odp,zip,rar,7z'
WHERE `preview_external_extensions` IS NULL
   OR TRIM(`preview_external_extensions`) = ''
   OR `preview_external_extensions` = 'doc,docx,xls,xlsx,ppt,pptx,ofd';
