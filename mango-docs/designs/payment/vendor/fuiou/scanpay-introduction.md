### 术语及定义
- `交易金额`: 金额以分为单位，不带小数点。

- `主扫`：用户主动扫付款二维码，也叫扫码支付，动态二维码支付。

- `被扫`：商家扫用户，也叫条码支付。

- `固码支付`：台卡、桌贴。使用公众号、服务窗统一下单。

- `刷卡支付`:(支付宝称当面付，被扫)：用户打开微信/支付宝，受理端扫码微信/支付宝条码/二维码完成资金划扣的支付方式。

- `公众号支付`：用户进入微信公众号通过 JSAPI 完成资金划扣的支付方式。

- `扫码支付(主扫)`：用户进入微信/支付宝启用“扫一扫”扫描商户二维码完成资金划扣的支付方式。

- `APP支付`：APP集成微信/支付宝SDK，用户点击跳转到微信/支付宝客户端后完成资金划扣的支付方式。


---

### 接口说明
`1.`被扫模式(商户扫用户微信/支付宝支付二维码):调用<条码支付>接口

`2.`台卡模式(用户使用微信/支付宝扫商户二维码):支付宝支付调用 3.3 接口(trade_type ==FWC)，微信支付调用 3.3 接口(trade_type==JSAPI)

`3.`下单成功之后请轮询调用订单查询接口查询订单状态,订单可能处于正在支付或取消状态,请轮询订单查询接口根据订单的状态执行相应的操作

`4.`订单状态请以查询为主，回调为辅

`5.`回调成功，请返回数字 1 说明收到回调，不然回调会继续发起，回调间隔时间30S，共回调5次

`6.`退款只有当商户账户有资金才能退款

`7.`提现交易的步骤为:查询可提现资金信息->查询提现手续费->发起提现

`8.`新开通提现和退货的商户,15 分钟后才能进行交易，不然会报无效商户

`9.`余额查询接口显示余额请使用账面余额字段，账面余额为已结算金额和未结算金额之和

`10.`订单查询可查询3天内的订单，超过3天的交易记录使用《历史查询接口v1.14.docx》文档中的历史查询接口查询。

`11.`使用公众号支付请注意以下几点:

* 入网，商户名称必须跟公众号主体名称一致
* 进去 https://mp.weixin.qq.com/ 设置相关开发参数 AppSecret，获取AppID
* 关于如何获取 openid,建议采取静默模式https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=4_4
* 关于微信版本号获取，5.0 以上才支持微信支付https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=7_5
* 在微信浏览器内打开H5调起支付APIhttps://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=7_7&index=6

`12.`支付宝交易，如何获取 buyer_id（即 user_id）:
https://doc.open.alipay.com/docs/doc.htm?spm=a219a.7629140.0.0.cPv%201DX&treeId=193&articleId=105193&docType=1#s10

`13.`银联商户出资，查询、回调的订单金额与原交易金额会不一致，此时返回的是实际清算金额

`14.`服务窗如何调起支付：https://docs.open.alipay.com/common/105591
使用接口返回的reserved_transaction_id调起支付即可

`15.`接口 3.3 公众号/服务窗统一下单 ，交易类型为小程序和公众号时，填写sub_appid 和 sub_openid。

`16.`微信 APP 下单使用"3.3 公众号/服务窗统一下单"接口，app支付时，prepayid字段取响应报文中的session_id 字段的值。

`17.`商户订单号UNIONPAY银联仅支持数字订单单号。

`18.`微信JSAPI，公众号支付完成后跳转说明：

关于支付完成后跳转页面升级为“点金计划”官方页面的详细接入指引：

点金计划产品介绍：https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/goldplan/chapter1_1.shtml

配置方式有两种：

* 自有渠道号合作方：可登录服务商平台——>服务商功能——>点金计划——>报名及配置。链接如下：https://pay.weixin.qq.com/index.php/xphp/cgoldplan_mgr/activity_index

* 使用富友渠道号：找对应运营同事开通点金计划和配置支付完成后所需要跳转的商家小票链接

