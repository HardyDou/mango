package io.mango.infra.docsign.vo;

import io.mango.common.contract.LocalCapabilityContract;

import java.time.Instant;

/**
 * Validation result for one document signature.
 */
@LocalCapabilityContract
public record SignatureValidationVO(
        String id,
        String type,
        String algorithm,
        String signerSubject,
        Instant signingTime,
        boolean cryptographicallyValid,
        boolean documentIntegrityValid,
        boolean certificateTimeValid,
        boolean trusted,
        boolean coversCurrentDocument,
        boolean valid,
        String message) {
}
