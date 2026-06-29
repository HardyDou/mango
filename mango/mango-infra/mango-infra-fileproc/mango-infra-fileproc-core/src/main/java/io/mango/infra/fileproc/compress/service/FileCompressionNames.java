package io.mango.infra.fileproc.compress.service;

import org.springframework.util.StringUtils;

import java.util.Locale;

final class FileCompressionNames {

    private FileCompressionNames() {
    }

    static String extension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        String name = fileName.trim();
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    static String pdfFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "compressed.pdf";
        }
        String name = fileName.trim();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name + ".pdf" : name.substring(0, dot) + ".pdf";
    }
}
