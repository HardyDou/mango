## 编写目的
封装扫码支付功能接口，对外提供给商户

---

## 术语及定义
交易金额: 金额以分为单位，不带小数点

---

## 接口说明
1. 被扫模式(商户扫用户微信/支付宝支付二维码):调用<条码支付>接口
2. 主扫模式(用户使用微信/支付宝扫商户二维码):支付宝支付调用 2.6接口(trade_type==FWC)，微信支付调用 2.6接口(trade_type==JSAPI)
3. 下单成功之后请轮询调用订单查询接口查询订单状态，订单可能处于正在支付或取消状态，请轮询订单查询接口根据订单的状态执行相应的操作
4. 订单状态请以主动查询为主，回调为辅
5. 回调成功，请返回数字 1 说明收到回调，不然回调会继续发起，回调间隔时间 30S，共回调 5 次
6. 退款只有当商户账户有资金才能退款
7. 提现交易的步骤为:查询可提现资金信息 -> 查询提现手续费 -> 发起提现
8. 新开通提现和退货的商户，D+1 日才能进行交易，不然会报无效商户
9. 余额查询接口显示余额请使用账面余额字段，账面余额为已结算金额和未结算金额之和
10. 订单查询可查询 3 天内的订单
11. 使用公众号支付请注意以下几点:
    - 入网，商户名称必须跟公众号主体名称一致
    - 进去 https://mp.weixin.qq.com/ 设置相关开发参数 AppSecret，获取AppID
    - 关于如何获取 openid，建议采取静默模式https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=4_4
    - 关于微信版本号获取，5.0以上才支持微信支付https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=7_5
    - H5调起支付API：https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=7_7&index=6

12. 支付宝交易,如何获取 buyer_id:如果来自支付宝APP
    获取用户user_id(即buyer_id)，传入交易接口sub_openid字段,user_id需要调用支付宝官方接口获取https://opendocs.alipay.com/open/02xtl8
13. 服务窗如何调起支付：https://docs.open.alipay.com/common/105591 使用接口返回的 reserved_transaction_id 调起支付即可
14. 注：以下报文中的 mchnt_order_no 必须全局永远唯一(不区分商户、日期)。<br>流水号规则：商户号编码（富友提供，5位`系统算法原因，以前的商户采用的是4位订单前缀，4位订单前缀使用完了之后开始生成5位前缀，目前是5位前缀，以此类推`）+日期（yyyyMMdd，8位）+其他随机数（8-17位，保证不重复即可，最长不超过30位）
15. 商户订单号UNIONPAY银联仅支持数字订单单号
16. 微信JSAPI，公众号支付完成后跳转说明：

    关于支付完成后跳转页面升级为“点金计划”官方页面的详细接入指引：

    点金计划产品介绍：https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/goldplan/chapter1_1.shtml

    配置方式有两种：

    自有渠道号合作方：可登录服务商平台——>服务商功能——>点金计划——>报名及配置。链接如下：https://pay.weixin.qq.com/index.php/xphp/cgoldplan_mgr/activity_index

    使用富友渠道号：找对应运营同事开通点金计划和配置支付完成后所需要跳转的商家小票链接

    支付完成后跳转页面需要对接微信官方js才可展示或跳转自定义页面，文档可参考：https://gtimg.wechatpay.cn/pay/download/goldplan/goldplan_product_description_v2.1.pdf 中3.4 商家小票开发指引



---

## 签名

采用MD5签名方式

### 通讯方式

信息通过`http 或 https`形式`post`请求递交给前置系统，编码必须为 UTF-8

### 计算方法

按照每个接口的sign字段所示，拼接签名原文，并加上`mchnt_key`做MD5摘要，其中`mchnt_key`为商户密钥，系统分配

### MD5 安全签名机制说明

MD5安全签名机制是商户和富友约定一个签名key，每次在做签名时附在待签名字符串后面，经MD5加密运算后得到一个签名串，商户和富友通过在检验时也采用同样的方法得到签名串，经比对后确定是否一致，如果一致，则签名通过。

- 请求时签名

将请求的验签字段标明的域加上系统分配的商户密钥，按照验签字段标明顺序拼成一个字符串，用“|”分割。对这个字符串做 MD5 摘要，字符串编码格式必须为“UTF-8”

- 通知和返回时验证签名

商户网站接收到该请求，取出 MD5 摘要数据。将返回的验签字段标明的域加上系统分配的商户密钥(MD5 摘要数据除外)，按照验签字段标明顺序拼成一个字符串，用“|”分割。对这个字符串做 MD5 摘要。比较两个 MD5 值是否相等，如果相等，说明该笔通知信息有效。



