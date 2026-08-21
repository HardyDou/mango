package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.AsposeWordToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.DocxToOfdConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocxToOfdConvertProviderTest {

    @Test
    void convert_docxWithTableAndSeal_runsRealDocxPdfOfdChain() throws Exception {
        byte[] license = Files.readAllBytes(Path.of("src/main/resources/aspose/license.xml"));
        DocxToOfdConvertProvider provider = new DocxToOfdConvertProvider(
                new AsposeWordToPdfConvertProvider(license));

        ConvertResultVO result = provider.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.DOCX)
                .targetFormat(ConvertFormat.OFD)
                .fileName("approval.docx")
                .inputStream(new ByteArrayInputStream(docxWithTableAndSeal()))
                .build());

        assertThat(result.format()).isEqualTo(ConvertFormat.OFD);
        assertThat(result.fileName()).isEqualTo("approval.ofd");
        assertThat(result.contentType()).isEqualTo("application/ofd");
        assertThat(result.content()).startsWith(new byte[]{0x50, 0x4B});
        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(result.content()))) {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void supports_onlyDocxToOfd() {
        DocxToOfdConvertProvider provider = new DocxToOfdConvertProvider(
                new AsposeWordToPdfConvertProvider());

        assertThat(provider.supports(ConvertFormat.DOCX, ConvertFormat.OFD)).isTrue();
        assertThat(provider.supports(ConvertFormat.DOC, ConvertFormat.OFD)).isFalse();
        assertThat(provider.supports(ConvertFormat.DOCX, ConvertFormat.PDF)).isFalse();
    }

    private byte[] docxWithTableAndSeal() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Approval document");
            XWPFTable table = document.createTable(2, 3);
            table.getRow(0).getCell(0).setText("Applicant");
            table.getRow(0).getCell(1).setText("Amount");
            table.getRow(0).getCell(2).setText("Status");
            table.getRow(1).getCell(0).setText("Mango");
            table.getRow(1).getCell(1).setText("1000");
            table.getRow(1).getCell(2).setText("Approved");
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.addPicture(new ByteArrayInputStream(sealImage()), Document.PICTURE_TYPE_PNG,
                    "seal.png", Units.toEMU(96), Units.toEMU(96));
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] sealImage() throws Exception {
        BufferedImage image = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(200, 0, 0, 230));
            graphics.setStroke(new java.awt.BasicStroke(7F));
            graphics.drawOval(8, 8, 144, 144);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            graphics.drawString("SEAL", 38, 94);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            assertThat(ImageIO.write(image, "png", outputStream)).isTrue();
            return outputStream.toByteArray();
        }
    }
}
