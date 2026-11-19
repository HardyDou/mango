package io.mango.infra.fileproc.aspose;

import com.aspose.words.FontSettings;
import com.aspose.words.PhysicalFontInfo;
import com.aspose.words.FontSourceBase;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 基于 Aspose.Words 全局字体设置的可用字体查询实现。
 */
public class WordsAsposeFontAvailability implements AsposeFontAvailability {

    @Override
    public Set<String> availableFontNames() {
        Set<String> names = new LinkedHashSet<>();
        // FontSettings 是进程级静态状态：这里只读，不修改任何来源。
        FontSourceBase[] sources = FontSettings.getDefaultInstance().getFontsSources();
        if (sources == null) {
            return names;
        }
        for (FontSourceBase source : sources) {
            java.util.ArrayList<PhysicalFontInfo> fonts = source.getAvailableFonts();
            if (fonts == null) {
                continue;
            }
            for (PhysicalFontInfo font : fonts) {
                String name = font == null ? null : font.getFontFamilyName();
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
        }
        return names;
    }
}
