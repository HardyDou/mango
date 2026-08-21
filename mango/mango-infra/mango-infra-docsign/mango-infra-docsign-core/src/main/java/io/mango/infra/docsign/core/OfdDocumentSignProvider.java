package io.mango.infra.docsign.core;

import io.mango.infra.docsign.command.DocumentSignCommand;
import io.mango.infra.docsign.command.DocumentStampCommand;
import io.mango.infra.docsign.command.DocumentVerifyCommand;
import io.mango.infra.docsign.enums.DocumentSignFormat;
import io.mango.infra.docsign.enums.StampSide;
import io.mango.infra.docsign.enums.StampType;
import io.mango.infra.docsign.spi.IDocumentSignProvider;
import io.mango.infra.docsign.vo.DocumentSignResultVO;
import io.mango.infra.docsign.vo.DocumentSignStreamResultVO;
import io.mango.infra.docsign.vo.DocumentVerifyResultVO;
import io.mango.infra.docsign.vo.SignatureValidationVO;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.core.signatures.Signatures;
import org.ofdrw.core.signatures.sig.SignedInfo;
import org.ofdrw.gm.ses.v4.SES_Signature;
import org.ofdrw.gm.ses.v4.SESeal;
import org.ofdrw.gm.sm2strut.ContentInfo;
import org.ofdrw.gm.sm2strut.SignedData;
import org.ofdrw.gm.sm2strut.SignerInfo;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;
import org.ofdrw.sign.OFDSigner;
import org.ofdrw.sign.SignMode;
import org.ofdrw.sign.signContainer.GBT35275DSContainer;
import org.ofdrw.sign.signContainer.SESV4Container;
import org.ofdrw.sign.stamppos.NormalStampPos;
import org.ofdrw.sign.stamppos.RidingStampPos;
import org.ofdrw.sign.stamppos.Side;
import org.ofdrw.sign.verify.OFDValidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * OFDRW provider for SM2 digital signatures and caller-supplied SES v4 electronic seals.
 */
public final class OfdDocumentSignProvider implements IDocumentSignProvider {

    private static final int IN_MEMORY_OUTPUT_GROWTH_BYTES = 64 * 1024;
    private static final long MAX_SIGNED_VALUE_BYTES = 4L * 1024L * 1024L;
    private static final long DEFAULT_MAX_IN_MEMORY_BYTES = 16L * 1024 * 1024;
    private static final long DEFAULT_MAX_DOCUMENT_BYTES = 2L * 1024 * 1024 * 1024;

    private final KeyMaterialLoader keyMaterialLoader = new KeyMaterialLoader();
    private final CertificateTrustValidator trustValidator = new CertificateTrustValidator();
    private final long maxInMemoryBytes;
    private final long maxDocumentBytes;
    private final Path temporaryDirectory;

    public OfdDocumentSignProvider() {
        this(DEFAULT_MAX_IN_MEMORY_BYTES, DEFAULT_MAX_DOCUMENT_BYTES, defaultTemporaryDirectory());
    }

    public OfdDocumentSignProvider(long maxInMemoryBytes, Path temporaryDirectory) {
        this(maxInMemoryBytes, DEFAULT_MAX_DOCUMENT_BYTES, temporaryDirectory);
    }

    public OfdDocumentSignProvider(long maxInMemoryBytes,
                                   long maxDocumentBytes,
                                   Path temporaryDirectory) {
        if (maxInMemoryBytes <= 0) {
            throw new IllegalArgumentException("OFD 内存接口大小上限必须大于 0");
        }
        if (maxDocumentBytes < maxInMemoryBytes) {
            throw new IllegalArgumentException("OFD 文档大小上限不能小于内存接口大小上限");
        }
        if (temporaryDirectory == null) {
            throw new IllegalArgumentException("OFD 临时目录不能为空");
        }
        this.maxInMemoryBytes = maxInMemoryBytes;
        this.maxDocumentBytes = maxDocumentBytes;
        this.temporaryDirectory = temporaryDirectory.toAbsolutePath().normalize();
    }

    @Override
    public boolean supports(DocumentSignFormat format) {
        return format == DocumentSignFormat.OFD;
    }

