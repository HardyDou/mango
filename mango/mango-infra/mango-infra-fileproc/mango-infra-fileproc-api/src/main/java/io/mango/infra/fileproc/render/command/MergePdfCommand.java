package io.mango.infra.fileproc.render.command;

import io.mango.infra.fileproc.render.vo.PdfSourceVO;

import java.util.List;

/**
 * PDF 合并命令。
 *
 * @param fileName 输出文件名。
 * @param sources PDF 输入列表。
 * @param rebuildBookmark 是否重建书签。
 * @param addPageNumber 是否添加页码。
 */
public record MergePdfCommand(
        String fileName,
        List<PdfSourceVO> sources,
        boolean rebuildBookmark,
        boolean addPageNumber) {
}
