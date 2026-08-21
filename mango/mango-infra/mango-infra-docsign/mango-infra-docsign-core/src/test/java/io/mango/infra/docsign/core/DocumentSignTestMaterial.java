package io.mango.infra.docsign.core;

import io.mango.infra.docsign.command.Pkcs12KeyMaterial;
import io.mango.infra.docsign.command.TrustStoreMaterial;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.ofdrw.gm.ses.v1.SES_ESPictrueInfo;
import org.ofdrw.gm.ses.v1.SES_Header;
import org.ofdrw.gm.ses.v4.CertInfoList;
import org.ofdrw.gm.ses.v4.SESeal;
import org.ofdrw.gm.ses.v4.SES_CertList;
import org.ofdrw.gm.ses.v4.SES_ESPropertyInfo;
import org.ofdrw.gm.ses.v4.SES_SealInfo;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

final class DocumentSignTestMaterial {

    private static final char[] PASSWORD = "test-password".toCharArray();

    private DocumentSignTestMaterial() {
    }

    static Material rsa() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return material(generator.generateKeyPair(), "SHA256withRSA", "CN=PDF RSA Test");
    }

    static Material sm2() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(
                "EC", CryptoProviderSupport.provider());
        generator.initialize(new ECGenParameterSpec("sm2p256v1"));
        return material(generator.generateKeyPair(), "SM3withSM2", "CN=OFD SM2 Test");
    }

    static Material ecdsaP256() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(
                "EC", CryptoProviderSupport.provider());
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return material(generator.generateKeyPair(), "SHA256withECDSA", "CN=PDF ECDSA Test");
    }

    static Material shortLivedRsa() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return shortLivedMaterial(
                generator.generateKeyPair(), "SHA256withRSA", "CN=Expired PDF Test");
    }

    static Material shortLivedSm2() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(
                "EC", CryptoProviderSupport.provider());
        generator.initialize(new ECGenParameterSpec("sm2p256v1"));
        return shortLivedMaterial(
                generator.generateKeyPair(), "SM3withSM2", "CN=Expired OFD Test");
    }

    private static Material shortLivedMaterial(KeyPair keyPair,
                                               String signatureAlgorithm,
                                               String subject) throws Exception {
        Instant now = Instant.now();
        return material(keyPair, signatureAlgorithm, subject,
                now.minus(1, ChronoUnit.DAYS), now.plusSeconds(4));
    }

    static byte[] stampImage() throws Exception {
        BufferedImage image = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(220, 0, 0, 210));
            graphics.setStroke(new BasicStroke(10F));
            graphics.drawOval(12, 12, 136, 136);
            graphics.drawLine(42, 80, 118, 80);
            graphics.drawLine(80, 42, 80, 118);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        return output.toByteArray();
    }

    static byte[] electronicSeal(Material material) throws Exception {
        Instant now = Instant.now();
        CertInfoList certInfoList = new CertInfoList()
                .add(new DEROctetString(material.certificate().getEncoded()));
        SES_ESPropertyInfo property = new SES_ESPropertyInfo()
                .setType(new ASN1Integer(1))
                .setName(new DERUTF8String("Mango Test Seal"))
                .setCertListType(SES_ESPropertyInfo.CertListType)
                .setCertList(new SES_CertList(certInfoList))
                .setCreateDate(new ASN1GeneralizedTime(Date.from(now)))
                .setValidStart(new ASN1GeneralizedTime(Date.from(now.minus(1, ChronoUnit.DAYS))))
                .setValidEnd(new ASN1GeneralizedTime(Date.from(now.plus(30, ChronoUnit.DAYS))));
        SES_ESPictrueInfo picture = new SES_ESPictrueInfo()
                .setType("PNG")
                .setData(stampImage())
                .setWidth(40)
                .setHeight(40);
        SES_SealInfo sealInfo = new SES_SealInfo()
                .setHeader(new SES_Header(SES_Header.V4, new org.bouncycastle.asn1.DERIA5String("MANGO")))
                .setEsID("TEST-SEAL-001")
                .setProperty(property)
                .setPicture(picture);
        Signature signature = Signature.getInstance(
                "SM3withSM2", CryptoProviderSupport.provider());
        signature.initSign(material.keyPair().getPrivate());
        signature.update(sealInfo.getEncoded("DER"));
        return new SESeal()
                .seteSealInfo(sealInfo)
                .setCert(material.certificate())
                .setSignAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignedValue(signature.sign())
                .getEncoded("DER");
    }

    private static Material material(KeyPair keyPair,
                                     String signatureAlgorithm,
                                     String subject) throws Exception {
        Instant now = Instant.now();
        return material(keyPair, signatureAlgorithm, subject,
                now.minus(1, ChronoUnit.DAYS), now.plus(30, ChronoUnit.DAYS));
    }

    private static Material material(KeyPair keyPair,
                                     String signatureAlgorithm,
                                     String subject,
                                     Instant validFrom,
                                     Instant validUntil) throws Exception {
        X500Name name = new X500Name(subject);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name,
                BigInteger.valueOf(System.nanoTime()).abs(),
                Date.from(validFrom),
                Date.from(validUntil),
                name,
                keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        ContentSigner certificateSigner = new JcaContentSignerBuilder(signatureAlgorithm)
                .setProvider(CryptoProviderSupport.provider())
                .build(keyPair.getPrivate());
        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider(CryptoProviderSupport.provider())
                .getCertificate(builder.build(certificateSigner));
        certificate.verify(keyPair.getPublic(), CryptoProviderSupport.provider());

        KeyStore keyStore = KeyStore.getInstance("PKCS12", CryptoProviderSupport.provider());
        keyStore.load(null, PASSWORD);
        keyStore.setKeyEntry("signer", keyPair.getPrivate(), PASSWORD,
                new java.security.cert.Certificate[]{certificate});
        ByteArrayOutputStream keyOutput = new ByteArrayOutputStream();
        keyStore.store(keyOutput, PASSWORD);

        KeyStore trustStore = KeyStore.getInstance("PKCS12", CryptoProviderSupport.provider());
        trustStore.load(null, PASSWORD);
        trustStore.setCertificateEntry("trusted-signer", certificate);
        ByteArrayOutputStream trustOutput = new ByteArrayOutputStream();
        trustStore.store(trustOutput, PASSWORD);

        return new Material(
                new Pkcs12KeyMaterial(keyOutput.toByteArray(), PASSWORD, "signer"),
                new TrustStoreMaterial("PKCS12", trustOutput.toByteArray(), PASSWORD),
                keyPair,
                certificate);
    }

    record Material(
            Pkcs12KeyMaterial keyMaterial,
            TrustStoreMaterial trustStore,
            KeyPair keyPair,
            X509Certificate certificate) {

        void awaitExpiration() throws InterruptedException {
            long waitMillis = certificate.getNotAfter().getTime() - System.currentTimeMillis() + 100L;
            if (waitMillis > 0) {
                Thread.sleep(waitMillis);
            }
        }
    }
}
