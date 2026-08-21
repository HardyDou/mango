package io.mango.infra.docsign.enums;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * Page edge used for a riding stamp.
 */
@LocalCapabilityContract
public enum StampSide {

    /** Left page edge. */
    LEFT,

    /** Right page edge. */
    RIGHT,

    /** Top page edge. */
    TOP,

    /** Bottom page edge. */
    BOTTOM
}
