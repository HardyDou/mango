package io.mango.infra.docsign.enums;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * Visible stamp placement type.
 */
@LocalCapabilityContract
public enum StampType {

    /** One complete stamp on one page. */
    NORMAL,

    /** One stamp split across document pages. */
    RIDING
}
