package io.mango.infra.docsign.core;

import io.mango.infra.docsign.command.DocumentSignCommand;
import io.mango.infra.docsign.command.DocumentStampCommand;
import io.mango.infra.docsign.command.DocumentVerifyCommand;
import io.mango.infra.docsign.enums.DocumentSignFormat;
import io.mango.infra.docsign.enums.DocumentSignatureAlgorithm;
import io.mango.infra.docsign.enums.StampSide;
import io.mango.infra.docsign.enums.StampType;
import io.mango.infra.docsign.spi.IDocumentSignProvider;
import io.mango.infra.docsign.vo.DocumentSignResultVO;
import io.mango.infra.docsign.vo.DocumentSignStreamResultVO;
import io.mango.infra.docsign.vo.DocumentVerifyResultVO;
import io.mango.infra.docsign.vo.SignatureValidationVO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedDataParser;
import org.bouncycastle.cms.CMSTypedStream;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * PDFBox 3 provider for visible stamps, detached CMS signing and offline validation.
 */
public final class PdfDocumentSignProvider implements IDocumentSignProvider {

    private static final int BYTE_RANGE_COMPONENT_COUNT = 4;
    private static final int SECOND_RANGE_START_INDEX = 2;
    private static final int SECOND_RANGE_LENGTH_INDEX = 3;
    private static final float POINTS_PER_MM = 72F / 25.4F;
    private static final int PREFERRED_SIGNATURE_SIZE = 65536;
    private static final long DEFAULT_MAX_IN_MEMORY_BYTES = 16L * 1024 * 1024;
    private static final long DEFAULT_MAX_DOCUMENT_BYTES = 2L * 1024 * 1024 * 1024;

    private final KeyMaterialLoader keyMaterialLoader = new KeyMaterialLoader();
    private final CertificateTrustValidator trustValidator = new CertificateTrustValidator();
    private final long maxInMemoryBytes;
    private final long maxDocumentBytes;
    private final Path temporaryDirectory;

    public PdfDocumentSignProvider() {
        this(DEFAULT_MAX_IN_MEMORY_BYTES, DEFAULT_MAX_DOCUMENT_BYTES, defaultTemporaryDirectory());
    }

    public PdfDocumentSignProvider(long maxInMemoryBytes, Path temporaryDirectory) {
        this(maxInMemoryBytes, DEFAULT_MAX_DOCUMENT_BYTES, temporaryDirectory);
    }

    public PdfDocumentSignProvider(long maxInMemoryBytes,
                                   long maxDocumentBytes,
                                   Path temporaryDirectory) {
        if (maxInMemoryBytes <= 0) {
            throw new IllegalArgumentException("PDF 内存接口大小上限必须大于 0");
        }
        if (maxDocumentBytes < maxInMemoryBytes) {
            throw new IllegalArgumentException("PDF 文档大小上限不能小于内存接口大小上限");
        }
        if (temporaryDirectory == null) {
            throw new IllegalArgumentException("PDF 临时目录不能为空");
        }
        this.maxInMemoryBytes = maxInMemoryBytes;
        this.maxDocumentBytes = maxDocumentBytes;
        this.temporaryDirectory = temporaryDirectory.toAbsolutePath().normalize();
    }

    @Override
    public boolean supports(DocumentSignFormat format) {
        return format == DocumentSignFormat.PDF;
    }

    @Override
    public DocumentSignResultVO sign(DocumentSignCommand command) {
        byte[] source = inMemoryContent(command.content(), "待签 PDF");
        ByteArrayOutputStream output = new ByteArrayOutputStream(source.length + PREFERRED_SIGNATURE_SIZE);
        DocumentSignStreamResultVO result = sign(command, new ByteArrayInputStream(source), output);
        return new DocumentSignResultVO(result.format(), output.toByteArray(), result.signatureCount());
    }

    @Override
    public DocumentSignStreamResultVO sign(DocumentSignCommand command,
                                           InputStream document,
                                           OutputStream signedDocument) {
        requireFormat(command.format());
        if (document == null || signedDocument == null) {
            throw new IllegalArgumentException("PDF 签章输入流和输出流不能为空");
        }
        try (TemporaryDocumentFile source = TemporaryDocumentFile.copyOf(
                document, temporaryDirectory, maxDocumentBytes)) {
            return sign(command, source.path(), signedDocument);
        } catch (IOException ex) {
            throw new IllegalStateException("PDF 文档签章失败", ex);
        }
    }

    @Override
    public DocumentVerifyResultVO verify(DocumentVerifyCommand command) {
        byte[] content = inMemoryContent(command.content(), "验签 PDF");
        return verify(command, new ByteArrayInputStream(content));
    }

