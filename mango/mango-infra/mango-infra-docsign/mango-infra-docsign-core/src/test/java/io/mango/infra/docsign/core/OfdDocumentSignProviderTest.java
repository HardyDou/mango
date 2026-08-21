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
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.graphics2d.OFDPageGraphics2D;
import org.ofdrw.reader.OFDReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfdDocumentSignProviderTest {

    private final OfdDocumentSignProvider provider = new OfdDocumentSignProvider();

    @TempDir
    private Path temporaryDirectory;

    @Test
    void sign_sm2Gbt35275_generatesTrustedSignature() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();

        DocumentSignResultVO signed = provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.OFD)
                .content(ofd(1))
                .keyMaterial(material.keyMaterial())
                .signatureAlgorithm(DocumentSignatureAlgorithm.SM3_WITH_SM2)
                .build());
        DocumentVerifyResultVO verified = verify(signed.content(), material);

        assertThat(signed.signatureCount()).isEqualTo(1);
        assertThat(verified.valid()).isTrue();
        assertThat(verified.signatures()).singleElement().satisfies(signature -> {
            assertThat(signature.type()).isEqualTo("GBT_35275_SIGN");
            assertThat(signature.cryptographicallyValid()).isTrue();
            assertThat(signature.documentIntegrityValid()).isTrue();
            assertThat(signature.trusted()).isTrue();
        });
    }

    @Test
    void verify_withoutTrustStore_reportsIntegrityButFailsClosed() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();
        byte[] signed = sign(ofd(1), material).content();

        DocumentVerifyResultVO verified = provider.verify(DocumentVerifyCommand.builder()
                .format(DocumentSignFormat.OFD)
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
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.shortLivedSm2();
        byte[] signed = sign(ofd(1), material).content();
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
    void sign_twice_preservesBothSignatures() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();
        DocumentSignResultVO first = sign(ofd(1), material);
        DocumentSignResultVO second = sign(first.content(), material);

        DocumentVerifyResultVO verified = verify(second.content(), material);

        assertThat(second.signatureCount()).isEqualTo(2);
        assertThat(verified.valid()).isTrue();
        assertThat(verified.signatures()).hasSize(2).allMatch(signature -> signature.valid());
    }

    @Test
    void verify_modifiedProtectedContent_failsIntegrity() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();
        byte[] signed = sign(ofd(1), material).content();

        DocumentVerifyResultVO verified = verify(tamperContentXml(signed), material);

        assertThat(verified.valid()).isFalse();
        assertThat(verified.signatures()).singleElement().satisfies(signature -> {
            assertThat(signature.documentIntegrityValid()).isFalse();
            assertThat(signature.valid()).isFalse();
        });
    }

    @Test
    void sign_normalElectronicSeal_recordsExpectedPlacement() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();
        DocumentSignResultVO signed = provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.OFD)
                .content(ofd(2))
                .keyMaterial(material.keyMaterial())
                .stamp(DocumentStampCommand.normal(2, 20, 30, 40, 40)
                        .ofdSeal(DocumentSignTestMaterial.electronicSeal(material))
                        .build())
                .build());

        assertThat(verify(signed.content(), material).valid()).isTrue();
        List<StampAnnot> annots = stampAnnots(signed.content());
        assertThat(annots).singleElement().satisfies(annot -> {
            assertThat(annot.getBoundary().getTopLeftX()).isEqualTo(20D);
            assertThat(annot.getBoundary().getTopLeftY()).isEqualTo(30D);
            assertThat(annot.getBoundary().getWidth()).isEqualTo(40D);
            assertThat(annot.getBoundary().getHeight()).isEqualTo(40D);
        });
    }

    @Test
    void sign_ridingElectronicSeal_recordsAppearanceOnEveryPage() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();
        DocumentSignResultVO signed = provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.OFD)
                .content(ofd(3))
                .keyMaterial(material.keyMaterial())
                .stamp(DocumentStampCommand.riding(StampSide.RIGHT, 45, 45)
                        .ofdSeal(DocumentSignTestMaterial.electronicSeal(material))
                        .build())
                .build());

        assertThat(verify(signed.content(), material).valid()).isTrue();
        List<StampAnnot> annots = stampAnnots(signed.content());
        assertThat(annots).hasSize(3);
        assertThat(annots).allSatisfy(annot -> {
            assertThat(annot.getBoundary().getWidth()).isEqualTo(45D);
            assertThat(annot.getBoundary().getHeight()).isEqualTo(45D);
            assertThat(annot.getClip()).isNotNull();
        });
    }

    @Test
    void sign_withRsaKey_rejectsUnsupportedOfdAlgorithm() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.rsa();
        DocumentSignCommand command = DocumentSignCommand.builder()
                .format(DocumentSignFormat.OFD)
                .content(ofd(1))
                .keyMaterial(material.keyMaterial())
                .build();

        assertThatThrownBy(() -> provider.sign(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("sm2p256v1");
    }

    @Test
    void sign_explicitRsaAlgorithm_rejectsUnsupportedOfdAlgorithm() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();
        DocumentSignCommand command = DocumentSignCommand.builder()
                .format(DocumentSignFormat.OFD)
                .content(ofd(1))
                .keyMaterial(material.keyMaterial())
                .signatureAlgorithm(DocumentSignatureAlgorithm.SHA256_WITH_RSA)
                .build();

        assertThatThrownBy(() -> provider.sign(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("OFD 标准签名只支持 SM3_WITH_SM2");
    }

    @Test
    void sign_streamingRidingSeal_handlesTwelvePagesWithoutOwningStreams() throws Exception {
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();
        OfdDocumentSignProvider streamingProvider = new OfdDocumentSignProvider(128, temporaryDirectory);
        TrackingInputStream source = new TrackingInputStream(ofd(12));
        TrackingOutputStream target = new TrackingOutputStream();

        DocumentSignStreamResultVO signed = streamingProvider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.OFD)
                .keyMaterial(material.keyMaterial())
                .stamp(DocumentStampCommand.riding(StampSide.RIGHT, 45, 45)
                        .ofdSeal(DocumentSignTestMaterial.electronicSeal(material))
                        .build())
                .build(), source, target);

        TrackingInputStream signedSource = new TrackingInputStream(target.toByteArray());
        DocumentVerifyResultVO verified = streamingProvider.verify(DocumentVerifyCommand.builder()
                .format(DocumentSignFormat.OFD)
                .trustStore(material.trustStore())
                .build(), signedSource);

        assertThat(signed.contentLength()).isEqualTo(target.size());
        assertThat(signed.signatureCount()).isEqualTo(1);
        assertThat(verified.valid()).isTrue();
        assertThat(stampAnnots(target.toByteArray())).hasSize(12);
        assertThat(source.closed).isFalse();
        assertThat(signedSource.closed).isFalse();
        assertThat(target.closed).isFalse();
        writeInteropSample("mango-streaming-riding-seal-signed.ofd", target.toByteArray());
        assertTemporaryDirectoryEmpty();
    }

    @Test
    void sign_byteArrayAboveLimit_requiresStreamingApi() throws Exception {
        byte[] source = ofd(1);
        OfdDocumentSignProvider limitedProvider = new OfdDocumentSignProvider(
                source.length - 1L, temporaryDirectory);
        DocumentSignTestMaterial.Material material = DocumentSignTestMaterial.sm2();
        DocumentSignCommand command = DocumentSignCommand.builder()
                .format(DocumentSignFormat.OFD)
                .content(source)
                .keyMaterial(material.keyMaterial())
                .build();

        assertThatThrownBy(() -> limitedProvider.sign(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("请使用流式接口");

        TrackingOutputStream target = new TrackingOutputStream();
        DocumentSignStreamResultVO signed = limitedProvider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.OFD)
                .keyMaterial(material.keyMaterial())
                .build(), new TrackingInputStream(source), target);
        assertThat(signed.contentLength()).isEqualTo(target.size());
        assertTemporaryDirectoryEmpty();
    }

    private DocumentSignResultVO sign(byte[] content,
                                      DocumentSignTestMaterial.Material material) {
        return provider.sign(DocumentSignCommand.builder()
                .format(DocumentSignFormat.OFD)
                .content(content)
                .keyMaterial(material.keyMaterial())
                .build());
    }

    private DocumentVerifyResultVO verify(byte[] content,
                                          DocumentSignTestMaterial.Material material) {
        return provider.verify(DocumentVerifyCommand.builder()
                .format(DocumentSignFormat.OFD)
                .content(content)
                .trustStore(material.trustStore())
                .build());
    }

    private byte[] ofd(int pages) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             OFDGraphicsDocument document = new OFDGraphicsDocument(output)) {
            for (int page = 1; page <= pages; page++) {
                OFDPageGraphics2D graphics = document.newPage(210, 297);
                graphics.setColor(Color.BLACK);
                graphics.drawString("Mango OFD page " + page, 20, 30);
                graphics.dispose();
            }
            document.close();
            return output.toByteArray();
        }
    }

    private List<StampAnnot> stampAnnots(byte[] content) throws IOException {
        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(content))) {
            List<StampAnnot> annots = new ArrayList<>();
            reader.getStampAnnots().forEach(entity -> annots.addAll(entity.getStampAnnots()));
            return annots;
        }
    }

    private byte[] tamperContentXml(byte[] content) throws IOException {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(content));
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            ZipEntry entry;
            boolean modified = false;
            while ((entry = input.getNextEntry()) != null) {
                byte[] entryContent = input.readAllBytes();
                if (!modified && entry.getName().endsWith("/Content.xml")) {
                    byte[] marker = "<!--tampered-->".getBytes(StandardCharsets.UTF_8);
                    byte[] changed = new byte[entryContent.length + marker.length];
                    System.arraycopy(entryContent, 0, changed, 0, entryContent.length);
                    System.arraycopy(marker, 0, changed, entryContent.length, marker.length);
                    entryContent = changed;
                    modified = true;
                }
                zipOutput.putNextEntry(new ZipEntry(entry.getName()));
                zipOutput.write(entryContent);
                zipOutput.closeEntry();
            }
            assertThat(modified).as("protected Content.xml exists").isTrue();
            zipOutput.finish();
            return output.toByteArray();
        }
    }

    private void assertTemporaryDirectoryEmpty() throws Exception {
        try (java.util.stream.Stream<Path> files = Files.list(temporaryDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    private void writeInteropSample(String fileName, byte[] content) throws Exception {
        Path directory = Path.of("target", "interop");
        Files.createDirectories(directory);
        Files.write(directory.resolve(fileName), content);
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