    @Override
    public DocumentSignResultVO sign(DocumentSignCommand command) {
        byte[] source = inMemoryContent(command.content(), "待签 OFD");
        ByteArrayOutputStream output = new ByteArrayOutputStream(source.length + IN_MEMORY_OUTPUT_GROWTH_BYTES);
        DocumentSignStreamResultVO result = sign(command, new ByteArrayInputStream(source), output);
        return new DocumentSignResultVO(result.format(), output.toByteArray(), result.signatureCount());
    }

    @Override
    public DocumentSignStreamResultVO sign(DocumentSignCommand command,
                                           InputStream document,
                                           OutputStream signedDocument) {
        requireFormat(command.format());
        if (document == null || signedDocument == null) {
            throw new IllegalArgumentException("OFD 签章输入流和输出流不能为空");
        }
        try (TemporaryDocumentFile source = TemporaryDocumentFile.copyOf(
                document, temporaryDirectory, maxDocumentBytes)) {
            return sign(command, source.path(), signedDocument);
        } catch (IOException | GeneralSecurityException ex) {
            throw new IllegalStateException("OFD 文档签章失败", ex);
        }
    }

    @Override
    public DocumentVerifyResultVO verify(DocumentVerifyCommand command) {
        byte[] content = inMemoryContent(command.content(), "验签 OFD");
        return verify(command, new ByteArrayInputStream(content));
    }

    @Override
    public DocumentVerifyResultVO verify(DocumentVerifyCommand command, InputStream document) {
        requireFormat(command.format());
        if (document == null) {
            throw new IllegalArgumentException("OFD 验签输入流不能为空");
        }
        try (TemporaryDocumentFile source = TemporaryDocumentFile.copyOf(
                document, temporaryDirectory, maxDocumentBytes)) {
            return verify(command, source.path());
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取 OFD 验签文档失败", ex);
        }
    }

    private DocumentSignStreamResultVO sign(DocumentSignCommand command,
                                            Path source,
                                            OutputStream signedDocument)
            throws IOException, GeneralSecurityException {
        LoadedKeyMaterial keyMaterial = keyMaterialLoader.load(command.keyMaterial());
        DocumentSignatureAlgorithmResolver.resolveOfd(command.signatureAlgorithm(), keyMaterial);
        CountingNonClosingOutputStream output = new CountingNonClosingOutputStream(signedDocument);
        try (OFDReader reader = new OFDReader(source)) {
            OFDSigner signer = new OFDSigner(reader, output).setSignMode(SignMode.ContinueSign);
            if (command.hasStamp()) {
                configureElectronicSeal(signer, command.stamp(), keyMaterial);
            } else {
                signer.setSignContainer(new GBT35275DSContainer(
                        keyMaterial.certificate(), keyMaterial.privateKey()));
            }
            Signatures signatures = signer.exeSign();
            signer.close();
            output.flush();
            int count = signatures == null || signatures.getSignatures() == null
                    ? 0 : signatures.getSignatures().size();
            return new DocumentSignStreamResultVO(DocumentSignFormat.OFD, output.count(), count);
        }
    }

    private DocumentVerifyResultVO verify(DocumentVerifyCommand command, Path source) {
        OFDReader reader;
        try {
            reader = new OFDReader(source);
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取 OFD 验签文档失败", ex);
        }
        try (OFDValidator validator = new OFDValidator(reader)) {
            if (!reader.hasSignature()) {
                return new DocumentVerifyResultVO(DocumentSignFormat.OFD, false, false, List.of());
            }
            boolean documentValid = true;
            String validationMessage = "签名结构和受保护文件完整";
            try {
                validator.setValidator(new OfdSignatureValidator()).exeValidate();
            } catch (IOException | GeneralSecurityException ex) {
                documentValid = false;
                validationMessage = "OFD 签名或文件完整性验证失败: " + ex.getMessage();
            }
            List<SignatureValidationVO> validations = signatureValidations(
                    reader, command, documentValid, validationMessage);
            boolean valid = !validations.isEmpty() && validations.stream().allMatch(SignatureValidationVO::valid);
            return new DocumentVerifyResultVO(DocumentSignFormat.OFD, true, valid, validations);
        } catch (IOException ex) {
            throw new IllegalArgumentException("关闭 OFD 验签文档失败", ex);
        }
    }

