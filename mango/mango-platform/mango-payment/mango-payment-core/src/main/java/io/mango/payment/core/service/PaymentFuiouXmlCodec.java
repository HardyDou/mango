package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentFuiouXmlCodec {

    public String encode(Map<String, String> fields) {
        Require.notNull(fields, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友 XML 字段不能为空");
        StringBuilder builder = new StringBuilder("<?xml version=\"1.0\" encoding=\"GBK\" standalone=\"yes\"?><xml>");
        fields.forEach((key, value) -> builder.append('<').
                append(key).
                append('>').
                append(escape(value)).
                append("</").
                append(key).
                append('>'));
        return builder.append("</xml>").toString();
    }

    public Map<String, String> decode(String xml) {
        Require.notBlank(xml, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友响应报文不能为空");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            Map<String, String> fields = new LinkedHashMap<>();
            for (int index = 0; index < root.getChildNodes().getLength(); index++) {
                if (root.getChildNodes().item(index) instanceof Element element) {
                    fields.put(element.getTagName(), element.getTextContent());
                }
            }
            return fields;
        } catch (Exception ex) {
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友响应报文解析失败", ex);
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").
                replace("<", "&lt;").
                replace(">", "&gt;").
                replace("\"", "&quot;").
                replace("'", "&apos;");
    }
}
