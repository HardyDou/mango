package io.mango.infra.fileproc.render.vo;

import io.mango.common.result.Require;

import java.io.InputStream;

/**
 * PDF 输入源。
 *
 * @param name 输入名称。
 * @param inputStream PDF 输入流。
 */
public record PdfSourceVO(String name, InputStream inputStream) {

    public PdfSourceVO {
        Require.notNull(inputStream, "PDF 输入源不能为空");
    }
}