    private void configureElectronicSeal(OFDSigner signer,
                                         DocumentStampCommand stamp,
                                         LoadedKeyMaterial keyMaterial) throws IOException {
        if (!stamp.hasOfdSeal()) {
            throw new IllegalArgumentException("OFD 原生电子印章必须提供 SES v4 印章数据");
        }
        SESeal seal = SESeal.getInstance(ASN1Primitive.fromByteArray(stamp.ofdSeal()));
        signer.setSignContainer(new SESV4Container(
                keyMaterial.privateKey(), seal, keyMaterial.certificate()));
        if (stamp.type() == StampType.NORMAL) {
            signer.addApPos(new NormalStampPos(
                    stamp.page(), stamp.x(), stamp.y(), stamp.width(), stamp.height()));
        } else {
            signer.addApPos(new RidingStampPos(
                    side(stamp.side()), null, stamp.clipNumber(),
                    stamp.width(), stamp.height(), stamp.margin()));
        }
    }

    private Side side(StampSide side) {
        return switch (side) {
            case LEFT -> Side.Left;
            case RIGHT -> Side.Right;
            case TOP -> Side.Top;
            case BOTTOM -> Side.Bottom;
        };
    }

    private List<SignatureValidationVO> signatureValidations(OFDReader reader,
                                                              DocumentVerifyCommand command,
                                                              boolean documentValid,
                                                              String validationMessage) {
        Signatures signatures = reader.getDefaultSignatures();
        if (signatures == null || signatures.getSignatures() == null) {
            return List.of();
        }
        List<SignatureValidationVO> results = new ArrayList<>(signatures.getSignatures().size());
        ResourceLocator locator = reader.getResourceLocator();
        ST_Loc signaturesPath = reader.getDefaultDocSignaturesPath();
        locator.save();
        try {
            locator.cd(signaturesPath.parent());
            for (org.ofdrw.core.signatures.Signature record : signatures.getSignatures()) {
                results.add(signatureValidation(
                        locator, record, command, documentValid, validationMessage));
            }
        } finally {
            locator.restore();
        }
        return results;
    }

    private SignatureValidationVO signatureValidation(ResourceLocator locator,
                                                       org.ofdrw.core.signatures.Signature record,
                                                       DocumentVerifyCommand command,
                                                       boolean documentValid,
                                                       String validationMessage) {
        try {
            org.ofdrw.core.signatures.sig.Signature signature = locator.get(
                    record.getBaseLoc(), org.ofdrw.core.signatures.sig.Signature::new);
            SignedInfo signedInfo = signature.getSignedInfo();
            locator.save();
            byte[] signedValue;
            try {
                locator.cd(record.getBaseLoc().parent());
                Path signedValuePath = locator.getFile(signature.getSignedValue());
                signedValue = readSignedValue(signedValuePath);
            } finally {
                locator.restore();
            }
            SignatureCertificateInfo certificateInfo = certificateInfo(record.getType(), signedValue);
            Instant signingTime = parseSigningTime(signedInfo.getSignatureDateTime());
            Date validationTime = new Date();
            boolean certificateTimeValid = certificateTimeValid(
                    certificateInfo.signer(), validationTime);
            boolean trusted = certificateInfo.sealSignatureValid()
                    && trustValidator.isTrusted(
                    certificateInfo.signer(), certificateInfo.chain(), command.trustStore(), validationTime)
                    && (certificateInfo.sealCertificate() == null
                    || trustValidator.isTrusted(
                    certificateInfo.sealCertificate(), List.of(certificateInfo.sealCertificate()),
                    command.trustStore(), validationTime));
            boolean valid = documentValid && certificateTimeValid && trusted;
            String message = valid ? "签名有效" : validationMessage(
                    documentValid, certificateTimeValid, trusted, validationMessage);
            return new SignatureValidationVO(
                    record.getID(),
                    record.getType() == SigType.Seal ? "SES_V4_SEAL" : "GBT_35275_SIGN",
                    signedInfo.getSignatureMethod(),
                    certificateInfo.signer().getSubjectX500Principal().getName(),
                    signingTime,
                    documentValid,
                    documentValid,
                    certificateTimeValid,
                    trusted,
                    documentValid,
                    valid,
                    message);
        } catch (IOException | org.dom4j.DocumentException | GeneralSecurityException ex) {
            return new SignatureValidationVO(
                    record.getID(),
                    String.valueOf(record.getType()),
                    null,
                    null,
                    null,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    "OFD 签名详情解析失败: " + ex.getMessage());
        }
    }

