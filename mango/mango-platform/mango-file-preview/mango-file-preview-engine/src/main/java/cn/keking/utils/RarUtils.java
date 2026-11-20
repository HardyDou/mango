package cn.keking.utils;
import cn.keking.config.ConfigConstants;
import cn.keking.service.ZtreeNodeVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author : Gao
 * create : 2023-04-08
 **/
public class RarUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RarUtils.class);

    public static byte[] getUTF8BytesFromGBKString(String gbkStr) {
        int n = gbkStr.length();
        byte[] utfBytes = new byte[3 * n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            int m = gbkStr.charAt(i);
            if (m < 128 && m >= 0) {
                utfBytes[k++] = (byte) m;
                continue;
            }
            utfBytes[k++] = (byte) (0xe0 | (m >> 12));
            utfBytes[k++] = (byte) (0x80 | ((m >> 6) & 0x3f));
            utfBytes[k++] = (byte) (0x80 | (m & 0x3f));
        }
        if (k < utfBytes.length) {
            byte[] tmp = new byte[k];
            System.arraycopy(utfBytes, 0, tmp, 0, k);
            return tmp;
        }
        return utfBytes;
    }

    public static String getUtf8String(String str) {
        if (str != null && str.length() > 0) {
            String needEncodeCode = "ISO-8859-1";
            String neeEncodeCode = "ISO-8859-2";
            String gbkEncodeCode = "GBK";
            try {
                if (Charset.forName(needEncodeCode).newEncoder().canEncode(str)) {
                    str = new String(str.getBytes(needEncodeCode), StandardCharsets.UTF_8);
                }
                if (Charset.forName(neeEncodeCode).newEncoder().canEncode(str)) {
                    str = new String(str.getBytes(neeEncodeCode), StandardCharsets.UTF_8);
                }
                if (Charset.forName(gbkEncodeCode).newEncoder().canEncode(str)) {
                    str = new String(getUTF8BytesFromGBKString(str), StandardCharsets.UTF_8);
                }
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("Failed to convert string encoding: {}", str, e);
            }
        }
        return str;
    }
    /**
     * 判断是否是中日韩文字
     */
    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
    public static boolean judge(char c){
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
    }
    public static String specialSymbols(String str) {
        //去除压缩包文件字符串中特殊符号
        Pattern p = Pattern.compile("\\s|\t|\r|\n|\\+|#|&|=|�|\\p{P}");
  //    Pattern p = Pattern.compile("\\s|\\+|#|&|=|\\p{P}");
        Matcher m = p.matcher(str);
        return m.replaceAll("");
    }
    public static boolean isMessyCode(String strName) {
        //去除字符串中的空格 制表符 换行 回车
        strName = specialSymbols(strName);
        //处理之后转换成字符数组
        char[] ch = strName.trim().toCharArray();
        for (char c : ch) {
            //判断是否是数字或者英文字符
            if (!judge(c)) {
                //判断是否是中日韩文
                if (!isChinese(c)) {
                    //如果不是数字或者英文字符也不是中日韩文则表示是乱码返回true
                    return true;
                }
            }
        }
        //表示不是乱码 返回false
        return false;
    }

    /**
     * 读取文件目录树。
     *
     * <p>根目录为解包根 {@link ConfigConstants#getDecompressDir()}，该目录位于 file-dir 之外，不参与
     * {@code /preview-file/**} 与 {@code /**} 静态映射。节点 id 相对解包根，可直接作为条目端点
     * {@code GET /compressed-file} 的 {@code kkCompressfilepath}。
     */
    public static List<ZtreeNodeVo> getTree(String rootPath) {
        List<ZtreeNodeVo> nodes = new ArrayList<>();
        Path decompressRoot = Paths.get(ConfigConstants.getDecompressDir()).toAbsolutePath().normalize();
        Path root = decompressRoot.resolve(rootPath).normalize();
        if (!root.startsWith(decompressRoot)) {
            throw new SecurityException("Unsafe decompress path detected: " + rootPath);
        }
        nodes.add(traverse(root, decompressRoot));
        return nodes;
    }
    private static ZtreeNodeVo traverse(Path file, Path decompressRoot) {
        ZtreeNodeVo pathNodeVo = new ZtreeNodeVo();
        pathNodeVo.setId(relativePath(file, decompressRoot));
        pathNodeVo.setName(file.getFileName() == null ? "" : file.getFileName().toString());
        Path parent = file.getParent();
        pathNodeVo.setPid(parent == null ? "" : relativePath(parent, decompressRoot));
        if (Files.isDirectory(file)) {
            List<ZtreeNodeVo> subNodeVos;
            try (Stream<Path> subFiles = Files.list(file)) {
                subNodeVos = subFiles.map(subFile -> traverse(subFile, decompressRoot)).collect(Collectors.toList());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read decompress directory: " + file, e);
            }
            pathNodeVo.setChildren(subNodeVos);
        }
        return pathNodeVo;
    }

    private static String relativePath(Path path, Path decompressRoot) {
        return decompressRoot.relativize(path).toString().replace("\\", "/");
    }
}
