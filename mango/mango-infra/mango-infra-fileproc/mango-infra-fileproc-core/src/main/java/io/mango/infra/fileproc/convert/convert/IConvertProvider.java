package io.mango.infra.fileproc.convert.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

/**
 * 格式转换实现扩展点。
 * <p>
 * 该接口只供 convert core/starter 组装具体转换实现，不作为跨模块业务 API 暴露。
 */
public interface IConvertProvider {

    /**
     * 判断当前实现是否支持指定格式组合。
     *
     * @param sourceFormat 源格式。
     * @param targetFormat 目标格式。
     * @return 支持返回 true。
     */
    boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat);

    /**
     * 执行转换。
     *
     * @param command 转换命令。
     * @return 转换结果。
     */
    ConvertResultVO convert(ConvertCommand command);
}
