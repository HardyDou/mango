package io.mango.infra.docsign.core;

import io.mango.common.exception.BizException;
import io.mango.infra.docsign.command.DocumentSignCommand;
import io.mango.infra.docsign.command.DocumentStampCommand;
import io.mango.infra.docsign.command.DocumentVerifyCommand;
import io.mango.infra.docsign.enums.DocumentSignFormat;
import io.mango.infra.docsign.enums.DocumentSignatureAlgorithm;
import io.mango.infra.docsign.enums.StampSide;
import io.mango.infra.docsign.vo.DocumentSignResultVO;
import io.mango.infra.docsign.vo.DocumentSignStreamResultVO;
import io.mango.infra.docsign.vo.DocumentVerifyResultVO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfDocumentSignProviderTest {

    private final PdfDocumentSignProvider provider = new PdfDocumentSignProvider();

    @TempDir
    private Path temporaryDirectory;

    @Test
    void sign_rsaNormalStamp_generatesTrustedVisibleSignature() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();

        DocumentSignResultVO signed = provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(pdf(1))
                .keyMaterial(material.keyMaterial())
                .signerName("Mango RSA Signer")
                .reason("Contract approval")
                .stamp(DocumentStampCommand.normal(1, 20, 20, 35, 35)
                        .image(DocumentSignTestMaterial.stampImage())
                        .build())
                .build());

        DocumentVerifyResultVO verified = verify(signed.content(), material);
        assertThat(signed.signatureCount()).isEqualTo(1);
        assertThat(verified.valid()).isTrue();
        assertThat(signatureSubFilter(signed.content()))
                .isEqualTo(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED.getName());
        assertThat(verified.signatures()).singleElement().satisfies(signature -> {
            assertThat(signature.cryptographicallyValid()).isTrue();
            assertThat(signature.documentIntegrityValid()).isTrue();
            assertThat(signature.trusted()).isTrue();
            assertThat(signature.coversCurrentDocument()).isTrue();
        });
        assertThat(hasRedPixels(signed.content(), 0)).isTrue();
    }

    @Test
    void sign_sm2_generatesVerifiableCmsSignature() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();

        DocumentSignResultVO signed = provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(pdf(1))
                .keyMaterial(material.keyMaterial())
                .build());

        assertThat(verify(signed.content(), material).valid()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("explicitRsaAlgorithms")
    void sign_explicitRsaAlgorithm_generatesRequestedCmsSignature(
            DocumentSignatureAlgorithm algorithm,
            String expectedCmsAlgorithm) throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();

        DocumentSignResultVO signed = provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(pdf(1))
                .keyMaterial(material.keyMaterial())
                .signatureAlgorithm(algorithm)
                .build());

        DocumentVerifyResultVO verified = verify(signed.content(), material);
        assertThat(verified.valid()).isTrue();
        assertThat(verified.signatures()).singleElement()
                .satisfies(signature -> assertThat(signature.algorithm()).isEqualTo(expectedCmsAlgorithm));
    }

    @Test
    void sign_explicitSm3WithSm2_generatesRequestedCmsSignature() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();

        DocumentSignResultVO signed = provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(pdf(1))
                .keyMaterial(material.keyMaterial())
                .signatureAlgorithm(DocumentSignatureAlgorithm.SM3_WITH_SM2)
                .build());

        DocumentVerifyResultVO verified = verify(signed.content(), material);
        assertThat(verified.valid()).isTrue();
        assertThat(verified.signatures()).singleElement()
                .satisfies(signature -> assertThat(signature.algorithm())
                        .isEqualTo("1.2.156.10197.1.401/1.2.156.10197.1.501"));
    }

    @Test
    void sign_rsaAlgorithmWithSm2Material_rejectsMismatch() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();
        DocumentSignCommand command = DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(pdf(1))
                .keyMaterial(material.keyMaterial())
                .signatureAlgorithm(DocumentSignatureAlgorithm.SHA256_WITH_RSA)
                .build();

        assertThatThrownBy(() -> provider.sign(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("RSA 私钥和 RSA 签名证书");
    }

    @Test
    void sign_autoWithOrdinaryEcdsaMaterial_rejectsNonSm2Curve() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.ecdsaP256();
        DocumentSignCommand command = DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(pdf(1))
                .keyMaterial(material.keyMaterial())
                .build();

        assertThatThrownBy(() -> provider.sign(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("普通 ECDSA 证书当前不受支持");
    }

    @Test
    void sign_twice_preservesBothSignatures() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();
        DocumentSignResultVO first = sign(pdf(1), material);
        DocumentSignResultVO second = sign(first.content(), material);

        DocumentVerifyResultVO verified = verify(second.content(), material);
        assertThat(second.signatureCount()).isEqualTo(2);
        assertThat(verified.valid()).isTrue();
        assertThat(verified.signatures()).hasSize(2).allMatch(signature -> signature.valid());
        assertThat(verified.signatures().get(0).coversCurrentDocument()).isFalse();
        assertThat(verified.signatures().get(1).coversCurrentDocument()).isTrue();
    }

    @Test
    void verify_appendedBytes_failsWholeDocumentCoverage() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();
        byte[] signed = sign(pdf(1), material).content();
        byte[] tampered = Arrays.copyOf(signed, signed.length + 10);
        System.arraycopy("\n%tampered".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                0, tampered, signed.length, 10);

        DocumentVerifyResultVO verified = verify(tampered, material);

        assertThat(verified.valid()).isFalse();
        assertThat(verified.signatures()).singleElement()
                .satisfies(signature -> assertThat(signature.coversCurrentDocument()).isFalse());
    }

    @Test
    void verify_withoutTrustStore_reportsCryptoButFailsClosed() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();
        byte[] signed = sign(pdf(1), material).content();

        DocumentVerifyResultVO verified = provider.verify(DocumentVerifyCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(signed)
                .build());

        assertThat(verified.valid()).isFalse();
        assertThat(verified.signatures()).singleElement().satisfies(signature -> {
            assertThat(signature.cryptographicallyValid()).isTrue();
            assertThat(signature.trusted()).isFalse();
        });
    }

    @Test
    void verify_withExpiredCertificate_failsClosedAtValidationTime() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.shortLivedRsa();
        byte[] signed = sign(pdf(1), material).content();
        material.awaitExpiration();

        DocumentVerifyResultVO verified = verify(signed, material);

        assertThat(verified.valid()).isFalse();
        assertThat(verified.signatures()).singleElement().satisfies(signature -> {
            assertThat(signature.cryptographicallyValid()).isTrue();
            assertThat(signature.certificateTimeValid()).isFalse();
            assertThat(signature.valid()).isFalse();
        });
    }

    @Test
    void sign_ridingStamp_placesProtectedImageOnEveryPage() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();

        DocumentSignResultVO signed = provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(pdf(3))
                .keyMaterial(material.keyMaterial())
                .stamp(DocumentStampCommand.riding(StampSide.RIGHT, 45, 45)
                        .image(DocumentSignTestMaterial.stampImage())
                        .build())
                .build());

        assertThat(verify(signed.content(), material).valid()).isTrue();
        assertThat(hasRedPixels(signed.content(), 0)).isTrue();
        assertThat(hasRedPixels(signed.content(), 1)).isTrue();
        assertThat(hasRedPixels(signed.content(), 2)).isTrue();
    }

    @Test
    void sign_streamingRidingStamp_handlesTwentyFivePagesWithoutOwningStreams() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();
        PdfDocumentSignProvider streamingProvider = new PdfDocumentSignProvider(128, temporaryDirectory);
        TrackingInputStream source = new TrackingInputStream(pdf(25));
        TrackingOutputStream target = new TrackingOutputStream();

        DocumentSignStreamResultVO signed = streamingProvider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .keyMaterial(material.keyMaterial())
                .stamp(DocumentStampCommand.riding(StampSide.RIGHT, 45, 45)
                        .image(DocumentSignTestMaterial.stampImage())
                        .build())
                .build(), source, target);

        TrackingInputStream signedSource = new TrackingInputStream(target.toByteArray());
        DocumentVerifyResultVO verified = streamingProvider.verify(DocumentVerifyCommand.builder()
                .format(DocumentSignFormat.PDF)
                .trustStore(material.trustStore())
                .build(), signedSource);

        assertThat(signed.contentLength()).isEqualTo(target.size());
        assertThat(signed.signatureCount()).isEqualTo(1);
        assertThat(verified.valid()).isTrue();
        assertThat(source.closed).isFalse();
        assertThat(signedSource.closed).isFalse();
        assertThat(target.closed).isFalse();
        assertThat(hasImage(target.toByteArray(), 0)).isTrue();
        assertThat(hasImage(target.toByteArray(), 24)).isTrue();
        writeInteropSample("mango-streaming-riding-seal-signed.pdf", target.toByteArray());
        assertTemporaryDirectoryEmpty();
    }

    @Test
    void sign_byteArrayAboveLimit_requiresStreamingApi() throws Exception {
        byte[] source = pdf(1);
        PdfDocumentSignProvider limitedProvider = new PdfDocumentSignProvider(
                source.length - 1L, temporaryDirectory);
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();
        DocumentSignCommand command = DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(source)
                .keyMaterial(material.keyMaterial())
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> limitedProvider.sign(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("请使用流式接口");

        TrackingOutputStream target = new TrackingOutputStream();
        DocumentSignStreamResultVO signed = limitedProvider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .keyMaterial(material.keyMaterial())
                .build(), new TrackingInputStream(source), target);
        assertThat(signed.contentLength()).isEqualTo(target.size());
        assertTemporaryDirectoryEmpty();
    }

    @Test
    void sign_streamAboveConfiguredDocumentLimit_cleansTemporaryFile() throws Exception {
        byte[] source = pdf(1);
        PdfDocumentSignProvider limitedProvider = new PdfDocumentSignProvider(
                64, source.length - 1L, temporaryDirectory);
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> limitedProvider.sign(
                        DocumentSignCommand.builder()
                                .format(DocumentSignFormat.PDF)
                                .keyMaterial(material.keyMaterial())
                                .build(),
                        new TrackingInputStream(source),
                        new TrackingOutputStream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("流式处理大小上限");
        assertTemporaryDirectoryEmpty();
    }

    private DocumentSignResultVO sign(byte[] content,
                                      DocumentSignTestMaterial.Material material) {
        return provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(content)
                .keyMaterial(material.keyMaterial())
                .build());
    }

    private DocumentVerifyResultVO verify(byte[] content,
                                          DocumentSignTestMaterial.Material material) {
        return provider.verify(DocumentVerifyCommand.builder()
                .format(DocumentSignFormat.PDF)
                .content(content)
                .trustStore(material.trustStore())
                .build());
    }

    private static Stream<Arguments> explicitRsaAlgorithms() {
        return Stream.of(
                Arguments.of(DocumentSignatureAlgorithm.SHA256_WITH_RSA,
                        "2.16.840.1.101.3.4.2.1/1.2.840.113549.1.1.11"),
                Arguments.of(DocumentSignatureAlgorithm.SHA384_WITH_RSA,
                        "2.16.840.1.101.3.4.2.2/1.2.840.113549.1.1.12"),
                Arguments.of(DocumentSignatureAlgorithm.SHA512_WITH_RSA,
                        "2.16.840.1.101.3.4.2.3/1.2.840.113549.1.1.13"),
                Arguments.of(DocumentSignatureAlgorithm.SHA256_WITH_RSA_PSS,
                        "2.16.840.1.101.3.4.2.1/1.2.840.113549.1.1.10"));
    }

    private byte[] pdf(int pages) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int index = 0; index < pages; index++) {
                document.addPage(new PDPage(PDRectangle.A4));
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private boolean hasRedPixels(byte[] content, int page) throws Exception {
        try (PDDocument document = Loader.loadPDF(content)) {
            BufferedImage image = new PDFRenderer(document).renderImage(page, 0.8F);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    int red = rgb >>> 16 & 0xFF;
                    int green = rgb >>> 8 & 0xFF;
                    int blue = rgb & 0xFF;
                    if (red > 150 && red > green * 2 && red > blue * 2) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private boolean hasImage(byte[] content, int page) throws Exception {
        try (PDDocument document = Loader.loadPDF(content)) {
            for (org.apache.pdfbox.cos.COSName name
                    : document.getPage(page).getResources().getXObjectNames()) {
                if (document.getPage(page).getResources().getXObject(name) instanceof PDImageXObject) {
                    return true;
                }
            }
            return false;
        }
    }

    private String signatureSubFilter(byte[] content) throws Exception {
        try (PDDocument document = Loader.loadPDF(content)) {
            return document.getSignatureDictionaries().get(0).getSubFilter();
        }
    }

    private void writeInteropSample(String fileName, byte[] content) throws Exception {
        Path directory = Path.of("target", "interop");
        Files.createDirectories(directory);
        Files.write(directory.resolve(fileName), content);
    }

    private void assertTemporaryDirectoryEmpty() throws Exception {
        try (java.util.stream.Stream<Path> files = Files.list(temporaryDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private TrackingInputStream(byte[] content) {
            super(content);
        }

        @Override
        public byte[] readAllBytes() {
            throw new AssertionError("流式入口不得调用 readAllBytes()");
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class TrackingOutputStream extends ByteArrayOutputStream {

        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }
}