    @Override
    public DocumentVerifyResultVO verify(DocumentVerifyCommand command, InputStream document) {
        requireFormat(command.format());
        if (document == null) {
            throw new IllegalArgumentException("PDF 验签输入流不能为空");
        }
        try (TemporaryDocumentFile source = TemporaryDocumentFile.copyOf(
                document, temporaryDirectory, maxDocumentBytes)) {
            return verify(command, source.path());
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取 PDF 验签文档失败", ex);
        }
    }

    private DocumentSignStreamResultVO sign(DocumentSignCommand command,
                                            Path source,
                                            OutputStream signedDocument) throws IOException {
        LoadedKeyMaterial keyMaterial = keyMaterialLoader.load(command.keyMaterial());
        DocumentSignatureAlgorithm signatureAlgorithm =
                DocumentSignatureAlgorithmResolver.resolvePdf(command.signatureAlgorithm(), keyMaterial);
        CountingNonClosingOutputStream output = new CountingNonClosingOutputStream(signedDocument);
        try (PDDocument document = load(source, command.documentPassword());
             SignatureOptions options = new SignatureOptions()) {
            if (!document.getCurrentAccessPermission().canModify()) {
                throw new IllegalArgumentException("PDF 权限不允许添加签名");
            }
            if (command.hasStamp()) {
                applyStamp(document, command.stamp());
            }
            PDSignature signature = signatureDictionary(command, keyMaterial.certificate());
            options.setPreferredSignatureSize(PREFERRED_SIGNATURE_SIZE);
            PdfCmsSigner cmsSigner = new PdfCmsSigner(keyMaterial, signatureAlgorithm);
            document.addSignature(signature, cmsSigner::sign, options);
            document.saveIncremental(output);
            output.flush();
            return new DocumentSignStreamResultVO(
                    DocumentSignFormat.PDF, output.count(), document.getSignatureDictionaries().size());
        }
    }

    private DocumentVerifyResultVO verify(DocumentVerifyCommand command, Path source) throws IOException {
        long contentLength = Files.size(source);
        try (PDDocument document = load(source, command.documentPassword())) {
            List<PDSignature> dictionaries = document.getSignatureDictionaries();
            if (dictionaries.isEmpty()) {
                return new DocumentVerifyResultVO(DocumentSignFormat.PDF, false, false, List.of());
            }
            List<SignatureValidationVO> validations = new ArrayList<>(dictionaries.size());
            for (int index = 0; index < dictionaries.size(); index++) {
                validations.add(validateSignature(
                        dictionaries.get(index), index + 1, source, contentLength, command));
            }
            boolean latestCoversDocument = validations.get(validations.size() - 1).coversCurrentDocument();
            boolean valid = latestCoversDocument && validations.stream().allMatch(SignatureValidationVO::valid);
            return new DocumentVerifyResultVO(DocumentSignFormat.PDF, true, valid, validations);
        }
    }

    private PDDocument load(Path content, char[] password) throws IOException {
        return Loader.loadPDF(content.toFile(), password.length == 0 ? "" : new String(password));
    }

    private PDSignature signatureDictionary(DocumentSignCommand command, X509Certificate certificate) {
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName(command.signerName() == null || command.signerName().isBlank()
                ? certificate.getSubjectX500Principal().getName() : command.signerName());
        signature.setReason(command.reason());
        signature.setLocation(command.location());
        signature.setSignDate(GregorianCalendar.from(java.time.ZonedDateTime.now()));
        return signature;
    }

    private void applyStamp(PDDocument document, DocumentStampCommand stamp) throws IOException {
        if (!stamp.hasImage()) {
            throw new IllegalArgumentException("PDF 可见印章必须提供图片");
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(stamp.image()));
        if (image == null) {
            throw new IllegalArgumentException("PDF 印章图片格式无法识别");
        }
        if (stamp.type() == StampType.NORMAL) {
            applyNormalStamp(document, stamp, image);
        } else {
            applyRidingStamp(document, stamp, image);
        }
    }

    private void applyNormalStamp(PDDocument document,
                                  DocumentStampCommand stamp,
                                  BufferedImage image) throws IOException {
        if (stamp.page() > document.getNumberOfPages()) {
            throw new IllegalArgumentException("PDF 印章页码超出文档页数: " + stamp.page());
        }
        PDPage page = document.getPage(stamp.page() - 1);
        PDRectangle box = page.getCropBox();
        float width = mm(stamp.width());
        float height = mm(stamp.height());
        float x = box.getLowerLeftX() + mm(stamp.x());
        float y = box.getLowerLeftY() + box.getHeight() - mm(stamp.y()) - height;
        drawImage(document, page, image, x, y, width, height);
    }

    private void applyRidingStamp(PDDocument document,
                                  DocumentStampCommand stamp,
                                  BufferedImage image) throws IOException {
        int pageCount = document.getNumberOfPages();
        int segments = stamp.clipNumber() > 0 ? Math.min(stamp.clipNumber(), pageCount) : pageCount;
        for (int index = 0; index < segments; index++) {
            PDPage page = document.getPage(index);
            BufferedImage segment = slice(image, stamp.side(), index, segments);
            drawRidingSegment(document, page, segment, stamp, segments);
        }
    }

