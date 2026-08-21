package io.mango.infra.docsign.core;

import io.mango.infra.docsign.enums.DocumentSignatureAlgorithm;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedDataStreamGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;

/**
 * Detached CMS signer used by PDFBox incremental signatures.
 */
final class PdfCmsSigner {

    private static final int INITIAL_CMS_CAPACITY = 16 * 1024;

    private final LoadedKeyMaterial keyMaterial;
    private final DocumentSignatureAlgorithm signatureAlgorithm;

    PdfCmsSigner(LoadedKeyMaterial keyMaterial, DocumentSignatureAlgorithm signatureAlgorithm) {
        this.keyMaterial = keyMaterial;
        this.signatureAlgorithm = signatureAlgorithm;
    }

    byte[] sign(InputStream content) throws IOException {
        try {
            ContentSigner contentSigner = new JcaContentSignerBuilder(
                    DocumentSignatureAlgorithmResolver.jcaName(signatureAlgorithm))
                    .setProvider(CryptoProviderSupport.provider())
                    .build(keyMaterial.privateKey());
            CMSSignedDataStreamGenerator generator = new CMSSignedDataStreamGenerator();
            generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder()
                            .setProvider(CryptoProviderSupport.provider())
                            .build())
                    .build(contentSigner, keyMaterial.certificate()));
            generator.addCertificates(new JcaCertStore(keyMaterial.certificateChain()));
            ByteArrayOutputStream encoded = new ByteArrayOutputStream(INITIAL_CMS_CAPACITY);
            try (OutputStream signedContent = generator.open(encoded, false)) {
                content.transferTo(signedContent);
            }
            return encoded.toByteArray();
        } catch (OperatorCreationException | CMSException | CertificateEncodingException ex) {
            throw new IOException("生成 PDF CMS 签名失败", ex);
        }
    }

}
