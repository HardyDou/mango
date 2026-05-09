package io.mango.infra.iplocation.core.support;

import io.mango.infra.iplocation.api.IpLocation;

/**
 * IP 归属地展示格式化。
 */
public final class IpLocationFormatter {

    private IpLocationFormatter() {
    }

    public static String displayText(IpLocation location) {
        return location == null ? "未知" : location.displayText();
    }
}