    private SignatureCertificateInfo certificateInfo(SigType type, byte[] signedValue)
            throws IOException, GeneralSecurityException {
        if (type == SigType.Seal) {
            SES_Signature signature = SES_Signature.getInstance(signedValue);
            X509Certificate signer = certificate(signature.getCert().getOctets());
            SESeal seal = signature.getToSign().getEseal();
            X509Certificate sealCertificate = certificate(seal.getCert().getOctets());
            return new SignatureCertificateInfo(
                    signer,
                    List.of(signer),
                    sealCertificate,
                    verifySealSignature(seal, sealCertificate));
        }
        ContentInfo contentInfo = ContentInfo.getInstance(ASN1Primitive.fromByteArray(signedValue));
        SignedData signedData = SignedData.getInstance(contentInfo.getContent());
        ASN1Set signerInfos = signedData.getSignerInfos();
        if (signerInfos == null || signerInfos.size() != 1) {
            throw new GeneralSecurityException("GB/T 35275 签名者数量不是 1");
        }
        SignerInfo signerInfo = SignerInfo.getInstance(signerInfos.getObjectAt(0));
        org.bouncycastle.asn1.x509.Certificate bcCertificate = signedData.getSignCert(
                signerInfo.getIssuerAngSerialNumber());
        X509Certificate signer = new JcaX509CertificateConverter()
                .setProvider(CryptoProviderSupport.provider())
                .getCertificate(new org.bouncycastle.cert.X509CertificateHolder(bcCertificate));
        return new SignatureCertificateInfo(signer, List.of(signer), null, true);
    }

    private X509Certificate certificate(byte[] encoded) throws GeneralSecurityException {
        CertificateFactory factory = CertificateFactory.getInstance(
                "X.509", CryptoProviderSupport.provider());
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(encoded));
    }

    private byte[] readSignedValue(Path signedValuePath) throws IOException {
        long size = Files.size(signedValuePath);
        if (size <= 0 || size > MAX_SIGNED_VALUE_BYTES) {
            throw new IOException("OFD 签名值大小无效: " + size);
        }
        try (InputStream input = Files.newInputStream(signedValuePath)) {
            byte[] content = input.readNBytes((int) size + 1);
            if (content.length != size) {
                throw new IOException("OFD 签名值长度与文件元数据不一致");
            }
            return content;
        }
    }

    private boolean verifySealSignature(SESeal seal, X509Certificate certificate)
            throws IOException, GeneralSecurityException {
        Signature verifier = Signature.getInstance(
                seal.getSignAlgID().getId(), CryptoProviderSupport.provider());
        verifier.initVerify(certificate);
        verifier.update(seal.geteSealInfo().getEncoded("DER"));
        ASN1BitString signedValue = seal.getSignedValue();
        return verifier.verify(signedValue.getOctets());
    }

    private boolean certificateTimeValid(X509Certificate certificate, Date validationTime) {
        try {
            certificate.checkValidity(validationTime);
            return true;
        } catch (CertificateException ex) {
            return false;
        }
    }

    private Instant parseSigningTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, OFDSigner.DF)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String validationMessage(boolean documentValid,
                                     boolean certificateTimeValid,
                                     boolean trusted,
                                     String documentMessage) {
        if (!documentValid) {
            return documentMessage;
        }
        if (!certificateTimeValid) {
            return "签名证书在验证时间无效";
        }
        if (!trusted) {
            return "签名或电子印章证书不受调用方信任库信任";
        }
        return "签名无效";
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
            throw new IllegalArgumentException("OFD provider 不支持格式: " + format);
        }
    }

    private static Path defaultTemporaryDirectory() {
        return Path.of(System.getProperty("java.io.tmpdir"), "mango-docsign");
    }

    private record SignatureCertificateInfo(
            X509Certificate signer,
            List<X509Certificate> chain,
            X509Certificate sealCertificate,
            boolean sealSignatureValid) {

        SignatureCertificateInfo {
            chain = List.copyOf(chain);
        }
    }
}
