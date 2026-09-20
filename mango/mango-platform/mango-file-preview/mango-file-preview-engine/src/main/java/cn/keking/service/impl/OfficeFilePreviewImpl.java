package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.service.OfficeToPdfService;
import cn.keking.service.PdfToJpgService;
import cn.keking.service.ConversionCoordinator;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.FileConvertStatusManager;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.OfficeUtils;
import cn.keking.utils.WebUtils;
import cn.keking.web.filter.BaseUrlFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jodconverter.core.office.InstalledOfficeManagerHolder;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import java.time.Duration;

/**
 * Created by kl on 2018/1/17.
 * Content :处理office文件
 */
@Service
public class OfficeFilePreviewImpl implements FilePreview {

    public static final String OFFICE_PREVIEW_TYPE_IMAGE = "image";
    public static final String OFFICE_PREVIEW_TYPE_ALL_IMAGES = "allImages";
    private static final Logger LOGGER = LoggerFactory.getLogger(OfficeFilePreviewImpl.class);
    private static final int PROGRESS_START = 20;
    private static final int PROGRESS_CONVERTING = 60;
    private static final int PROGRESS_HTML = 95;
    private static final int PROGRESS_IMAGES = 90;
    private static final int PROGRESS_COMPLETE = 100;

    // 用于处理回调的线程池
    private static final ExecutorService CALLBACK_EXECUTOR = Executors.newFixedThreadPool(3);

    private static final int DEFAULT_CONVERSION_TIMEOUT_MINUTES = 5;

    private final FileHandlerService fileHandlerService;
    private final OfficeToPdfService officeToPdfService;
    private final OtherFilePreviewImpl otherFilePreview;
    private final PdfToJpgService pdftojpgservice;
    private final ConversionCoordinator conversionCoordinator;

    @Value("${office.plugin.task.timeout:5m}")
    private Duration conversionTimeout = Duration.ofMinutes(DEFAULT_CONVERSION_TIMEOUT_MINUTES);

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OfficeFilePreviewImpl(FileHandlerService fileHandlerService, OfficeToPdfService officeToPdfService, OtherFilePreviewImpl otherFilePreview, PdfToJpgService pdftojpgservice, ConversionCoordinator conversionCoordinator) {
        this.fileHandlerService = fileHandlerService;
        this.officeToPdfService = officeToPdfService;
        this.otherFilePreview = otherFilePreview;
        this.pdftojpgservice = pdftojpgservice;
        this.conversionCoordinator = conversionCoordinator;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        if (!isOfficeManagerAvailable()) {
            return otherFilePreview.notSupportedFile(model, fileAttribute, "Mango 文件预览暂未启用 Office 转换能力，请下载文件查看，或联系管理员启用文档转换服务");
        }
        String officePreviewType = fileAttribute.getOfficePreviewType();
        String cacheName = fileAttribute.getCacheName();  //转换后的文件名
        String convertStatusResult = checkAndHandleConvertStatus(model, fileAttribute.getName(), cacheName, fileAttribute);
        if (convertStatusResult != null) {
            return convertStatusResult;
        }
        String webPreview = handleWebPreview(url, model, fileAttribute);
        if (webPreview != null) {
            return webPreview;
        }
        String imagePreview = handleImagePreview(model, fileAttribute, officePreviewType);
        if (imagePreview != null) {
            return imagePreview;
        }
        return handleRegularOfficePreview(model, fileAttribute, fileAttribute.getName(),
                fileAttribute.forceUpdatedCache(), cacheName, fileAttribute.getOutFilePath(),
                fileAttribute.isHtmlView(), fileAttribute.getUsePasswordCache(), fileAttribute.getFilePassword());
    }

    private String handleWebPreview(String url, Model model, FileAttribute fileAttribute) {
        if (fileAttribute.getOfficePreviewType().equalsIgnoreCase("html")
                || !ConfigConstants.getOfficeTypeWeb().equalsIgnoreCase("web")) {
            return null;
        }
        String suffix = fileAttribute.getSuffix();
        if (suffix.equalsIgnoreCase("xlsx")) {
            model.addAttribute("pdfUrl", KkFileUtils.htmlEscape(url));
            return XLSX_FILE_PREVIEW_PAGE;
        }
        if (suffix.equalsIgnoreCase("csv")) {
            model.addAttribute("csvUrl", KkFileUtils.htmlEscape(url));
            return CSV_FILE_PREVIEW_PAGE;
        }
        return null;
    }

