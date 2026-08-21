package io.mango.infra.docsign.enums;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * Controlled signature algorithms supported by document signing providers.
 */
@LocalCapabilityContract
public enum DocumentSignatureAlgorithm {

    /** Select the format-compatible default from the supplied signing key. */
    AUTO,

    /** RSA PKCS#1 v1.5 with a SHA-256 digest. */
    SHA256_WITH_RSA,

    /** RSA PKCS#1 v1.5 with a SHA-384 digest. */
    SHA384_WITH_RSA,

    /** RSA PKCS#1 v1.5 with a SHA-512 digest. */
    SHA512_WITH_RSA,

    /** RSASSA-PSS with SHA-256 and matching MGF1 parameters. */
    SHA256_WITH_RSA_PSS,

    /** Chinese commercial cryptography SM2 signature with an SM3 digest. */
    SM3_WITH_SM2
}
