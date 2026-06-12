!> **注：以下报文中的mchnt_order_no, refund_order_no必须全局永远唯一(不区分商户、日期)。**

!> **流水号规则：机构编码（富友侧提供，4位）+日期（yyyyMMdd，8位）+其他随机数（8-18位，只能包含字母和数字，区分大小写，保证不重复）。如果流水号重复，会直接拒绝交易！！！**

### 3.1 条码支付，商户扫用户二维码收款

!> `注：`条码支付存在需要用户输入支付密码的场景(实时返回用户支付中)，是否需要输入支付密码，与支付宝、微信的风控规则有关。<br>**`此时需要商户主动发起查询来确定最终的订单状态（对接3.4订单查询接口）`**。(建议：**交易发起5S以后开始查询。查询间隔5S以上**)<br>**`此时需要商户主动发起查询来确定最终的订单状态（对接3.4订单查询接口）`**<br>**`此时需要商户主动发起查询来确定最终的订单状态（对接3.4订单查询接口）`**<br>

!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`<br>

#### 请求地址：

*测试地址：* `https://fundwx.payfuiouo2o.com/micropay`

*生产地址：*

#### 场景说明
![被扫流程图](_images/micropay-process.jpg '被扫流程图')

- `步骤1：`用户和商户确认订单金额
- `步骤2：`用户向商户展示支付二维码(微信、支付宝等)
- `步骤3：`商户使用扫码枪，扫描用户二维码并提交到富友扫码，并实时返回支付结果.

!> `注：`条码支付存在需要用户输入支付密码的场景(实时返回用户支付中)，是否需要输入支付密码，与支付宝、微信的风控规则有关。**`此时需要商户主动发起查询来确定最终的订单状态`**。(建议：**交易发起5S以后开始查询。查询间隔5S以上**)


