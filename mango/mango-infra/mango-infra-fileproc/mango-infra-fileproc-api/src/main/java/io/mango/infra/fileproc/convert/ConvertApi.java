package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertFormatPairVO;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.util.Set;

/**
 * 格式转换能力契约。
 * <p>
 * 本接口只表达格式转换能力，不承载文件存储、缓存、权限、租户或 fileId 语义。
 */
public interface ConvertApi {

    /**
     * 判断当前运行时是否支持指定格式转换。
     *
     * @param sourceFormat 源格式，不能为空；为空时返回 false。
     * @param targetFormat 目标格式，不能为空；为空时返回 false。
     * @return 支持返回 true，否则返回 false。
     */
    boolean canConvert(ConvertFormat sourceFormat, ConvertFormat targetFormat);

    /**
     * 执行一次本地格式转换。
     *
     * @param command 转换命令，必须包含源格式、目标格式和输入流。
     * @return 转换后的文件内容、格式和内容类型。
     */
    ConvertResultVO convert(ConvertCommand command);

    /**
     * 获取当前运行时注册的跨格式转换能力。
     *
     * @return 支持的源格式与目标格式组合，不包含同格式直通复制能力。
     */
    Set<ConvertFormatPairVO> supportedConversions();
}
