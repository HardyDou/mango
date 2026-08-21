package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.ConvertOptionKeys;
import io.mango.infra.fileproc.convert.convert.PdfToOfdConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.reader.OFDReader;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfToOfdConvertProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void convert_pdfWithTableAndSeal_writesReadableOfd() throws Exception {
        PdfToOfdConvertProvider provider = new PdfToOfdConvertProvider();

        ConvertResultVO result = provider.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.PDF)
                .targetFormat(ConvertFormat.OFD)
                .fileName("contract.pdf")
                .inputStream(new ByteArrayInputStream(pdfWithTableAndSeal()))
                .build());

        assertThat(result.format()).isEqualTo(ConvertFormat.OFD);
        assertThat(result.fileName()).isEqualTo("contract.ofd");
        assertThat(result.contentType()).isEqualTo("application/ofd");
        assertThat(result.content()).startsWith(new byte[]{0x50, 0x4B});
        Map<String, byte[]> entries = zipEntries(result.content());
        assertThat(entries).containsKey("OFD.xml");
        assertThat(entries.keySet()).anyMatch(name -> name.endsWith("/Content.xml"));
        String pageContent = entries.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("/Content.xml"))
                .map(entry -> new String(entry.getValue(), StandardCharsets.UTF_8))
                .findFirst()
                .orElseThrow();
        assertThat(pageContent).contains("PathObject", "DrawParam");
        assertThat(pageContent.split("PathObject", -1).length - 1).isGreaterThan(3);
        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(result.content()))) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void convert_withTargetPath_writesOfdFile() throws Exception {
        PdfToOfdConvertProvider provider = new PdfToOfdConvertProvider();
        Path target = tempDir.resolve("out/contract.ofd");

        ConvertResultVO result = provider.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.PDF)
                .targetFormat(ConvertFormat.OFD)
                .fileName("contract.pdf")
                .inputStream(new ByteArrayInputStream(pdfWithTableAndSeal()))
                .targetPath(target)
                .build());

        assertThat(result.content()).isEmpty();
        assertThat(result.outputPath()).isEqualTo(target);
        assertThat(Files.readAllBytes(target)).startsWith(new byte[]{0x50, 0x4B});
        try (OFDReader reader = new OFDReader(target)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void supports_onlyPdfToOfd() {
        PdfToOfdConvertProvider provider = new PdfToOfdConvertProvider();

        assertThat(provider.supports(ConvertFormat.PDF, ConvertFormat.OFD)).isTrue();
        assertThat(provider.supports(ConvertFormat.DOCX, ConvertFormat.OFD)).isFalse();
        assertThat(provider.supports(ConvertFormat.PDF, ConvertFormat.PNG)).isFalse();
    }

    @Test
    void convert_passwordProtectedPdf_usesPasswordOption() throws Exception {
        PdfToOfdConvertProvider provider = new PdfToOfdConvertProvider();

        ConvertResultVO result = provider.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.PDF)
                .targetFormat(ConvertFormat.OFD)
                .fileName("protected.pdf")
                .inputStream(new ByteArrayInputStream(passwordProtectedPdf()))
                .option(ConvertOptionKeys.PASSWORD, "reader-password")
                .build());

        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(result.content()))) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
        }
    }

    private byte[] pdfWithTableAndSeal() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setStrokingColor(Color.BLACK);
                content.setLineWidth(1F);
                for (int row = 0; row <= 2; row++) {
                    content.moveTo(72, 700 - row * 36F);
                    content.lineTo(420, 700 - row * 36F);
                }
                for (int column = 0; column <= 3; column++) {
                    content.moveTo(72 + column * 116F, 700);
                    content.lineTo(72 + column * 116F, 628);
                }
                content.stroke();
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                content.newLineAtOffset(82, 676);
                content.showText("CONTRACT TABLE");
                content.endText();
                content.setStrokingColor(Color.RED);
                content.setLineWidth(4F);
                content.addRect(430, 610, 90, 90);
                content.stroke();
                content.beginText();
                content.setNonStrokingColor(Color.RED);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                content.newLineAtOffset(445, 650);
                content.showText("SEAL");
                content.endText();
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private Map<String, byte[]> zipEntries(byte[] content) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zip.readAllBytes());
                }
            }
        }
        return entries;
    }

    private byte[] passwordProtectedPdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    "owner-password", "reader-password", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