#### 条码支付请求报文
|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:----|:-----|
|1|version|必填|String|8|1.0|
|2|ins_cd|必填|String|13|机构号,接入机构在富友的唯一代码|
|3|mchnt_cd|必填|String|15|商户号, 富友分配给二级商户的商户号|
|4|term_id|必填|String|8|终端号(没有真实终端号统一填88888888)|
|5|random_str|必填|String|32|随机字符串|
|6|sign|必填|String|512|签名, 详见签名生成算法|
|7|order_type|非必填|String|20|订单类型：<br>ALIPAY(支付宝)<br>WECHAT(微信)<br>UNIONPAY(银联二维码)<br>BESTPAY(翼支付)<br>DIGICCY(数字货币)<br>若此字段不传，则响应报文中order_type字段会返回交易类型<br>查询接口仍需上传交易类型，故代理需自行保存交易类型|
|8|goods_des|必填|String|128|商品名称, 显示在用户账单的商品、商品说明等地方|
|9|goods_detail|非必填|String|6000|单品优惠功能字段，见文档中[good_detail说明字段](/scanpay/introduction#goods_detail说明字段)|
|10|addn_inf|非必填|String|50|附加数据<br/>如果需要用到微信点餐数据回传，该字段需要填写OrderSource=FoodOrder|
|11|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号（5到30个字符、 只能包含字母数字,区分大小写)|
|12|curr_type|非必填|String|3|货币类型,默认人民币：CNY|
|13|order_amt|必填|Number|16|总金额, 订单总金额，单位为`分`|
|14|term_ip|必填|String|128|实时交易终端IP(后期富友、银联侧风控主要依据，请真实填写)|
|15|txn_begin_ts|必填|String|14|交易起始时间, 订单生成时间，格式为yyyyMMddHHmmss|
|16|goods_tag|非必填|String|32|商品标记|
|17|auth_code|必填|String|128|扫码支付授权码，设备读取用户的条码或者二维码信息|
|18|sence|非必填|String|1|支付场景,默认1；<br />1: 条码支付 <br />2: 声波支付 <br />3: 刷脸支付|
|19|reserved_sub_appid|非必填|String|32|子商户公众号id|
|20|reserved_limit_pay|非必填|String|32|限制支付<br />no_credit:不能使用信用卡<br />credit_group：不能使用花呗以及信用卡|
|21|reserved_expire_minute|非必填|Number|8|交易关闭时间<br>默认120min|
|22|reserved_fy_term_id|非必填|String|8|富友终端号(富友终端号与reserved_terminal_info.serial_num二选一),<br>富友采购或自带机入网填此字段|
|23|reserved_fy_term_type|非必填|String|1|0:其他<br />1:富友终端<br />2:POS机<br />3:台卡<br />4:PC软件|
|24|reserved_fy_term_sn|非必填|String|20|终端序列号|
|25|reserved_device_info|非必填|String|32|设备信息，托传给微信。用于单品券核销|
|26|reserved_ali_extend_params|非必填|String||JSON串<br/>花呗分期示例值：<br/>{"dynamic_token_out_biz_no":"66666","hb_fq_num":"3","industry_reflux_info":{"scene_code":"metro_tradeorder","channel":"xxxx","scene_data":{"asset_name":"ALIPAY"}},"food_order_type":"qr_order"}<br/>信用卡分期示例值：{"fq_num":"3"}|
|27|└ hb_fq_num|非必填|String| 3                                                                                                                                                                                                                |花呗分期期数：仅支持3、6、12|
|28|└ hb_fq_seller_percent|非必填|String| 3                                                                                                                                                                                                                |花呗分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br />目前该字段未生效，我司会默认当0处理。|
|29|└ industry_reflux_info|非必填|String| 512                                                                                                                                                                                                              |行业数据回流信息。示例值：{\\"scene_code\\":\\"metro_tradeorder\\",<br />\\"channel\\":\\"xxxx\\",<br />\\"scene_data\\":{\\"asset_name\\":\\"ALIPAY\\"}}|
|30|└ dynamic_token_out_biz_no|非必填|String| 128                                                                                                                                                                                                              |条码支付中，目前支持的用法为：sence_no|
|31|└ fq_num|非必填|String| 3                                                                                                                                                                                                                |信用卡分期期数：仅支持3、6、12|
|32|└ fq_seller_percent|非必填|String| 3                                                                                                                                                                                                                |信用卡分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br />目前该字段未生效，我司会默认当0处理。|
|33|└ fq_channels|非必填|String| 20                                                                                                                                                                                                               |优先使⽤该资产进⾏⽀付，信用卡分期时传入，目前只支持传alipayfq_cc|
|34|└ food_order_type|非必填|String| 32 |点餐场景类型：<br />qr_order(店内扫码点餐)，<br />pre_order(预点到店自提)，<br />home_delivery(外送到家)，<br />direct_payment(直接付款)，<br />other(其他)|
|35|reserved_store_code|非必填|String| 32                                                                                                                                                                                                               |门店id（目前仅支持支付宝，自定义门店id）|
|36|reserved_alipay_store_id|非必填|String| 32                                                                                                                                                                                                               |支付宝店铺编号(支付宝返回id)|
|37|**`reserved_terminal_info`**|必填|json|| 终端信息说明字段，见文档中[reserved_terminal_info终端信息说明字段（259号文，终端信息）](/scanpay/transform#reserved_terminal_info终端信息说明字段（259号文，终端信息）)<br><span style="color:#FF0000">以下示例仅测试环境可用：{"serial_num":"12345678901SN012"}            |
|38|reserved_business_params|非必填|json| 512                                                                                                                                                                                                              |商户传入业务信息，应用于安全，营销等参数直传场景，格式为 json 格式：{"data":"123"}|
|39|reserved_scene_info|非必填|json| 256                                                                                                                                                                                                              |场景信息说明字段，见文档中[reserved_scene_info场景信息说明字段示例](/scanpay/introduction#reserved_scene_info场景信息说明字段示例)|


*reserved_ali_extend_params字段示例：*
```xml
<reserved_ali_extend_params>
{
	"dynamic_token_out_biz_no": "66666",
	"hb_fq_num": "3",
	"industry_reflux_info": {
		"scene_code": "metro_tradeorder",
		"channel": "xxxx",
		"scene_data": {
			"asset_name": "ALIPAY"
		}
	},
	"food_order_type": "qr_order"
}
</reserved_ali_extend_params>
```


#### 条码支付响应报文
|序号|参数名|出现要求|型态|长度|说明|
|:-----:|:-----|:-----:|:-----:|:-----:|:-----|
|1|result_code|必填|String|16|错误代码, 000000 成功,其他详细参见错误列表|
|2|result _msg|必填|String|128|错误代码描述|
|3|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|5|term_id|非必填|String|8|终端号(没有真实终端号统一填88888888)|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|order_type|必填|String|20|订单类型:ALIPAY, WECHAT,UNIONPAY(银联二维码）,BESTPAY(翼支付)|
|9|total_amount|必填|Number|16|订单金额，分为单位的整数|
|10|buyer_id|非必填|String|128|买家在渠道(微信、支付宝)的用户ID|
|11|transaction_id|必填|String|64|渠道交易流水号|
|12|addn_inf|非必填|String|50|附加数据|
|13|reserved_fy_order_no|非必填|String|30|富友生成的订单号,需要商户与商户订单号进行关联|
|14|reserved_mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号|
|15|reserved_fy_settle_dt|必填|String|8|富友交易日期|
|16|reserved_coupon_fee|非必填|String|10|优惠金额**`（分）`**微信、银联二维码类型返回|
|17|reserved_buyer_logon_id|非必填|String|128|买家在渠道登录账号|
|18|reserved_fund_bill_list|非必填|String|不定长|支付宝交易资金渠道,详细渠道|
|19|reserved_fy_trace_no|必填|String|12|富友系统内部追踪号|
|20|reserved_channel_order_id|非必填|String|64|条码流水号，用户账单二维码对应的流水|
|21|reserved_is_credit|非必填|String|8|1：表示信用卡~~或者花呗~~<br />0：表示其他(非信用方式)<br />不填，表示未知|
|22|reserved_txn_fin_ts|非必填|String|14|用户支付时间yyyyMMddHHmmss|
|23|reserved_settlement_amt|非必填|Number|16|应结算订单金额，以分为单位的整数<br />只有成功交易才会返回<br /><br />如果使用了商户免充值优惠券，该值为订单金额-商户免充值<br />如果没有使用商户免充值，该值等于订单金额|
|24|**`reserved_bank_type`**|非必填|String|16|付款方式|
|25|**`reserved_promotion_detail`**|非必填|String|6000|微信营销详情（资金单位：分），见文档中[reserved_promotion_detail说明字段](scanpay/introduction#reserved_promotion_detail说明字段)|
|26|reserved_voucher_detail_list|非必填|String|-|支付宝交易支付时所使用的所有优惠券信息（资金单位：元） <br>"[{"amount":"1.00","merchant_contribute":"1.00",<br/>"name":"1.00元代金券","other_contribute":"0.00",<br/>"template_id":"20221107000730017617007JGOUG",<br>"id":"202211070007300282330EB2Y9QQ",<br/>"type":"ALIPAY_BIZ_VOUCHER"}]"|
|27|reserved_discount_goods_detail|非必填|String|-|支付宝交易支付所使用的单品券优惠的商品优惠信息（资金单位：元）<br/> "[{\"goodsId\":\"STANDARD1026181538\",\" goodsName\":\"雪碧",\"discountAmount\":\"10.00\"}]"|
|28|reserved_hb_is_seller|非必填|String|1|返回值：1=商户出息|
|29|reserved_service_charge_flag|非必填|String|1|手续费减免标识<br>Y：表示减免（默认）<br>N：表示不减免|
|30|reserved_sub_mchnt_cd|非必填|String||交易子商户号|
|31|reserved_trade_type|非必填|String||交易类型<br>ALBS支付宝被扫、WXBS微信被扫、WXFP微信刷脸、ALFP支付宝刷脸、|

<!--|26|**`reserved_risk_info`**|非必填|json||见文档中[reserved_risk_info说明字段](/scanpay/transform#reserved_risk_info说明字段)|
-->

#### 报文示例
*请求报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <reserved_fy_term_sn></reserved_fy_term_sn>
    <reserved_device_info></reserved_device_info>
    <term_id>12345678</term_id>
    <random_str>8617299e474e70707777d5671d1c267a</random_str>
    <reserved_limit_pay></reserved_limit_pay>
    <reserved_sub_appid></reserved_sub_appid>
    <ins_cd>08A9999999</ins_cd>
    <reserved_fy_term_type></reserved_fy_term_type>
    <version>1</version>
    <addn_inf></addn_inf>
    <auth_code>135034103113415344</auth_code>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <reserved_expire_minute></reserved_expire_minute>
    <term_ip>127.0.0.1</term_ip>
    <sence>1</sence>
    <order_amt>1</order_amt>
    <goods_des>支付</goods_des>
    <reserved_hb_fq_seller_percent></reserved_hb_fq_seller_percent>
    <curr_type></curr_type>
    <txn_begin_ts>20201201152942</txn_begin_ts>
    <sign>
        NOIkC6TH1BaNJD54VXSbljKHUmnfEkp7rr5kDbgkllj6dU6sKNM/Bhvu54wxkUDhPex09/Oh1oFawe5fV3cHh5i/1z03J33KjZkm2UNVPsImqTtA7LQy1vXi6FhAUfl9IJsPNvD/T9xObfzpxrYY/d1mHrk5tZrbpEBvJyKu4B4=
    </sign>
    <goods_tag></goods_tag>
    <goods_detail></goods_detail>
    <reserved_fy_term_id></reserved_fy_term_id>
    <reserved_hb_fq_num></reserved_hb_fq_num>
    <mchnt_order_no>2020120115294222446445</mchnt_order_no>
    <order_type>WECHAT</order_type>
</xml>
```

*请求报文签名原文*
```text
addn_inf=&auth_code=135034103113415344&curr_type=&goods_des=支付&goods_detail=&goods_tag=&ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115294222446445&order_amt=1&order_type=WECHAT&random_str=8617299e474e70707777d5671d1c267a&sence=1&term_id=12345678&term_ip=127.0.0.1&txn_begin_ts=20201201152942&version=1
```

*响应报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <addn_inf></addn_inf>
    <buyer_id></buyer_id>
    <ins_cd>08A9999999</ins_cd>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <order_type>WECHAT</order_type>
    <random_str>EB8S5ITD6TLAFISXEJT5KZLUYHOC8Y9M</random_str>
    <reserved_buyer_logon_id></reserved_buyer_logon_id>
    <reserved_channel_order_id></reserved_channel_order_id>
    <reserved_coupon_fee></reserved_coupon_fee>
    <reserved_fund_bill_list></reserved_fund_bill_list>
    <reserved_fy_order_no></reserved_fy_order_no>
    <reserved_fy_settle_dt>20201201</reserved_fy_settle_dt>
    <reserved_fy_trace_no>120186438960</reserved_fy_trace_no>
    <reserved_is_credit></reserved_is_credit>
    <reserved_mchnt_order_no>2020120115294222446445</reserved_mchnt_order_no>
    <reserved_txn_fin_ts></reserved_txn_fin_ts>
    <result_code>030010</result_code>
    <result_msg>等待用户支付中</result_msg>
    <sign>
        f6yR5oMsa36A/kEnZAKulvJuNdaCy8qlFGC8YxUUkK2baEfRzHyrMtYTdhUFyFHtnk636LWLsLpKIugJjwSxDmt9QU78bzXRuspJ3ecodz50of9aef7b0aVolMGn2fS/x2oPafd0KrBYKG0A+ta7CmbmLbxquciJdOmqP/b0OZw=
    </sign>
    <term_id></term_id>
    <total_amount>1</total_amount>
    <transaction_id></transaction_id>
</xml>
```

*响应报文签名原文*
```text
addn_inf=&buyer_id=&ins_cd=08A9999999&mchnt_cd=0002900F0370542&order_type=WECHAT&random_str=EB8S5ITD6TLAFISXEJT5KZLUYHOC8Y9M&result_code=030010&result_msg=等待用户支付中&term_id=&total_amount=1&transaction_id=
```

---

### 3.2 主扫统一下单
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/preCreate`

*生产地址：*

#### 场景说明

![主扫统一下单流程图](_images/preCreate-process.jpg '主扫统一下单流程图')

* `步骤1：`商户发送报文至富友进行下单，实时返回下单后的支付二维码地址
* `步骤2：`商户展示二维码给用户，用户使用微信、支付宝等扫码支付
* `步骤3：`支付结果以回调(3.5)的方式通知到商户

!> 注：`下单成功之后开始查询。`建议：查询间隔`5S`以上。如主动发起查询请在下单成功之后发起查询。

!> **说明：2019年7月23日开始，微信新增商户不再具有Native主扫下单权限；2019年9月15日开始，该接口为富友使用收单机构模式JS封装的主扫，新增、存量商户可以继续使用该接口。**

#### 统一下单请求报文
|序号|参数名| 出现要求 |型态|长度| 说明                                                                                                                                                                                                                                                     |
|:-----|:-----|:-----|:-----|:-----|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|1|version| 必填   |String|8| 1.0                                                                                                                                                                                                                                                    |
|2|ins_cd| 必填   |String|20| 机构号,接入机构在富友的唯一代码                                                                                                                                                                                                                                       |
|3|mchnt_cd| 必填   |String|15| 商户号, 富友分配给二级商户的商户号                                                                                                                                                                                                                                     |
|4|term_id| 必填   |String|8| 终端号(没有真实终端号统一填88888888)                                                                                                                                                                                                                                |
|5|random_str| 必填   |String|32| 随机字符串                                                                                                                                                                                                                                                  |
|6|sign| 必填   |String|512| 签名, 详见签名生成算法                                                                                                                                                                                                                                           |
|7|order_type| 必填   |String|32| 订单类型：<br>ALIPAY(支付宝)<br>WECHAT(微信)<br>UNIONPAY(银联二维码)<br>~~BESTPAY(翼支付)~~<br>PY68(银联分期-商户贴息)<br>PY69(银联分期-持卡人贴息)<br>~~WXXS(微信线上)~~<br>DIGICCY(数字货币)                                                                                     |
|8|goods_des| 必填   |String|128| 商品名称, 显示在用户账单的商品、商品说明等地方                                                                                                                                                                                                                               |
|9|goods_detail| 非必填  |String|6000| 单品优惠功能字段，见文档中[good_detail说明字段](/scanpay/introduction#goods_detail说明字段)                                                                                                                                                                                 |
|10|addn_inf| 非必填  |String|50| 附加数据<br/>如果需要用到微信点餐数据回传，该字段需要填写OrderSource=FoodOrder                                                                                                                                                                                                   |
|11|mchnt_order_no| 必填   |String|30| 商户订单号,商户系统内部的订单号（5到30个字符、只能包含字母数字,区分大小写)                                                                                                                                                                                                               |
|12|curr_type| 非必填  |String|3| 货币类型,默认人民币：CNY                                                                                                                                                                                                                                         |
|13|order_amt| 必填   |Number|16| 总金额, 订单总金额，单位为分                                                                                                                                                                                                                                        |
|14|term_ip| 必填   |String|128| 实时交易终端IP(后期富友、银联侧风控主要依据，请真实填写)                                                                                                                                                                                                           |
|15|txn_begin_ts| 必填   |String|14| 交易起始时间, 订单生成时间，格式为yyyyMMddHHmmss                                                                                                                                                                                                                       |
|16|goods_tag| 非必填  |String|32| 商品标记                                                                                                                                                                                                                                                   |
|17|notify_url| 必填   |String|256| 接收富友异步通知回调地址<br>通知url必须为直接公网可访问的url,只能是域名，内网地址如192.168或者172,10开头的地址不能使用，若需加白使用，需要找我司索要出口IP,不能携带参数主扫`                                                                                                                                                                                                    |
|18|reserved_sub_appid| 非必填  |String|32| 子商户公众号id(后期拓展字段，请先不要填写)                                                                                                                                                                                                                                |
|19|reserved_limit_pay| 非必填  |String|32| 限制支付,<br />no_credit:不能使用信用卡,<br />credit_group：不能使用花呗以及信用卡                                                                                                                                                                                            |
|20|reserved_expire_minute| 必填   |Number|8| 交易关闭时间<br>默认120min                                                                                                                                                                                                                           |
|21|reserved_fy_term_id| 非必填  |String|8| 富友终端号(富友终端号与reserved_terminal_info.serial_num二选一),<br>富友采购或自带机入网填此字段                                                                                                                                                                     |
|22|reserved_fy_term_type| 非必填  |String|1| 富友终端类型<br />0:其他<br />1:富友终端<br />2:POS机<br />3:台卡<br />4:PC软件                                                                                                                                                                                         |
|23|reserved_fy_term_sn| 非必填  |String|20| 终端序列号                                                                                                                                                                                                                                                  |
|24|reserved_device_info| 非必填  |String|32| 设备信息，托传给微信。用于单品券核销。                                                                                                                                                                                                                                    |
|25|reserved_ali_extend_params| 非必填  |String||JSON串<br/>花呗分期示例值：<br>{"dynamic_token_out_biz_no":"66666","hb_fq_num":"3","industry_reflux_info":{"scene_code":"metro_tradeorder","channel":"xxxx","scene_data":{"asset_name":"ALIPAY"}},"food_order_type":"qr_order"}<br>信用卡分期示例值：{"fq_num":"3"}<br/>银联分期示例值：{"hb_fq_num":"3","industry_reflux_info":"ICBC,ABC,CCB"}|
|26|└ hb_fq_num| 非必填  |String|3| 花呗分期期数：仅支持3、6、12<br><span style="color:#FF0000">银联分期模式下必传                                                                                                                                                                                              |
|27|└ hb_fq_seller_percent| 非必填  |非必填|3| 花呗分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br />目前该字段未生效，我司会默认当0处理。                                                                                                                                                                                              |
|28|└ industry_reflux_info| 非必填  |非必填|512| 花呗分期模式：行业数据回流信息。示例值：<br>{\\"scene_code\\":\\"metro_tradeorder\\",<br />\\"channel\\":\\"xxxx\\",<br />\\"scene_data\\":{\\"asset_name\\":\\"ALIPAY\\"}}<br><br><span style="color:#FF0000">银联分期模式：传支持的银行，格式为银行的缩写，多个银行用逗号隔开，示例：ICBC,ABC,CCB</span><br> |
|29|└ fq_num| 非必填  |String|3| 信用卡分期期数：仅支持3、6、12                                                                                                                                                                                                                                      |
|30|└ fq_seller_percent| 非必填  |String|3| 信用卡分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br />目前该字段未生效，我司会默认当0处理。                                                                                                                                                                                             |
|31|└ fq_channels| 非必填  |String|20| 优先使⽤该资产进⾏⽀付，信用卡分期时传入，目前只支持传alipayfq_cc                                                                                                                                                                                                                 |
|32|└ food_order_type| 非必填  |String|32| 点餐场景类型：<br />qr_order(店内扫码点餐)，<br />pre_order(预点到店自提)，<br />home_delivery(外送到家)，<br />direct_payment(直接付款)，<br />other(其他)                                                                                                                             |
|33|reserved_store_code| 非必填  |String|32| 门店id（目前仅支持支付宝）                                                                                                                                                                                                                                         |
|34|reserved_alipay_store_id| 非必填  |String|32| 支付宝店铺编号                                                                                                                                                                                                                                                |
|35|**`reserved_terminal_info`**| 非必填  |String||终端信息说明字段，见文档中[reserved_terminal_info终端信息说明字段（259号文，终端信息）](/scanpay/transform#reserved_terminal_info终端信息说明字段（259号文，终端信息）)|
|36|reserved_business_params| 非必填  |json|512| 商户传入业务信息，应用于安全，营销等参数直传场景，格式为 json 格式：{"data":"123"}                                                                                                                                                                                                    |

*reserved_ali_extend_params字段示例：*

```xml
<reserved_ali_extend_params>
{
	"dynamic_token_out_biz_no": "66666",
	"hb_fq_num": "3",
	"industry_reflux_info": {
		"scene_code": "metro_tradeorder",
		"channel": "xxxx",
		"scene_data": {
			"asset_name": "ALIPAY"
		}
	},
	"food_order_type": "qr_order"
}
</reserved_ali_extend_params>
```

#### 统一下单响应报文
|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码,000000 成功,其他详细参见错误列表|
|2|result_msg|必填|String|128|错误代码描述|
|3|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|5|term_id|非必填|String|8|终端号|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|order_type|必填|String|20|订单类型:ALIPAY,WECHAT,UNIONPAY(银联二维码),BESTPAY(翼支付)|
|9|session_id|非必填|String|64|预支付交易会话标识,富友返回支付宝生成的预支付回话标识，用于后续接口调用中使用，该值有效期为2小时|
|10|qr_code|非必填|String|64|二维码链接|
|11|reserved_fy_order_no|非必填|String|30|富友生成的订单号,需要商户与商户订单号进行关联|
|12|reserved_fy_trace_no|非必填|String|12|富友系统内部追踪号|
|13|reserved_channel_order_id|非必填|String|64|条码流水号，用户账单二维码对应的流水|


#### 报文示例
*请求报文*
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

*请求报文签名原文*
```text
addn_inf=&curr_type=&goods_des=卡盟测试&goods_detail=asasda&goods_tag=&ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=202012011518020724446&notify_url=https://mail.qq.com/cgi-bin/frame_html?sid=pEYG5nBgQiNVqANe&r=4a6c47ad7d279a80630dec073cda96e2&order_amt=1&order_type=WECHAT&random_str=d0194c1024f180065d2434fa8b6a2f82&term_id=12345678&term_ip=127.0.0.1&txn_begin_ts=20201201151802&version=1
```

*响应报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <ins_cd>08A9999999</ins_cd>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <order_type>WECHAT</order_type>
    <qr_code>https://insuretest.fuioupay.com/native?token=20201201120186438897</qr_code>
    <random_str>ZO2559LK6JSWSQKW2XGQH1FSFFMW980F</random_str>
    <reserved_addn_inf></reserved_addn_inf>
    <reserved_channel_order_id></reserved_channel_order_id>
    <reserved_fy_order_no></reserved_fy_order_no>
    <reserved_fy_trace_no>120186438897</reserved_fy_trace_no>
    <result_code>000000</result_code>
    <result_msg>SUCCESS</result_msg>
    <session_id></session_id>
    <sign>
        fjd0m17izCCWSP6Pygw0JDHM3TesIpXsW/lESKFTzSySswkTICw7Uu+iuSRqYYNgocTBRctut1M5Ryd985mftJDPUW/Hk08cY7MSvD+Rmnv6uhjTWe/9cm+4W25VXpm/WTS4l7YeBtfWVQBj6KD1MMxRYcLFyHV9LVYEZpIKzt0=
    </sign>
    <term_id></term_id>
</xml>
```

*响应报文签名原文*
```text
ins_cd=08A9999999&mchnt_cd=0002900F0370542&order_type=WECHAT&qr_code=https://insuretest.fuioupay.com/native?token=20201201120186438897&random_str=ZO2559LK6JSWSQKW2XGQH1FSFFMW980F&result_code=000000&result_msg=SUCCESS&session_id=&term_id=
```

---

### 3.3 微信公众号(小程序)、支付宝服务窗(小程序)统一下单
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址
*测试地址：* `https://fundwx.payfuiouo2o.com/wxPreCreate`

*正式地址：*

#### *特别说明*

?> **测试环境微信公众号支付说明：**<br>
由于测试环境均使用的是银联的渠道号。 且富友作为第三方支付公司无法以支付公司的主体进行入网。
导致测试环境，不管使用何种方式，都不能使用富友的appid进行交易的。
<br>
<br>
**测试环境公众号测试办法：**<br>
`本接口调至 sub_appid与sub_mchnt_id不匹配，或者appid与openid不匹配即可`<br>


#### *银联云闪付js调起支付方法*
?>
**前置条件：域名加白，台卡域名需要通过富友到银联总行进行加白。**
<br>1、如果是来自云闪付APP<br>（UnionPay/1.0 CloudPa、云闪付境外钱包：UnionPay/1.0 3999XXXX、 工行APP：UnionPay/1.0 ICBCeLife等，即user_agent包含  “UnionPay/”。需要合作方重定向：https://qr.95516.com/qrcGtwWeb-web/api/userAuth?version=1.0.0&redirectUrl=合作方银联二维码授权码接收地址   【注：该地址需要进行urlencode】）
<br>2、通过步骤 2中的redirectUrl重定向获取到【授权码】
<br>3、通过上述的【授权码】 调用【[3.11 微信授权码查询openid / 银联授权码获取user_id](/scanpay/api#_311-微信授权码查询openid-银联授权码获取user_id)】接口获取user_id
<br>4、使用上述的【user_id】 调用本下单接口进行下单<br>（即trade_type=UNIONPAY时，sub_appid，openid填空，sub_openid = user_id）
<br>5、通过接口返回的qr_code，直接重定向调起支付
<br>支持银联境外钱包兼容改造文档如下：[富友-银联二维码银行APP及境外云闪付agent兼容改造指引](https://open.fuioupay.com/downLoad.do?fileId=701d96d2-dd59-45ad-88e7-af5f9a9378db '富友-银联二维码银行APP及境外云闪付agent兼容改造指引')

<br>Trade_type使用说明：由于APPLE和云闪付付款方式不一样，后期费率、补贴会不一样。



#### **微信JSAPI，公众号支付不跳转说明：**

?>

可参考以下公告(所有拥有渠道号的服务商应该都有收到微信下发的通知)：<br>

【微信支付服务商平台发了公告，服务商jsapi支付后回到商家页面的能力，会陆续回收，由点金计划来承载。

如果没有开通点金计划的服务商，支付后就会直接关闭商家页面。详情可查看公告说明：

https://pay.weixin.qq.com/index.php/public/cms/content_detail?platformType=1&lang=zh&id=121505

<br>另外，点金计划相关接口可参考这个文档

https://pay.weixin.qq.com/docs/partner/products/gold-plan/introduction.html<br>

如果合作方有渠道号，按上述公告文档自行做出调整。<br>

若使用富友渠道号代理需要配置商家小票，则可根据微信文档开发完成后，联系富友侧同事配置商家小票链接并完成商家小票链接调试<br>

（一）商家自行开发商家小票邮件内容【邮箱地址pay_prepay@fuioupay.com】：<br>
1、提供审核资料申请开通<br>
(1)       商户需要提供盖章版《微信商家小票功能使用承诺书》
(2)       提供链接展示页面截图
(3)       提供域名  ICP  备案证书（需与商户主体一致）
(4)       富友商户号、商户名称
2、获取商家小票配置文件<br>
审核通过后，富友对接人员提供配置文件，商家技术人员需放在指定的代码中。<br>
3、配置商家链接测试上线<br>
商家测试交易验证是否正常展示小票内容，验证即可上线使用。<br>




#### 场景说明
![公众号/服务窗统一下单流程图](_images/wxPreCreate-process.jpg '公众号/服务窗统一下单流程图')

!> 该接口常应用于**`聚合二维码(静态二维码、统一收款码、台卡等不同叫法，需合作方自己使用H5页面地址生成二维码)`**，用户扫二维码进入微信公众号/支付宝服务窗/QQJS 页面，页面调此接口生成订单，接受订单参数后调起官方支付接口支付。详见公众号/服务窗对接流程

* `步骤1：`用户通过支付宝(服务窗)、微信(公众号)进入到商户H5/SDK页面，或者是通过扫描台卡进入。

  合作方H5页面中可以通过user-agent获取用户使用的是哪种支付方式

  * 如果来自微信APP

    * 获取用户openid，传入交易接口sub_openid字段

      获取用户openid有两种模式：

      a: 使用富友appid(即商户没有自有公众号或者合作方没有申请自有渠道号)的情况下，使用接口3.12服务商模式获取openid

      b:使用合作方或者商户自有公众号的情况，公众号appid传入sub_appid字段中，openid需要调用微信官方接口获取 https://pay.weixin.qq.com/doc/v2/merchant/4011939975#14%E3%80%81Openid

  * 如果来自支付宝APP

    * 获取用户user_id(即buyer_id)，传入交易接口sub_openid字段,user_id需要调用支付宝官方接口获取https://opendocs.alipay.com/open/02xtl8
    * 若接口只能获取到openid，则需要申请回退user_id链接：https://opendocs.alipay.com/mini/0ai736?pathHash=46388508
* `步骤2：`用户选择商品、输入支付金额等进行下单支付
* `步骤3：`商户将单信息发送给富友，返回支付信息(用于调起支付宝、微信的参数)。
* `步骤4：`商户拿到支付信息后调起微信或者支付宝进行支付

  * 微信调起支付，使用sdk开头的参数 https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=7_7&index=6
  * 支付宝调起支付，使用reserved_transaction_id字段参数 https://opendocs.alipay.com/open/common/105591

* `步骤5：`支付结果以回调(3.5)的方式通知到商户







#### 微信公众号(小程序)、支付宝服务窗(小程序)统一下单请求报文

|序号|参数名| 出现要求 |型态|长度| 说明                                                                                                                                                                                                                                  |
|:-----|:-----|:-----|:-----|:-----|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|1|version| 必填   |String|8| 1.0                                                                                                                                                                                                                                 |
|2|ins_cd| 必填   |String|20| 机构号,接入机构在富友的唯一代码                                                                                                                                                                                                                    |
|3|mchnt_cd| 必填   |String|15| 商户号,富友分配给二级商户的商户号                                                                                                                                                                                                                   |
|4|term_id| 必填   |String|8| 终端号(没有真实终端号统一填88888888)                                                                                                                                                                                                             |
|5|random_str| 必填   |String|32| 随机字符串                                                                                                                                                                                                                               |
|6|sign| 必填   |String|512| 签名, 详见签名生成算法                                                                                                                                                                                                                        |
|7|goods_des| 必填   |String|128| 商品名称, 显示在用户账单的商品、商品说明等地方                                                                                                                                                                                                            |
|8|goods_detail| 非必填  |String|6000| 单品优惠功能字段，见文档中[good_detail说明字段](/scanpay/introduction#goods_detail说明字段)                                                                                                                                                              |
|9|goods_tag| 非必填  |String|32| 商品标记                                                                                                                                                                                                                                |
|10|product_id| 非必填  |String|32| 商品标识                                                                                                                                                                                                                                |
|11|addn_inf| 非必填  |String|50| 附加数据<br/>如果需要用到微信点餐数据回传，该字段需要填写OrderSource=FoodOrder                                                                                                                                                                                |
|12|mchnt_order_no| 必填   |String|30| 商户订单号,商户系统内部的订单号（5到30个字符、只能包含字母数字,区分大小写)                                                                                                                                                                                            |
|13|curr_type| 非必填  |String|3| 货币类型,默认人民币：CNY                                                                                                                                                                                                                      |
|14|order_amt| 必填   |Number|16| 总金额,订单总金额,单位为**`分`**                                                                                                                                                                                                                |
|15|term_ip| 必填   |String|128| 实时交易终端IP(后期富友、银联侧风控主要依据，请真实填写)                                                                                                                                                                                                      |
|16|txn_begin_ts| 必填   |String|14| 交易起始时间,订单生成时间,格式为yyyyMMddHHmmss                                                                                                                                                                                                     |
|17|notify_url| 必填   |String|256| 接收富友异步通知回调地址<br>通知url必须为直接公网可访问的url,只能是域名，内网地址如192.168或者172,10开头的地址不能使用，若需加白使用，需要找我司索要出口IP,不能携带参数主扫                                                                                                                                                                                          |
|18|limit_pay| 非必填  |String|32| 限制支付,<br />no_credit:不能使用信用卡<br />credit_group：不能使用花呗以及信用卡                                                                                                                                                                          |
|19|trade_type| 必填   |String|16| 订单类型:<br>JSAPI--公众号支付<br>FWC--支付宝服务窗、支付宝小程序<br>LETPAY-微信小程序<br>BESTPAY--翼支付js<br>MPAY--云闪付小程序（控件支付）<br>~~APPLEPAY--APPLE相机扫码~~（监管要求，已停用）<br>UNIONPAY--云闪付扫码<br>UPBXJS--云闪付保险缴费<br>LPXS(小程序线上)<br>JDBT(京东白条用户贴息)<br>JDBTSH（京东白条商户贴息） |
|20|openid| 非必填  |String|128| 用户标识`(暂已废弃,不影响已对接完成的)`                                                                                                                                                                                                              |
|21|sub_openid| 非必填  |String|128| 子商户用户标识<br />支付宝服务窗为用户buyer_id(此场景必填)<br />微信公众号为用户的openid(小程序,公众号,服务窗必填)<br>APPLEPAY（相机扫码）、JDBT(京东白条用户贴息)该值不用填<br>JDBTSH(京东白条商户贴息)该值不用填<br/>UNIONPAY（云闪付扫码）该值必填,`客若是用自己的appid，则去对应官网获取openid,若是用富友的appid,则去3.12获取openid`           |
|22|sub_appid| 非必填  |String|32| 子商户公众号id,<br />微信交易为商户的appid(小程序,公众号必填)<br>JDBT(京东白条)该值不用填,`若是客户有自己的appid，可以用自己的，若没有，则用富友的`                                                                                                                                         |
|23|reserved_expire_minute| 非必填  |Number|8| 交易关闭时间<br>默认120min                                                                                                                                                                                                        |
|24|reserved_fy_term_id| 非必填  |String|8| 富友终端号(富友终端号与reserved_terminal_info.serial_num二选一),<br/>富友采购或自带机入网填此字段                                                                                                                                                               |
|25|reserved_fy_term_type| 非必填  |String|1| 富友终端类型<br />0:其他<br />1:富友终端<br />2:POS机<br />3:台卡<br />4:PC软件                                                                                                                                                                      |
|26|reserved_txn_bonus| 非必填  |Number|16| 积分抵扣金额,单位为分                                                                                                                                                                                                                         |
|27|reserved_fy_term_sn| 非必填  |String|20| 终端序列号                                                                                                                                                                                                                               |
|28|reserved_device_info| 非必填  |String|32| 设备信息，托传给微信。用于单品券核销。                                                                                                                                                                                                                 |
|29|reserved_ali_extend_params| 非必填  |String||JSON串<br/>花呗分期示例值：<br/>{"dynamic_token_out_biz_no":"66666","hb_fq_num":"3","industry_reflux_info":{"scene_code":"metro_tradeorder","channel":"xxxx","scene_data":{"asset_name":"ALIPAY"}},"food_order_type":"qr_order"}<br/>信用卡分期示例值：{"fq_num":"3"}|
|30|└ hb_fq_num| 非必填  |String|3| 花呗分期、京东白条 分期期数：仅支持3、6、12<br>`京东白条商户贴息时，该字段必填`                                                                                                                                                                                       |
|31|└ hb_fq_seller_percent| 非必填  |非必填|3| 花呗分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br />目前该字段未生效，我司会默认当0处理。<br>`京东白条商户贴息时，该字段必须传100`                                                                                                                                                  |
|32|└ industry_reflux_info| 非必填  |非必填|512| 行业数据回流信息。示例值：<br />{\\"scene_code\\":\\"metro_tradeorder\\",<br />\\"channel\\":\\"xxxx\\",<br />\\"scene_data\\":{\\"asset_name\\":\\"ALIPAY\\"}}                                                                                  |
|33|└ food_order_type| 非必填  |String|32| 点餐场景类型：<br />qr_order(店内扫码点餐)，<br />pre_order(预点到店自提)，<br />home_delivery(外送到家)，<br />direct_payment(直接付款)，<br />other(其他)                                                                                                          |
|34|└ fq_num| 非必填  |String|3| 信用卡分期期数：仅支持3、6、12                                                                                                                                                                                                                   |
|35|└ fq_seller_percent| 非必填  |String|3| 信用卡分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br />目前该字段未生效，我司会默认当0处理。                                                                                                                                                                          |
|36|└ fq_channels| 非必填  |String|20| 优先使⽤该资产进⾏⽀付，信用卡分期时传入，目前只支持传alipayfq_cc                                                                                                                                                                                              |
|37|reserved_store_code| 非必填  |String|32| 门店id（目前仅支持支付宝）                                                                                                                                                                                                                      |
|38|reserved_alipay_store_id| 非必填  |String|32| 支付宝店铺编号                                                                                                                                                                                                                             |
|39|reserved_user_truename| 非必填  |String|20| 姓名                                                                                                                                                                                                                                  |
|40|reserved_user_creid| 非必填  |String|48| 身份证                                                                                                                                                                                                                                 |
|41|**`reserved_terminal_info`**| 非必填  |String||终端信息说明字段，见文档中[reserved_terminal_info终端信息说明字段（259号文，终端信息）](/scanpay/transform#reserved_terminal_info终端信息说明字段（259号文，终端信息）)|
|42|reserved_business_params| 非必填  |json|512| 商户传入业务信息，应用于安全，营销等参数直传场景，格式为 json 格式：{"data":"123"}                                                                                                                                                                                 |
|43|reserved_scene_info| 非必填  |json|256| 场景信息说明字段，见文档中[reserved_scene_info场景信息说明字段示例](/scanpay/introduction#reserved_scene_info场景信息说明字段示例)                                                                                                                                   |
|44|reserved_front_url| 非必填  |String|256| 云闪付交易（UNIONPAY、MPAY、UPBXJS）支付成功跳转页面                                                                                                                                                                                                 |
|45|reserved_reserved_front_fail_url| 非必填  |String|256| 云闪付交易（UNIONPAY、MPAY、UPBXJS）支付未完成跳转                                                                                                                                                                                                  |


*reserved_ali_extend_params字段示例：*
```xml
<reserved_ali_extend_params>
{
	"dynamic_token_out_biz_no": "66666",
	"hb_fq_num": "3",
	"industry_reflux_info": {
		"scene_code": "metro_tradeorder",
		"channel": "xxxx",
		"scene_data": {
			"asset_name": "ALIPAY"
		}
	},
	"food_order_type": "qr_order"
}
</reserved_ali_extend_params>
```


#### 微信公众号(小程序)、支付宝服务窗(小程序)统一下单响应报文（sdk开头的参数用于调起H5支付）

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码,000000成功,其他详细参见错误列表|
|2|result_msg|必填|String|128|错误代码描述|
|3|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|5|term_id|非必填|String|8|终端号|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|sub_mer_id|非必填|String||支付通道对应的子商户识别码|
|9|session_id|非必填|String|64|预支付交易会话标识|
|10|qr_code|非必填|String|64|二维码链接<br>`trade_type 为 APPLEPAY、UNIONPAY、JDBT时返回，用以重定向拉起支付`|
|11|sub_appid|非必填|String|32|子商户公众号id|
|12|sub_openid|非必填|String|128|子商户用户标识|
|13|sdk_appid|非必填|String|16|公众号id|
|14|sdk_timestamp|非必填|String|32|时间戳，自1970年1月1日 0点0分0秒以来的秒数|
|15|sdk_noncestr|非必填|String|32|随字符串|
|16|sdk_package|非必填|String|128|订单详情扩展字符串|
|17|sdk_signtype|非必填|String|32|签名方式，trade_type 为 JSAPI、LETPAY时才返回|
|18|sdk_paysign|非必填|String|512|签名|
|19|sdk_partnerid|非必填|String|32|trade_type 为 APP时才返回|
|20|reserved_fy_order_no|非必填|String|30|富友生成的订单号,需要商户与商户订单号进行关联|
|21|reserved_fy_settle_dt|非必填|String|8|富友交易日期|
|22|reserved_transaction_id|非必填|String|64|渠道交易流水号<br>`trade_type 为 FWC、MPAY时返回（用于调起支付）`|
|23|reserved_fy_trace_no|非必填|String|12|富友系统内部追踪号|
|24|reserved_pay_info|非必填|String|不定长|支付参数|
|25|reserved_channel_order_id|非必填|String|64|条码流水号，用户账单二维码对应的流水|
|26|reserved_addn_inf|非必填|String|50|附加数据|

#### 报文示例
*请求报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <reserved_user_truename></reserved_user_truename>
    <reserved_user_mobile></reserved_user_mobile>
    <reserved_fy_term_sn></reserved_fy_term_sn>
    <product_id></product_id>
    <reserved_device_info></reserved_device_info>
    <sub_openid>osgI-t3iTLkEdGhhwTwyYy_QiqFM</sub_openid>
    <term_id>88888888</term_id>
    <random_str>875243e3c24a1e13a29dd30b5a07a7aa</random_str>
    <ins_cd>08A9999999</ins_cd>
    <version>1.0</version>
    <reserved_fy_term_type></reserved_fy_term_type>
    <sub_appid>wx04bdf63c774e12ce</sub_appid>
    <addn_inf></addn_inf>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <reserved_expire_minute></reserved_expire_minute>
    <openid></openid>
    <term_ip>127.0.0.1</term_ip>
    <limit_pay></limit_pay>
    <notify_url>https://mail.qq.com/cgi-bin/frame_html?sid=pEYG5nBgQiNVqANe&amp;r=4a6c47ad7d279a80630dec073cda96e2
    </notify_url>
    <order_amt>1</order_amt>
    <goods_des>111</goods_des>
    <reserved_hb_fq_seller_percent></reserved_hb_fq_seller_percent>
    <trade_type>JSAPI</trade_type>
    <curr_type></curr_type>
    <txn_begin_ts>20201201153156</txn_begin_ts>
    <sign>
        UBdgsA5lTmU1IaRhH5GcHyfHBcREIeGOwn4jRzBqO0LsYKdtPxzvPZ+DUG/60vitGJ53FLHXX9fNFAbaKaHUQDG3baHOkjn6CjsQ9O89oYq+4ptsLxr+/YIBV5dzdOdy/ybh0CzA0Q6FNZQSCnM/5AvMtkNSq/162NEu14yeihk=
    </sign>
    <goods_detail></goods_detail>
    <goods_tag></goods_tag>
    <reserved_fy_term_id></reserved_fy_term_id>
    <reserved_hb_fq_num></reserved_hb_fq_num>
    <reserved_user_creid></reserved_user_creid>
    <mchnt_order_no>2020120115315627534769</mchnt_order_no>
    <reserved_txn_bonus></reserved_txn_bonus>
</xml>

```

*请求报文签名原文*
```text
addn_inf=&curr_type=&goods_des=111&goods_detail=&goods_tag=&ins_cd=08A9999999&limit_pay=&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115315627534769&notify_url=https://mail.qq.com/cgi-bin/frame_html?sid=pEYG5nBgQiNVqANe&r=4a6c47ad7d279a80630dec073cda96e2&openid=&order_amt=1&product_id=&random_str=875243e3c24a1e13a29dd30b5a07a7aa&sub_appid=wx04bdf63c774e12ce&sub_openid=osgI-t3iTLkEdGhhwTwyYy_QiqFM&term_id=88888888&term_ip=127.0.0.1&trade_type=JSAPI&txn_begin_ts=20201201153156&version=1.0
```

*响应报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <ins_cd>08A9999999</ins_cd>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <qr_code></qr_code>
    <random_str>ENE12MLASJFSHT2H6JPLO57Z2CKKK41V</random_str>
    <reserved_addn_inf></reserved_addn_inf>
    <reserved_channel_order_id></reserved_channel_order_id>
    <reserved_fy_order_no></reserved_fy_order_no>
    <reserved_fy_settle_dt></reserved_fy_settle_dt>
    <reserved_fy_trace_no>120186438961</reserved_fy_trace_no>
    <reserved_pay_info></reserved_pay_info>
    <reserved_transaction_id></reserved_transaction_id>
    <result_code>030001</result_code>
    <result_msg>sub_mch_id与sub_appid不匹配</result_msg>
    <sdk_appid></sdk_appid>
    <sdk_noncestr></sdk_noncestr>
    <sdk_package></sdk_package>
    <sdk_partnerid></sdk_partnerid>
    <sdk_paysign></sdk_paysign>
    <sdk_signtype></sdk_signtype>
    <sdk_timestamp></sdk_timestamp>
    <session_id></session_id>
    <sign>
        XmXZlKVBenim1lP+z/tidFIrxZGKfr2zGZh1MCCysYDO35rK3XuIFxEKmFy+NadNSb4JjoSi1s4E+h9qP+Ykc6dauGtnx4kxgii36aKd/+TfuvpJQcX0A1tdktNNALRAaou3dXz3AHpJLo2OIhxR+Ky+a85PHNi4Z1reBfDkwhY=
    </sign>
    <sub_appid>wx04bdf63c774e12ce</sub_appid>
    <sub_mer_id>206502993</sub_mer_id>
    <sub_openid>osgI-t3iTLkEdGhhwTwyYy_QiqFM</sub_openid>
    <term_id></term_id>
</xml>

```

*响应报文签名原文*
```text
ins_cd=08A9999999&mchnt_cd=0002900F0370542&qr_code=&random_str=ENE12MLASJFSHT2H6JPLO57Z2CKKK41V&result_code=030001&result_msg=sub_mch_id与sub_appid不匹配&sdk_appid=&sdk_noncestr=&sdk_package=&sdk_partnerid=&sdk_paysign=&sdk_signtype=&sdk_timestamp=&session_id=&sub_appid=wx04bdf63c774e12ce&sub_mer_id=206502993&sub_openid=osgI-t3iTLkEdGhhwTwyYy_QiqFM&term_id=
```

---



### 3.4 订单查询
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址
*测试地址：* `https://fundwx.payfuiouo2o.com/commonQuery`

*正式地址：*

#### 场景说明

!> 由于不可抗因素导致交易超时，或者状态不明确时，需要商户主动发起交易查询。来确定交易的最终状态。目前系统支持往前查询**`3`**日的交易。即**`D-3 到 D 日`**的交易才能查询到记录。<br /><br />3日前的交易查询请参考[3.10历史订单查询](#_310-历史订单查询)

#### 查询请求报文

|序号|参数名|出现要求|型态|长度| 说明   6                                                                                                                                                                         |
|:-----|:-----|:-----|:-----|:-----|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|1|version|必填|String|8| 1.0                                                                                                                                                                            |
|2|ins_cd|必填|String|20| 机构号,接入机构在富友的唯一代码                                                                                                                                                               |
|3|mchnt_cd|必填|String|15| 商户号,富友分配给二级商户的商户号                                                                                                                                                              |
|4|term_id|必填|String|8| 终端号(没有真实终端号统一填88888888)                                                                                                                                                        |
|5|order_type|必填|String|20| 订单类型:<br />ALIPAY(统一下单、条码支付、服务窗支付)<br />WECHAT(统一下单、条码支付、公众号支付、小程序)<br />UNIONPAY<br />BESTPAY(翼支付)<br>PY68(银联分期-商户贴息)<br>PY69(银联分期-持卡人贴息)<br>WXXS(下单类型为LPXS时使用)<br>JDBT(京东白条) |
|6|mchnt_order_no|必填|String|30| 商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)                                                                                                                                |
|7|random_str|必填|String|32| 随机字符串，不长于32位                                                                                                                                                                   |
|8|sign|必填|String|512| 签名, 详见签名生成算法                                                                                                                                                                   |

#### 查询响应报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码,000000成功,其他详细参见错误列表|
|2|result_msg|必填|String|128|错误代码描述|
|3|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|5|term_id|非必填|String|8|终端号|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|buyer_id|非必填|String|128|买家在渠道(微信、支付宝)的用户ID|
|9|order_type|必填|String|20|订单类型:<br>JSAPI--公众号支付<br/>FWC--支付宝服务窗、支付宝小程序<br/>LETPAY-微信小程序<br/>MPAY--云闪付小程序（控件支付）<br/>UPBXJS--云闪付保险缴费<br/>ALIPAY(支付宝)<br/>WECHAT(微信)<br/>UNIONPAY(银联二维码)<br/>BESTPAY(翼支付)<br/>DIGICCY(数字货币)<br>PY68(银联分期-商户贴息)<br/>PY69(银联分期-持卡人贴息)|
|10|trans_stat|必填|String|32|查询状态，详见[应答码表--查询应答码](/scanpay/code#查询应答码)|
|11|order_amt|必填|Number|9|订单金额，单位为**`分`**|
|12|transaction_id|必填|String|64|渠道订单号|
|13|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)|
|14|addn_inf|非必填|String|50|附加数据|
|15|reserved_fy_settle_dt|必填|String|8|富友交易日期|
|16|reserved_coupon_fee|非必填|String|10|优惠金额**`(分)`**|
|17|reserved_buyer_logon_id|非必填|String|128|买家在渠道登录账号|
|18|reserved_fund_bill_list|非必填|String|不定长|支付宝交易资金渠道，详细渠道|
|19|reserved_fy_trace_no|非必填|String|12|富友系统内部追踪号|
|20|reserved_channel_order_id|非必填|String|64|条码流水号，用户账单二维码对应的流水|
|21|reserved_fy_term_id|非必填|String|8|富友终端号(富友终端号与TUSN号二选一)|
|22|reserved_is_credit|非必填|String|8|1--表示信用卡~~或者花呗~~<br />0--表示其他(非信用方式)<br />不填，表示未知|
|23|reserved_txn_fin_ts|非必填|String|14|用户支付时间yyyyMMddHHmmss|
|24|reserved_settlement_amt|非必填|Number|16|应结算订单金额，分为单位的整数。<br />只有成功交易才会返回<br />如果使用了商户免充值优惠券，该值为订单金额-商户免充值<br />如果没有使用商户免充值，该值等于订单金额|
|25|**`reserved_bank_type`**|非必填|String|16|付款方式|
|26|**`reserved_promotion_detail`**|非必填|String|6000|微信营销详情（资金单位：分），见文档中[reserved_promotion_detail说明字段](scanpay/introduction#reserved_promotion_detail说明字段)|
|27|reserved_voucher_detail_list|非必填|String|-|支付宝交易支付时所使用的所有优惠券信息（资金单位：元） <br>"[{"amount":"1.00","merchant_contribute":"1.00",<br/>"name":"1.00元代金券","other_contribute":"0.00",<br/>"template_id":"20221107000730017617007JGOUG",<br>"id":"202211070007300282330EB2Y9QQ",<br/>"type":"ALIPAY_BIZ_VOUCHER"}]"|
|28|reserved_discount_goods_detail|非必填|String|-|支付宝交易支付所使用的单品券优惠的商品优惠信息（资金单位：元）<br/> "[{\"goodsId\":\"STANDARD1026181538\",\" goodsName\":\"雪碧",\"discountAmount\":\"10.00\"}]"|
|29|reserved_hb_is_seller|非必填|String|1|返回值：1=商户出息|
|30|reserved_service_charge_flag|非必填|String|1|手续费减免标识<br>Y：表示减免（默认）<br>N：表示不减免|
|31|reserved_sub_mchnt_cd|非必填|String||交易子商户号|
|32|reserved_trade_type|非必填|String||交易类型<br>ALBS支付宝被扫、WXBS微信被扫、WXFP微信刷脸、ALFP支付宝刷脸、|

<!--|27|**`reserved_risk_info`**|非必填|json||见文档中[reserved_risk_info说明字段](/scanpay/transform#reserved_risk_info说明字段)|
-->

#### 报文示例

*请求报文*

```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <sign>
        KERtorHOmzWl9mlB5CI6jpQTkRZPVB1z+dZotqgG/dRRE5TaN/SC9AG3yV7RA73HlQZnJR3/68d7tyDioGPTp3b7SSjhgp3v/BbZtn7nNFcA0Mg0XQjSlEA2G3LK1njan82X/9ocqtaPvpt0XrD3D2w7rIVWhVPajpCQnARtmIE=
    </sign>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <term_id>12345678</term_id>
    <random_str>4ad630c1565608452dc2e818755a853e</random_str>
    <mchnt_order_no>2020120115294222446445</mchnt_order_no>
    <ins_cd>08A9999999</ins_cd>
    <version>1</version>
    <order_type>WECHAT</order_type>
</xml>

```

*请求报文签名原文*
```text
ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115294222446445&order_type=WECHAT&random_str=4ad630c1565608452dc2e818755a853e&term_id=12345678&version=1
```


*响应报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <addn_inf></addn_inf>
    <buyer_id>oi_0ms6QTI61lzX647RDjHhcVDBY</buyer_id>
    <ins_cd>08A9999999</ins_cd>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <mchnt_order_no>2020120115294222446445</mchnt_order_no>
    <order_amt>1</order_amt>
    <order_type>WECHAT</order_type>
    <random_str>F6P6XQR8QKNS3HHONR3TY6T00ANC7ZWH</random_str>
    <reserved_bank_type>COMM_DEBIT</reserved_bank_type>
    <reserved_buyer_logon_id></reserved_buyer_logon_id>
    <reserved_channel_order_id>2020120115294222446445</reserved_channel_order_id>
    <reserved_coupon_fee>0</reserved_coupon_fee>
    <reserved_fund_bill_list></reserved_fund_bill_list>
    <reserved_fy_settle_dt>20201201</reserved_fy_settle_dt>
    <reserved_fy_trace_no>120186438960</reserved_fy_trace_no>
    <reserved_is_credit>0</reserved_is_credit>
    <reserved_settlement_amt>1</reserved_settlement_amt>
    <reserved_txn_fin_ts>20201201152946</reserved_txn_fin_ts>
    <result_code>000000</result_code>
    <result_msg>SUCCESS</result_msg>
    <sign>
        Ky1ITNPASHlHb3pfAJcUfDgAOCi3p5lDABXU4ej0Oe29hWqnKJFLwsHjKqKPBbD6vzkRouGGQUVPPXEDIgc0h5+krEl+nJDUG7+UEKWZqN7X5rPZyB5UBTLrkZJTTOmqXnyyV6UG9gnsg36/v/6yunN4mUt5hbysvXtVUiSjEoE=
    </sign>
    <term_id>12345678</term_id>
    <trans_stat>SUCCESS</trans_stat>
    <transaction_id>4200000747202012013659872912</transaction_id>
</xml>

```

*响应报文签名原文*
```text
addn_inf=&buyer_id=oi_0ms6QTI61lzX647RDjHhcVDBY&ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115294222446445&order_amt=1&order_type=WECHAT&random_str=F6P6XQR8QKNS3HHONR3TY6T00ANC7ZWH&result_code=000000&result_msg=SUCCESS&term_id=12345678&trans_stat=SUCCESS&transaction_id=4200000747202012013659872912
```

---


### 3.5 支付结果通知
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `商户报文所填notify_url`

*正式地址：* `商户报文所填notify_url`

#### 场景说明

![支付结果通知回调流程图](_images/callBack-process.jpg '支付结果通知回调流程图')

!> 如图中异步回调，下单(主扫)交易的结果是以异步的形式进行回调的。富友在接受到支付宝等支付通道的回调结果以后再回调商户。

!> `若在接收到富友回调之前发送了查询，并查到了交易的具体交易结果，富友则不再发送支付结果通知。商户接收回调成功处理成功后返回字符串1，后富友停止回调给商户。最多回调6次，每次间隔30S。`

!> **重要!!! 重要!!! 重要!!!**：**`不保证通知最终一定能成功`**，在订单状态不明或者没有收到支付结果通知的情况下，建议商户主动调用【3.4 订单查询】确认订单状态<br />只有`主扫统一下单`、`公众号/服务窗统一下单`会通过此接口发异步通知，`条码支付没有异步通知`

?> 回调形式：req={encode之后的xml报文}<br><br>收到回调后直接在 request 里面获取req参数的值，然后decode一下就得到xml格式报文

#### 支付结果通知请求报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码,000000成功,其他详细参见错误列表|
|2|result_msg|必填|String|128|错误代码描述|
|3|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|5|term_id|非必填|String|8|终端号|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|user_id|非必填|String|128|用户在商户的id|
|9|order_amt|必填|Number|9|订单金额，单位为**`分`**|
|10|settle_order_amt|必填|Number|16|应结订单金额|
|11|curr_type|非必填|String|3|货币类型,默认人民币：CNY|
|12|transaction_id|必填|String|64|渠道订单号|
|13|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)|
|14|order_type|非必填|String|20|订单类型:<br/>JSAPI--公众号支付<br/>FWC--支付宝服务窗、支付宝小程序<br/>LETPAY-微信小程序<br/>MPAY--云闪付小程序（控件支付）<br/>UPBXJS--云闪付保险缴费<br/>ALIPAY(支付宝)<br/>WECHAT(微信)<br/>UNIONPAY(银联二维码)<br/>BESTPAY(翼支付)<br/>DIGICCY(数字货币)<br>PY68(银联分期-商户贴息)<br/>PY69(银联分期-持卡人贴息)<br>JDBT(京东白条)|
|15|txn_fin_ts|必填|String|14|支付完成时间，订单支付时间，格式为yyyyMMddHHmmss|
|16|reserved_fy_settle_dt|必填|String|8|富友交易日期|
|17|reserved_coupon_fee|非必填|String|10|优惠金额**`(分)`**|
|18|reserved_buyer_logon_id|非必填|String|128|买家在渠道登录账号|
|19|reserved_fund_bill_list|非必填|String|不定长|支付宝交易资金渠道，详细渠道|
|20|reserved_fy_trace_no|非必填|String|12|富友系统内部追踪号|
|21|reserved_channel_order_id|非必填|String|64|条码流水号，用户账单二维码对应的流水|
|22|reserved_is_credit|非必填|String|8|1--表示信用卡~~或者花呗~~<br />0--表示其他(非信用方式)<br />不填，表示未知|
|23|reserved_addn_inf|非必填|String|50|附加数据|
|24|reserved_settlement_amt|非必填|Number|16|应结算订单金额，分为单位的整数。<br />只有成功交易才会返回<br />如果使用了商户免充值优惠券，该值为订单金额-商户免充值<br />如果没有使用商户免充值，该值等于订单金额|
|25|**`reserved_bank_type`**|非必填|String|16|付款方式|
|26|**`reserved_promotion_detail`**|非必填|String|6000|微信营销详情（资金单位：分），见文档中[reserved_promotion_detail说明字段](scanpay/introduction#reserved_promotion_detail说明字段)|
|27|reserved_hb_is_seller|非必填|String|1|返回值：1=商户出息|
|28|reserved_service_charge_flag|非必填|String|1|手续费减免标识<br>Y：表示减免（默认）<br>N：表示不减免|
|29|reserved_voucher_detail_list|非必填|String|-|支付宝交易支付时所使用的所有优惠券信息（资金单位：元） <br>"[{"amount":"1.00","merchant_contribute":"1.00",<br/>"name":"1.00元代金券","other_contribute":"0.00",<br/>"template_id":"20221107000730017617007JGOUG",<br>"id":"202211070007300282330EB2Y9QQ",<br/>"type":"ALIPAY_BIZ_VOUCHER"}]"|
|30|reserved_discount_goods_detail|非必填|String|-|支付宝交易支付所使用的单品券优惠的商品优惠信息（资金单位：元）<br/> "[{\"goodsId\":\"STANDARD1026181538\",\" goodsName\":\"雪碧",\"discountAmount\":\"10.00\"}]"|
|31|reserved_sub_mchnt_cd|非必填|String||交易子商户号|

<!--|27|**`reserved_risk_info`**|非必填|json||见文档中[reserved_risk_info说明字段](/scanpay/transform#reserved_risk_info说明字段)|
-->

#### 支付结果通知响应报文

*直接往流里写入数字：1*

#### 报文示例

*请求报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <curr_type>CNY</curr_type>
    <ins_cd>08M0026557</ins_cd>
    <mchnt_cd>0005840F2316822</mchnt_cd>
    <mchnt_order_no>14202019071254529897</mchnt_order_no>
    <order_amt>10</order_amt>
    <order_type>LETPAY</order_type>
    <random_str>O28ZNTTVIS4CCNMO8CF3GLALC86T3P22</random_str>
    <reserved_addn_inf></reserved_addn_inf>
    <reserved_bank_type>CMB_CREDIT</reserved_bank_type>
    <reserved_buyer_logon_id></reserved_buyer_logon_id>
    <reserved_channel_order_id>14202019071254529897</reserved_channel_order_id>
    <reserved_coupon_fee>0</reserved_coupon_fee>
    <reserved_fund_bill_list></reserved_fund_bill_list>
    <reserved_fy_settle_dt>20190712</reserved_fy_settle_dt>
    <reserved_fy_trace_no>071232502573</reserved_fy_trace_no>
    <reserved_is_credit>1</reserved_is_credit>
    <reserved_settlement_amt>10</reserved_settlement_amt>
    <result_code>000000</result_code>
    <result_msg>SUCCESS</result_msg>
    <settle_order_amt>10</settle_order_amt>
    <sign>FVEzQWnZqVXEyHKwmCHfeGtGNGTKKL27PuIpK7KXe9oVHaBEpYQ+PECy4Dztv6/AlTmF+Ff2JwQRN7Gyl1Xu6GvJO7ig4X
        kEVwXONwktIdyY9chrBos8oomN9N7X+0p5vetGg2Bu5lHlvxD4PQ9q3aOXgg0t2xQkj/yqNllATE=
    </sign>
    <term_id></term_id>
    <transaction_id>4200000313201907123538202724</transaction_id>
    <txn_fin_ts>20190712194238</txn_fin_ts>
    <user_id>o4cbx5MBeXj4Ct8m-4DR40fgx37k</user_id>
</xml>
```

请求报文签名原文
```text
curr_type=CNY&ins_cd=08M0026557&mchnt_cd=0005840F2316822&mchnt_order_no=14202019071254529897&order_amt=10&order_type=LETPAY&random_str=O28ZNTTVIS4CCNMO8CF3GLALC86T3P22&result_code=000000&result_msg=SUCCESS&settle_order_amt=10&term_id=&transaction_id=4200000313201907123538202724&txn_fin_ts=20190712194238&user_id=o4cbx5MBeXj4Ct8m-4DR40fgx37k
```

---



### 3.6 退款申请
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/commonRefund`

*正式地址：*

#### 场景说明

!> 当交易发生之后一段时间内，由于买家或者卖家的原因需要退款时，卖家可以通过退款接口将支付款退还给买家，扫码支付平台将在收到退款请求并且验证成功之后，按照退款规则将支付款按原路退到买家帐号上。<br /><br />总退款金额不能超过订单总金额。

!> 注：目前. 微信 360天、支付宝（ 银联300天，网联180天)、银联二维码/翼支付 180天，支持全额或者是多次部分退款。
<br>如需支持360天内的退款，需要在请求报文中填写：reserved_origi_dt(原交易日期，格式：yyyyMMdd)<br>**`不填该值，支持30天内的交易进行退款。`**<br><!--**`填写该值，支持180天`**5月份开始，退款支持180天--><br>                ● 用户贴息交易order_type=JDBT,已经还的息费不退，未还可退；

​                ● 商户贴息交易order_type=JDBTSH,交易退款时间超过15天，则不退还手续费

​                ● 退款路径：白条遵循原返规则，用户当期有待还会直接冲抵账单，如果用户当期账单已经还清，直接退至用户最近的一张还款银行卡

#### 退款请求报文

|序号|参数名|出现要求|型态|长度| 说明                                                                                                                                                               |
|:-----|:-----|:-----|:-----|:-----|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
|1|version|必填|String|8| 1.0                                                                                                                                                              |
|2|ins_cd|必填|String|20| 机构号,接入机构在富友的唯一代码                                                                                                                                                 |
|3|mchnt_cd|必填|String|15| 商户号,富友分配给二级商户的商户号                                                                                                                                                |
|4|term_id|必填|String|8| 终端号(没有真实终端号统一填88888888)                                                                                                                                          |
|5|mchnt_order_no|必填|String|30| 原正交易商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)                                                                                                          |
|6|random_str|必填|String|32| 随机字符串                                                                                                                                                            |
|7|sign|必填|String|512| 签名, 详见签名生成算法                                                                                                                                                     |
|8|order_type|必填|String|20| 订单类型:<br />ALIPAY(统一下单、条码支付、服务窗支付)<br />WECHAT(统一下单、条码支付、公众号支付、小程序)<br />UNIONPAY<br />BESTPAY(翼支付)<br>PY68(银联分期-商户贴息)<br>PY69(银联分期-持卡人贴息)<br>WXXS(下单类型为LPXS时使用)<br>JDBT(京东用户贴息)<br>JDBTSH（京东白条商户贴息） |
|9|refund_order_no|必填|String|30| 商户退款单号<br />(5到30个字符、只能包含字母数字或者下划线，区分大小写)(也要加订单前缀)                                                                                                               |
|10|total_amt|必填|Number|9| 总金额                                                                                                                                                              |
|11|refund_amt|必填|Number|9| 退款金额                                                                                                                                                             |
|12|operator_id|非必填|String|32| 操作员                                                                                                                                                              |
|13|reserved_fy_term_id|非必填|String|8| 富友终端号(富友终端号与TUSN号二选一)                                                                                                                                            |
|14|reserved_origi_dt|非必填|String|8| 原交易日期(yyyyMMdd)！该值必定等于reserved_fy_settle_dt(富友接收交易时间。理论和合作方下单时间一致。微量跨日交易会不一致)。<br />不填该值，支持30天内的交易进行退款。<br />填写该值，支持360天内的退款。                                    |
|15|reserved_addn_inf|非必填|String|50| 附加数据                                                                                                                                                             |
|16|reserved_refund_desc|非必填|String|40| 退款原因（微信支付；支付宝app不展示，PC端才展示；云闪付不支持） |


#### 退款响应报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码,000000成功,其他详细参见错误列表|
|2|result_msg|必填|String|128|错误代码描述|
|3|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|5|term_id|非必填|String|8|终端号|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|order_type|必填|String|20|订单类型:ALIPAY，WECHAT，BESTPAY，UNIONPAY|
|9|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)|
|10|refund_order_no|必填|String|30|商户退款单号<br />(5到30个字符、只能包含字母数字或者下划线，区分大小写)|
|11|transaction_id|必填|String|64|渠道订单号|
|12|refund_id|必填|String|64|渠道退款流水号|
|13|reserved_fy_settle_dt|必填|String|8|富友交易日期|
|14|reserved_refund_amt|非必填|String|9|退款金额|
|15|reserved_fy_trace_no|非必填|String|12|富友系统内部追踪号|
|16|reserved_anct_type|非必填|String|64|退款账户类型（1:退款专用户）|

#### 报文示例

*请求报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <operator_id></operator_id>
    <random_str>46cc0563a6a2699a20f1876b6889430e</random_str>
    <term_id>88888888</term_id>
    <reserved_origi_dt></reserved_origi_dt>
    <ins_cd>08A9999999</ins_cd>
    <version>1.0</version>
    <refund_amt>1</refund_amt>
    <sign>
        NMhzl2h3m4+uYL3iPw1mwoN1u2lANgPAIL93A263UgbtVycE6QXI/aJ315QAfQas0PuoMLzxwBKIZVBF1UmrF+kAGnWe1GA+RAfVV0AI8yoZmyTjcYSEBgs3aq4RzNyU1kcatgcwzMG1tnv3iXCaw1lFThKJ4F28QNRaDQ4jT78=
    </sign>
    <reserved_fy_term_id></reserved_fy_term_id>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <total_amt>1</total_amt>
    <refund_order_no>2020120115362692413627_refund</refund_order_no>
    <mchnt_order_no>2020120115294222446445</mchnt_order_no>
    <order_type>WECHAT</order_type>
</xml>

```

*请求报文签名原文*
```text
ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115294222446445&operator_id=&order_type=WECHAT&random_str=46cc0563a6a2699a20f1876b6889430e&refund_amt=1&refund_order_no=2020120115362692413627_refund&term_id=88888888&total_amt=1&version=1.0
```

*响应报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <ins_cd>08A9999999</ins_cd>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <mchnt_order_no>2020120115294222446445</mchnt_order_no>
    <order_type>WECHAT</order_type>
    <random_str>XNHAK3QC09N78X3PWJOZV26NA2ODLKX4</random_str>
    <refund_id>50100006672020120104376121338</refund_id>
    <refund_order_no>2020120115362692413627_refund</refund_order_no>
    <reserved_fy_settle_dt>20201201</reserved_fy_settle_dt>
    <reserved_fy_trace_no>120186438962</reserved_fy_trace_no>
    <reserved_refund_amt>1</reserved_refund_amt>
    <result_code>000000</result_code>
    <result_msg>SUCCESS</result_msg>
    <sign>
        TGgEe75W1hvMuvozZZ6JbW3hLBxRewHu5xO+iuAjyWfspUoVqzvSw74KMMD2v8NbwafFdG6vvH4Q6S8AiyPuvXtJMvYkg9XUov1ywjqVnhuCaWVcwHzEyCHQzxQhtJUIxEnO/Bw1Mlt1AxwyZWnlIGTDJJYljQxewMHgZHiN4+U=
    </sign>
    <term_id>88888888</term_id>
    <transaction_id>4200000747202012013659872912</transaction_id>
</xml>

```

*响应报文签名原文*
```text
ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115294222446445&order_type=WECHAT&random_str=XNHAK3QC09N78X3PWJOZV26NA2ODLKX4&refund_id=50100006672020120104376121338&refund_order_no=2020120115362692413627_refund&result_code=000000&result_msg=SUCCESS&term_id=88888888&transaction_id=4200000747202012013659872912
```

---



### 3.7 关闭订单
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/closeorder`

*正式地址：*

#### 场景说明

!> 本接口支持微信、支付宝各种下单类交易(order_type)的关闭。**`只有未支付的订单才能发起关闭`**。

[//]: # (!> 注意：订单生成后不能马上调用关单接口，最短调用时间间隔为`5`分钟。)

?> **支付宝主扫调用关单接口**<br><br>如果未扫码直接关单的话会关单不成功的，一定要扫码后未支付才能调用关单接口。支付宝动态码的交易，下单成功（返回动态码链接只是对应支付宝的预下单，并不是下单），因此在查询、关单的时候支付宝会返回“交易不存在”， 用户扫码以后才会去下单。<br><br>即，调用了统一下单接口后，产生的qr_code二维码链接，必须扫一下那个链接，支付宝那边才算生成了订单，不付款，才能调用关单接口。

?> **微信主扫调用关单接口**<br><br>微信主扫，调用了统一下单接口，就可以直接关单，不需要去扫qr_code产生的二维码链接

#### 关单请求报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|version|必填|String|8|1.0|
|2|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|3|mchnt_cd|必填|String|15|商户号,富友分配给二级商户的商户号|
|4|term_id|必填|String|8|终端号(没有真实终端号统一填88888888)|
|5|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|order_type|必填|String|20|订单类型:<br />ALIPAY(统一下单、服务窗支付)<br />WECHAT(统一下单、公众号支付、小程序支付)<br />UNIONPAY<br />WXAPP(微信 app)<br />WXH5(微信 h5)|
|9|sub_appid|非必填|String|32|子商户公众号id，子商户配置多个公众号时必填|

#### 关单响应报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码,000000成功,其他详细参见错误列表|
|2|result_msg|必填|String|128|错误代码描述|
|3|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|5|term_id|非必填|String|8|终端号|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|order_type|必填|String|20|订单类型:ALIPAY，WECHAT，BESTPAY，UNIONPAY|
|9|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)|

#### 报文示例

*请求报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <sign>
        Hl0N2doK6czBf/9Gt/Qc9XGEHnBSL+SlirFLmCUwP5+iBMjoWMuziQ/+P5yemgnKmR68G0eXeDsm7O7ikMU+EwtUWHJz2PCW/KYuxQNmMZMNoMdffS/Af8V8VXUeGQpKURu1VieettdGt4tXTDg/DH81XiOJrkwsf83T8acGUto=
    </sign>
    <sub_appid></sub_appid>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <term_id>12345678</term_id>
    <random_str>5360cbddc4e1165196d71f4f003ad0ba</random_str>
    <mchnt_order_no>2020120115455553865290</mchnt_order_no>
    <ins_cd>08A9999999</ins_cd>
    <version>1</version>
    <order_type>WECHAT</order_type>
</xml>

```

*请求报文签名原文*
```text
ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115455553865290&order_type=WECHAT&random_str=5360cbddc4e1165196d71f4f003ad0ba&sub_appid=&term_id=12345678&version=1
```

*响应报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <ins_cd>08A9999999</ins_cd>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <mchnt_order_no>2020120115455553865290</mchnt_order_no>
    <order_type>WECHAT</order_type>
    <random_str>396TATUYWMODSBOPH2L93B4N52GM4IYK</random_str>
    <result_code>000000</result_code>
    <result_msg>CLOSED</result_msg>
    <sign>
        brFKDV0nJef4nbwk3Odwhm0tA/6Z59nnOSwfAByV8s69+LgQlWvxRxgN8FwSdBbugo7DpaIIuZYiFNHtEsyL5zBMXphgsFGaKJxLwIOZURq0yM1Vr9pcga2YCtpNClFE9OsjRVyfaGrXpOCsNxh00JU22crTOi+6aZimnjYRMYA=
    </sign>
    <term_id></term_id>
</xml>

```

*响应报文签名原文*
```text
ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115455553865290&order_type=WECHAT&random_str=396TATUYWMODSBOPH2L93B4N52GM4IYK&result_code=000000&result_msg=CLOSED&term_id=
```



---



### 3.8 撤销接口
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/cancelorder`

*正式地址：*

#### 场景说明

!> 本接口只支持`条码支付`撤销。<br>**建议先发起查询确认订单状态未支付(或者支付中)后发起撤销**。
`如果富友测原交易状态成功，该接口功能同退款接口，但还是建议使用退款接口`<br/>
`注意：成功交易调用此接口会退款，请对接合作方、商户等接口调用方谨慎调用，尤其在定时任务、预期自动跑批等场景。`
<br />建议：原交易请求完至少`15S`后发起撤销。<br />目前仅支持撤销`当日`交易。

#### 撤销请求报文
|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|version|必填|String|8|1.0|
|2|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|3|mchnt_cd|必填|String|15|商户号,富友分配给二级商户的商户号|
|4|term_id|必填|String|8|终端号(没有真实终端号统一填88888888)|
|5|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|order_type|必填|String|20|订单类型:<br />ALIPAY(条码支付)<br />WECHAT(条码支付)<br>UNIONPAY<br />JDBT(京东用户贴息)<br/>JDBTSH（京东白条商户贴息）|
|9|cancel_order_no|必填|String|30|商户撤销单号|
|10|operator_id|非必填|String|32|操作员|
|11|reserved_fy_term_id|非必填|String|8|富友终端号(富友终端号与TUSN号二选一)|

#### 撤销响应报文
|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码,000000成功,其他详细参见错误列表|
|2|result_msg|必填|String|128|错误代码描述|
|3|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|5|term_id|非必填|String|8|终端号|
|6|random_str|必填|String|32|随机字符串|
|7|sign|必填|String|512|签名, 详见签名生成算法|
|8|order_type|必填|String|20|订单类型|
|9|mchnt_order_no|必填|String|30|原交易商户订单号|
|10|cancel_order_no|必填|String|30|商户撤销单号|
|11|transaction_id|必填|String|32|渠道交易流水号|
|12|cancel_id|非必填|String|32|渠道撤销流水号|
|13|fund_change|非必填|String|8|是否有资金流向(只有支付宝有该字段)|
|14|recall|非必填|String|8|是否需要新调用撤销(当为Y时，需要重新调用撤销接口)|
|15|reserved_fy_settle_dt|必填|String|8|富友交易日期|
|16|reserved_fy_trace_no|非必填|String|12|富友系统内部追踪号|

#### 报文示例

*请求报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <sign>
        cFuyzNX/bFxiusQuz1U3VjVPJwIfGgggtLEuLtrYDJRP/G+fEzF/VFh0jdYI49vv8MWSFtn5HfyHqTa/9Cl+I7odu/jfYpsjhpdrvQ4QKQBoKJzmZT5/41Afv65lPTV09qbTu1VCjQE7A1jmblE0UdTnuqdytIZ+3ztr/sVqDWw=
    </sign>
    <reserved_fy_term_id></reserved_fy_term_id>
    <operator_id></operator_id>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <term_id>12345678</term_id>
    <random_str>0707c5a27d1732fdb2e1a3df246558a9</random_str>
    <cancel_order_no>2020120115410800368877_cancel</cancel_order_no>
    <mchnt_order_no>2020120115402317114302</mchnt_order_no>
    <ins_cd>08A9999999</ins_cd>
    <version>1</version>
    <order_type>WECHAT</order_type>
</xml>

```

*请求报文签名原文*
```text
cancel_order_no=2020120115410800368877_cancel&ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115402317114302&operator_id=&order_type=WECHAT&random_str=0707c5a27d1732fdb2e1a3df246558a9&term_id=12345678&version=1
```

*响应报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <cancel_id></cancel_id>
    <cancel_order_no>2020120115410800368877_cancel</cancel_order_no>
    <fund_change></fund_change>
    <ins_cd>08A9999999</ins_cd>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <mchnt_order_no>2020120115402317114302</mchnt_order_no>
    <order_type>WECHAT</order_type>
    <random_str>YJ50AF72CWIMFH7MIS7D9K60MGTOW7Z7</random_str>
    <recall>N</recall>
    <reserved_fy_settle_dt>20201201</reserved_fy_settle_dt>
    <reserved_fy_trace_no>120186438997</reserved_fy_trace_no>
    <result_code>000000</result_code>
    <result_msg>订单已撤销</result_msg>
    <sign>
        c40IOKhazSvNjBmSLUhI2qT2xyrJbajiwtFKpdXxCwmQ67wi03/B1FjMEK+a9pvAHkefCGigWUh1kGYoBuFjB2pvNcdCpvrtaRB/YPLdMdBwC+UpgSvTMlrZ15qEX1DGFCIajNsUe6tsj3flnm4zv8R0bjPNFE5Ok8uj3ub0o/0=
    </sign>
    <term_id></term_id>
    <transaction_id></transaction_id>
</xml>

```

*响应报文签名原文*
```text
cancel_id=&cancel_order_no=2020120115410800368877_cancel&fund_change=&ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020120115402317114302&order_type=WECHAT&random_str=YJ50AF72CWIMFH7MIS7D9K60MGTOW7Z7&recall=N&result_code=000000&result_msg=订单已撤销&term_id=&transaction_id=
```

---


### 3.9 退款查询
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/refundQuery`

*正式地址：*

#### 场景说明

?> 本接口支持所有退款交易查询(时间限制：`3日内`)

#### 退款查询请求报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|version|必填|String|8|1.0|
|2|ins_cd|必填|String|20|机构号，接入机构在富友的唯一代码|
|3|mchnt_cd|必填|String|15|富友分配的商户号|
|4|term_id|必填|String|8|终端号|
|5|refund_order_no|必填|String|30|商户系统内部的订单号|
|6|random_str|必填|String|32|随机字符串，不长于32位|
|7|sign|必填|String|512|签名，详见签名生成算法|

#### 退款查询响应报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码， 000000成功，其他详细参见错误列表|
|2|result_msg|非必填|String|128|返回信息，返回错误原因|
|3|trans_stat|必填|String|32|交易状态<br />SUCCESS—退款成功<br />PAYERROR--退款失败|
|4|ins_cd|必填|String|20|机构号，接入机构在富友的唯一代码|
|5|mchnt_cd|必填|String|15|富友分配的商户号|
|6|term_id|非必填|String|8|终端号|
|7|random_str|必填|String|32|随机字符串，不长于32位|
|8|sign|必填|String|512|签名，详见签名生成算法|
|9|order_type|必填|String|20|订单类型:ALIPAY，WECHAT，JD(京东钱包)，QQ(QQ钱包)，UNIONPAY，|
|10|mchnt_order_no|必填|String|30|商户订单号|
|11|refund_order_no|必填|String|32|商户退款单号|
|12|transaction_id|非必填|String|32|渠道交易流水号|
|13|refund_id|非必填|String|32|渠道退款流水号|
|14|reserved_fy_settle_dt|必填|String|8|富友交易日期|
|15|reserved_fy_trace_no|非必填|String|12|富友系统内部追踪号|
|16|reserved_refund_amt|非必填|Number|16|退款金额|
|17|reserved_promotion_detail|非必填|String||优惠信息|
|18|reserved_anct_type|非必填|String|64|退款账户类型（1:退款专用户）|

#### 报文示例

*请求报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <sign>
        TIjqUUz9EtXUeMpmYEnJAUJu0tO36lVzjrMi68M48PxxKDeSzN3YiWZpuFJBkhNEeUDJsVhmT5LBrsMG84GI6ZazSu9fLa+TeeHAfBnUZkgSuHxeIMiDgXR1dWc0IJqSJbGXHBa/JXkIN/RF6Zia+znOwAByxVu81N5d5kD6bcg=
    </sign>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <term_id>12345678</term_id>
    <random_str>35ffa26a694340ea09f122f398bf077b</random_str>
    <refund_order_no>2020100911324975038342_refund</refund_order_no>
    <ins_cd>08A9999999</ins_cd>
    <version>1</version>
</xml>
```

*请求报文签名原文*
```text
ins_cd=08A9999999&mchnt_cd=0002900F0370542&random_str=35ffa26a694340ea09f122f398bf077b&refund_order_no=2020100911324975038342_refund&term_id=12345678&version=1
```

*响应报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <ins_cd>08A9999999</ins_cd>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <mchnt_order_no>2020100911285487257295</mchnt_order_no>
    <order_type>WECHAT</order_type>
    <random_str>O8HDNAA8WI58UFDRTGBG0DDAJQQR9EQT</random_str>
    <refund_id>50100706022020100903144575196</refund_id>
    <refund_order_no>2020100911324975038342_refund</refund_order_no>
    <reserved_fy_settle_dt>20201009</reserved_fy_settle_dt>
    <reserved_fy_trace_no>100063825481</reserved_fy_trace_no>
    <reserved_refund_amt>1</reserved_refund_amt>
    <result_code>000000</result_code>
    <result_msg>SUCCESS</result_msg>
    <sign>
        erkaG7BJr9Tvcp3oBqloRIJ6sTI+kdKjYQupIXz9kNtsbUtOnTyK2jhaBhI8FtUBiblRkR3d6G7E2uJU1zgowHgG57ACjozGOaj87tuz0dF+r50CfuxZNNr6yWeLohyuYAOyGA51nJ/d+v7al8F5xoDeuMDqDgZB768ffuRj0/Y=
    </sign>
    <term_id>88888888</term_id>
    <trans_stat>SUCCESS</trans_stat>
    <transaction_id>4200000700202010090766639303</transaction_id>
</xml>
```

*响应报文签名原文*
```text
ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020100911285487257295&order_type=WECHAT&random_str=O8HDNAA8WI58UFDRTGBG0DDAJQQR9EQT&refund_id=50100706022020100903144575196&refund_order_no=2020100911324975038342_refund&result_code=000000&result_msg=SUCCESS&term_id=88888888&trans_stat=SUCCESS&transaction_id=4200000700202010090766639303
```

---


### 3.10 历史订单查询
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/hisTradeQuery`

*正式地址：*

#### 场景说明

?> 本接口提供给商户查询历史交易记录。查询实时交易状态、结果(当前交易)须使用[3.4订单查询](#_34-订单查询)

!> `注`：mchnt_order_no、channel_order_id、transaction_id三选一

#### 历史订单查询请求报文

|序号|参数名|出现要求|型态|长度| 说明                                                                                                                                                                                         |
|:-----|:-----|:-----|:-----|:-----|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|1|version|必填|String|8| 1.0                                                                                                                                                                                        |
|2|ins_cd|必填|String|20| 机构号，接入机构在富友的唯一代码                                                                                                                                                                           |
|3|mchnt_cd|必填|String|15| 富友分配的商户号                                                                                                                                                                                   |
|4|term_id|必填|String|8| 富友分配终端号                                                                                                                                                                                    |
|5|order_type|必填|String|20| 订单类型：<br />`ALIPAY`(统一下单、条码支付、服务窗支付)<br />`WECHAT`(统一下单、条码支付、公众号支付)<br />`UNIONPAY`(银联)<br />`WXAPP`(微信app)<br />`ALIAPP`(支付宝app)<br />`WXH5`(微信h5)<br />`ALIH5`(支付宝h5)<br />`WXXS`(小程序线上)<br>JDBT(京东白条) |
|6|trade_dt|非必填|String||交易日期，非必填，默认当天。<br />查询往日交易时，必填，支持查询90天内交易|
|7|mchnt_order_no|非必填|String|30| 商户系统内部的订单号                                                                                                                                                                                 |
|8|channel_order_id|非必填|String|32| 条码流水(线下支付，条码流水号)                                                                                                                                                                           |
|9|transaction_id|非必填|String|32| 渠道订单号(微信、支付宝流水号)                                                                                                                                                                           |
|10|random_str|必填|String|32| 随机字符串，不长于32位                                                                                                                                                                               |
|11|sign|必填|String|512| 签名，详见签名生成算法                                                                                                                                                                                |

#### 历史订单查询响应报文 (同实时交易查询接口)

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码，000000成功，其他详细参见错误列表|
|2|result_msg|非必填|String|128|返回信息，返回错误原因|
|3|ins_cd|必填|String|20|机构号，接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|富友分配的商户号|
|5|term_id|非必填|String|8|终端号|
|6|random_str|必填|String|32|随机字符串，不长于32位|
|7|sign|必填|String|512|签名，详见签名生成算法|
|8|buyer_id|非必填|String|128|用户在商户的id|
|9|order_type|必填|String|20|订单类型:ALIPAY，WECHAT|
|10|trans_stat|必填|String|32|交易状态<br />`SUCCESS`—支付成功<br />`REFUND`—转入退款<br />`NOTPAY`—未支付<br />`CLOSED`—已关闭<br />`REVOKED`—已撤销<br />`USERPAYING`--用户支付中<br />`PAYERROR`--支付失败(其他原因，如银行返回失败)|
|11|order_amt|必填|Number|16|订单金额, 单位为`(分)`|
|12|transaction_id|必填|String|32|渠道订单号|
|13|mchnt_order_no|必填|String|30|商户订单号，商户系统的订单号，与请求一致|
|14|addn_inf|非必填|String|128|附加数据|
|15|reserved_fy_settle_dt|必填|String|8|富友交易日期|
|16|reserved_coupon_fee|非必填|String|10|优惠金额`(分)`|
|17|reserved_buyer_logon_id|非必填|String|128|买家在渠道登录账号|
|18|reserved_fund_bill_list|非必填|String|不定长|支付宝交易资金渠道，详细渠道|
|19|reserved_fy_trace_no|必填|String|12|富友系统内部追踪号|
|20|reserved_channel_order_id|非必填|String|32|条码流水号，用户账单二维码对应的流水|
|21|reserved_fy_term_id|非必填|String|8|富友终端号(富友终端号与TUSN号二选一)|

#### 报文示例

*请求报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <sign>
        SfJhTk1VFjdpDJ/lryLmcdmv3mS4LQy1naKUHcmkyzztnynkkU/bYXcwEA7/dl0RWcgsp28TbM3Bo1iJWwGPIl1COLUSx6aP1Bep+Xd8ATHHGPdSUcSxDN3TpG3hI0xfxf6nherdS2yL1NKLluhrZHMvhkYlOVB1ovoXWA4iOx0=
    </sign>
    <trade_dt>20201009</trade_dt>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <term_id>12345678</term_id>
    <random_str>daa6ee4d1e8096d4376ab9c5a65840ce</random_str>
    <channel_order_id></channel_order_id>
    <mchnt_order_no>2020100911285487257295</mchnt_order_no>
    <ins_cd>08A9999999</ins_cd>
    <transaction_id></transaction_id>
    <version>1</version>
    <order_type>WECHAT</order_type>
</xml>
```

*请求报文签名原文*
```text
channel_order_id=&ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020100911285487257295&order_type=WECHAT&random_str=daa6ee4d1e8096d4376ab9c5a65840ce&term_id=12345678&trade_dt=20201009&transaction_id=&version=1
```

*响应报文*
```xml
<?xml version="1.0" encoding="GBK" standalone="yes"?>
<xml>
    <addn_inf></addn_inf>
    <buyer_id>oUpF8uDQU2VTS1pmCQbWT8fcOP64</buyer_id>
    <ins_cd>08A9999999</ins_cd>
    <mchnt_cd>0002900F0370542</mchnt_cd>
    <mchnt_order_no>2020100911285487257295</mchnt_order_no>
    <order_amt>1</order_amt>
    <order_type>WECHAT</order_type>
    <random_str>XMDSWZMRXAOIQ5WESLP7CB6J2LTI2EWA</random_str>
    <reserved_bank_type>COMM_DEBIT</reserved_bank_type>
    <reserved_buyer_logon_id></reserved_buyer_logon_id>
    <reserved_channel_order_id>2020100911285487257295</reserved_channel_order_id>
    <reserved_coupon_fee></reserved_coupon_fee>
    <reserved_fund_bill_list></reserved_fund_bill_list>
    <reserved_fy_settle_dt>20201009</reserved_fy_settle_dt>
    <reserved_fy_trace_no>100063825452</reserved_fy_trace_no>
    <reserved_is_credit>0</reserved_is_credit>
    <reserved_settlement_amt>1</reserved_settlement_amt>
    <reserved_txn_fin_ts>20201009112859</reserved_txn_fin_ts>
    <result_code>000000</result_code>
    <result_msg>订单已退款</result_msg>
    <sign>
        hm3/1Y0uF7HbZxmd9kK9O/I/pCPgzlLeherAATlrDPxRE11CWu+ObHGPxt7to68W8AKwD1wKqF1vS0Ao6Bd5Fyz3gV9ZWMJwrBBHvK7HhKtZ99uon5WnY6gac12i4QwSKObS1rYRkDo03SliStYEaHGquuLavg13GhfjVcdkosk=
    </sign>
    <term_id>12345678</term_id>
    <trans_stat>REFUND</trans_stat>
    <transaction_id>4200000700202010090766639303</transaction_id>
</xml>
```

*响应报文签名原文*
```text
addn_inf=&buyer_id=oUpF8uDQU2VTS1pmCQbWT8fcOP64&ins_cd=08A9999999&mchnt_cd=0002900F0370542&mchnt_order_no=2020100911285487257295&order_amt=1&order_type=WECHAT&random_str=XMDSWZMRXAOIQ5WESLP7CB6J2LTI2EWA&result_code=000000&result_msg=订单已退款&term_id=12345678&trans_stat=REFUND&transaction_id=4200000700202010090766639303
```

---

### 3.11 微信授权码查询openid / 银联授权码获取user_id
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`

#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/auth2Openid`

*正式地址：*

#### 场景说明

!> 通过授权码查询公众号Openid，调用查询后，该授权码只能由此商户号发起扣款，直至授权码更新。

!> 如果子商户号没有绑定appid，只返回openid<br />如果子商户号绑定一个appid，入参sub_appid非必传，返回openid和sub_openid<br />如果子商户号绑定多个appid，入参sub_appid必传，返回openid和sub_openid

?> 请求和响应中的`reserved开头的所有字段和sign字段不参与签名原文`，其他必填、非必填字段、为空`必须`参与验签。而且后续扩展中，会根据实际业务需求不定期添加reserved开头的字段，请自行兼容！

#### 微信授权码查询openid / 银联授权码获取user_id请求报文

|序号|参数名|出现要求|型态|长度| 说明                                                                                                                       |
|:-----|:-----|:-----|:-----|:-----|:-------------------------------------------------------------------------------------------------------------------------|
|1|version|必填|String|8| 1.0                                                                                                                      |
|2|ins_cd|必填|String|13| 机构号,接入机构在富友的唯一代码                                                                                                         |
|3|mchnt_cd|必填|String|15| 富友分配的商户号                                                                                                                 |
|4|sub_appid|非必填|String|32| 1、微信：微信分配的子商户公众账号ID<br>2、银联：填空或填固定值: UnionPay/1.0 CloudPay<br>填错获取不到user_id<br>目前支持云闪付，待后期其他APP支持后，填写其他银行APP的User Agent |
|5|auth_code|必填|String|128| 扫码支付授权码，设备读取用户微信中的条码或者二维码信息                                                                                              |
|6|term_ip|必填|String|128| 终端IP                                                                                                                     |
|7|random_str|必填|String|32| 随机字符串                                                                                                                    |
|8|order_type|必填|String|13| 订单类型：<br>1、微信：WECHAT<br>2、银联：UNIONPAY(测试环境不可测试)                                                                          |
|9|order_amt|非必填|Number|16| 订单金额，暂时无用，后期拓展字段                                                                                                         |
|10|sign|必填|String|512| 签名，详见签名生成算法                                                                                                              |

#### 微信授权码查询openid / 银联授权码获取user_id响应报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|result_code|必填|String|16|错误代码，000000成功，其他详细参见错误列表|
|2|result_msg|非必填|String|128|返回信息, 返回错误原因|
|3|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|4|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|5|random_str|必填|String|32|随机字符串|
|6|sign|必填|String|512|签名，详见签名生成算法|
|7|appid|必填|String|32|微信分配的公众账号ID|
|8|sub_appid|非必填|String|32|微信分配的子商户公众账号ID|
|9|openid|必填|String|128|用户在商户appid下的唯一标识|
|10|`sub_openid`|非必填|String|128|`用户在子商户appid下的唯一标识`|

---


### 3.12 服务商模式获取openid
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/oauth2/getOpenid`

*正式地址：*

#### 场景说明

?> **合作方下的商户使用富友渠道号的appid发起公众号交易，需要获取该appid下的openid。**
<br /><br />`交易方式`：合作方组装URL请求富友，富友验证参数并封装URL请求微信，获取openid之后重定向到合作方指定的收银台URL

#### 签名说明

签名方式：与交易接口一致

#### 请求方式

**`GET`**，请将表格中的参数做URLEncode之后拼接(详见文档下方的demo示例)

#### 服务商模式获取openid请求报文
|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|1|ins_cd|必填|String|13|机构号,接入机构在富友的唯一代码|
|2|mchnt_cd|必填|String|15|商户号，富友分配给二级商户的商户号|
|3|type|必填|String|8|交易类型，暂时写死：1|
|4|appid|非必填|String|32|后期扩展字段：获取指定appid对应的openid`该字段填空`|
|5|redirect_uri|必填|String|256|接受appid/openid的收银台地址<br /><br />若无特殊场景需求，不要出现特殊字符、中文，如果出现中文，需要对url后面的参数做encode处理(详见demo示例)|
|6|sign|必填|String|512|签名, 详见签名生成算法|

#### 重定向响应
> redirect_url+appid+openid+state+`access_token`

*示例*
```text
https://m.baidu.com/from=844b/s?b=2&a=1&word=%D6%D0%CE%C4&state=wxfa089da95020ba1a&openid=ooIeqs4nrFiC634N2Spt3caubrUM&appid=wxfa089da95020ba1a`&access_token=ACCESS_TOKEN`
```

!> 说明：为了兼容众多合作方的代码，appid和state均填写的是appid的值，openid为appid对应的，公众号交易下单时，`使用重定向返回的appid+openid的值填入到公众号下单接口中的sub_appid和sub_openid`。

*示例代码(Java)*

```java
//商家参数
Map<String, String> mchntParamMap = new HashMap<String, String>();
mchntParamMap.put("a", "1");
mchntParamMap.put("b", "2");
mchntParamMap.put("word", "中文");

//参数需要进行 encode
String mchnt_redirect_url = getRequestUrl("https://m.baidu.com/from=844b/s",mchntParamMap);
Map<String, String> fuParamMap = new HashMap<String, String>();
fuParamMap.put("ins_cd", "08A9999999");
fuParamMap.put("mchnt_cd", "0002900F0313432");
fuParamMap.put("type", "1");
fuParamMap.put("appid", "");
fuParamMap.put("redirect_uri", mchnt_redirect_url);
String signContent = RSAUtil.generateSignContent(fuParamMap);
String sign = RSAUtil.sign(signContent, PRIVATE_KEY);
fuParamMap.put("sign", sign);
String url = getRequestUrl(BASE_URL + "oauth2/getOpenid", fuParamMap);
System.out.println("mchnt_redirect_url : " + mchnt_redirect_url);
System.out.println("signContent : " + signContent);
System.out.println("sign : " + sign);
System.out.println("url : " + url);
public static String getRequestUrl(String url, Map<String, String> map) throws IOException {
    StringBuffer redirectOpenidThirdUrl = new StringBuffer(url+"?");
    try {
        if (CollectionUtils.isEmpty(map)) {
            return redirectOpenidThirdUrl.toString();
        }
        Set<Map.Entry<String, String>> entries = map.entrySet();
        Iterator<Map.Entry<String, String>> entryIterator = entries.iterator();
        boolean hasParam = false;
        while (entryIterator.hasNext()) {
            Map.Entry<String, String> entry = entryIterator.next();
            String name = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value))
            {
                if (hasParam) {
                    redirectOpenidThirdUrl.append("&");
                } else {
                    hasParam = true;
                }
                redirectOpenidThirdUrl.append(name).append("=").append(URLEncoder.encode(value,  "GBK"));
            }
        }
        return redirectOpenidThirdUrl.toString();
    } catch (Exception e) {
        e.printStackTrace();
    }
    return redirectOpenidThirdUrl.toString();
}
```

```text
上述 demo 日志(测试环境)：
mchnt_redirect_url :
https://m.baidu.com/from=844b/s?b=2&a=1&word=%D6%D0%CE%C4

signContent :
appid=&ins_cd=08A9999999&mchnt_cd=0002900F0313432&redirect_uri=https://m.baidu.com/from=844b/s?b=2&a=1&word=%D6%D0%CE%C4&type=1

sign :
Of0xF8WekXfuGAI7Zq6xlodAPhTY0YfM9EicBwOBys1bel1Yz3CDAnVu4n39DpOSWH7h1vqXoTgiZcxyt8ciPUigGlty601V9JADx51/5+ur8VfOwzPPO1o/yzfJH1O4WOPx0o9rBkrXub6UQkYKH3ehevn7nK92/GY4ZX71Ux0=

url :
http://116.239.4.195:28164/oauth2/getOpenid?sign=Of0xF8WekXfuGAI7Zq6xlodAPhTY0YfM9EicBwOBys1bel1Yz3CDAnVu4n39DpOSWH7h1vqXoTgi%0D%0AZcxyt8ciPUigGlty601V9JADx51%2F5%2Bur8VfOwzPPO1o%2FyzfJH1O4WOPx0o9rBkrXub6UQkYKH3eh%0D%0Aevn7nK92%2FGY4ZX71Ux0%3D%0D%0A&redirect_uri=https%3A%2F%2Fm.baidu.com%2Ffrom%3D844b%2Fs%3Fb%3D2%26a%3D1%26word%3D%25D6%25D0%25CE%25C4&mchnt_cd=0002900F0313432&type=1&ins_cd=08A9999999

返回重定向示例:
https://m.baidu.com/from=844b/s?b=2&a=1&word=%D6%D0%CE%C4&state=wxfa089da95020ba1a&openid=ooIeqs4nrFiC634N2Spt3caubrUM&appid=wxfa089da95020ba1a
```

---



### 3.13 微信委托代扣
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/withhold`

*生产地址：*

#### 场景说明

?> 委托代扣可应用于定期扣款或需事后扣款以期提高效率的场景。<br>例如离线刷脸快速支付，在用户授权给微信后，进行委托扣款的场景。

<!--

!> 1、对于 trade_scene 取值为车场停车（PARKING）、车位停车（PARKING_SPACE）、加油（GAS）、高速（HIGHWAY）、路桥（BRIDGE）、生活缴费（LIFE_PAY）场景，此接口同步返回的为受理状态，订单状态通过通知接口或订单查询接口获取。扣费时首先按签约协议中记录的优先支付方式扣费，否则从可用扣款方式中依次选择扣款。
-->
!> 对于 trade_scene 取值为离线刷脸场景（K12_OFFLINE _FACE），此接口同步返回的为订单状态，机构也可通过代扣结果查询接口获取订单状态。

!> 本接口目前仅支持离线刷脸场景（K12_OFFLINE _FACE）

#### 微信委托代扣请求报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|version|必填|String|8|1.0|
|02|ins_cd|必填|String|13|机构号,接入机构在富友的唯一代码|
|03|mchnt_cd|必填|String|15|商户号, 富友分配给二级商户的商户号|
|04|term_id|必填|String|8|终端号(没有真实终端号统一填88888888)|
|05|random_str|必填|String|32|随机字符串|
|06|sign|必填|String|512|签名, 详见签名生成算法|
|07|order_type|必填|String|20|订单类型：<!--<br>ALIPAY(支付宝)<br>WECHAT(微信)<br>UNIONPAY(银联二维码)<br>BESTPAY(翼支付)<br>DIGICCY(数字货币)`暂时仅支持测试环境`--><br>WXPAP(微信委托代扣)`目前仅支持此类型`|
|08|goods_des|必填|String|128|商品名称, 显示在用户账单的商品、商品说明等地方|
|09|goods_detail|非必填|String|6000|单品优惠功能字段，见文档中[good_detail说明字段](/scanpay/introduction#goods_detail说明字段)|
|10|addn_inf|非必填|String|50|附加数据<br/>如果需要用到微信点餐数据回传，该字段需要填写OrderSource=FoodOrder|
|11|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号（5到30个字符、 只能包含字母数字,区分大小写)|
|12|curr_type|非必填|String|3|货币类型,默认人民币：CNY|
|13|order_amt|必填|Number|16|总金额, 订单总金额，单位为`分`|
|14|term_ip|必填|String|128|终端IP|
|15|txn_begin_ts|必填|String|14|交易起始时间, 订单生成时间，格式为yyyyMMddHHmmss|
|16|goods_tag|非必填|String|32|商品标记|
|17|reserved_sub_appid|非必填|String|32|子商户公众号id|
|18|reserved_fy_term_id|非必填|String|8|富友终端号(富友终端号与TUSN号二选一)|
|19|reserved_trade_scene|非必填|String|16|交易场景<br>委托代扣的交易场景值，目前支持 ：<br>1. PARKING：车场停车场景<br>2. PARKING_SPACE：车位停车场景<br>3. GAS：加油场景<br>4. HIGHWAY：高速场景<br>5. BRIDGE：路桥场景<br>6. LIFE_PAY：生活缴费行业<br>7. K12_OFFLINE_FACE：离线刷脸代扣场景<br>`目前仅支持K12_OFFLINE_FACE类型`|
|20|reserved_auth_openid|非必填|String|128|授权用户标识<br>1、trade_scene 为 LIFE_PAY 生活缴费场景时，为 appid 下用户标识；其他车主服务场景则用户在对应授权 appid 下用户唯一标识。<br>2、trade_scene 为K12_OFFLINE_FACE 离线刷脸代扣场景时，为刷脸用户标识，收单机构要保证同一个刷脸用户在同一个机构下唯一。|
|21|reserved_terminal_info|非必填|String||终端信息说明字段，见文档中[terminal_info终端信息说明字段](/scanpay/introduction#terminal_info终端信息说明字段)|
|22|`reserved_busi_data`|非必填|json||`业务特有订单数据，详细说明见下文`|

*`reserved_busi_data 字段说明：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|`scene_info`|必填|json||场景信息值，格式为json：不同业务场景设置不同的值<br>示例：<br>"scene_info":{"bridge_info":{"start_time":"2017-08-26T10：43：39+08:00", "plate_number":" 粤B100000","exit":"虎门大桥"}}<br>`详细说明见下文`|
|02|auth_appid|非必填|String|32|车主场景必传<br>用户授权商户可使用微信支付车主服务的 appid，从业机构 appid 或者特约商户的 appid 两者之一。|
|03|contract_id|非必填|String|128|签约返回的委托代扣协议 id|
|04|auth_code|必填|String|2000|trade_scene 为 K12_OFFLINE_FACE 离线刷脸代扣场景时必送<br>离线刷脸扣款凭证，用户刷脸成功后由终端设备生成，约 8KB。|

*reserved_busi_data示例：*
```xml
<reserved_busi_data>
{
    "scene_info":{
        "bridge_info":{
            "start_time":"2017-08-26T10：43：39+08:00",
            "plate_number":"粤B100000",
            "exit":"虎门大桥"
        }
    },
    "auth_appid":"wx8888888888888888",
    "contract_id":"Wx15463511252015071056489715 ",
    "auth_code":"离线刷脸扣款凭证"
}
</reserved_busi_data>
```

*`scene_info 字段说明`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|`trade_scene`|必填|String|32|委托代扣的交易场景值，目前支持 ：<br>1. PARKING：车场停车场景<br>2. PARKING_SPACE：车位停车场景<br>3. GAS：加油场景<br>4. HIGHWAY：高速场景<br>5. BRIDGE：路桥场景<br>6. LIFE_PAY：生活缴费行业<br>7. K12_OFFLINE_FACE：离线刷脸代扣场景|
|02|highway_info|非必填|||高速场景信息`详细说明见下文`|
|03|parking_info|非必填|||停车场景信息`详细说明见下文`|
|04|parking_space_info|非必填|||停车位场景信息`详细说明见下文`|
|05|bridge_info|非必填|||路桥场景信息`详细说明见下文`|
|06|gas_info|非必填|||加油场景信息`详细说明见下文`|
|07|life_info|非必填|||生活缴费行业信息`详细说明见下文`|
|08|k12_offline_face_info |非必填|||K12 离线刷脸业务信息`详细说明见下文`|
|09|device_id|非必填|String|32|终端设备号(商户自定义，如门店编号)<br>例：013467007045764|
|10|device_ip|必填|String|40|调用微信支付 API 的机器 IP<br>例：8.8.8.8|

**以下为trade_scene的具体场景**

*`当 trade_scene 场景为：PARKING（车场停车） 时，parking_info 传如下值：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|start_time|必填|String|32|用户入闸时间，按照使用 rfc3339所定义的格式<br>格式为：YYYYMM-DDThh：mm：ss+TIMEZONE|
|02|end_time|必填|String|32|用户出闸时间，按照使用 rfc3339所定义的格式<br>格式为：YYYYMM-DDThh：mm：ss+TIMEZONE|
|03|charge_duration|必填|Int|3600|计费的时间长。单位为秒|
|04|plate_number|必填|String|32|车牌号。仅包括省份+车牌，不包括特殊字符<br>例：粤B888888|
|05|car_type|非必填|String|16|停车车辆的类型<br>可选值：大型车、小型车|
|06|parking_name|必填|String|32|停车场名称|

*`当 trade_scene 场景为：PARKING_SPACE（车位停车） 时，parking_space_info 传如下值：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|start_time|必填|String|32|用户入闸时间，按照使用 rfc3339所定义的格式<br>格式为：YYYYMM-DDThh：mm：ss+TIMEZONE|
|02|end_time|必填|String|32|用户出闸时间，按照使用 rfc3339所定义的格式<br>格式为：YYYYMM-DDThh：mm：ss+TIMEZONE|
|03|space_number|必填|String|32|车位编号|
|04|charge_duration|必填|Int|3600|计费的时间长。单位为秒|
|05|car_type|非必填|String|16|停车车辆的类型<br>可选值：大型车、小型车|
|06|parking_name|必填|String|32|停车场名称|

*`当 trade_scene 场景为：GAS（加油场景） 时，gas_info 传如下值：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|start_time|必填|String|32|用户进行加油时间，按照使用 rfc3339所定义的格式<br>格式为：YYYYMM-DDThh：mm：ss+TIMEZONE|
|02|plate_number|必填|String|32|车牌号。仅包括省份+车牌，不包括特殊字符<br>例：粤B888888|
|03|car_type|非必填|String|16|停车车辆的类型<br>可选值：大型车、小型车|
|04|gas_station|必填|String|32|加油站名称|
|05|gas_label_no|必填|Int|98|油品标号，可选值：89、90、92、93、95、97、98、5、0、-10、-20、-35、-50|
|06|gas_standard|必填|String|32|油品标准名称，可选：国V，国IV，粤IV|
|07|gas_amount|必填|Int|50120|油量（单位毫升 ml）|
|08|gas_type|必填|String|32|油品类型名称|
|09|gas_gun_no|必填|Int|10|加油枪号，纯数字|

*`当 trade_scene 场景为：HIGHWAY（高速场景） 时，highway_info 传如下值：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|start_time|必填|String|32|用户入闸时间，按照使用 rfc3339所定义的格式<br>格式为：YYYYMM-DDThh：mm：ss+TIMEZONE|
|02|end_time|必填|String|32|用户出闸时间，按照使用 rfc3339所定义的格式<br>格式为：YYYYMM-DDThh：mm：ss+TIMEZONE|
|03|plate_number|必填|String|32|车牌号。仅包括省份+车牌，不包括特殊字符<br>例：粤B888888|
|04|channel_type|必填|String|16|高速通道类型，目前可选：ETC、MTC<br>商户扣费前必须确认当前车牌的标识属性，用户车牌必须具有该通道标识时，才允许扣费。|
|05|entrance|必填|String|32|入口站名称|
|06|exit|必填|String|32|出口站名称|
|07|car_type|非必填|String|16|车辆类型<br>可选值：大型车、小型车|
|08|carrying_capacity|非必填|String|32|核载人数|
|09|carrying_range|非必填|String|32|核载区间|

*`当 trade_scene 场景为：BRIDGE（路桥场景） 时，bridge_info 传如下值：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|start_time|必填|String|32|用户入闸时间，按照使用 rfc3339所定义的格式<br>格式为：YYYYMM-DDThh：mm：ss+TIMEZONE|
|02|plate_number|必填|String|32|车牌号。仅包括省份+车牌，不包括特殊字符<br>例：粤B888888|
|03|car_type|非必填|String|16|车辆类型<br>可选值：大型车、小型车|
|04|exit|必填|String|32|收费出口站的名称，需向用户进行展示|

*`当 trade_scene 场景为：LIFE_PAY（生活缴费行业） 时，life_info 传如下值：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|bill_key|必填|String|64|生活缴费户号编号|
|02|company_id|必填|String|64|项目编号|

*`当 trade_scene 场景为：K12_OFFLINE_FACE（离线刷脸代扣） 时，k12_offline_face_info 传如下值：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|business_scene|必填|String|32|取值：<br>测试：K12_TEST<br>食堂：K12_CANTEEN<br>超市：K12_SUPERMARKET<br>医院：K12_HOSPITAL<br>按月支付：K12_MONTHLY_PAYMENT|


#### 微信委托代扣响应报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----:|:-----|:-----:|:-----:|:-----:|:-----|
|01|result_code|必填|String|16|错误代码, 000000 成功,其他详细参见错误列表|
|02|result_msg|必填|String|128|错误代码描述|
|03|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|04|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|05|term_id|非必填|String|8|终端号(没有真实终端号统一填88888888)|
|06|random_str|必填|String|32|随机字符串|
|07|sign|必填|String|512|签名, 详见签名生成算法|
|08|order_type|必填|String|20|订单类型:<!--ALIPAY（支付宝）<br>WECHAT（微信）<br>UNIONPAY(银联二维码）<br>BESTPAY（翼支付）--><br>WXPAP（微信委托代扣）`目前仅支持此类型`|
|09|total_amount|必填|Number|16|订单金额，分为单位的整数|
|10|buyer_id|非必填|String|128|买家在渠道账号|
|11|transaction_id|必填|String|64|渠道交易流水号|
|12|addn_inf|非必填|String|50|附加数据|
|13|reserved_fy_order_no|非必填|String|30|富友生成的订单号,需要商户与商户订单号进行关联|
|14|reserved_mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号|
|15|reserved_fy_settle_dt|必填|String|8|富友交易日期|
|16|reserved_coupon_fee|非必填|String|10|优惠金额**`（分）`**|
|17|reserved_buyer_logon_id|非必填|String|128|买家在渠道登录账号|
|18|reserved_fund_bill_list|非必填|String|不定长|支付宝交易资金渠道,详细渠道|
|19|reserved_fy_trace_no|必填|String|12|富友系统内部追踪号|
|20|reserved_channel_order_id|非必填|String|64|条码流水号，用户账单二维码对应的流水|
|21|reserved_is_credit|非必填|String|8|1：表示信用卡~~或者花呗~~<br />0：表示其他(非信用方式)<br />不填，表示未知|
|22|reserved_txn_fin_ts|非必填|String|14|用户支付时间yyyyMMddHHmmss|
|23|reserved_settlement_amt|非必填|Number|16|应结算订单金额，以分为单位的整数<br />只有成功交易才会返回<br /><br />如果使用了商户免充值优惠券，该值为订单金额-商户免充值<br />如果没有使用商户免充值，该值等于订单金额|
|24|**`reserved_bank_type`**|非必填|String|16|付款方式|
|25|**`reserved_promotion_detail`**|非必填|String|6000|微信营销详情，见文档中[reserved_promotion_detail说明字段](scanpay/introduction#reserved_promotion_detail说明字段)|
|26|reserved_trade_state|非必填|String|32|交易状态ACCEPTED—已受理；支付状态通过查单确认。（与 accept_time 同时出现）<br><br>当请求中的 trade_scene 为K12_OFFLINE_FACE 时，取值如下：<br>“SUCCESS” -支付成功（支付成功无需处理）<br>“REFUND" -转入退款（支付成功无需处理）<br>“NOTPAY” -未支付（重试扣款）<br>“CLOSED" -已关闭（换单号重试）<br>“USERPAYING” -用户支付中（重试扣款）<br>“PAYERROR” -支付失败(换单号重试)；|
|27|reserved_accept_time|非必填|String|32|即受理订单的受理时间，按照使用 rfc3339所定义的格式<br>格式为：YYYYMM-DDThh：mm：ss+TIMEZONE|
|28|reserved_error_type|非必填|String|32|扣款时返回，支付成功时为 SUCCESS，否则为错误类型，如 NOT_ENOUGH|
|29|reserved_debt_state|非必填|String|32|欠款状态<br>NOT_DEBT：无欠款（表示订单无垫资支付，无欠款）<br>DEBT：有欠款（表示订单为垫资支付，有欠款）<br>REPAYMENT：已还款（表示订单为垫资支付，但已还款，无欠款）|
|30|reserved_repayment_transaction_id|非必填|String|32|微信还款订单号<br>debt_state 为 REPAYMENT 时返回|
|31|reserved_busi_data|非必填|||业务特有订单数据`详细说明见下文`|

*`reserved_busi_data 字段说明：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----:|:-----|:-----:|:-----:|:-----:|:-----|
|01|scene_info|必填|||场景信息值<br>例："scene_info":{"device_id":"sxxxxx"}|

*`场景信息 scene_info 字段说明：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----:|:-----|:-----:|:-----:|:-----:|:-----|
|01|device_id|非必填|String|32|终端设备号(商户自定义，如门店编号)<br>例：013467007045764|
|02|device_ip|必填|String|40|调用微信支付 API 的机器 IP|



---



### 3.14 微信委托代扣查询
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

*测试地址：* `https://fundwx.payfuiouo2o.com/withholdQuery`

*正式地址：*

#### 场景说明

?> 该接口提供所有微信支付订单的查询，商户可以通过该接口主动查询订单状态，完成下一步的业务逻辑。

!> 需要调用查询接口的情况：
<br>1) 当商户后台、网络、服务器等出现异常，商户系统最终未接收到支付通知；
<br>2) 调用支付接口后，返回系统错误或未知交易状态情况；
<br>3) 调用代扣支付 API，返回 ACCEPTED、NOPAY 或 USERPAYING 的状态。


#### 微信委托代扣查询请求报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|version|必填|String|8|1.0|
|02|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|03|mchnt_cd|必填|String|15|商户号,富友分配给二级商户的商户号|
|04|term_id|必填|String|8|终端号(没有真实终端号统一填88888888)|
|05|order_type|必填|String|20|订单类型:<!--<br />ALIPAY(统一下单、条码支付、服务窗支付)<br />WECHAT(统一下单、条码支付、公众号支付、小程序)<br />UNIONPAY<br />BESTPAY(翼支付)--><br>WXPAP(微信委托代扣)`目前仅支持此类型`|
|06|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)|
|07|random_str|必填|String|32|随机字符串，不长于32位|
|08|sign|必填|String|512|签名, 详见签名生成算法|
|09|reserved_trade_scene|非必填|String|16|交易场景<br>委托代扣的交易场景值，目前支持：<br>1. PARKING：车场停车场景<br>2. PARKING_SPACE：车位停车场景<br>3. GAS：加油场景<br>4. HIGHWAY：高速场景<br>5. BRIDGE：路桥场景<br>6. LIFE_PAY 生活缴费行业<br>7. K12_OFFLINE_FACE：离线刷脸代扣场景<br>`目前仅支持K12_OFFLINE_FACE类型`|

#### 微信委托代扣查询响应报文

|序号|参数名|出现要求|型态|长度|说明|
|:-----|:-----|:-----|:-----|:-----|:-----|
|01|result_code|必填|String|16|错误代码,000000成功,其他详细参见错误列表|
|02|result_msg|必填|String|128|错误代码描述|
|03|ins_cd|必填|String|20|机构号,接入机构在富友的唯一代码|
|04|mchnt_cd|必填|String|15|商户号, 富友分配的商户号|
|05|term_id|非必填|String|8|终端号|
|06|random_str|必填|String|32|随机字符串|
|07|sign|必填|String|512|签名, 详见签名生成算法|
|08|buyer_id|非必填|String|128|用户在商户的id|
|09|order_type|必填|String|20|订单类型:<!--ALIPAY<br>WECHAT<br>UNIONPAY（银联二维码）<br>BESTPAY（翼支付）--><br>WXPAP（微信委托代扣）`目前仅支持此类型`|
|10|trans_stat|必填|String|32|查询状态，详见[应答码表--查询应答码](/scanpay/code#查询应答码)|
|11|order_amt|必填|Number|9|订单金额，单位为**`分`**|
|12|transaction_id|必填|String|64|渠道订单号|
|13|mchnt_order_no|必填|String|30|商户订单号, 商户系统内部的订单号<br />(5到30个字符、只能包含字母数字,区分大小写)|
|14|addn_inf|非必填|String|50|附加数据|
|15|reserved_fy_settle_dt|必填|String|8|富友交易日期|
|16|reserved_coupon_fee|非必填|String|10|优惠金额**`(分)`**|
|17|reserved_buyer_logon_id|非必填|String|128|买家在渠道登录账号|
|18|reserved_fund_bill_list|非必填|String|不定长|支付宝交易资金渠道，详细渠道|
|19|reserved_fy_trace_no|非必填|String|12|富友系统内部追踪号|
|20|reserved_channel_order_id|非必填|String|64|条码流水号，用户账单二维码对应的流水|
|21|reserved_fy_term_id|非必填|String|8|富友终端号(富友终端号与TUSN号二选一)|
|22|reserved_is_credit|非必填|String|8|1--表示信用卡~~或者花呗~~<br />0--表示其他(非信用方式)<br />不填，表示未知|
|23|reserved_txn_fin_ts|非必填|String|14|用户支付时间yyyyMMddHHmmss|
|24|reserved_settlement_amt|非必填|Number|16|应结算订单金额，分为单位的整数。<br />只有成功交易才会返回<br />如果使用了商户免充值优惠券，该值为订单金额-商户免充值<br />如果没有使用商户免充值，该值等于订单金额|
|25|**`reserved_bank_type`**|非必填|String|16|付款方式|
|26|**`reserved_promotion_detail`**|非必填|String|6000|微信营销详情，见文档中[reserved_promotion_detail说明字段](scanpay/introduction#reserved_promotion_detail说明字段)|
|27|reserved_auth_openid|非必填|String|128|授权用户标识，如果受理扣款有传入 auth_openid ，则返回该auth_openid；车主服务场景为用户在对应授权appid下用户唯一标识；<br>如受理扣款无 auth_openid，则返回 openid及 sub_openid|
|28|reserved_error_type|非必填|String|32|扣款时返回，支付成功时为 SUCCESS，否则为错误类型，如 NOT_ENOUGH|
|29|reserved_debt_state|非必填|String|32|欠款状态<br>trade_scene 为 K12_OFFLINE_FACE 离线刷脸代扣场景时返回。<br>NOT_DEBT：无欠款（表示订单无垫资支付，无欠款）<br>DEBT：有欠款（表示订单为垫资支付，有欠款）<br>REPAYMENT：已还款（表示订单为垫资支付，但已还款，无欠款）|
|30|reserved_repayment_transaction_id|非必填|String|32|微信还款订单号<br>debt_state 为 REPAYMENT 时返回|
|31|**`reserved_busi_data`**|非必填|||业务特有订单数据`详细说明见下文`|

<!--|31|**`reserved_risk_info`**|非必填|json||见文档中[reserved_risk_info说明字段](/scanpay/transform#reserved_risk_info说明字段)|
-->

*`reserved_busi_data 字段说明：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----:|:-----|:-----:|:-----:|:-----:|:-----|
|01|scene_info|必填|||场景信息值<br>例："scene_info":{"device_id":"sxxxxx"}<br>`详细说明见下文`|
|02|trade_state|必填|String|32|订单支付状态，可选值：<br>SUCCESS--支付成功<br>REFUND--转入退款<br>ACCEPTED--已受理，未支付<br>PAY_FAIL--支付失败(其他原因，如银行返回失败)|
|03|auth_appid|非必填|String|32|车主场景必传，用户授权商户可使用微信支付车主服务的 appid，从业机构 appid 或者特约商户的 appid 两者之一。|
|04|contract_id|非必填|String|128|签约返回的委托代扣协议id|

*`场景信息 scene_info 字段说明：`*

|序号|参数名|出现要求|型态|长度|说明|
|:-----:|:-----|:-----:|:-----:|:-----:|:-----|
|01|device_id|非必填|String|32|终端设备号(商户自定义，如门店编号)<br>例：013467007045764|
|02|device_ip|必填|String|40|调用微信支付 API 的机器 IP|







### 3.15 交易手续费推送接口
!> `注：sign及reserved开头字段除外的其他非必填字段也需要参与验签。我司会根据后期业务需求，新增reserved开头字段,请提前做好兼容(简而言之，我们会新增reserved开头字段的字段，这些字段都不参与验签)。`
#### 请求地址

​	合作方回调地址

?> 合作方提供回调地址配置：发邮件到techsupport@fuioupay.com<br>邮件主题：配置交易手续费推送回调地址<br>邮件内容：机构号/商户号，机构名称/商户名称，回调地址，说明是配置交易手续 费推送回调地址。<br>



!> 支持商户配置，按照商户维度配置富友默认使用同一套密钥验签，商户验签公钥使用下面公钥；<br>

代理接收回调成功处理成功后返回字符串1，后富友停止回调给商户。最多回调5次，每次间隔15S

按代理维度(一代和二代都支持)配置的推送地址，如果代理没有配置密钥，也默认使用下面公钥验签

```
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCsNPi5LbtX0qteBsDqRfS2qNij9rxjR+eAnvDmTBKQVfVsvYvQqavSYABSzE9c6sb1LvjrSRl+13qGas6+iFyv+8lI3NHQXPoJOCScOMo49ds/y1OGSYIb8IkOW231aj3DhrcGm4iCSw81IT7uALCF8nr/tbPTO7djRcYceMfmxQIDAQAB
```

!> 该接口只推送交易手续费，推送交易类型为：TX09,TX15,TX27,TX37,TX42,TX23,TX91,TX93,CX91,CX93,TX18,TX02,TX03,CX02,TX01,CX01,TX10,TX68,TX70,<br>PY70,PY71,PY68,PY69,TX66,CX68,CX70,CX69,CX71,PX52,PX57,PX61,PX63,PX64,PX11,PX15<br>业务代码对应描述详见[交易类型](/duizhang/README#交易类型)<br>其中刷卡类交易的撤销交易和退款交易（业务代码：TX03,CX02,CX01,CX68,CX70,CX69,CX71,CX00），手续费推送orig_mchnt_order_no字段中展示“‘***原交易日期（yyyyMMdd）+原交易本地跟踪号***’”，需要关联原交易的代理或商户可自行解析关联<br>结算类交易mchnt_order_no为空

报文格式：    xml

| **序号** | **参数名**             | **出现要求** | **型态** | **长度** | **说明**                               |
| -------- | ---------------------- | ------------ | -------- | -------- | -------------------------------------- |
| 1        | ins_cd                 | 必填         | String   | 15       | 机构号                                 |
| 2        | mchnt_cd               | 必填         | String   | 15       | 商户号                                 |
| 3        | orig_mchnt_order_no    | 非必填       | String   | 30       | 原商户订单号（退款交易时为原正订单号） |
| 4        | mchnt_order_no         | 必填         | String   | 30       | 商户订单号（退款交易时为订单号）       |
| 5        | channel_order_no       | 必填         | String   | 30       | 渠道订单号                             |
| 6        | txn_fin_ts             | 必填         | String   | 14       | 支付完成时间                           |
| 7        | txn_amt                | 必填         | String   | 11       | 交易金额                               |
| 8  | fee_amt       | 必填     | String | 11   | 手续费金额（退款交易手续费为负数），已废弃。<br />建议使用reserved_paidin_amt、reserved_receivable_amt |
| 9        | kbps_trace_no          | 必填         | String   | 12       | 富友本地跟踪号                         |
| 10       | reserved_fas_settle_dt | 必填         | String   | 8        | 清算日期                               |
| 11   | reserved_discount_amt | 非必填   | String | 11   | 减免手续费金额，已废弃。<br />建议使用reserved_paidin_amt减去reserved_receivable_amt计算 |
| 12 | reserved_set_cd | 非必填 | String | 5 | 交易手续费扣率模版 |
| 13 | reserved_bank_active_id | 非必填 | String | 10 | 银行活动号 |
| 14 | reserved_bank_active_name | 非必填 | String | 100 | 银行活动名称 |
| 15 | reserved_bank_active_deduction_fee | 非必填 | String | 16 | 银行活动减免手续费金额 |
| 16 | reserved_bank_active_package_quota | 非必填 | String | 16 | 银行活动额度包总额度 |
| 17 | reserved_receivable_amt | 必填 | String | 11 | 实收商户手续费金额（退款时，手续费为负数） |
| 18 | reserved_paidin_amt | 必填 | String | 11 | 应收商户手续费金额（退款时，手续费为负数） |
| 19 | reserved_card_discount_rate_flag | 非必填 | String | 5 | 外卡交易是否优惠费率标识：<br> 91:优惠计费、90 :标准费率<br>其他交易不返回该字段 |
| 20 | reserved_busi_cd | 非必填 | String | 5 | 业务代码 |
| 21 | sign                   | 必填         | String   | 512      | 签名，详见签名生成算法                 |



 报文示例：

```
<?xml  version="1.0"  encoding="GBK"  standalone="yes"?>
<xml>
<channel_order_no>4200000742202012028596001565</channel_order_no>
<fee_amt>0</fee_amt>
<ins_cd>08K0023029</ins_cd>
<kbps_trace_no>120497549430</kbps_trace_no>
<mchnt_cd>0004510F3217505</mchnt_cd>
<mchnt_order_no>E20201202171223382229376/TX003542</mchnt_order_no>
<orig_mchnt_order_no></orig_mchnt_order_no>
<reserved_fas_settle_dt>20201202</reserved_fas_settle_dt>
<sign>CMymXF1lxXQJC2f3ISX6TLZhjRdi/IwJpA3jG4TDYuUHUmvsCtIt0y4G0/I+hzoHhE
XXD+ev78/3ETR4Q222ra575u43aHiEKiyHNaNb7iGyphGr7t9Av70EbU7B3MEzO3eB96HWrez
6lCxc7cqT0eJzdcPRthYeIf5cNvnz3fk=</sign>
<txn_amt>1</txn_amt>
<txn_fin_ts>20201202173828</txn_fin_ts>
</xml>
```



签名原文：
channel_order_no=4200000742202012028596001565&fee_amt=0&ins_cd=08K0023029&kb
ps_trace_no=120497549430&mchnt_cd=0004510F3217505&mchnt_order_no=E202012021
71223382229376/TX003542&orig_mchnt_order_no=&txn_amt=1&txn_fin_ts=202012021738
28
交易手续费推送响应报文：
对请求body中的数据进行URLDecode既可获得请求参数xml报文
