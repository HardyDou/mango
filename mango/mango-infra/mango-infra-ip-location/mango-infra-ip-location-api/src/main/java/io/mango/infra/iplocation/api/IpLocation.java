package io.mango.infra.iplocation.api;

import java.util.StringJoiner;

/**
 * IP 归属地解析结果。
 */
public class IpLocation {

    private String ip;
    private String country;
    private String region;
    private String province;
    private String city;
    private String isp;
    private String source;
    private boolean privateAddress;
    private boolean resolved;

    public static IpLocation empty(String ip) {
        IpLocation location = new IpLocation();
        location.setIp(ip);
        location.setResolved(false);
        return location;
    }

    public String displayText() {
        StringJoiner joiner = new StringJoiner(" ");
        addIfPresent(joiner, country);
        addIfPresent(joiner, province);
        addIfPresent(joiner, city);
        addIfPresent(joiner, isp);
        String text = joiner.toString();
        return text.isBlank() ? "未知" : text;
    }

    private void addIfPresent(StringJoiner joiner, String value) {
        if (value != null && !value.isBlank() && !"0".equals(value)) {
            joiner.add(value.trim());
        }
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isPrivateAddress() {
        return privateAddress;
    }

    public void setPrivateAddress(boolean privateAddress) {
        this.privateAddress = privateAddress;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
