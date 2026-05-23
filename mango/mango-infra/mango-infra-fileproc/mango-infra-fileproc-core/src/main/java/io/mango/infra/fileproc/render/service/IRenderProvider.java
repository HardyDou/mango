package io.mango.infra.fileproc.render.service;

import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.RenderResultVO;

import java.util.List;

/**
 * 文档渲染实现扩展点。
 * <p>
 * 该接口只供 render core/starter 组装具体渲染实现，不作为跨模块业务 API 暴露。
 */
public interface IRenderProvider {

    /**
     * 判断当前实现是否支持指定格式组合。
     *
     * @param sourceFormat 源格式。
     * @param targetFormat 目标格式。
     * @return 支持返回 true。
     */
    boolean supports(RenderFormat sourceFormat, RenderFormat targetFormat);

    /**
     * 执行渲染。
     *
     * @param command 渲染命令。
     * @return 渲染结果。
     */
    RenderResultVO render(RenderCommand command);

    /**
     * 提取渲染输入中的变量名。
     *
     * @param command 渲染命令。
     * @return 变量名列表。
     */
    default List<String> extractVariables(RenderCommand command) {
        return List.of();
    }
}
