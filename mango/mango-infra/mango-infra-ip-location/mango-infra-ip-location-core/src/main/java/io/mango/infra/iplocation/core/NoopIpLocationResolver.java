package io.mango.infra.iplocation.core;

import io.mango.infra.iplocation.api.IpLocation;
import io.mango.infra.iplocation.api.IpLocationResolver;

/**
 * 空 IP 归属地解析器。
 */
public class NoopIpLocationResolver implements IpLocationResolver {

    @Override
    public IpLocation resolve(String ip) {
        IpLocation location = IpLocation.empty(ip);
        location.setPrivateAddress(IpAddressClassifier.isPrivateOrLocal(ip));
        location.setSource("noop");
        return location;
    }
}
