package io.mango.infra.fileproc.aspose;

import com.aspose.words.FolderFontSource;
import com.aspose.words.FontSettings;
import com.aspose.words.FontSourceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 把额外字体目录追加到 Aspose.Words 的字体来源。
 *
 * <p>只追加不替换：{@code FontSettings#setFontsFolders} 会用给定目录替换全部来源，
 * 连 {@code SystemFontSource} 一起干掉，导致系统字体不可见、中文回退到 Noto 后显示为方块。
 * 因此在现有 {@code FontSourceBase} 数组上追加 {@link FolderFontSource} 再整体写回。</p>
 *
 * <p>没有配置目录时完全不触碰全局 {@code FontSettings}：它是进程级静态状态，
 * 会影响宿主应用里其他使用 Aspose 的地方。</p>
 */
public final class AsposeFontRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsposeFontRegistrar.class);

    private AsposeFontRegistrar() {
    }

    /**
     * 追加字体目录。
     *
     * @param folders 字体目录；为空或不存在的目录会被跳过。
     * @return 实际追加的目录数量。
     */
    public static int appendFontFolders(List<String> folders) {
        if (folders == null || folders.isEmpty()) {
            return 0;
        }
        List<FolderFontSource> appended = new ArrayList<>();
        for (String folder : folders) {
            if (folder == null || folder.isBlank()) {
                continue;
            }
            File directory = new File(folder.trim());
            if (!directory.isDirectory()) {
                LOGGER.warn("Aspose 字体目录不存在，已跳过: {}", directory.getAbsolutePath());
                continue;
            }
            appended.add(new FolderFontSource(directory.getAbsolutePath(), true));
        }
        if (appended.isEmpty()) {
            return 0;
        }
        FontSettings settings = FontSettings.getDefaultInstance();
        FontSourceBase[] existing = settings.getFontsSources();
        List<FontSourceBase> merged = new ArrayList<>();
        if (existing != null) {
            merged.addAll(List.of(existing));
        }
        merged.addAll(appended);
        settings.setFontsSources(merged.toArray(new FontSourceBase[0]));
        LOGGER.info("Aspose 字体来源追加 {} 个目录，现有来源 {} 个", appended.size(), merged.size());
        return appended.size();
    }
}
