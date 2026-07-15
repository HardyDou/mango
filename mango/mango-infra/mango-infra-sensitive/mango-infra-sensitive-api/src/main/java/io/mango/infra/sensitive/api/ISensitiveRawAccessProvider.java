package io.mango.infra.sensitive.api;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * Checks whether the current caller can view unmasked sensitive output.
 */
@LocalCapabilityContract
public interface ISensitiveRawAccessProvider {

    /**
     * Returns true when the current caller can view the raw value.
     *
     * @param authority authority configured for raw sensitive output
     * @return true when raw output is allowed
     */
    boolean canViewRaw(String authority);
}
