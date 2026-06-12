package com.test;

/**
 * Created by Ricky on 2016/11/19.
 */
public class Const {

    //编码
    public static String charset = "GBK";

    //机构私钥
    public static final String INS_PRIVATE_KEY ="MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJgAzD8fEvBHQTyxUEeK963mjziM\n" +
           "WG7nxpi+pDMdtWiakc6xVhhbaipLaHo4wVI92A2wr3ptGQ1/YsASEHm3m2wGOpT2vrb2Ln/S7lz1\n" +
           "ShjTKaT8U6rKgCdpQNHUuLhBQlpJer2mcYEzG/nGzcyalOCgXC/6CySiJCWJmPyR45bJAgMBAAEC\n" +
           "gYBHFfBvAKBBwIEQ2jeaDbKBIFcQcgoVa81jt5xgz178WXUg/awu3emLeBKXPh2i0YtN87hM/+J8\n" +
           "fnt3KbuMwMItCsTD72XFXLM4FgzJ4555CUCXBf5/tcKpS2xT8qV8QDr8oLKA18sQxWp8BMPrNp0e\n" +
           "pmwun/gwgxoyQrJUB5YgZQJBAOiVXHiTnc3KwvIkdOEPmlfePFnkD4zzcv2UwTlHWgCyM/L8SCAF\n" +
           "clXmSiJfKSZZS7o0kIeJJ6xe3Mf4/HSlhdMCQQCnTow+TnlEhDTPtWa+TUgzOys83Q/VLikqKmDz\n" +
           "kWJ7I12+WX6AbxxEHLD+THn0JGrlvzTEIZyCe0sjQy4LzQNzAkEAr2SjfVJkuGJlrNENSwPHMugm\n" +
           "vusbRwH3/38ET7udBdVdE6poga1Z0al+0njMwVypnNwy+eLWhkhrWmpLh3OjfQJAI3BV8JS6xzKh\n" +
           "5SVtn/3Kv19XJ0tEIUnn2lCjvLQdAixZnQpj61ydxie1rggRBQ/5vLSlvq3H8zOelNeUF1fT1QJA\n" +
           "DNo+tkHVXLY9H2kdWFoYTvuLexHAgrsnHxONOlSA5hcVLd1B3p9utOt3QeDf6x2i1lqhTH2w8gzj\n" +
           "vsnx13tWqg==";
    //机构公钥
    public static final String INS_PUBLIC_KEY="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCYAMw/HxLwR0E8sVBHivet5o84jFhu58aYvqQzHbVompHOsVYYW2oqS2h6OMFSPdgNsK96bRkNf2LAEhB5t5tsBjqU9r629i5/0u5c9UoY0ymk/FOqyoAnaUDR1Li4QUJaSXq9pnGBMxv5xs3MmpTgoFwv+gskoiQliZj8keOWyQIDAQAB";


   //富友公钥  用于验签
    public static final String FY_PUBLIC_KEY ="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBv9K+jiuHqXIehX81oyNSD2RfVn+KTPb7NRT5HDPFE35CjZJd7Fu40r0U2Cp7Eyhayv/mRS6ZqvBT/8tQqwpUExTQQBbdZjfk+efb9bF9a+uCnAg0RsuqxeJ2r/rRTsORzVLJy+4GKcv06/p6CcBc5BI1gqSKmyyNBlgfkxLYewIDAQAB";


    //机构号
    public static String ins_cd = "08A9999999";

    //商户号
    public static String mchnt_cd = "0002900F0370542";//0002900F0370542

    //终端号
    public static String term_id = "";

    //终端IP
    public static String term_ip = "127.0.0.1";

    //异步通知
    public static String notify_url = "http://www.wrx.cn";

    //下单
    public static String fuiou_21_url = "https://fundwx.fuiou.com/preCreate";
    //扫码
    public static String fuiou_22_url = "https://fundwx.fuiou.com/micropay";
    //公众号/服务窗统一下单
    public static String fuiou_23_url = "https://fundwx.fuiou.com/wxPreCreate";
    //退款
    public static String fuiou_24_url = "https://fundwx.fuiou.com/commonRefund";
    //资金划拨信息
//    public static String fuiou_xx_url = "https://fundwx.fuiou.com/queryChnlPayAmt";
    //查询可提现资金
    public static String fuiou_27_url = "https://fundwx.fuiou.com/queryWithdrawAmt";
    //查询手续费
    public static String fuiou_28_url = "https://fundwx.fuiou.com/queryFeeAmt";
    //提现
    public static String fuiou_29_url = "https://fundwx.fuiou.com/withdraw";

    //查询
    public static String fuiou_30_url = "https://fundwx.fuiou.com/commonQuery";

}
