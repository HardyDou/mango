package io.mango.file.preview.core.task;

import io.mango.common.result.Require;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.preview.api.enums.FilePreviewTaskStatus;
import io.mango.file.preview.api.enums.FilePreviewCode;
import io.mango.file.preview.api.vo.FilePreviewTaskVO;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.gateway.FilePreviewFileGateway;
import io.mango.infra.fileproc.convert.ConvertApi;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import io.mango.infra.kv.api.ITokenStore;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.api.LockLease;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 预览任务编排器。任务按文件内容版本去重，转换过程完全异步，避免请求线程被 Office 进程占用。
 * <p>任务状态写入共享 TokenStore，转换执行通过共享 lease 协调，因此多个实例可以共同消费同一任务。</p>
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed collaborators are injected once")
public class FilePreviewTaskServiceImpl implements IFilePreviewTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewTaskServiceImpl.class);
    private static final long LEASE_TTL_SECONDS = 15L * 60;
    private static final String LEASE_PREFIX = "file-preview:convert:";
    private static final String WORKER_SLOT_PREFIX = "file-preview:worker-slot:";
    private static final String OWNER = java.util.UUID.randomUUID().toString();
    private static final long TASK_RETENTION_SECONDS = 30L * 24 * 60 * 60;
    private static final String OFFICE_PREVIEW_CONFIRM_MESSAGE = "文件太大，预览需要较长时间，建议下载查看";
    private static final String TASK_PREFIX = "file-preview:task:";
    private static final int PROGRESS_PENDING = 5;
    private static final int PROGRESS_CONVERTING = 15;
    private static final int PROGRESS_SAVING = 85;
    private static final int PROGRESS_COMPLETE = 100;
    private static final long WORKER_POLL_INTERVAL_MILLIS = 100L;
    private static final Set<String> OFFICE_EXTENSIONS = Set.of("doc", "docx", "ppt", "pptx");
    private static final Set<String> DIRECT_PREVIEW_EXTENSIONS = Set.of(
            "pdf", "ofd", "png", "jpg", "jpeg", "tif", "tiff", "txt", "html", "htm",
            "xls", "xlsx",
            "dwg", "dxf", "dwf", "dwt", "stl", "step", "stp", "iges", "igs");

    private final FilePreviewFileGateway fileGateway;
    private final IFileContentProvider contentProvider;
    private final ConvertApi convertApi;
    private final ITokenStore taskStore;
    private final ILeaseLocker leaseLocker;
    private final ObjectMapper objectMapper;
    private final FilePreviewProperties properties;
    private final ExecutorService conversionExecutor;
    private final Map<String, FilePreviewTaskVO> tasks = new ConcurrentHashMap<>();
    private final Map<Long, String> latestKeys = new ConcurrentHashMap<>();
    private final java.util.Set<String> activeKeys = ConcurrentHashMap.newKeySet();

    public FilePreviewTaskServiceImpl(
            FilePreviewFileGateway fileGateway,
            IFileContentProvider contentProvider,
            ConvertApi convertApi,
            ITokenStore taskStore,
            ILeaseLocker leaseLocker,
            ObjectMapper objectMapper,
            FilePreviewProperties properties,
            @Qualifier("filePreviewConversionExecutor") ExecutorService conversionExecutor) {
        this.fileGateway = fileGateway;
        this.contentProvider = contentProvider;
        this.convertApi = convertApi;
        this.taskStore = taskStore;
        this.leaseLocker = leaseLocker;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.conversionExecutor = conversionExecutor;
    }

    @Override
    public FilePreviewTaskVO submit(Long fileId, boolean allowLargeFile) {
        Require.notNull(fileId, FilePreviewCode.FILE_ID_EMPTY);
        FileRecordVO record = fileGateway.find(fileId);
        Require.notNull(record, FilePreviewCode.FILE_NOT_FOUND);
        String key = versionKey(record);
        latestKeys.put(fileId, key);
        FilePreviewTaskVO persisted = readPersisted(key);
        if (persisted != null) {
            tasks.putIfAbsent(key, persisted);
        }
        AtomicBoolean created = new AtomicBoolean(false);
        FilePreviewTaskVO task = tasks.compute(key, (ignored, existing) -> {
            if (existing != null && (existing.getStatus() == FilePreviewTaskStatus.QUEUED
                    || existing.getStatus() == FilePreviewTaskStatus.PROCESSING
                    || existing.getStatus() == FilePreviewTaskStatus.SUCCEEDED
                    || (existing.getStatus() == FilePreviewTaskStatus.CONFIRM_REQUIRED && !allowLargeFile))) {
                return existing;
            }
            created.set(true);
            return newTask(fileId);
        });
        if (exceedsOfficePreviewLimit(record) && !allowLargeFile) {
            task.setStatus(FilePreviewTaskStatus.CONFIRM_REQUIRED);
            task.setProgress(0);
            task.setMessage(OFFICE_PREVIEW_CONFIRM_MESSAGE);
            touch(task);
            return copy(task);
        }
        if (task.getStatus() == FilePreviewTaskStatus.CONFIRM_REQUIRED && allowLargeFile) {
            task.setStatus(FilePreviewTaskStatus.QUEUED);
            task.setProgress(0);
            task.setMessage("已确认，正在进入预览队列");
            touch(task);
        }
        if (created.get() || task.getStatus() == FilePreviewTaskStatus.QUEUED) {
            schedule(key, record);
        }
        return copy(task);
    }

    @Override
    public FilePreviewTaskVO status(Long fileId, boolean allowLargeFile) {
        Require.notNull(fileId, FilePreviewCode.FILE_ID_EMPTY);
        FileRecordVO record = fileGateway.find(fileId);
        Require.notNull(record, FilePreviewCode.FILE_NOT_FOUND);
        String key = versionKey(record);
        latestKeys.put(fileId, key);
        FilePreviewTaskVO persisted = readPersisted(key);
        if (persisted != null) {
            tasks.putIfAbsent(key, persisted);
        }
        FilePreviewTaskVO task = tasks.get(key);
        if (task == null) {
            return submit(fileId, allowLargeFile);
        }
        if (task.getStatus() == FilePreviewTaskStatus.CONFIRM_REQUIRED && allowLargeFile) {
            return submit(fileId, true);
        }
        if (task.getStatus() == FilePreviewTaskStatus.QUEUED) {
            schedule(key, record);
        }
        return copy(task);
    }

    private void schedule(String key, FileRecordVO record) {
        if (!activeKeys.add(key)) {
            return;
        }
        try {
            conversionExecutor.execute(() -> {
                try {
                    process(key, record);
                } finally {
                    activeKeys.remove(key);
                }
            });
        } catch (RuntimeException ex) {
            activeKeys.remove(key);
            FilePreviewTaskVO task = tasks.get(key);
            if (task != null) {
                update(task, FilePreviewTaskStatus.FAILED, PROGRESS_COMPLETE, "预览任务提交失败，请稍后重试");
            }
        }
    }

    private void process(String key, FileRecordVO record) {
        FilePreviewTaskVO task = tasks.get(key);
        if (task == null) {
            return;
        }
        LockLease lease = leaseLocker.tryAcquire(LEASE_PREFIX + key, OWNER, LEASE_TTL_SECONDS).orElse(null);
        if (lease == null) {
            return;
        }
        LockLease workerSlot = acquireWorkerSlot();
        if (workerSlot == null) {
            leaseLocker.release(lease);
            return;
        }
        try {
            update(task, FilePreviewTaskStatus.PROCESSING, PROGRESS_PENDING, "正在读取原文件");
            String extension = extension(record);
            if (DIRECT_PREVIEW_EXTENSIONS.contains(extension) && !OFFICE_EXTENSIONS.contains(extension)) {
                update(task, FilePreviewTaskStatus.SUCCEEDED, PROGRESS_COMPLETE, "预览已就绪");
                touch(task);
                return;
            }
            var parsedFormat = ConvertFormat.parse(extension);
            if (parsedFormat.isEmpty()) {
                update(task, FilePreviewTaskStatus.FAILED, PROGRESS_COMPLETE, "暂不支持该文件格式的预览");
                return;
            }
            ConvertFormat source = parsedFormat.get();
            if (!convertApi.canConvert(source, ConvertFormat.PDF)) {
                update(task, FilePreviewTaskStatus.FAILED, PROGRESS_COMPLETE, "暂不支持该文件格式转换为 PDF");
                return;
            }
            FileDownloadVO sourceFile = fileGateway.download(record.getId());
            update(task, FilePreviewTaskStatus.PROCESSING, PROGRESS_CONVERTING, "正在转换为 PDF");
            ConvertResultVO result;
            try (InputStream input = sourceFile.inputStream()) {
                result = convertApi.convert(ConvertCommand.builder()
                        .sourceFormat(source)
                        .targetFormat(ConvertFormat.PDF)
                        .inputStream(input)
                        .fileName(sourceFile.fileName())
                        .build());
            }
            update(task, FilePreviewTaskStatus.PROCESSING, PROGRESS_SAVING, "正在保存预览产物");
            SaveFileCommand save = new SaveFileCommand();
            save.setInputStream(resultInput(result));
            save.setFileName(previewName(record));
            save.setFileSize(resultSize(result));
            save.setContentType(ConvertFormat.PDF.contentType());
            save.setPurpose("file-preview-artifact");
            save.setAccessLevel("PRIVATE");
            save.setBizType("file-preview");
            save.setBizId(String.valueOf(record.getId()));
            save.setBizMeta("{\"sourceFileId\":" + record.getId() + ",\"sourceVersion\":\""
                    + versionKey(record) + "\"}");
            FileRecordVO artifact = contentProvider.savePreviewArtifact(save);
            task.setPreviewFileId(artifact.getId());
            update(task, FilePreviewTaskStatus.SUCCEEDED, PROGRESS_COMPLETE, "预览已就绪");
        } catch (Exception ex) {
            LOGGER.warn("文件预览转换失败，fileId={}, fileSize={}, message={}",
                    record.getId(), record.getFileSize(), ex.getMessage(), ex);
            update(task, FilePreviewTaskStatus.FAILED, PROGRESS_COMPLETE,
                    ex.getMessage() == null ? "预览生成失败，请下载原文件查看" : ex.getMessage());
        } finally {
            leaseLocker.release(workerSlot);
            leaseLocker.release(lease);
        }
    }

    private LockLease acquireWorkerSlot() {
        int workerCount = properties.getConversion().effectiveWorkerCount();
        while (!Thread.currentThread().isInterrupted()) {
            for (int slot = 0; slot < workerCount; slot++) {
                LockLease lease = leaseLocker.tryAcquire(
                        WORKER_SLOT_PREFIX + slot, OWNER, LEASE_TTL_SECONDS).orElse(null);
                if (lease != null) {
                    return lease;
                }
            }
            try {
                Thread.sleep(WORKER_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private InputStream resultInput(ConvertResultVO result) throws IOException {
        if (result.hasOutputPath()) {
            return Files.newInputStream(result.outputPath());
        }
        return new ByteArrayInputStream(result.content());
    }

    private long resultSize(ConvertResultVO result) throws IOException {
        return result.hasOutputPath() ? Files.size(result.outputPath()) : result.content().length;
    }

    private FilePreviewTaskVO newTask(Long fileId) {
        Instant now = Instant.now();
        FilePreviewTaskVO task = new FilePreviewTaskVO();
        task.setFileId(fileId);
        task.setStatus(FilePreviewTaskStatus.QUEUED);
        task.setProgress(0);
        task.setMessage("已进入预览队列");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    private void update(FilePreviewTaskVO task, FilePreviewTaskStatus status, int progress, String message) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setMessage(message);
        touch(task);
    }

    private void touch(FilePreviewTaskVO task) {
        task.setUpdatedAt(Instant.now());
        try {
            taskStore.store(TASK_PREFIX + latestKeys.get(task.getFileId()),
                    objectMapper.writeValueAsString(task), TASK_RETENTION_SECONDS);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("Unable to serialize preview task state for file {}", task.getFileId(), ex);
        } catch (RuntimeException ex) {
            LOGGER.warn("Unable to persist preview task state for file {}", task.getFileId(), ex);
        }
    }

    private FilePreviewTaskVO readPersisted(String key) {
        try {
            String value = taskStore.get(TASK_PREFIX + key);
            return value == null ? null : objectMapper.readValue(value, FilePreviewTaskVO.class);
        } catch (JsonProcessingException | RuntimeException ex) {
            LOGGER.warn("Unable to read persisted preview task state for key {}", key, ex);
            return null;
        }
    }

    private FilePreviewTaskVO copy(FilePreviewTaskVO source) {
        FilePreviewTaskVO copy = new FilePreviewTaskVO();
        copy.setFileId(source.getFileId());
        copy.setStatus(source.getStatus());
        copy.setProgress(source.getProgress());
        copy.setQueueAhead(queueAhead(source));
        copy.setWorkerCount(properties.getConversion().effectiveWorkerCount());
        copy.setMessage(source.getMessage());
        copy.setPreviewFileId(source.getPreviewFileId());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private int queueAhead(FilePreviewTaskVO task) {
        if (task.getStatus() != FilePreviewTaskStatus.QUEUED) {
            return 0;
        }
        Instant createdAt = task.getCreatedAt();
        if (createdAt == null) {
            return 0;
        }
        return (int) tasks.values().stream()
                .filter(item -> item.getStatus() == FilePreviewTaskStatus.QUEUED)
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isBefore(createdAt))
                .count();
    }

    private String versionKey(FileRecordVO record) {
        return String.join("|", String.valueOf(record.getId()), String.valueOf(record.getFileHash()),
                String.valueOf(record.getFileSize()), String.valueOf(record.getUpdatedTime()));
    }

    private String extension(FileRecordVO record) {
        String extension = record.getFileExt();
        if (extension == null || extension.isBlank()) {
            String name = record.getFileName();
            int dot = name == null ? -1 : name.lastIndexOf('.');
            extension = dot < 0 ? "" : name.substring(dot + 1);
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    private boolean exceedsOfficePreviewLimit(FileRecordVO record) {
        return OFFICE_EXTENSIONS.contains(extension(record))
                && record.getFileSize() != null
                && record.getFileSize() > fileGateway.previewMaxSize();
    }

    private String previewName(FileRecordVO record) {
        String name = record.getFileName() == null ? String.valueOf(record.getId()) : record.getFileName();
        int dot = name.lastIndexOf('.');
        return (dot > 0 ? name.substring(0, dot) : name) + ".pdf";
    }
}
