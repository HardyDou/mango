package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.utils.RarUtils;
import cn.keking.web.filter.BaseUrlFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private static final int BUFFER_SIZE = 8192;

    private final FileHandlerService fileHandlerService;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed collaborator is injected once and retained for the reader lifetime")
    public CompressFileReader(FileHandlerService fileHandlerService) {
        this.fileHandlerService = fileHandlerService;
    }

    public String unRar(String filePath, String filePassword, String fileName, FileAttribute fileAttribute) throws Exception {
        List<String> imgUrls = new ArrayList<>();
        String baseUrl = BaseUrlFilter.getBaseUrl();
        String compressFileKey = resolveCompressFileKey(filePath, fileAttribute);
        Path folderPath = resolveFolderPath(compressFileKey);
        Files.createDirectories(folderPath);

        String suffix = fileAttribute.getSuffix();
        if (ZIP_SUFFIX.equalsIgnoreCase(suffix) || JAR_SUFFIX.equalsIgnoreCase(suffix)) {
            extractZip(filePath, fileName, compressFileKey, folderPath, baseUrl, imgUrls);
        } else if (SEVEN_Z_SUFFIX.equalsIgnoreCase(suffix) || SEVEN_ZIP_SUFFIX.equalsIgnoreCase(suffix)) {
            extractSevenZ(filePath, filePassword, fileName, compressFileKey, folderPath, baseUrl, imgUrls);
        } else {
            extractWithSevenZip(filePath, filePassword, fileName, compressFileKey, folderPath, baseUrl, imgUrls);
        }
        fileHandlerService.putImgCache(fileName + PACKAGE_PATH, imgUrls);
        return compressFileKey;
    }

    /**
     * 计算压缩包解包目录相对解包根的唯一键，同时作为条目端点 {@code GET /compressed-file} 的
     * {@code kkCompressfileKey} 入参。
     *
     * <p>键由压缩包相对 file-dir 的路径加 {@code "_"} 后缀组成；嵌套压缩包额外加 {@code "_decompression"}
     * 前缀，保证一个压缩包对应一个解包目录，不会跨包串目录。
     */
    private String resolveCompressFileKey(String filePath, FileAttribute fileAttribute) {
        String relativePath = filePath.replace(ConfigConstants.getFileDir(), "").replace("\\", "/");
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        if (fileAttribute.isCompressFile()) {
            relativePath = "_decompression" + relativePath;
        }
        return relativePath + PACKAGE_PATH;
    }

    /**
     * 解包目录落在 {@link ConfigConstants#getDecompressDir()} 之下，该根目录位于 file-dir 之外，
     * 不参与 {@code /preview-file/**} 与 {@code /**} 静态映射。
     */
    private Path resolveFolderPath(String compressFileKey) {
        Path decompressRoot = Paths.get(ConfigConstants.getDecompressDir()).toAbsolutePath().normalize();
        Path folderPath = decompressRoot.resolve(compressFileKey).normalize();
        if (!folderPath.startsWith(decompressRoot)) {
            throw new SecurityException("Unsafe decompress path detected: " + compressFileKey);
        }
        return folderPath;
    }

    private void extractZip(String filePath, String fileName, String compressFileKey, Path folderPath,
                            String baseUrl, List<String> imgUrls) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(Paths.get(filePath)), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path filePathInsideArchive = getFilePathInsideArchive(entry.getName(), folderPath);
                if (entry.isDirectory()) {
                    Files.createDirectories(filePathInsideArchive);
                    continue;
                }
                createParentDirectories(filePathInsideArchive);
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(filePathInsideArchive.toFile(), false))) {
                    zipInputStream.transferTo(out);
                }
                cachePictureUrl(filePathInsideArchive, compressFileKey, folderPath, baseUrl, imgUrls);
                zipInputStream.closeEntry();
            }
        } catch (Exception e) {
            throw new Exception("Error processing ZIP file: " + e.getMessage(), e);
        }
    }

    private void extractSevenZ(String filePath, String filePassword, String fileName, String compressFileKey,
                               Path folderPath, String baseUrl, List<String> imgUrls) throws Exception {
        char[] password = StringUtils.hasText(filePassword) ? filePassword.toCharArray() : null;
        File file = new File(filePath);
        try (SevenZFile sevenZFile = password == null ? new SevenZFile(file) : new SevenZFile(file, password)) {
            SevenZArchiveEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((entry = sevenZFile.getNextEntry()) != null) {
                Path filePathInsideArchive = getFilePathInsideArchive(entry.getName(), folderPath);
                if (entry.isDirectory()) {
                    Files.createDirectories(filePathInsideArchive);
                    continue;
                }
                createParentDirectories(filePathInsideArchive);
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(filePathInsideArchive.toFile(), false))) {
                    int length;
                    while ((length = sevenZFile.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
                cachePictureUrl(filePathInsideArchive, compressFileKey, folderPath, baseUrl, imgUrls);
            }
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.toLowerCase(Locale.ROOT).contains(PASSWORD_MESSAGE)) {
                throw new Exception("Password", e);
            }
            throw new Exception("Error processing 7Z file: " + message, e);
        }
    }

    private void extractWithSevenZip(String filePath, String filePassword, String fileName, String compressFileKey,
                                     Path folderPath, String baseUrl, List<String> imgUrls) throws Exception {
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
                    cachePictureUrl(filePathInsideArchive, compressFileKey, folderPath, baseUrl, imgUrls);
                }
            }
        } catch (Exception e) {
            throw new Exception("Error processing archive file: " + e.getMessage(), e);
        }
    }

    /**
     * 压缩包内图片条目改为指向受控端点 {@code GET /compressed-file}，不再拼 file-dir 下的静态路径：
     * 静态映射根下已没有解包内容，条目只能由端点按 key + 包内相对路径读取。
     */
    private void cachePictureUrl(Path filePathInsideArchive, String compressFileKey, Path folderPath,
                                 String baseUrl, List<String> imgUrls) {
        if (!FileType.PICTURE.equals(FileType.typeFromUrl(filePathInsideArchive.toString()))) {
            return;
        }
        String entryPath = compressFileKey + "/"
                + folderPath.relativize(filePathInsideArchive).toString().replace("\\", "/");
        imgUrls.add(baseUrl + "compressed-file?kkCompressfileKey="
                + URLEncoder.encode(compressFileKey, StandardCharsets.UTF_8)
                + "&kkCompressfilepath=" + URLEncoder.encode(entryPath, StandardCharsets.UTF_8)
                + "&fullfilename=" + URLEncoder.encode(filePathInsideArchive.getFileName().toString(), StandardCharsets.UTF_8));
    }

    private Path getFilePathInsideArchive(ISimpleInArchiveItem item, Path folderPath) throws SevenZipException, UnsupportedEncodingException {
        String insideFileName = RarUtils.getUtf8String(item.getPath());
        if (RarUtils.isMessyCode(insideFileName)) {
            insideFileName = new String(item.getPath().getBytes(StandardCharsets.ISO_8859_1), "gbk");
        }
        Path normalizedPath = getFilePathInsideArchive(insideFileName, folderPath);
        try {
            createParentDirectories(normalizedPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + normalizedPath.getParent(), e);
        }
        return normalizedPath;
    }

    private void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private Path getFilePathInsideArchive(String insideFileName, Path folderPath) {
        Path normalizedPath = folderPath.resolve(insideFileName).normalize();
        if (!normalizedPath.startsWith(folderPath)) {
            throw new SecurityException("Unsafe path detected: " + insideFileName);
        }
        return normalizedPath;
    }

}
