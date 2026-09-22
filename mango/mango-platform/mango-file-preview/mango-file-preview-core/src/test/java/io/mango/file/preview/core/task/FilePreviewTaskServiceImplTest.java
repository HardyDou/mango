package io.mango.file.preview.core.task;

import io.mango.file.api.FileApi;
import io.mango.file.api.FileSettingsApi;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.preview.api.enums.FilePreviewTaskStatus;
import io.mango.file.preview.api.vo.FilePreviewTaskVO;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.gateway.FilePreviewFileGateway;
import io.mango.infra.fileproc.convert.ConvertApi;
import io.mango.infra.kv.api.ITokenStore;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.api.LockLease;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FilePreviewTaskServiceImplTest {

    @Test
    void repeatedRequestsReuseSameVersionTaskAndConversion() throws Exception {
        FileApi fileApi = mock(FileApi.class);
        IFileContentProvider contentProvider = mock(IFileContentProvider.class);
        ConvertApi convertApi = mock(ConvertApi.class);
        ITokenStore taskStore = mock(ITokenStore.class);
        ILeaseLocker leaseLocker = availableLeaseLocker();
        FileRecordVO record = record(42L, "sample.docx");
        when(fileApi.get(42L)).thenReturn(io.mango.common.result.R.ok(record));
        when(convertApi.canConvert(any(), any())).thenReturn(false);
        FilePreviewTaskServiceImpl service = new FilePreviewTaskServiceImpl(
                new FilePreviewFileGateway(fileApi, contentProvider), contentProvider, convertApi,
                taskStore, leaseLocker, new ObjectMapper().registerModule(new JavaTimeModule()),
                new FilePreviewProperties(), testExecutor());

        FilePreviewTaskVO first = service.submit(42L, false);
        FilePreviewTaskVO second = service.submit(42L, false);

        assertThat(second.getFileId()).isEqualTo(first.getFileId());
        assertThat(second.getCreatedAt()).isEqualTo(first.getCreatedAt());
        await(() -> service.status(42L, false).getStatus() == FilePreviewTaskStatus.FAILED);
        verify(leaseLocker, atLeastOnce()).tryAcquire(any(), any(), anyLong());
        verify(taskStore, atLeastOnce()).store(any(), contains("FAILED"), eq(30L * 24 * 60 * 60));
        verify(convertApi, never()).convert(any());
    }

    @Test
    void supportedDocumentIsConvertedAndSavedAsPrivateArtifact() throws Exception {
        FileApi fileApi = mock(FileApi.class);
        IFileContentProvider contentProvider = mock(IFileContentProvider.class);
        ConvertApi convertApi = mock(ConvertApi.class);
        ITokenStore taskStore = mock(ITokenStore.class);
        ILeaseLocker leaseLocker = availableLeaseLocker();
        FileRecordVO record = record(43L, "sample.docx");
        FileRecordVO artifact = record(99L, "sample.pdf");
        when(fileApi.get(43L)).thenReturn(io.mango.common.result.R.ok(record));
        when(convertApi.canConvert(any(), any())).thenReturn(true);
        when(convertApi.convert(any())).thenReturn(io.mango.infra.fileproc.convert.vo.ConvertResultVO.builder()
                .format(io.mango.infra.fileproc.convert.enums.ConvertFormat.PDF)
                .fileName("sample.pdf").content(new byte[]{1, 2, 3}).build());
        when(contentProvider.downloadForService(43L))
                .thenReturn(new FileDownloadVO(new ByteArrayInputStream(new byte[]{4}), "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 1));
        when(contentProvider.savePreviewArtifact(any())).thenReturn(artifact);
        FilePreviewTaskServiceImpl service = new FilePreviewTaskServiceImpl(
                new FilePreviewFileGateway(fileApi, contentProvider), contentProvider, convertApi,
                taskStore, leaseLocker, new ObjectMapper().registerModule(new JavaTimeModule()),
                new FilePreviewProperties(), testExecutor());

        service.submit(43L, false);
        await(() -> service.status(43L, false).getStatus() == FilePreviewTaskStatus.SUCCEEDED);

        FilePreviewTaskVO result = service.status(43L, false);
        assertThat(result.getPreviewFileId()).isEqualTo(99L);
        assertThat(result.getProgress()).isEqualTo(100);
        verify(contentProvider, times(1)).savePreviewArtifact(any());
    }

    @Test
    void spreadsheetUsesDirectPreviewWithoutPdfConversion() throws Exception {
        FileApi fileApi = mock(FileApi.class);
        IFileContentProvider contentProvider = mock(IFileContentProvider.class);
        ConvertApi convertApi = mock(ConvertApi.class);
        ITokenStore taskStore = mock(ITokenStore.class);
        FileRecordVO record = record(44L, "sheet.xlsx");
        when(fileApi.get(44L)).thenReturn(io.mango.common.result.R.ok(record));
        FilePreviewTaskServiceImpl service = new FilePreviewTaskServiceImpl(
                new FilePreviewFileGateway(fileApi, contentProvider), contentProvider, convertApi,
                taskStore, availableLeaseLocker(), new ObjectMapper().registerModule(new JavaTimeModule()),
                new FilePreviewProperties(), testExecutor());

        service.submit(44L, false);
        await(() -> service.status(44L, false).getStatus() == FilePreviewTaskStatus.SUCCEEDED);

        FilePreviewTaskVO result = service.status(44L, false);
        assertThat(result.getPreviewFileId()).isNull();
        verify(convertApi, never()).convert(any());
        verify(contentProvider, never()).save(any());
    }

    @Test
    void configuredOfficeLimitRequiresExplicitConfirmationBeforeSchedulingConversion() {
        FileApi fileApi = mock(FileApi.class);
        FileSettingsApi settingsApi = mock(FileSettingsApi.class);
        IFileContentProvider contentProvider = mock(IFileContentProvider.class);
        ConvertApi convertApi = mock(ConvertApi.class);
        ITokenStore taskStore = mock(ITokenStore.class);
        FileRecordVO record = record(45L, "large.docx");
        record.setFileSize(10L * 1024 * 1024);
        FileSettingsVO settings = new FileSettingsVO();
        settings.setPreviewMaxSize(5L * 1024 * 1024);
        when(fileApi.get(45L)).thenReturn(io.mango.common.result.R.ok(record));
        when(settingsApi.get()).thenReturn(io.mango.common.result.R.ok(settings));
        FilePreviewTaskServiceImpl service = new FilePreviewTaskServiceImpl(
                new FilePreviewFileGateway(fileApi, contentProvider, settingsApi), contentProvider, convertApi,
                taskStore, availableLeaseLocker(), new ObjectMapper().registerModule(new JavaTimeModule()),
                new FilePreviewProperties(), testExecutor());

        FilePreviewTaskVO result = service.submit(45L, false);

        assertThat(result.getStatus()).isEqualTo(FilePreviewTaskStatus.CONFIRM_REQUIRED);
        assertThat(result.getMessage()).isEqualTo("文件太大，预览需要较长时间，建议下载查看");
        verifyNoInteractions(convertApi);
        verify(contentProvider, never()).downloadForService(any());
    }

    @Test
    void conversionExecutorUsesConfiguredWorkerCountAsConcurrencyLimit() throws Exception {
        FileApi fileApi = mock(FileApi.class);
        IFileContentProvider contentProvider = mock(IFileContentProvider.class);
        ConvertApi convertApi = mock(ConvertApi.class);
        ITokenStore taskStore = mock(ITokenStore.class);
        Map<Long, FileRecordVO> records = Map.of(
                51L, record(51L, "one.docx"),
                52L, record(52L, "two.docx"),
                53L, record(53L, "three.docx"),
                54L, record(54L, "four.docx"));
        when(fileApi.get(anyLong())).thenAnswer(invocation -> io.mango.common.result.R.ok(
                records.get(invocation.getArgument(0, Long.class))));
        when(convertApi.canConvert(any(), any())).thenReturn(true);
        when(contentProvider.downloadForService(anyLong())).thenReturn(new FileDownloadVO(
                new ByteArrayInputStream(new byte[]{4}), "source.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 1));
        when(contentProvider.savePreviewArtifact(any())).thenReturn(record(99L, "preview.pdf"));

        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        when(convertApi.convert(any())).thenAnswer(invocation -> {
            int current = active.incrementAndGet();
            maximum.accumulateAndGet(current, Math::max);
            started.countDown();
            try {
                release.await(3, TimeUnit.SECONDS);
            } finally {
                active.decrementAndGet();
            }
            return io.mango.infra.fileproc.convert.vo.ConvertResultVO.builder()
                    .format(io.mango.infra.fileproc.convert.enums.ConvertFormat.PDF)
                    .fileName("preview.pdf").content(new byte[]{1}).build();
        });

        FilePreviewProperties properties = new FilePreviewProperties();
        properties.getConversion().setWorkerCount(2);
        FilePreviewTaskServiceImpl service = new FilePreviewTaskServiceImpl(
                new FilePreviewFileGateway(fileApi, contentProvider), contentProvider, convertApi,
                taskStore, availableLeaseLocker(), new ObjectMapper().registerModule(new JavaTimeModule()),
                properties, testExecutor(2));

        records.keySet().forEach(fileId -> service.submit(fileId, false));

        assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(maximum.get()).isEqualTo(2);
        Thread.sleep(150);
        assertThat(maximum.get()).isEqualTo(2);
        release.countDown();
        for (Long fileId : records.keySet()) {
            await(() -> service.status(fileId, false).getStatus() == FilePreviewTaskStatus.SUCCEEDED);
        }
    }

    private FileRecordVO record(long id, String name) {
        FileRecordVO record = new FileRecordVO();
        record.setId(id);
        record.setFileName(name);
        record.setFileExt(name.substring(name.lastIndexOf('.') + 1));
        record.setFileHash("hash-" + id);
        record.setFileSize(10L);
        return record;
    }

    private ILeaseLocker availableLeaseLocker() {
        ILeaseLocker leaseLocker = mock(ILeaseLocker.class);
        Instant now = Instant.now();
        when(leaseLocker.tryAcquire(any(), any(), anyLong()))
                .thenReturn(Optional.of(new LockLease("preview", "test", "token", now, now.plusSeconds(900))));
        return leaseLocker;
    }

    private ExecutorService testExecutor() {
        return testExecutor(2);
    }

    private ExecutorService testExecutor(int workerCount) {
        return Executors.newFixedThreadPool(workerCount, runnable -> {
            Thread thread = new Thread(runnable, "file-preview-test-convert");
            thread.setDaemon(true);
            return thread;
        });
    }

    private void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