支付完成后跳转页面需要对接微信官方js才可展示或跳转自定义页面，文档可参考：https://gtimg.wechatpay.cn/pay/download/goldplan/goldplan_product_description_v2.1.pdf 中3.4 商家小票开发指引
`19.` 云闪付的主被扫交易设置的订单关闭时间不生效。

### 通讯方式

通信采用HTTP协议,`POST表单`发送/`接收`xml格式的报文,`post参数名为req。`参数值为以下接口中各个字段组成的xml报文,xml在发送之前先进行一次`URLencode`.接收来自富友的返回报文时也需先做一个`URLdecode`
<br>`报文中有中文需要进行两次encode操作`

java参考代码：

```java
//解码
URLDecoder.decode(reqXml,"GBK");
//编码
URLEncoder.encode(rspXml,"GBK");
```

---

### 签名

采用`1024bit`长度密钥的RSA签名方式,加密算法使用`MD5WithRSA`，计算签名和验签的时候的需要使用`GBK编码。`

#### 计算方法
1. 将接口文档中的每-个字段( sign及reserved开头字段除外 ),，以字典顺序排序之后,按照key1=value1&key2=value2.....的顺序,进行拼接。
2. 对得到的字符串进行RSA签名/验签

!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`

#### 签名计算工具
[点击此处跳转](https://fundwx.payfuiouo2o.com/doc-yzf/app/doc/sign/index.html)

#### 签名实例
参数：
```xml 
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <reserved_fy_term_sn></reserved_fy_term_sn>
    <reserved_device_info></reserved_device_info>
    <term_id>12345678</term_id>
    <random_str>d0194c1024f180065d2434fa8b6a2f82</random_str>
    <reserved_limit_pay></reserved_limit_pay>
    <reserved_sub_appid></reserved_sub_appid>
    <ins_cd>08A9999999</ins_cd>
    <reserved_fy_term_type></reserved_fy_term_type>
    <version>1</version>
    <addn_inf></addn_inf>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <reserved_expire_minute></reserved_expire_minute>
    <term_ip>127.0.0.1</term_ip>
    <notify_url>https://mail.qq.com/cgi-bin/frame_html?sid=pEYG5nBgQiNVqANe&amp;r=4a6c47ad7d279a80630dec073cda96e2
    </notify_url>
    <order_amt>1</order_amt>
    <goods_des>卡盟测试</goods_des>
    <reserved_hb_fq_seller_percent></reserved_hb_fq_seller_percent>
    <curr_type></curr_type>
    <txn_begin_ts>20201201151802</txn_begin_ts>
    <sign>
        DXNr4zU78EF7R/dknRsVYNWwJ29M6l4YMZrOjqZbNYW9m90yq/n76lO4sS4r8rls0cEz6aRppfLaMkcoyyEqOCRyRnZoCdbi/EW8toU8UhIm+J0J+I9lFe+IqD6fOQ73c6iNzQ0Bvt9nTYvU2WvbKH4xIDntA0Sw7prNGJiq6iI=
    </sign>
    <goods_tag></goods_tag>
    <goods_detail>asasda</goods_detail>
    <reserved_fy_term_id></reserved_fy_term_id>
    <reserved_hb_fq_num></reserved_hb_fq_num>
    <mchnt_order_no>202012011518020724446</mchnt_order_no>
    <order_type>WECHAT</order_type>