    private String handleImagePreview(Model model, FileAttribute fileAttribute, String officePreviewType) {
        if (fileAttribute.isHtmlView() || BaseUrlFilter.getBaseUrl() == null
                || !isImagePreviewType(officePreviewType)) {
            return null;
        }
        String cacheName = fileAttribute.getCacheName();
        String outFilePath = fileAttribute.getOutFilePath();
        if (hasEncryptedPreview(fileAttribute, outFilePath)) {
            return getPreviewType(model, fileAttribute, officePreviewType, cacheName, outFilePath);
        }
        if (!shouldStartImageConversion(fileAttribute, cacheName)) {
            return getPreviewType(model, fileAttribute, officePreviewType, cacheName, outFilePath);
        }
        return startImageConversion(model, fileAttribute, officePreviewType);
    }

    private boolean isImagePreviewType(String officePreviewType) {
        return OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType)
                || OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType);
    }

    private boolean hasEncryptedPreview(FileAttribute fileAttribute, String outFilePath) {
        return StringUtils.hasLength(fileAttribute.getFilePassword())
                && pdftojpgservice.hasEncryptedPdfCacheSimple(outFilePath);
    }

    private boolean shouldStartImageConversion(FileAttribute fileAttribute, String cacheName) {
        return fileAttribute.forceUpdatedCache()
                || !fileHandlerService.listConvertedFiles().containsKey(cacheName)
                || !ConfigConstants.isCacheEnabled();
    }

    private String startImageConversion(Model model, FileAttribute fileAttribute, String officePreviewType) {
        String fileName = fileAttribute.getName();
        ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
        if (response.isFailure()) {
            return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
        }
        String filePath = response.getContent();
        if (requiresPassword(filePath, fileAttribute)) {
            model.addAttribute("needFilePassword", true);
            model.addAttribute("fileName", fileName);
            model.addAttribute("cacheName", fileAttribute.getCacheName());
            return EXEL_FILE_PREVIEW_PAGE;
        }
        try {
            startAsyncOfficeConversion(filePath, fileAttribute.getOutFilePath(), fileAttribute.getCacheName(),
                    fileAttribute, officePreviewType);
            model.addAttribute("fileName", fileName);
            model.addAttribute("time", ConfigConstants.getTime());
            model.addAttribute("message", "文件正在转换中，请稍候...");
            return WAITING_FILE_PREVIEW_PAGE;
        } catch (Exception exception) {
            LOGGER.error("Failed to start Office conversion: {}", filePath, exception);
            return otherFilePreview.notSupportedFile(model, fileAttribute, "文件转换异常，请联系管理员");
        }
    }

    private boolean requiresPassword(String filePath, FileAttribute fileAttribute) {
        return OfficeUtils.isPwdProtected(filePath) && !StringUtils.hasLength(fileAttribute.getFilePassword());
    }

    private boolean isOfficeManagerAvailable() {
        OfficeManager officeManager = InstalledOfficeManagerHolder.getInstance();
        return officeManager != null && officeManager.isRunning();
    }

    /**
     * 启动异步Office转换
     */
    private void startAsyncOfficeConversion(String filePath, String outFilePath, String cacheName,
                                            FileAttribute fileAttribute,
                                            String officePreviewType) {
        CompletableFuture<List<String>> conversionFuture = conversionCoordinator.submit(
                cacheName + "|" + officePreviewType,
                conversionTimeout,
                () -> executeOfficeConversion(filePath, outFilePath, cacheName, fileAttribute, officePreviewType));
        conversionFuture.whenComplete((imageUrls, error) -> {
            if (error != null) {
                FileConvertStatusManager.ConvertStatus status = FileConvertStatusManager.getConvertStatus(cacheName);
                if (status == null || status.getStatus() == FileConvertStatusManager.Status.CONVERTING) {
                    LOGGER.error("Office转换任务失联或超时: {}", cacheName, error);
                    FileConvertStatusManager.markTimeout(cacheName);
                }
                KkFileUtils.deleteFileByPath(outFilePath);
            }
        });
        conversionFuture.thenAcceptAsync(imageUrls -> {
            try {
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    if (!fileAttribute.isCompressFile() && ConfigConstants.getDeleteSourceFile()) {
                        KkFileUtils.deleteFileByPath(filePath);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Office转换后续处理失败: {}", filePath, e);
            }
        }, CALLBACK_EXECUTOR);
    }

    private List<String> executeOfficeConversion(String filePath, String outFilePath, String cacheName,
                                                 FileAttribute fileAttribute, String officePreviewType) {
        try {
            if (hasCachedConversion(cacheName)) {
                FileConvertStatusManager.convertSuccess(cacheName);
                LOGGER.info("复用已完成的Office转换结果: {}", cacheName);
                return List.of();
            }
            FileConvertStatusManager.startConvert(cacheName);
            FileConvertStatusManager.updateProgress(cacheName, "正在启动Office转换", PROGRESS_START);
            FileConvertStatusManager.updateProgress(cacheName, "正在转换Office到jpg", PROGRESS_CONVERTING);
            convertToPdfAtomically(filePath, outFilePath, fileAttribute);
            processHtmlOutput(outFilePath, cacheName, fileAttribute);
            List<String> imageUrls = convertPdfToImages(outFilePath, cacheName, fileAttribute, officePreviewType);
            cacheConversion(filePath, outFilePath, cacheName, fileAttribute);
            FileConvertStatusManager.updateProgress(cacheName, "转换完成", PROGRESS_COMPLETE);
            FileConvertStatusManager.convertSuccess(cacheName);
            return imageUrls;
        } catch (OfficeException exception) {
            handleOfficeException(filePath, cacheName, fileAttribute, exception);
            return null;
        } catch (Exception exception) {
            handleConversionException(cacheName, exception);
            return null;
        }
    }

    private boolean hasCachedConversion(String cacheName) {
        return ConfigConstants.isCacheEnabled() && fileHandlerService.listConvertedFiles().containsKey(cacheName);
    }

    private void convertToPdfAtomically(String filePath, String outFilePath, FileAttribute fileAttribute)
            throws OfficeException, IOException {
        String temporaryOutputPath = temporaryOutputPath(outFilePath);
        try {
            officeToPdfService.openOfficeToPDF(filePath, temporaryOutputPath, fileAttribute);
            moveOutputAtomically(temporaryOutputPath, outFilePath);
        } finally {
            KkFileUtils.deleteFileByPath(temporaryOutputPath);
        }
    }

    private void processHtmlOutput(String outFilePath, String cacheName, FileAttribute fileAttribute) {
        if (fileAttribute.isHtmlView()) {
            FileConvertStatusManager.updateProgress(cacheName, "处理HTML编码", PROGRESS_HTML);
            fileHandlerService.doActionConvertedFile(outFilePath);
        }
    }

    private List<String> convertPdfToImages(String outFilePath, String cacheName, FileAttribute fileAttribute,
                                            String officePreviewType) throws Exception {
        if (!OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType)
                && !OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType)) {
            return null;
        }
        FileConvertStatusManager.updateProgress(cacheName, "正在转换PDF为图片", PROGRESS_IMAGES);
        return pdftojpgservice.pdf2jpg(outFilePath, outFilePath, fileAttribute);
    }

    private void cacheConversion(String filePath, String outFilePath, String cacheName, FileAttribute fileAttribute) {
        boolean isPwdProtectedOffice = OfficeUtils.isPwdProtected(filePath);
        String filePassword = fileAttribute.getFilePassword();
        if (ConfigConstants.isCacheEnabled()
                && (ObjectUtils.isEmpty(filePassword) || fileAttribute.getUsePasswordCache() || !isPwdProtectedOffice)) {
            fileHandlerService.addConvertedFile(cacheName, fileHandlerService.getRelativePath(outFilePath));
        }
    }

    private void handleOfficeException(String filePath, String cacheName, FileAttribute fileAttribute,
                                       OfficeException exception) {
        boolean isPwdProtectedOffice = OfficeUtils.isPwdProtected(filePath);
        String filePassword = fileAttribute.getFilePassword();
        if (isPwdProtectedOffice && !OfficeUtils.isCompatible(filePath, filePassword)) {
            FileConvertStatusManager.markError(cacheName, "文件密码错误，请重新输入");
        } else {
            LOGGER.error("Office转换执行失败: {}", cacheName, exception);
            FileConvertStatusManager.markError(cacheName, "Office转换失败: " + exception.getMessage());
        }
    }

    private void handleConversionException(String cacheName, Exception exception) {
        LOGGER.error("Office转换执行失败: {}", cacheName, exception);
        FileConvertStatusManager.ConvertStatus status = FileConvertStatusManager.getConvertStatus(cacheName);
        if (status == null || status.getStatus() != FileConvertStatusManager.Status.TIMEOUT) {
            FileConvertStatusManager.markError(cacheName, "转换失败: " + exception.getMessage());
        }
    }

    private static String temporaryOutputPath(String outputPath) {
        int extensionIndex = outputPath.lastIndexOf('.');
        if (extensionIndex < 0) {
            return outputPath + ".part-" + UUID.randomUUID();
        }
        return outputPath.substring(0, extensionIndex) + ".part-" + UUID.randomUUID()
                + outputPath.substring(extensionIndex);
    }

    private static void moveOutputAtomically(String temporaryOutputPath, String outputPath) throws IOException {
        Path source = Path.of(temporaryOutputPath);
        Path target = Path.of(outputPath);
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 获取预览类型（图片预览）
     */
    String getPreviewType(Model model, FileAttribute fileAttribute, String officePreviewType,
                          String cacheName, String outFilePath) {
        String suffix = fileAttribute.getSuffix();
        boolean isPPT = suffix.equalsIgnoreCase("ppt") || suffix.equalsIgnoreCase("pptx");
        List<String> imageUrls;

        try {
            if (pdftojpgservice.hasEncryptedPdfCacheSimple(outFilePath)) {
                imageUrls = pdftojpgservice.getEncryptedPdfCache(outFilePath);
            } else {
                imageUrls = fileHandlerService.loadPdf2jpgCache(outFilePath);
            }

            if (imageUrls == null || imageUrls.isEmpty()) {
                return otherFilePreview.notSupportedFile(model, fileAttribute, "Office转换缓存异常，请联系管理员");
            }

            model.addAttribute("imgUrls", imageUrls);
            model.addAttribute("currentUrl", imageUrls.getFirst());

            if (OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType)) {
                // PPT 图片模式使用专用预览页面
                return (isPPT ? PPT_FILE_PREVIEW_PAGE : OFFICE_PICTURE_FILE_PREVIEW_PAGE);
            } else {
                return PICTURE_FILE_PREVIEW_PAGE;
            }
        } catch (Exception e) {
            LOGGER.error("渲染Office预览页面失败: {}", cacheName, e);
            return otherFilePreview.notSupportedFile(model, fileAttribute, "渲染预览页面异常，请联系管理员");
        }
    }

    /**
     * 处理普通Office预览（转PDF）
     */
    private String handleRegularOfficePreview(Model model, FileAttribute fileAttribute,
                                              String fileName, boolean forceUpdatedCache, String cacheName,
                                              String outFilePath, boolean isHtmlView, boolean userToken,
                                              String filePassword) {
        if (!forceUpdatedCache && fileHandlerService.listConvertedFiles().containsKey(cacheName)
                && ConfigConstants.isCacheEnabled()) {
            return renderRegularPreview(model, cacheName, isHtmlView);
        }
        ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
        if (response.isFailure()) {
            return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
        }
        String filePath = response.getContent();
        boolean isPwdProtectedOffice = OfficeUtils.isPwdProtected(filePath);
        if (isPwdProtectedOffice && !StringUtils.hasLength(filePassword)) {
            model.addAttribute("needFilePassword", true);
            return EXEL_FILE_PREVIEW_PAGE;
        }
        if (StringUtils.hasText(outFilePath)) {
            String errorPage = convertRegularOfficeFile(model, fileAttribute, filePath, outFilePath, cacheName,
                    isHtmlView, userToken, filePassword, isPwdProtectedOffice);
            if (errorPage != null) {
                return errorPage;
            }
        }
        return renderRegularPreview(model, cacheName, isHtmlView);
    }

    private String convertRegularOfficeFile(Model model, FileAttribute fileAttribute, String filePath,
                                            String outFilePath, String cacheName, boolean isHtmlView,
                                            boolean userToken, String filePassword, boolean isPwdProtectedOffice) {
        try {
            convertToPdfAtomically(filePath, outFilePath, fileAttribute);
        } catch (OfficeException exception) {
            if (isPwdProtectedOffice && !OfficeUtils.isCompatible(filePath, filePassword)) {
                model.addAttribute("needFilePassword", true);
                model.addAttribute("filePasswordError", true);
                return EXEL_FILE_PREVIEW_PAGE;
            }
            return otherFilePreview.notSupportedFile(model, fileAttribute, "抱歉，该文件版本不兼容，文件版本错误。");
        } catch (IOException exception) {
            LOGGER.error("Office转换结果写入失败: {}", cacheName, exception);
            return otherFilePreview.notSupportedFile(model, fileAttribute, "文件转换结果写入异常，请联系管理员");
        }
        if (isHtmlView) {
            fileHandlerService.doActionConvertedFile(outFilePath);
        }
        if (!fileAttribute.isCompressFile() && ConfigConstants.getDeleteSourceFile()) {
            KkFileUtils.deleteFileByPath(filePath);
        }
        if (userToken || !isPwdProtectedOffice) {
            fileHandlerService.addConvertedFile(cacheName, fileHandlerService.getRelativePath(outFilePath));
        }
        return null;
    }

    private String renderRegularPreview(Model model, String cacheName, boolean isHtmlView) {
        model.addAttribute("pdfUrl", WebUtils.encodeFileName(cacheName));
        return isHtmlView ? EXEL_FILE_PREVIEW_PAGE : PDF_FILE_PREVIEW_PAGE;
    }

    /**
     * 异步方法
     */
    public String checkAndHandleConvertStatus(Model model, String fileName, String cacheName, FileAttribute fileAttribute) {
        FileConvertStatusManager.ConvertStatus status = FileConvertStatusManager.getConvertStatus(cacheName);
        int refreshSchedule = ConfigConstants.getTime();
        boolean forceUpdatedCache = fileAttribute.forceUpdatedCache();

        if (status != null) {
            if (status.getStatus() == FileConvertStatusManager.Status.CONVERTING) {
                // 正在转换中，返回等待页面
                model.addAttribute("fileName", fileName);
                model.addAttribute("time", refreshSchedule);
                model.addAttribute("message", status.getRealTimeMessage());
                return WAITING_FILE_PREVIEW_PAGE;
            } else if (status.getStatus() == FileConvertStatusManager.Status.TIMEOUT) {
                // 超时状态，检查是否有强制更新命令
                if (forceUpdatedCache) {
                    // 强制更新命令，清除状态，允许重新转换
                    FileConvertStatusManager.convertSuccess(cacheName);
                    LOGGER.info("强制更新命令跳过超时状态，允许重新转换: {}", cacheName);
                    return null; // 返回null表示继续执行
                } else {
                    // 没有强制更新，不允许重新转换
                    return otherFilePreview.notSupportedFile(model, fileAttribute, "文件转换已超时，无法继续转换");
                }
            } else if (status.getStatus() == FileConvertStatusManager.Status.FAILED) {
                // 失败状态，检查是否有强制更新命令
                if (forceUpdatedCache) {
                    // 强制更新命令，清除状态，允许重新转换
                    FileConvertStatusManager.convertSuccess(cacheName);
                    LOGGER.info("强制更新命令跳过失败状态，允许重新转换: {}", cacheName);
                    return null; // 返回null表示继续执行
                } else {
                    // 没有强制更新，不允许重新转换
                    return otherFilePreview.notSupportedFile(model, fileAttribute, "文件转换失败，无法继续转换");
                }
            }
        }
        return null;
    }
}
