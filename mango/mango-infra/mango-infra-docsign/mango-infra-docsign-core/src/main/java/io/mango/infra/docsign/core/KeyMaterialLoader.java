package io.mango.infra.docsign.core;

import io.mango.infra.docsign.command.Pkcs12KeyMaterial;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Loads caller-provided PKCS#12 key material without retaining password arrays.
 */
final class KeyMaterialLoader {

    LoadedKeyMaterial load(Pkcs12KeyMaterial material) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12", CryptoProviderSupport.provider());
            char[] password = material.password();
            keyStore.load(new ByteArrayInputStream(material.content()), password);
            String alias = resolveAlias(keyStore, material.alias());
            Key key = keyStore.getKey(alias, password);
            if (!(key instanceof PrivateKey privateKey)) {
                throw new IllegalArgumentException("PKCS#12 别名不包含私钥: " + alias);
            }
            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain == null || chain.length == 0) {
                throw new IllegalArgumentException("PKCS#12 私钥缺少证书链: " + alias);
            }
            List<X509Certificate> certificates = new ArrayList<>(chain.length);
            for (Certificate certificate : chain) {
                if (!(certificate instanceof X509Certificate x509Certificate)) {
                    throw new IllegalArgumentException("PKCS#12 证书链包含非 X.509 证书");
                }
                certificates.add(x509Certificate);
            }
            certificates.get(0).checkValidity();
            validateKeyAlgorithm(privateKey);
            return new LoadedKeyMaterial(privateKey, certificates.get(0), certificates);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException
                 | CertificateException | UnrecoverableKeyException ex) {
            throw new IllegalArgumentException("读取 PKCS#12 签名密钥失败", ex);
        }
    }

    private String resolveAlias(KeyStore keyStore, String requestedAlias) throws KeyStoreException {
        if (requestedAlias != null) {
            if (!keyStore.isKeyEntry(requestedAlias)) {
                throw new IllegalArgumentException("PKCS#12 中不存在私钥别名: " + requestedAlias);
            }
            return requestedAlias;
        }
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;
            }
        }
        throw new IllegalArgumentException("PKCS#12 中没有可用私钥");
    }

    private void validateKeyAlgorithm(PrivateKey privateKey) {
        String algorithm = privateKey.getAlgorithm();
        if (!"RSA".equalsIgnoreCase(algorithm) && !"EC".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException("文档签章只支持 RSA 或 SM2 私钥，实际为: " + algorithm);
        }
    }
}
