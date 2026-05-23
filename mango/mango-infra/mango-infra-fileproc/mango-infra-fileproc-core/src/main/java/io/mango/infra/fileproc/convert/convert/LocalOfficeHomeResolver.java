package io.mango.infra.fileproc.convert.convert;

import org.jodconverter.core.util.OSUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * 本地 Office 安装目录解析器，迁移自 kkFileView 的默认查找策略。
 */
final class LocalOfficeHomeResolver {

    private static final String EXECUTABLE_DEFAULT = "program/soffice.bin";

    private static final String EXECUTABLE_MAC = "program/soffice";

    private static final String EXECUTABLE_MAC_41 = "MacOS/soffice";

    private static final String EXECUTABLE_WINDOWS = "program/soffice.exe";

    private LocalOfficeHomeResolver() {
    }

    static File resolve() {
        if (OSUtils.IS_OS_WINDOWS) {
            String userDir = System.getProperty("user.dir");
            String programFiles64 = System.getenv("ProgramFiles");
            String programFiles32 = System.getenv("ProgramFiles(x86)");
            return findOfficeHome(EXECUTABLE_WINDOWS,
                    userDir + File.separator + "LibreOfficePortable" + File.separator + "App" + File.separator + "libreoffice",
                    programFiles32 + File.separator + "LibreOffice",
                    programFiles64 + File.separator + "LibreOffice 7",
                    programFiles32 + File.separator + "LibreOffice 7",
                    programFiles64 + File.separator + "LibreOffice 6",
                    programFiles32 + File.separator + "LibreOffice 6",
                    programFiles64 + File.separator + "LibreOffice 5",
                    programFiles32 + File.separator + "LibreOffice 5",
                    programFiles64 + File.separator + "LibreOffice 4",
                    programFiles32 + File.separator + "LibreOffice 4",
                    programFiles32 + File.separator + "OpenOffice 4");
        }
        if (OSUtils.IS_OS_MAC) {
            File homeDir = findOfficeHome(EXECUTABLE_MAC_41,
                    "/Applications/LibreOffice.app/Contents",
                    "/Applications/OpenOffice.app/Contents",
                    "/Applications/OpenOffice.org.app/Contents");
            if (homeDir == null) {
                homeDir = findOfficeHome(EXECUTABLE_MAC,
                        "/Applications/LibreOffice.app/Contents",
                        "/Applications/OpenOffice.app/Contents",
                        "/Applications/OpenOffice.org.app/Contents");
            }
            return homeDir;
        }
        return findOfficeHome(EXECUTABLE_DEFAULT,
                "/opt/libreoffice7.6",
                "/opt/libreoffice24.2",
                "/opt/libreoffice24.8",
                "/opt/libreoffice25.2",
                "/usr/lib64/libreoffice",
                "/usr/lib/libreoffice",
                "/usr/local/lib64/libreoffice",
                "/usr/local/lib/libreoffice",
                "/opt/libreoffice",
                "/usr/lib64/openoffice",
                "/usr/lib/openoffice",
                "/opt/openoffice4");
    }

    private static File findOfficeHome(String executablePath, String... homePaths) {
        return Stream.of(homePaths)
                .filter(homePath -> homePath != null && !homePath.startsWith("null"))
                .filter(homePath -> Files.isRegularFile(Paths.get(homePath, executablePath)))
                .findFirst()
                .map(File::new)
                .orElse(null);
    }
}
