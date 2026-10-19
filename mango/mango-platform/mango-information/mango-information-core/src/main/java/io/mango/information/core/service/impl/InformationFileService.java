package io.mango.information.core.service.impl;

import io.mango.common.result.Require;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.information.api.enums.InformationCode;
import io.mango.information.api.vo.InformationFileVO;
import io.mango.information.api.vo.InformationUploadTicketVO;
import io.mango.information.core.entity.InformationFormEntity;
import io.mango.information.core.service.IInformationFileService;
import io.mango.information.core.service.IInformationFormService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

/**
 * 信息采集公开上传服务。
 * <p>
 * 托管表单是匿名入口，文件模块的上传接口要求登录；这里提供表单级公开入口，
 * 校验表单状态与上传限制后，通过 mango-file 的服务能力保存文件，文件仍归文件模块管理。
 */
@Service
@RequiredArgsConstructor
public class InformationFileService implements IInformationFileService {

    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "zip", "rar",
            "png", "jpg", "jpeg", "gif", "webp", "bmp");

    private final IInformationFormService formService;
    private final IFileContentProvider fileContentProvider;

    /**
     * 大小与扩展名限制开关。
     * <p>
     * 首版按产品负责人确认不启用；上传仍必须先获取短效票据，票据始终校验。
     */
    @Value("${mango.information.upload.restrict:false}")
    private boolean restrict;

    /** 上传票据有效期，单位秒。 */
    @Value("${mango.information.upload.ticket-ttl-seconds:600}")
    private long ticketTtlSeconds;

    @Value("${mango.information.upload.max-size-bytes:20971520}")
    private long maxSizeBytes;

    @Value("${mango.information.upload.allowed-extensions:}")
    private String allowedExtensions;

    /** 公开预览地址的签名密钥，仅在启用限制时参与校验。 */
    @Value("${mango.information.upload.sign-secret:}")
    private String signSecret;

    @Override
    public InformationUploadTicketVO issueTicket(String formCode) {
        formService.selectPublished(formCode);
        long expiresAt = System.currentTimeMillis() / 1000 + ticketTtlSeconds;
        InformationUploadTicketVO vo = new InformationUploadTicketVO();
        vo.setTicket(expiresAt + "." + signTicket(formCode, expiresAt));
        vo.setExpiresIn(ticketTtlSeconds);
        return vo;
    }

    @Override
    public InformationFileVO upload(String formCode, String fileName, String contentType, long fileSize, InputStream inputStream, String ticket) {
        Require.isTrue(verifyTicket(formCode, ticket), InformationCode.INVALID_SUBMISSION, "上传票据无效或已过期，请重新打开页面");
        InformationFormEntity form = formService.selectPublished(formCode);
        Require.isTrue(StringUtils.hasText(fileName), InformationCode.INVALID_SUBMISSION, "文件名不能为空");
        Require.isTrue(fileSize > 0, InformationCode.INVALID_SUBMISSION, "文件大小必须大于 0");
        if (restrict) {
            Require.isTrue(fileSize <= maxSizeBytes, InformationCode.INVALID_SUBMISSION,
                    "文件不能超过 " + (maxSizeBytes / 1024 / 1024) + "MB");
            Require.isTrue(allowed(fileName), InformationCode.INVALID_SUBMISSION, "不允许上传该类型的文件");
        }

        SaveFileCommand command = new SaveFileCommand();
        command.setInputStream(inputStream);
        command.setFileName(fileName);
        command.setFileSize(fileSize);
        command.setContentType(contentType);
        command.setPurpose("information-form");
        command.setAccessLevel("PRIVATE");
        command.setBizType("information-form");
        command.setBizId(form.getFormCode());

        FileRecordVO record = withTenantContext(form.getTenantId(), () -> fileContentProvider.save(command));
        Require.notNull(record, InformationCode.INVALID_SUBMISSION, "文件保存失败");
        return toPublicVO(record, form.getFormCode());
    }

    @Override
    public InformationFileVO detail(String formCode, Long id) {
        InformationFormEntity form = formService.selectPublished(formCode);
        FileDownloadVO download = withTenantContext(form.getTenantId(), () -> fileContentProvider.downloadForService(id));
        Require.notNull(download, InformationCode.INVALID_SUBMISSION, "文件不存在");
        InformationFileVO vo = new InformationFileVO();
        vo.setId(id);
        vo.setFileName(download.fileName());
        vo.setFileSize(download.contentLength());
        vo.setContentType(download.contentType());
        String base = previewUrl(form.getFormCode(), id);
        vo.setUrl(base);
        vo.setPreviewUrl(base);
        vo.setDownloadUrl(base);
        return vo;
    }

    /** 公开页面只暴露本表单的上传文件，预览地址带签名，避免跨表单读取内容。 */
    private InformationFileVO toPublicVO(FileRecordVO record, String formCode) {
        InformationFileVO vo = new InformationFileVO();
        vo.setId(record.getId());
        vo.setFileName(record.getFileName());
        vo.setFileSize(record.getFileSize());
        vo.setContentType(record.getContentType());
        String base = "/information/open/forms/" + formCode + "/files/preview?id=" + record.getId()
                + (restrict ? "&sig=" + sign(formCode, record.getId()) : "");
        vo.setUrl(base);
        vo.setPreviewUrl(base);
        vo.setDownloadUrl(base);
        return vo;
    }

    /** 下载地址在 detail 中同样带签名，保证前端拿到即可预览。 */
    private String previewUrl(String formCode, Long id) {
        return "/information/open/forms/" + formCode + "/files/preview?id=" + id
                + (restrict ? "&sig=" + sign(formCode, id) : "");
    }

    /**
     * 公开预览读取文件：租户由表单归属决定；启用限制时要求携带有效签名。
     */
    public FileDownloadVO download(String formCode, Long id, String signature) {
        if (restrict) {
            Require.isTrue(verify(formCode, id, signature), InformationCode.INVALID_SUBMISSION, "文件预览地址无效或已过期");
        }
        InformationFormEntity form = formService.selectPublished(formCode);
        FileDownloadVO download = withTenantContext(form.getTenantId(),
                () -> fileContentProvider.downloadForService(id));
        Require.notNull(download, InformationCode.INVALID_SUBMISSION, "文件不存在");
        return download;
    }

    /** 票据 = 过期时间 + 表单绑定签名，短效且不可跨表单复用。 */
    private String signTicket(String formCode, long expiresAt) {
        return signPayload(formCode + ":ticket:" + expiresAt);
    }

    private boolean verifyTicket(String formCode, String ticket) {
        if (!StringUtils.hasText(ticket)) {
            return false;
        }
        int separator = ticket.indexOf('.');
        if (separator <= 0) {
            return false;
        }
        long expiresAt;
        try {
            expiresAt = Long.parseLong(ticket.substring(0, separator));
        } catch (NumberFormatException e) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis() / 1000) {
            return false;
        }
        String expected = expiresAt + "." + signTicket(formCode, expiresAt);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), ticket.getBytes(StandardCharsets.UTF_8));
    }

    private String signPayload(String payload) {
        String secret = StringUtils.hasText(signSecret) ? signSecret : "mango-information-upload";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign information payload", e);
        }
    }

    private String sign(String formCode, Long id) {
        String secret = StringUtils.hasText(signSecret) ? signSecret : "mango-information-upload";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((formCode + ":" + id).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign information file url", e);
        }
    }

    private boolean verify(String formCode, Long id, String signature) {
        if (!StringUtils.hasText(signature)) {
            return false;
        }
        return MessageDigest.isEqual(sign(formCode, id).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private boolean allowed(String fileName) {
        int dot = fileName.lastIndexOf('.');
        Require.isTrue(dot > 0 && dot < fileName.length() - 1, InformationCode.INVALID_SUBMISSION, "文件缺少扩展名");
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        Set<String> allowed = StringUtils.hasText(allowedExtensions)
                ? Set.of(allowedExtensions.toLowerCase(Locale.ROOT).split(","))
                : DEFAULT_ALLOWED_EXTENSIONS;
        return allowed.contains(extension);
    }

    private <T> T withTenantContext(String tenantId, java.util.function.Supplier<T> supplier) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        if (StringUtils.hasText(tenantId) && !tenantId.equals(previous.tenantId())) {
            MangoContextHolder.set(previous.withTenantId(tenantId));
        }
        try {
            return supplier.get();
        } finally {
            MangoContextHolder.set(previous);
        }
    }
}
