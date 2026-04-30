package io.mango.infra.crypto.impl.digest;

import io.mango.infra.crypto.impl.IDigester;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 基于 JDK 的 SHA-256 摘要实现。
 */
public class Sha256Digester implements IDigester {

    @Override
    public String digest(String data) {
        return bytesToHex(digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public byte[] digest(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("当前运行环境不支持 SHA-256", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
