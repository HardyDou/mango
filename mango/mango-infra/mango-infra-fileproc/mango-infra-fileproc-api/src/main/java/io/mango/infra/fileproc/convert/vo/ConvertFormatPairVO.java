package io.mango.infra.fileproc.convert.vo;

import io.mango.infra.fileproc.convert.enums.ConvertFormat;

/**
 * 源格式与目标格式组合。
 *
 * @param sourceFormat 源格式。
 * @param targetFormat 目标格式。
 */
public record ConvertFormatPairVO(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
}
