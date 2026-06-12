package com.test;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public class Main
{
    public static void main( String[] args ) throws Exception {

        //2.1	统一下单
        //    run(Builder.buildFuiou21(), Const.fuiou_21_url);
        //2.2	条码支付
          run(Builder.buildFuiou22(), Const.fuiou_22_url);

        //2.3 公众号/服务窗统一下单  具体请阅读“公众号、服务窗下单必读”
    //    run(Builder.buildFuiou23(), Const.fuiou_23_url);

    }

    public static void run(Map<String, String> map, String url) throws Exception{
        Map<String, String> reqs = new HashMap<>();
        Map<String, String> nvs = new HashMap<>();

        reqs.putAll(map);

        String sign = Utils.getSign(reqs);
        reqs.put("sign", sign);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("xml");

        Iterator it=reqs.keySet().iterator();
        while(it.hasNext()){
            String key = it.next().toString();
            String value = reqs.get(key);

            root.addElement(key).addText(value);
        }

        // String reqBody = doc.getRootElement().asXML();
       String reqBody = "<?xml version=\"1.0\" encoding=\"GBK\" standalone=\"yes\"?>" + doc.getRootElement().asXML();

        System.out.println("==============================待编码字符串==============================\r\n" + reqBody);

        reqBody = URLEncoder.encode(reqBody, Const.charset);

        System.out.println("==============================编码后字符串==============================\r\n" + reqBody);

        nvs.put("req", reqBody);

        StringBuffer result = new StringBuffer("");

        HttpUtils httpUtils = new HttpUtils();
        httpUtils.post(url, nvs, result);
        String rspXml = URLDecoder.decode(result.toString(),Const.charset);
        System.out.println("==============================响应报文==============================\r\n"+rspXml);
        //响应报文验签
        Map<String,String> resMap = Utils.xmlStr2Map(rspXml);
        String str = resMap.get("sign");
        System.out.println("str:"+str);
        System.out.println("验签结果："+Utils.verifySign(resMap, str));



    }





}
