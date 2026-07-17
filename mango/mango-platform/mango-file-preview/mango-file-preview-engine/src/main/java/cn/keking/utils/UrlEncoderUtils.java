package cn.keking.utils;

import java.util.BitSet;

public class UrlEncoderUtils {

    private static final BitSet DONT_NEED_ENCODING;

    static {
        DONT_NEED_ENCODING = new BitSet();
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            DONT_NEED_ENCODING.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            DONT_NEED_ENCODING.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            DONT_NEED_ENCODING.set(i);
        }
        DONT_NEED_ENCODING.set('+');
        /*
         * 这里会有误差,比如输入一个字符串 123+456,它到底是原文就是123+456还是123 456做了urlEncode后的内容呢？<br>
         * 其实问题是一样的，比如遇到123%2B456,它到底是原文即使如此，还是123+456 urlEncode后的呢？ <br>
         * 在这里，我认为只要符合urlEncode规范的，就当作已经urlEncode过了<br>
         * 毕竟这个方法的初衷就是判断string是否urlEncode过<br>
         */

        DONT_NEED_ENCODING.set('-');
        DONT_NEED_ENCODING.set('_');
        DONT_NEED_ENCODING.set('.');
        DONT_NEED_ENCODING.set('*');
    }

    private UrlEncoderUtils() {
    }

    /**
     * 判断str是否urlEncoder.encode过<br>
     * 经常遇到这样的情况，拿到一个URL,但是搞不清楚到底要不要encode.<Br>
     * 不做encode吧，担心出错，做encode吧，又怕重复了<Br>
     */
    public static boolean hasUrlEncoded(String str) {

        /*
         * 支持JAVA的URLEncoder.encode出来的string做判断。 即: 将' '转成'+' <br>
         * 0-9a-zA-Z保留 <br>
         * '-'，'_'，'.'，'*'保留 <br>
         * 其他字符转成%XX的格式，十六进制字符大小写均合法。
         * 只要存在合法编码字节，原始字符不会使判断失效；非法百分号序列不能进入URLDecoder。
         */
        boolean hasEncodedOctet = false;
        boolean needEncode = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '%') {
                if ((i + 2) >= str.length()
                        || !isDigit16Char(str.charAt(i + 1))
                        || !isDigit16Char(str.charAt(i + 2))) {
                    return false;
                }
                hasEncodedOctet = true;
                i += 2;
                continue;
            }
            if (!DONT_NEED_ENCODING.get(c)) {
                needEncode = true;
            }
        }

        return hasEncodedOctet || !needEncode;
    }

    /**
     * 判断c是否是16进制的字符
     */
    private static boolean isDigit16Char(char c) {
        return isBetween(c, '0', '9') || isBetween(c, 'A', 'F') || isBetween(c, 'a', 'f');
    }

    private static boolean isBetween(char value, char start, char end) {
        return value >= start && value <= end;
    }
}
