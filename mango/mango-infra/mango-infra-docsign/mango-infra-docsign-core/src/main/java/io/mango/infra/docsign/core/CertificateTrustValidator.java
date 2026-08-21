package io.mango.infra.docsign.core;

import io.mango.infra.docsign.command.TrustStoreMaterial;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Offline X.509 trust validation. Revocation and TSA checks are intentionally outside the first release.
 */
final class CertificateTrustValidator {

    boolean isTrusted(X509Certificate signer,
                      List<X509Certificate> embeddedChain,
                      TrustStoreMaterial material,
                      Date validationTime) {
        if (material == null) {
            return false;
        }
        try {
            Set<TrustAnchor> anchors = loadTrustAnchors(material);
            if (anchors.isEmpty()) {
                return false;
            }
            for (TrustAnchor anchor : anchors) {
                if (signer.equals(anchor.getTrustedCert())) {
                    signer.checkValidity(validationTime);
                    return true;
                }
            }
            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(signer);
            PKIXBuilderParameters parameters = new PKIXBuilderParameters(anchors, selector);
            parameters.setRevocationEnabled(false);
            parameters.setDate(validationTime);
            List<X509Certificate> candidates = new ArrayList<>(embeddedChain);
            candidates.add(signer);
            parameters.addCertStore(CertStore.getInstance(
                    "Collection", new CollectionCertStoreParameters(candidates)));
            CertPathBuilder.getInstance("PKIX").build(parameters);
            return true;
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }

    private Set<TrustAnchor> loadTrustAnchors(TrustStoreMaterial material) throws GeneralSecurityException {
        try {
            KeyStore trustStore = "PKCS12".equalsIgnoreCase(material.type())
                    ? KeyStore.getInstance("PKCS12", CryptoProviderSupport.provider())
                    : KeyStore.getInstance(material.type());
            trustStore.load(new ByteArrayInputStream(material.content()), material.password());
            Set<TrustAnchor> anchors = new HashSet<>();
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                java.security.cert.Certificate certificate = trustStore.getCertificate(aliases.nextElement());
                if (certificate instanceof X509Certificate x509Certificate) {
                    anchors.add(new TrustAnchor(x509Certificate, null));
                }
            }
            return anchors;
        } catch (IOException ex) {
            throw new KeyStoreException("读取信任库失败", ex);
        }
    }
}