</xml>
```

经过字典序排序后的字符串 (签名原文)string 为：
```text
addn_inf=&curr_type=&goods_des=卡盟测试&goods_detail=asasda&goods_tag=&ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=202012011518020724446&notify_url=https://mail.qq.com/cgi-bin/frame_html?sid=pEYG5nBgQiNVqANe&r=4a6c47ad7d279a80630dec073cda96e2&order_amt=1&order_type=WECHAT&random_str=d0194c1024f180065d2434fa8b6a2f82&term_id=12345678&term_ip=127.0.0.1&txn_begin_ts=20201201151802&version=1
```

---
---

### RSA密钥对生成工具
RSA密钥对生成工具，可以快速生成非对称加密的RSA密钥对（RSA Key pair），提供生成长度为`1024bit、2048bit`的`pkcs8`格式的密钥对。

[点击此处跳转](https://fundwx.payfuiouo2o.com/doc-yzf/app/doc/rsa/index.html)

---


### 测试环境参数 (`此为测试环境参数，生产参数验收之后即可得到，生产环境建议使用网易邮箱申请密钥，以免收不到`)
测试商户号：`0002900FB013834`<br />测试机构号：`08A9999999`<br />测试Appid：`wxfa089da95020ba1a`用于微信，若是支付宝，需要去注册支付宝服务商，然后申请生活号<br />富友测试密钥用于请求报文签名原文加密，富友公钥用于响应报文签名原文解密<br />测试地址见接口文档
针对接口第一次请求接口不支付，第二次请求相同订单报“商户订单号重复的解决办法”：接口加上Reserved_repeat_order=1这个字段，支持重复幂等下单。
*富友测试私钥--用于请求报文签名原文加密*
```text
PRIVATE KEY : 
MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJgAzD8fEvBHQTyxUEeK963mjziM
WG7nxpi+pDMdtWiakc6xVhhbaipLaHo4wVI92A2wr3ptGQ1/YsASEHm3m2wGOpT2vrb2Ln/S7lz1
ShjTKaT8U6rKgCdpQNHUuLhBQlpJer2mcYEzG/nGzcyalOCgXC/6CySiJCWJmPyR45bJAgMBAAEC
gYBHFfBvAKBBwIEQ2jeaDbKBIFcQcgoVa81jt5xgz178WXUg/awu3emLeBKXPh2i0YtN87hM/+J8
fnt3KbuMwMItCsTD72XFXLM4FgzJ4555CUCXBf5/tcKpS2xT8qV8QDr8oLKA18sQxWp8BMPrNp0e
pmwun/gwgxoyQrJUB5YgZQJBAOiVXHiTnc3KwvIkdOEPmlfePFnkD4zzcv2UwTlHWgCyM/L8SCAF
clXmSiJfKSZZS7o0kIeJJ6xe3Mf4/HSlhdMCQQCnTow+TnlEhDTPtWa+TUgzOys83Q/VLikqKmDz
kWJ7I12+WX6AbxxEHLD+THn0JGrlvzTEIZyCe0sjQy4LzQNzAkEAr2SjfVJkuGJlrNENSwPHMugm
vusbRwH3/38ET7udBdVdE6poga1Z0al+0njMwVypnNwy+eLWhkhrWmpLh3OjfQJAI3BV8JS6xzKh
5SVtn/3Kv19XJ0tEIUnn2lCjvLQdAixZnQpj61ydxie1rggRBQ/5vLSlvq3H8zOelNeUF1fT1QJA
DNo+tkHVXLY9H2kdWFoYTvuLexHAgrsnHxONOlSA5hcVLd1B3p9utOt3QeDf6x2i1lqhTH2w8gzj
vsnx13tWqg==
```

*富友测试公钥--用于响应报文签名原文解密*
```text
PUBLIC KEY : 
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBv9K+jiuHqXIehX81oyNSD2RfVn+KTPb7NRT5HDPFE35CjZJd7Fu40r0U2Cp7Eyhayv/mRS6ZqvBT/8tQqwpUExTQQBbdZjfk+efb9bF9a+uCnAg0RsuqxeJ2r/rRTsORzVLJy+4GKcv06/p6CcBc5BI1gqSKmyyNBlgfkxLYewIDAQAB
```


---

### 测试注意(重要重要！！！必看！！！！！)
!> `注意：`测试环境提供的银行间连生产测试账号是生产环境用来进行支付业务体验的账号，`交易中产生的一切信息均为生产环境数据`。用户支付需使用正式版微信、支付宝登陆用户的支付账号，使用用户的账号余额或绑定的银行卡等支付渠道进行支付。由于测试账号业务特点，不会产生清算资金划拨。所以`使用测试账号时请务必使用小于1元的小金额`，并请务必在支付`当日`完成退款。否则因隔日退款失败造成的测试资金损失由测试机构承担，并视为放弃资金。由于该账号为测试体验账号，所以`禁止将该账号用于生产业务用途，禁止在商户真实业务中使用`。对于将测试账号误用在商户真实业务造成的商户、用户等资金损失，由测试机构承担。
`使用该测试账号则视为同意上述使用协议。`


---


### goods_detail说明字段

*支付宝*

```text
[{
    "goods_id":"apple-01",//商品编号 必填
    "alipay_goods_id":"20010001",//支付宝定义的统一商品编号 非必填
    "goods_name":"单品",//商品名称 必填
    "quantity":"1",//商品数量 必填
    "price":"0.21",//商品单价，单位为元 必填
    "goods_category":"",//商品类目 非必填
    "categories_tree":"124868003|126232002|126252004",//商品类目树，从商品类目根节点到叶子节点的类目 id 组成，类目 id 值使用|分割
    "body":"",//商品描述信息 非必填
    "show_url":"",//商品的展示地址 非必填
}]
```



*微信*

```text
{
    "cost_price":608800, //订单原价 非必填 int 单位分
    "receipt_id":"wx123",//商品小票ID 非必填 String
    //注意goods_detail字段的格式为"goods_detail":[{}],较多商户写成"goods_detail":{}
    "goods_detail":
    [{
        "goods_id":"12345",//商品编码  必填 String
        "wxpay_goods_id":"1001",//微信侧商品编码 非必填 String
        "goods_name":"",//商品名称  非必填 String
        "quantity":1,  //商品数量 必填 int
        "price":528800 //商品单价 必填 int
    },
    {
        "goods_id":"23456",
        "wxpay_goods_id":"1002",
        "goods_name":"iPhone6s 32G",
        "quantity":1,
        "price":608800
    }]
}
```

*银联二维码* **`只支持条码支付`**

```text
{
    "orderInfo":  //订单信息
    {
        "title" :"日用品",  //订单标题。必填
        "dctAmount" :"10000",//可优惠金额 非必填
        "addnInfo" :"屈臣氏(人民广场)店"  //附加信息 非必填
    },
    "goodsInfo": //商品信息
    [{
        "id": "1234567890",  //商品编号 必填
        "name": "商品 1",//商品名称 必填
        "price": "500",//商品单价 必填 单位分
        "quantity": "1", //商品数目 必填
        "category": "类目 1", //商品类目 非必填
        "addnInfo": "商品图片 http://www.95516.com/xxx.jpg" //附加信息 非必填
    },
    {
        "id": "1234567891",
        "name": "商品 2",
        "price": "1000",
        "quantity": "2"
    }]
}
```

---


### reserved_promotion_detail说明字段示例

```text
[{
    "activity_id":"12345", //活动id
    "amount":1, //优惠券面额 单位分
    "merchant_contribute":1,//商户出资 单位分
    "name":"BSagiiBBXm", //优惠名称
    "other_contribute":0, //其他出资 单位分
    "promotion_id":"10000", //券ID
    "scope":"GLOBAL", //优惠范围 GLOBAL-全场代金券；SINGLE-单品优惠
    "type":"DISCOUNT", //优惠类型 COUPON-代金券，需要走结算资金的充值型代金券；DISCOUNT-优惠券，不走结算资金的免充值型优惠券
    "wxpay_contribute":0,//微信出资 单位分
    "goods_detail":[
                {
                    "goods_remark":"商品备注信息",  //商品备注
                    "quantity":1, //商品数量
                    "discount_amount":1, //商品优惠金额
                    "goods_id":"M1006", //商品id
                    "price":100 //商品单价
                },
                {
                    "goods_remark":"商品备注信息",
                    "quantity":1,
                    "discount_amount":1,
                    "goods_id":"M1006",
                    "price":100
                }
            ]
}]
```


---

### reserved_scene_info场景信息说明字段示例

|序号|参数名|出现要求|型态|长度| 说明                                                                                              |
|:-----|:-----|:-----|:-----|:-----|:------------------------------------------------------------------------------------------------|
|01|id|必填|String|32|门店id|
|02|name|非必填|String|64|门店名称|
|03|area_code|非必填|String|6|门店行政区划码 |
|04|address|非必填|String|128|门店详细地址|

```text
{
"store_info" : {
    "id": "SZTX001",
    "name": "腾大餐厅",
    "area_code": "440305",
    "address": "科技园中一路腾讯大厦" }
}
```



































































