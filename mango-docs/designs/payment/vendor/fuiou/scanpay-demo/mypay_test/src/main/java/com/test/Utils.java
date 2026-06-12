package com.test;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * Created by Ricky on 2016/11/20.
 */
public class Utils {

    public static Map<String, String> paraFilter(Map<String, String> map) {

        Map<String, String> result = new HashMap<>();

        if (map == null || map.size() <= 0) {
            return result;
        }

        for (String key : map.keySet()) {
            String value = map.get(key);
            if (key.equalsIgnoreCase("sign") || (key.length() >= 8 && key.substring(0, 8).equalsIgnoreCase("reserved"))) {
                continue;
            }
            result.put(key, value);
        }

        return result;
    }

    public static String createLinkString(Map<String, String> map) {

        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);

        String prestr = "";

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = map.get(key);

            if (i == keys.size() - 1) {
                //拼接时，不包括最后一个&字符
                prestr = prestr + key + "=" + value;
            } else {
                prestr = prestr + key + "=" + value + "&";
            }
        }

        return prestr;
    }

    public static String getSign(Map<String, String> map) throws InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, IOException {

        Map<String, String> mapNew = paraFilter(map);

        String preSignStr = createLinkString(mapNew);

        System.out.println("==============================待签名字符串==============================\r\n" + preSignStr);

        String sign = Sign.sign(preSignStr, Const.INS_PRIVATE_KEY);

        sign = sign.replace("\r\n", "");

        System.out.println("==============================签名字符串==============================\r\n" + sign);

        return sign;
    }

    public static Boolean verifySign(Map<String, String> map, String sign) throws Exception {

        Map<String, String> mapNew = paraFilter(map);

        String preSignStr = createLinkString(mapNew);

        return Sign.verify(preSignStr.getBytes(Const.charset), Const.FY_PUBLIC_KEY, sign);
    }

    public static Map<String,String> xmlStr2Map(String xmlStr){
        Map<String,String> map = new HashMap<String,String>();
        Document doc;
        try {
            doc = DocumentHelper.parseText(xmlStr);
            Element resroot = doc.getRootElement();
            List children = resroot.elements();
            if(children != null && children.size() > 0) {
                for(int i = 0; i < children.size(); i++) {
                    Element child = (Element)children.get(i);
//                    map.put(child.getName(), child.getTextTrim());//会将换行符转换成空格
                    map.put(child.getName(), child.getStringValue().trim());
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void main(String[] args) {
        String xmlStr = "<?xml version=\"1.0\" encoding=\"GBK\" standalone=\"yes\"?>\n" +
                "<xml>\n" +
                "    <sign>1\n2 3   4:5</sign>\n" +
                "</xml>";
        xmlStr2Map(xmlStr);
    }

}
