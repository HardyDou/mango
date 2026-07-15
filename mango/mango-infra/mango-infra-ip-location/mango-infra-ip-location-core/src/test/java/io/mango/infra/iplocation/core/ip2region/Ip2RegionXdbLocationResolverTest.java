package io.mango.infra.iplocation.core.ip2region;

import io.mango.infra.iplocation.api.IpLocation;
import org.junit.jupiter.api.Test;
import org.lionsoul.ip2region.xdb.Searcher;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Ip2RegionXdbLocationResolverTest {

    @Test
    void shouldResolvePublicIpv4FromRealXdbSearcher() throws Exception {
        try (Ip2RegionXdbLocationResolver resolver = resolverWithRegion("中国|0|浙江省|杭州市|电信")) {
            IpLocation result = resolver.resolve("8.8.8.8");

            assertThat(result.isResolved()).isTrue();
            assertThat(result.getCountry()).isEqualTo("中国");
            assertThat(result.getProvince()).isEqualTo("浙江省");
            assertThat(result.getCity()).isEqualTo("杭州市");
            assertThat(result.getIsp()).isEqualTo("电信");
            assertThat(result.getSource()).isEqualTo("ip2region");
        }
    }

    @Test
    void shouldNotMarkBlankXdbResultAsResolved() throws Exception {
        try (Ip2RegionXdbLocationResolver resolver = resolverWithRegion("")) {
            IpLocation result = resolver.resolve("8.8.8.8");

            assertThat(result.isResolved()).isFalse();
            assertThat(result.displayText()).isEqualTo("未知");
        }
    }

    @Test
    void shouldShortCircuitPrivateAndInvalidAddresses() throws Exception {
        try (Ip2RegionXdbLocationResolver resolver = resolverWithRegion("中国|0|浙江省|杭州市|电信")) {
            IpLocation privateResult = resolver.resolve("127.0.0.1");
            IpLocation invalidResult = resolver.resolve("localhost");

            assertThat(privateResult.isPrivateAddress()).isTrue();
            assertThat(privateResult.isResolved()).isFalse();
            assertThat(invalidResult.isResolved()).isFalse();
        }
    }

    private Ip2RegionXdbLocationResolver resolverWithRegion(String region) throws Exception {
        return new Ip2RegionXdbLocationResolver(Searcher.newWithBuffer(xdb(region)));
    }

    static byte[] xdb(String region) {
        byte[] data = region.getBytes(StandardCharsets.UTF_8);
        int segmentOffset = Searcher.HeaderInfoLength
                + Searcher.VectorIndexRows * Searcher.VectorIndexCols * Searcher.VectorIndexSize;
        int dataOffset = segmentOffset + Searcher.SegmentIndexSize;
        byte[] xdb = new byte[dataOffset + data.length];
        for (int offset = Searcher.HeaderInfoLength; offset < segmentOffset; offset += Searcher.VectorIndexSize) {
            writeInt(xdb, offset, segmentOffset);
            writeInt(xdb, offset + 4, segmentOffset);
        }
        writeInt(xdb, segmentOffset, 0);
        writeInt(xdb, segmentOffset + 4, -1);
        writeShort(xdb, segmentOffset + 8, data.length);
        writeInt(xdb, segmentOffset + 10, dataOffset);
        System.arraycopy(data, 0, xdb, dataOffset, data.length);
        return xdb;
    }

    private static void writeInt(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
        target[offset + 2] = (byte) (value >>> 16);
        target[offset + 3] = (byte) (value >>> 24);
    }

    private static void writeShort(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
    }
}
