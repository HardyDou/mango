package io.mango.infra.iplocation.starter;

import org.lionsoul.ip2region.xdb.Searcher;

import java.nio.charset.StandardCharsets;

final class XdbFixture {

    private XdbFixture() {
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
