package io.mango.infra.iplocation.core.ip2region;

import io.mango.infra.iplocation.api.IpLocation;
import io.mango.infra.iplocation.api.IpLocationResolver;
import io.mango.infra.iplocation.core.IpAddressClassifier;
import org.lionsoul.ip2region.xdb.Searcher;

/**
 * ip2region xdb 归属地解析器。
 */
public class Ip2RegionXdbLocationResolver implements IpLocationResolver, AutoCloseable {

    private static final int PROVINCE_INDEX = 2;
    private static final int CITY_INDEX = 3;
    private static final int ISP_INDEX = 4;

    private final Searcher searcher;

    public Ip2RegionXdbLocationResolver(Searcher searcher) {
        this.searcher = searcher;
    }

    @Override
    public IpLocation resolve(String ip) {
        if (IpAddressClassifier.isBlank(ip) || IpAddressClassifier.isInvalid(ip)) {
            return IpLocation.empty(ip);
        }
        IpLocation location = IpLocation.empty(ip);
        location.setPrivateAddress(IpAddressClassifier.isPrivateOrLocal(ip));
        location.setSource("ip2region");
        if (location.isPrivateAddress()) {
            return location;
        }
        try {
            String region = search(ip.trim());
            fill(location, region);
            location.setResolved(region != null && !region.isBlank());
            return location;
        } catch (Exception e) {
            return location;
        }
    }

    private synchronized String search(String ip) throws Exception {
        return searcher.search(ip);
    }

    private void fill(IpLocation location, String region) {
        location.setRegion(region);
        if (region == null || region.isBlank()) {
            return;
        }
        String[] parts = region.split("\\|", -1);
        location.setCountry(part(parts, 0));
        location.setProvince(part(parts, PROVINCE_INDEX));
        location.setCity(part(parts, CITY_INDEX));
        location.setIsp(part(parts, ISP_INDEX));
    }

    private String part(String[] parts, int index) {
        if (parts.length <= index) {
            return null;
        }
        String value = parts[index];
        if (value == null || value.isBlank() || "0".equals(value)) {
            return null;
        }
        return value;
    }

    @Override
    public void close() throws Exception {
        searcher.close();
    }
}