    private BufferedImage slice(BufferedImage image, StampSide side, int index, int count) {
        if (side == StampSide.LEFT || side == StampSide.RIGHT) {
            int start = index * image.getWidth() / count;
            int end = (index + 1) * image.getWidth() / count;
            return image.getSubimage(start, 0, Math.max(1, end - start), image.getHeight());
        }
        int start = index * image.getHeight() / count;
        int end = (index + 1) * image.getHeight() / count;
        return image.getSubimage(0, start, image.getWidth(), Math.max(1, end - start));
    }

    private void drawRidingSegment(PDDocument document,
                                   PDPage page,
                                   BufferedImage segment,
                                   DocumentStampCommand stamp,
                                   int segments) throws IOException {
        PDRectangle box = page.getCropBox();
        float margin = mm(stamp.margin());
        float width = mm(stamp.width());
        float height = mm(stamp.height());
        float segmentWidth = width;
        float segmentHeight = height;
        if (stamp.side() == StampSide.LEFT || stamp.side() == StampSide.RIGHT) {
            segmentWidth = width / segments;
        } else {
            segmentHeight = height / segments;
        }
        float x;
        float y;
        switch (stamp.side()) {
            case LEFT -> {
                x = box.getLowerLeftX() + margin;
                y = box.getLowerLeftY() + (box.getHeight() - segmentHeight) / 2F;
            }
            case RIGHT -> {
                x = box.getLowerLeftX() + box.getWidth() - margin - segmentWidth;
                y = box.getLowerLeftY() + (box.getHeight() - segmentHeight) / 2F;
            }
            case TOP -> {
                x = box.getLowerLeftX() + (box.getWidth() - segmentWidth) / 2F;
                y = box.getLowerLeftY() + box.getHeight() - margin - segmentHeight;
            }
            case BOTTOM -> {
                x = box.getLowerLeftX() + (box.getWidth() - segmentWidth) / 2F;
                y = box.getLowerLeftY() + margin;
            }
            default -> throw new IllegalArgumentException("不支持的骑缝章方向: " + stamp.side());
        }
        drawImage(document, page, segment, x, y, segmentWidth, segmentHeight);
    }

    private void drawImage(PDDocument document,
                           PDPage page,
                           BufferedImage image,
                           float x,
                           float y,
                           float width,
                           float height) throws IOException {
        PDImageXObject imageObject = LosslessFactory.createFromImage(document, image);
        try (PDPageContentStream stream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.drawImage(imageObject, x, y, width, height);
        }
    }

