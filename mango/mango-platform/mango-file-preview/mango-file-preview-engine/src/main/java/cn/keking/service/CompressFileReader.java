package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.utils.RarUtils;
import cn.keking.web.filter.BaseUrlFilter;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 压缩包解包器。
 *
 * <p>ZIP/JAR 使用 JDK 实现，7Z 使用 commons-compress 的纯 Java 实现，
 * 不依赖 SevenZipJBinding 的本机库，macOS arm64 等没有对应 native 的平台也能解包。
 * RAR 仍使用 SevenZipJBinding，因为 junrar 不支持 RAR5。</p>
 *
 * @author yudian-it
 * create 2017/11/27
 */
@Component
public class CompressFileReader {
    private static final String ZIP_SUFFIX = "zip";
    private static final String JAR_SUFFIX = "jar";
    private static final String SEVEN_Z_SUFFIX = "7z";
    private static final String SEVEN_ZIP_SUFFIX = "7-zip";
    private static final String PACKAGE_PATH = "_";
    private static final String PASSWORD_MESSAGE = "password";

    private final FileHandlerService fileHandlerService;

    public CompressFileReader(FileHandlerService fileHandlerService) {
        this.fileHandlerService = fileHandlerService;
    }

    public String unRar(String filePath, String filePassword, String fileName, FileAttribute fileAttribute) throws Exception {
        List<String> imgUrls = new ArrayList<>();
        String baseUrl = BaseUrlFilter.getBaseUrl();
        String fileDir = ConfigConstants.getFileDir();
        String folderName = filePath.replace(fileDir, ""); //修复压缩包 多重目录获取路径错误
        if (fileAttribute.isCompressFile()) {
            folderName = "_decompression" + folderName;
        }
        Path folderPath = Paths.get(fileDir, folderName + PACKAGE_PATH);
        Files.createDirectories(folderPath);

        String suffix = fileAttribute.getSuffix();
        if (ZIP_SUFFIX.equalsIgnoreCase(suffix) || JAR_SUFFIX.equalsIgnoreCase(suffix)) {
            extractZip(filePath, fileName, folderPath, baseUrl, imgUrls);
        } else if (SEVEN_Z_SUFFIX.equalsIgnoreCase(suffix) || SEVEN_ZIP_SUFFIX.equalsIgnoreCase(suffix)) {
            extractSevenZ(filePath, filePassword, fileName, folderPath, baseUrl, imgUrls);
        } else {
            extractWithSevenZip(filePath, filePassword, fileName, folderPath, baseUrl, imgUrls);
        }
        fileHandlerService.putImgCache(fileName + PACKAGE_PATH, imgUrls);
        return folderName + PACKAGE_PATH;
    }

    private void extractZip(String filePath, String fileName, Path folderPath,
                            String baseUrl, List<String> imgUrls) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(Paths.get(filePath)), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path filePathInsideArchive = getFilePathInsideArchive(entry.getName(), folderPath);
                if (entry.isDirectory()) {
                    Files.createDirectories(filePathInsideArchive);
                    continue;
                }
                Files.createDirectories(filePathInsideArchive.getParent());
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(filePathInsideArchive.toFile(), false))) {
                    zipInputStream.transferTo(out);
                }
                cachePictureUrl(filePathInsideArchive, fileName, folderPath, baseUrl, imgUrls);
                zipInputStream.closeEntry();
            }
        } catch (Exception e) {
            throw new Exception("Error processing ZIP file: " + e.getMessage(), e);
        }
    }

    private void extractSevenZ(String filePath, String filePassword, String fileName, Path folderPath,
                               String baseUrl, List<String> imgUrls) throws Exception {
        char[] password = StringUtils.hasText(filePassword) ? filePassword.toCharArray() : null;
        File file = new File(filePath);
        try (SevenZFile sevenZFile = password == null ? new SevenZFile(file) : new SevenZFile(file, password)) {
            SevenZArchiveEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = sevenZFile.getNextEntry()) != null) {
                Path filePathInsideArchive = getFilePathInsideArchive(entry.getName(), folderPath);
                if (entry.isDirectory()) {
                    Files.createDirectories(filePathInsideArchive);
                    continue;
                }
                Files.createDirectories(filePathInsideArchive.getParent());
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(filePathInsideArchive.toFile(), false))) {
                    int length;
                    while ((length = sevenZFile.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
                cachePictureUrl(filePathInsideArchive, fileName, folderPath, baseUrl, imgUrls);
            }
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.toLowerCase(Locale.ROOT).contains(PASSWORD_MESSAGE)) {
                throw new Exception("Password", e);
            }
            throw new Exception("Error processing 7Z file: " + message, e);
        }
    }

    private void extractWithSevenZip(String filePath, String filePassword, String fileName, Path folderPath,
                                     String baseUrl, List<String> imgUrls) throws Exception {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "r");
             IInArchive inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile))) {

            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
            for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                if (!item.isFolder()) {
                    final Path filePathInsideArchive = getFilePathInsideArchive(item, folderPath);
                    Files.deleteIfExists(filePathInsideArchive);
                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(filePathInsideArchive.toFile(), false))) {
                        ExtractOperationResult result = item.extractSlow(data -> {
                            try {
                                out.write(data);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return data.length;
                        }, filePassword);
                        if (result != ExtractOperationResult.OK) {
                            if (ExtractOperationResult.WRONG_PASSWORD.equals(result)) {
                                throw new Exception("Password");
                            }
                            throw new Exception("Failed to extract archive file.");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    cachePictureUrl(filePathInsideArchive, fileName, folderPath, baseUrl, imgUrls);
                }
            }
        } catch (Exception e) {
            throw new Exception("Error processing archive file: " + e.getMessage(), e);
        }
    }

    private void cachePictureUrl(Path filePathInsideArchive, String fileName, Path folderPath,
                                 String baseUrl, List<String> imgUrls) {
        if (!FileType.PICTURE.equals(FileType.typeFromUrl(filePathInsideArchive.toString()))) {
            return;
        }
        imgUrls.add(baseUrl + URLEncoder.encode(fileName + PACKAGE_PATH + "/"
                + folderPath.relativize(filePathInsideArchive).toString().replace("\\", "/"), StandardCharsets.UTF_8)
                .replaceAll("%2F", "/"));
    }

    private Path getFilePathInsideArchive(ISimpleInArchiveItem item, Path folderPath) throws SevenZipException, UnsupportedEncodingException {
        String insideFileName = RarUtils.getUtf8String(item.getPath());
        if (RarUtils.isMessyCode(insideFileName)) {
            insideFileName = new String(item.getPath().getBytes(StandardCharsets.ISO_8859_1), "gbk");
        }
        Path normalizedPath = getFilePathInsideArchive(insideFileName, folderPath);
        try {
            Files.createDirectories(normalizedPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + normalizedPath.getParent(), e);
        }
        return normalizedPath;
    }

    private Path getFilePathInsideArchive(String insideFileName, Path folderPath) {
        Path normalizedPath = folderPath.resolve(insideFileName).normalize();
        if (!normalizedPath.startsWith(folderPath)) {
            throw new SecurityException("Unsafe path detected: " + insideFileName);
        }
        return normalizedPath;
    }

}
