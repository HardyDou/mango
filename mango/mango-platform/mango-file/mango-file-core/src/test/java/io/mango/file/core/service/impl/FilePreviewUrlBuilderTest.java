package io.mango.file.core.service.impl;

import io.mango.file.core.entity.FileRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewUrlBuilderTest {

    @Test
    void build_相对地址无占位符_自动追加基础参数() {
        FileRecord record = record();

        String url = FilePreviewUrlBuilder.build("/file-preview/files/preview", record,
                "/file/files/download?id=100", 600L);

        assertThat(url).isEqualTo("/file-preview/files/preview?fileId=100"
                + "&fileName=%E5%90%88%E5%90%8C%201.docx&expireSeconds=600");
    }

    @Test
    void build_绝对地址无占位符_自动追加基础参数() {
        FileRecord record = record();

        String url = FilePreviewUrlBuilder.build("https://preview.example.com/onlinePreview", record,
                "https://file.example.com/download?id=100", 900L);

        assertThat(url).isEqualTo("https://preview.example.com/onlinePreview?fileId=100"
                + "&fileName=%E5%90%88%E5%90%8C%201.docx&expireSeconds=900");
    }

    @Test
    void build_地址模板含占位符_替换占位符() {
        FileRecord record = record();

        String url = FilePreviewUrlBuilder.build("/preview/{fileId}/{fileName}?source={fileUrl}", record,
                "/file/files/download?id=100", 600L);

        assertThat(url).isEqualTo("/preview/100/%E5%90%88%E5%90%8C%201.docx"
                + "?source=/file/files/download%3Fid%3D100");
    }

    @Test
    void build_无文件ID_自动使用fileUrl参数() {
        FileRecord record = record();
        record.setId(null);

        String url = FilePreviewUrlBuilder.build("https://preview.example.com/onlinePreview", record,
                "https://file.example.com/download?id=100", 900L);

        assertThat(url).isEqualTo("https://preview.example.com/onlinePreview?fileUrl=https://file.example.com/download%3Fid%3D100"
                + "&fileName=%E5%90%88%E5%90%8C%201.docx&expireSeconds=900");
    }

    private FileRecord record() {
        FileRecord record = new FileRecord();
        record.setId(100L);
        record.setFileName("合同 1.docx");
        return record;
    }
}
