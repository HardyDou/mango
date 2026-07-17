package cn.keking.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlEncoderUtilsTest {

    @Test
    void hasUrlEncoded_混合编码与原始字符_识别为已编码() {
        assertTrue(UrlEncoderUtils.hasUrlEncoded("%E4%B8%AD%E6%96%87%20(1).docx"));
    }

    @Test
    void hasUrlEncoded_小写十六进制编码_识别为已编码() {
        assertTrue(UrlEncoderUtils.hasUrlEncoded("%e4%b8%ad (1).docx"));
    }

    @Test
    void hasUrlEncoded_完整编码文件名_识别为已编码() {
        assertTrue(UrlEncoderUtils.hasUrlEncoded("%E4%B8%AD%E6%96%87%20%281%29.docx"));
    }

    @Test
    void hasUrlEncoded_普通安全文件名_保持既有判断() {
        assertTrue(UrlEncoderUtils.hasUrlEncoded("report-1.docx"));
    }

    @Test
    void hasUrlEncoded_只有原始待编码字符_识别为未编码() {
        assertFalse(UrlEncoderUtils.hasUrlEncoded("中文 (1).docx"));
    }

    @Test
    void hasUrlEncoded_包含非法百分号序列_识别为未编码() {
        assertFalse(UrlEncoderUtils.hasUrlEncoded("%E4%B8%AD%ZZ.docx"));
        assertFalse(UrlEncoderUtils.hasUrlEncoded("report%.docx"));
    }
}