---

## 测试环境参数

### 测试环境参数 (`此为测试环境参数，生产参数验收之后即可得到，生产环境建议使用网易邮箱申请密钥，以免收不到`)
测试环境订单前缀：`1066`

测试环境商户号：`0002900F5829371`

测试环境mchnt_key密钥：`f00dac5077ea11e754e14c9541bc0170`

测试&生产环境APPID：`wxfa089da95020ba1a   wx5ac8eb4651fe544f`

针对接口第一次请求接口不支付，第二次请求相同订单报“商户订单号重复的解决办法”：接口加上Reserved_repeat_order=1这个字段，支持重复幂等下单。

测试地址见接口文档

### 测试注意(重要重要！！！必看！！！！！)
!> `注意：`测试环境提供的银行间连生产测试账号是生产环境用来进行支付业务体验的账号，`交易中产生的一切信息均为生产环境数据`。用户支付需使用正式版微信、支付宝登陆用户的支付账号，使用用户的账号余额或绑定的银行卡等支付渠道进行支付。由于测试账号业务特点，不会产生清算资金划拨。所以`使用测试账号时请务必使用小于1元的小金额`，并请务必在支付`当日`完成退款。否则因隔日退款失败造成的测试资金损失由测试机构承担，并视为放弃资金。由于该账号为测试体验账号，所以`禁止将该账号用于生产业务用途，禁止在商户真实业务中使用`。对于将测试账号误用在商户真实业务造成的商户、用户等资金损失，由测试机构承担。
`使用该测试账号则视为同意上述使用协议。`


---

## 示例字段

### 花呗分期信息-reserved_ali_extend_params-示例字段
```json
{
    "reserved_ali_extend_params":{
        "dynamic_token_out_biz_no":"66666",
        "hb_fq_num":"3",
        "industry_reflux_info":{
            "scene_code":"metro_tradeorder",
            "channel":"xxxx",
            "scene_data":{
                "asset_name":"ALIPAY"
            }
        }
    }
}
```

### 行业数据回流信息-industry_reflux_info-示例字段
```json
{
    \"scene_code\":\"metro_tradeorder\",
    \"channel\":\"xxxx\",
    \"scene_data\":{
        \"asset_name\":\"ALIPAY\"
    }
}
```

### 划拨明细-mchntList-示例字段
```json
"mchntList":
[{
    "mchnt_cd":"0002900F0364619",
    "pay_amt":"798019",
    "pay_st":"1",
    "pay_st_desc":"成功",
    "settle_dt":"20160627"
},
{
    "mchnt_cd":"0002900F0364619",
    "pay_amt":"719",
    "pay_st":"1",
    "pay_st_desc":"成功",
    "settle_dt":"20160627"
}]
```

### 设备信息-reserved_device_info-示例字段
```json
//IOS 移动应用
{"type":"IOS","app_name":"王者荣耀","app_url":"com.tencent.wzryIOS"}//bundle_id
//安卓移动应用
{"type":"Android","app_name":"王者荣耀","app_url":"com.tencent.tmgp.sgame"}//package_name
//WAP 网站应用
{"type":"Wap","app_name":"京东官网","app_url":"https://m.jd.com"}//wetside
```


### goods_detail说明字段

*微信*

```text
{
    "cost_price":608800,
    "receipt_id":"wx123",
    //注意goods_detail字段的格式为"goods_detail":[{}],较多商户写成"goods_detail":{}
    "goods_detail":[
    {
        "goods_id":"商品编码",
        "wxpay_goods_id":"1001",
        "goods_name":"",
        "quantity":1,
        "price":528800
    },
    {
        "goods_id":"商品编码",
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
    "orderInfo":
    {
        "title" :"日用品",
        "dctAmount" :"10000",
        "addnInfo" :"屈臣氏(人民广场)店"
    },
    "goodsInfo":
    [{
        "id": "1234567890",
        "name": "商品 1",
        "price": "500",
        "quantity": "1"
    },
    {
        "id": "1234567891",
        "name": "商品 2",
        "price": "1000",
        "quantity": "2",
        "category": "类目 1",
        "addnInfo": "商品图片 http://www.95516.com/xxx.jpg"
    }]
}
```

---


### reserved_promotion_detail说明字段

```text
[{
    "activity_id":"12345",
    "amount":"1",
    "merchant_contribute":"1",
    "name":"BSagiiBBXm",
    "other_contribute":"0",
    "promotion_id":"10000",
    "scope":"GLOBAL",
    "type":"DISCOUNT",
    "wxpay_contribute":"0"
}]
```

