    private SignatureValidationVO validateSignature(PDSignature signature,
                                                    int index,
                                                    Path content,
                                                    long contentLength,
                                                    DocumentVerifyCommand command) {
        boolean byteRangeValid = isValidByteRange(signature.getByteRange(), contentLength);
        boolean coversCurrentDocument = byteRangeValid
                && signedRevisionLength(signature.getByteRange()) == contentLength;
        Instant signingTime = signature.getSignDate() == null
                ? null : signature.getSignDate().toInstant();
        if (!byteRangeValid) {
            return invalid(index, signature, signingTime, false,
                    false, "PDF 签名 ByteRange 无效");
        }
        CMSSignedDataParser signedData = null;
        try {
            byte[] contents;
            try (InputStream source = Files.newInputStream(content)) {
                contents = signature.getContents(source);
            }
            try (PdfByteRangeInputStream signedContent = new PdfByteRangeInputStream(
                    content, signature.getByteRange())) {
                signedData = new CMSSignedDataParser(
                        new JcaDigestCalculatorProviderBuilder()
                                .setProvider(CryptoProviderSupport.provider())
                                .build(),
                        new CMSTypedStream(signedContent),
                        contents);
                signedData.getSignedContent().drain();
            }
            SignerInformationStore signerStore = signedData.getSignerInfos();
            if (signerStore.size() != 1) {
                return invalid(index, signature, signingTime, byteRangeValid,
                        coversCurrentDocument, "PDF CMS 必须且只能包含一个签名者");
            }
            SignerInformation signer = signerStore.getSigners().iterator().next();
            List<X509Certificate> certificates = certificates(signedData.getCertificates());
            X509Certificate certificate = matchingCertificate(signedData.getCertificates(), signer);
            boolean cryptographic = signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider(CryptoProviderSupport.provider())
                    .build(certificate));
            Date validationTime = new Date();
            boolean certificateTimeValid = certificateTimeValid(certificate, validationTime);
            boolean trusted = trustValidator.isTrusted(
                    certificate, certificates, command.trustStore(), validationTime);
            boolean integrity = byteRangeValid && cryptographic;
            boolean valid = cryptographic && integrity && certificateTimeValid && trusted;
            String message = valid ? "签名有效" : validationMessage(
                    cryptographic, integrity, certificateTimeValid, trusted);
            return new SignatureValidationVO(
                    "pdf-signature-" + index,
                    "CMS",
                    signer.getDigestAlgOID() + "/" + signer.getEncryptionAlgOID(),
                    certificate.getSubjectX500Principal().getName(),
                    signingTime,
                    cryptographic,
                    integrity,
                    certificateTimeValid,
                    trusted,
                    coversCurrentDocument,
                    valid,
                    message);
        } catch (IOException | CMSException | OperatorCreationException | CertificateException ex) {
            return invalid(index, signature, signingTime, byteRangeValid,
                    coversCurrentDocument, "PDF 签名验证失败: " + ex.getMessage());
        } finally {
            if (signedData != null) {
                try {
                    signedData.close();
                } catch (IOException ex) {
                    throw new IllegalStateException("关闭 PDF CMS 验签解析器失败", ex);
                }
            }
        }
    }

    private List<X509Certificate> certificates(Store<X509CertificateHolder> store)
            throws CertificateException {
        List<X509Certificate> certificates = new ArrayList<>();
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter()
                .setProvider(CryptoProviderSupport.provider());
        Collection<X509CertificateHolder> holders = store.getMatches(null);
        for (X509CertificateHolder holder : holders) {
            certificates.add(converter.getCertificate(holder));
        }
        return certificates;
    }

    private X509Certificate matchingCertificate(Store<X509CertificateHolder> store,
                                                SignerInformation signer)
            throws CertificateException {
        Collection<X509CertificateHolder> matches = store.getMatches(signer.getSID());
        if (matches.isEmpty()) {
            throw new CertificateException("CMS 中找不到签名者证书");
        }
        return new JcaX509CertificateConverter()
                .setProvider(CryptoProviderSupport.provider())
                .getCertificate(matches.iterator().next());
    }

    private boolean certificateTimeValid(X509Certificate certificate, Date validationTime) {
        try {
            certificate.checkValidity(validationTime);
            return true;
        } catch (CertificateException ex) {
            return false;
        }
    }

    private boolean isValidByteRange(int[] byteRange, long contentLength) {
        return byteRange != null
                && byteRange.length == BYTE_RANGE_COMPONENT_COUNT
                && byteRange[0] == 0
                && byteRange[1] > 0
                && byteRange[SECOND_RANGE_START_INDEX] > byteRange[1]
                && byteRange[SECOND_RANGE_LENGTH_INDEX] >= 0
                && signedRevisionLength(byteRange) <= contentLength;
    }

    private long signedRevisionLength(int[] byteRange) {
        if (byteRange == null || byteRange.length != BYTE_RANGE_COMPONENT_COUNT) {
            return -1;
        }
        return byteRange[SECOND_RANGE_START_INDEX] + (long) byteRange[SECOND_RANGE_LENGTH_INDEX];
    }

    private SignatureValidationVO invalid(int index,
                                          PDSignature signature,
                                          Instant signingTime,
                                          boolean byteRangeValid,
                                          boolean coversCurrentDocument,
                                          String message) {
        Calendar signDate = signature.getSignDate();
        return new SignatureValidationVO(
                "pdf-signature-" + index,
                "CMS",
                signature.getSubFilter(),
                signature.getName(),
                signDate == null ? signingTime : signDate.toInstant(),
                false,
                false,
                false,
                false,
                coversCurrentDocument,
                false,
                message);
    }

    private String validationMessage(boolean cryptographic,
                                     boolean integrity,
                                     boolean certificateTimeValid,
                                     boolean trusted) {
        if (!cryptographic) {
            return "密码学签名无效";
        }
        if (!integrity) {
            return "PDF 签名范围或文档完整性无效";
        }
        if (!certificateTimeValid) {
            return "签名证书在验证时间无效";
        }
        if (!trusted) {
            return "签名证书不受调用方信任库信任";
        }
        return "签名无效";
    }

    private float mm(double value) {
        return (float) value * POINTS_PER_MM;
    }

    private byte[] inMemoryContent(byte[] content, String description) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException(description + "不能为空");
        }
        if (content.length > maxInMemoryBytes) {
            throw new IllegalArgumentException(
                    description + "超过内存接口上限 " + maxInMemoryBytes + " 字节，请使用流式接口");
        }
        return content;
    }

    private void requireFormat(DocumentSignFormat format) {
        if (!supports(format)) {
            throw new IllegalArgumentException("PDF provider 不支持格式: " + format);
        }
    }

    private static Path defaultTemporaryDirectory() {
        return Path.of(System.getProperty("java.io.tmpdir"), "mango-docsign");
    }
}
