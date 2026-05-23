package io.mango.infra.fileproc.render.vo;

import io.mango.infra.fileproc.render.enums.RenderFormat;

/**
 * 源格式与目标格式组合。
 *
 * @param sourceFormat 源格式。
 * @param targetFormat 目标格式。
 */
public record RenderFormatPairVO(RenderFormat sourceFormat, RenderFormat targetFormat) {
}
