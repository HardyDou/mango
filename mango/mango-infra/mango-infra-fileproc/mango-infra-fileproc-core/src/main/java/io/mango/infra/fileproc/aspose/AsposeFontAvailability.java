package io.mango.infra.fileproc.aspose;

import java.util.Set;

/**
 * Aspose 可用字体查询接口。
 *
 * <p>把「系统里现在能看到哪些字体」抽成接口，是为了让启动自检可测：单元测试提供桩实现，
 * 不需要在构建机安装真实字体文件。</p>
 */
@FunctionalInterface
public interface AsposeFontAvailability {

    /**
     * 返回当前 Aspose 可见的字体名。
     *
     * <p>只包含字体族族的拉丁名（例如 {@code SimSun}）。中文族名（例如 {@code 宋体}）
     * 不在其中：Aspose 依赖内置的中文名映射表把它解析到 {@code SimSun}。</p>
     *
     * @return 字体名集合；无法枚举时返回空集合。
     */
    Set<String> availableFontNames();
}
