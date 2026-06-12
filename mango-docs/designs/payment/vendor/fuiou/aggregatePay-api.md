
### 2.1 统一下单接口

#### 请求地址：

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/preCreate`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/preCreate`

*报文格式：* `JSON`

#### 场景说明

![统一下单流程图](_images/preCreate-process.jpg '统一下单流程图')

- `步骤1：`商户发送报文至富友进行下单，实时返回下单后的支付二维码地址
- `步骤2：`商户展示二维码给用户，用户使用微信、支付宝等扫码支付
- `步骤3：`支付结果以回调（2.5）的方式通知到商户

!> 说明：自2019年7月23日开始，微信新增商户不再具有主扫下单权限；<br>2019年9月15日开始，该接口为富友使用收单机构模式 JS 封装的主扫，<br>`新增、存量商户可以继续使用该接口`

#### 统一下单请求报文

|序号|参数中文名|参数名|长度| 说明                                                                                                                   | 必填  |
|:-----:|:-----|:-----|:-----:|:---------------------------------------------------------------------------------------------------------------------|:----|
|1|版本号|version|8| 1.0                                                                                                                  | 必填  |
|2|商户号|mchnt_cd|15| 富友分配的商户号<br>例：0002900F0313432                                                                                        | 必填  |
|3|随机字符串|random_str|32| 随机字符串                                                                                                                | 必填  |
|4|订单类型|order_type|20| 订单类型：<br>ALIPAY<br>WECHAT<br>~~WXXS(微信线上)~~<br>WXBX(微信保险类)）<br>ALBX(支付宝保险类)<br>UNIONPAY(银联二维码)<br>DIGICCY(数字货币)      | 必填  |
|5|订单总金额|order_amt|16| 以`分`为单位                                                                                                              | 必填  |
|6|商户订单号|mchnt_order_no|30| 商户系统内部的订单号                                                                                                           ||
|7|交易起始时间|txn_begin_ts|14| 订单生成时间<br>格式：<br>yyyyMMddHHmmss                                                                                      | 必填  |
|8|商品描述|goods_des|128| 商品或支付单简要描述                                                                                                           | 必填  |
|9|商品详情|goods_detail|600| 商品名称明细                                                                                                               | 非必填 |
|10|商品标记|goods_tag|32| 商品标记                                                                                                                 | 非必填 |
|11|终端号|term_id|8| 随机8字节数字字母组合                                                                                                          | 必填  |
|12|终端IP|term_ip|16| 终端IP                                                                                                                 | 必填  |
|13|附加数据|addn_inf|100| 附加数据                                                                                                                 | 非必填 |
|14|货币类型|curr_type|3| 默认人民币：CNY                                                                                                            | 非必填 |
|15|通知URL|notify_url|256| 接收富友异步通知回调地址<br>通知url必须为直接公网可访问的url,只能是域名，内网地址如192.168或者172,10开头的地址不能使用，若需加白使用，需要找我司索要出口IP,不能携带参数主扫| 必填  |
|16|子商户公众号id|reserved_sub_appid|32| 子商户配置多个公众号时必填                                                                                                        | 非必填 |
|17|限制支付|reserved_limit_pay|32| no_credit:不能使用信用卡<br>credit_group:不能使用花呗和信用卡<br>`不参与签名`                                                              | 非必填 |
|18|交易关闭时间|reserved_expire_minute|8| 交易关闭时间（单位分钟，例：60（1小时））,默认填0（2小时）                                                                                     | 非必填 |
|19|富友终端号|reserved_fy_term_id|20| 富友终端号`（如果不是用的富友的POS终端，此字段千万不要填，不然会影响清算）`                                                                             | 非必填 |
|20|用户身份证号|reserved_user_creid|64| 用户身份证号码<br>(实名保险类交易必填)                                                                                               | 非必填 |
|21|用户姓名|reserved_user_truename|20| 用户姓名                                                                                                                 | 非必填 |
|22|用户手机号|reserved_user_mobile|20| 用户手机号用户手机号<br>(仅支付宝认证)                                                                                               | 非必填 |
|23|保险公司订单号|reserved_bxno_inf|65| 保险公司订单号，认证机构单号<br>(使用英文逗号分隔)                                                                                         | 非必填 |
|24|富友终端类型|reserved_fy_term_type|1| 富友终端类型<br>0：其他<br>1：富友终端<br>2：POS机<br>3：台卡<br>4：PC软件                                                                 | 非必填 |
|25|终端序列号|reserved_fy_term_sn|20| 终端序列号                                                                                                                | 非必填 |
|26|设备信息|reserved_device_info|32| 设备信息，托传给微信。用于单品券核销<br>示例参照：[示例字段](/aggregatePay/introduction#设备信息-reserved_device_info-示例字段)                         | 非必填 |
|27|花呗分期信息|reserved_ali_extend_params||JSON串<br>示例参照：[示例字段](/aggregatePay/introduction#花呗分期信息-reserved_ali_extend_params-示例字段)| 非必填                                                                                                                  |
|28||└ hb_fq_num|3|花呗分期期数：仅支持3、6、12| 非必填                                                                                                                  |
|29||└ hb_fq_seller_percent|3|花呗分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br>目前该字段未生效，我司会默认当0处理| 非必填                                                                                                                  |
|30||└ industry_reflux_info|512|行业数据回流信息<br>示例参照：[示例字段](/aggregatePay/introduction#行业数据回流信息-industry_reflux_info-示例字段)| 非必填                                                                                                                  |
|31|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ order_type +"\|"<br>+order_amt +"\|"<br>+ mchnt_order_no+"\|"<br>+ txn_begin_ts +"\|"<br>+ goods_des +"\|"<br>+ term_id + "\|"<br>+ term_ip + "\|"<br>+notify_url+ "\|"<br>+random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key<br><br>并做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填|
|32|终端信息说明字段| reserved_terminal_info     | json | 见文档中[reserved_terminal_info终端信息说明字段（259号文，终端信息）](/aggregatePay/transform#reserved_terminal_info终端信息说明字段（259号文，终端信息）) | 非必填 |


#### 统一下单响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填||
|2|中文描述|result_msg|128|代码描述|必填||
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||
|4|终端号|term_id|8|终端号|非必填||
|5|随机字符串|random_str|32|随机字符串|必填||
|6|订单类型|order_type|20|订单类型：ALIPAY，WECHAT，UNIONPAY(银联二维码)等同上|必填||
|7|会话标识|session_id|64|预支付交易会话标识<br>富友返回支付宝生成的预支付回话标识<br>用于后续接口调用中使用，该值有效期为2小时|非必填||
|8|二维码链接|qr_code|64|二维码链接|非必填||
|9|富友生成的订单号|reserved_fy_order_no|30|富友生成的订单号，需要商户与商户订单号进行关联(预留)|非必填||
|10|追踪号|reserved_fy_trace_no|12|富友系统内部追踪号|必填||
|11|银行交易号|reserved_channel_order_id|32|银行返回的交易号|非必填||
|12|签名|sign||拼接<br><br>result_code+"\|"<br>+result_msg+"\|"<br>+mchnt_cd+"\|"<br>+reserved_fy_trace_no+"\|"<br>+random_str +"\|"<br>+mchnt_key<br><br>并做md5摘要<br>其中mchnt_key为商户密钥，系统分配|||

---


### 2.2 条码支付接口

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/micropay`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/micropay`

*报文格式：* `JSON`

#### 场景说明

![条码支付流程图](_images/micropay-process.jpg '条码支付流程图')

- `步骤1：`用户和商户确认订单金额
- `步骤2：`用户向商户展示支付二维码(微信、支付宝等)
- `步骤3：`商户使用扫码枪，扫描用户二维码并提交到富友扫码，并实时返回支付结果

!> 注：条码支付存在需要用户输入支付密码的场景(实时返回用户支付中)。<br>是否需要输入支付密码，与支付宝、微信的风控规则有关。<br>此时需要商户主动发起查询来确定最好的订单状态。<br>(建议：交易发起5S以后开始查询。查询间隔5S以上)

#### 条码支付请求报文

|序号|参数中文名|参数名|长度| 说明                                                                                              |必填|
|:-----:|:-----|:-----|:-----:|:------------------------------------------------------------------------------------------------|:-----|
|1|版本号|version|8| 1.0                                                                                             |必填|
|2|商户号|mchnt_cd|15| 富友分配的商户号<br>例：0002900F0313432                                                                   |必填|
|3|随机字符串|random_str|32| 随机字符串                                                                                           |必填|
|4|订单类型|order_type|20| 订单类型：<br>ALIPAY<br>WECHAT<br>WXBX(微信保险类)<br>ALBX(支付宝保险类)<br>UNIONPAY(银联二维码)<br>BESTPAY(翼支付)(不支持)<br>DIGICCY(数字货币)<br/>若交易为WXBX、ALBS 仍需上传本字段<br/>若此字段不传,则响应报文中order_type字段会返回交易类型<br/>查询接口仍需上传交易类型，故需自行保存交易类型 |非必填|
|5|订单总金额|order_amt|16| 以`分`为单位                                                                                         |必填|
|6|商户订单号|mchnt_order_no|30| 商户系统内部的订单号                                                                                      ||
|7|交易起始时间|txn_begin_ts|14| 订单生成时间<br>格式：<br>yyyyMMddHHmmss                                                                 |必填|
|8|商品描述|goods_des|128| 商品或支付单简要描述                                                                                      |必填|
|9|商品详情|goods_detail|600| 商品名称明细                                                                                          |非必填|
|10|商品标记|goods_tag|32| 商品标记                                                                                            |非必填|
|11|终端号|term_id|8| 随机8字节数字字母组合                                                                                     |必填|
|12|终端IP|term_ip|16| 终端IP                                                                                            |必填|
|13|附加数据|addn_inf|100| 附加数据                                                                                            |非必填|
|14|货币类型|curr_type|3| 默认人民币：CNY                                                                                       |非必填|
|15|扫码支付授权码|auth_code|128| 扫码支付授权码，设备读取用户的条码或者二维码信息                                                                        |必填|
|16|支付场景|sence|1| 默认1<br>条码支付：1<br>声波支付：2                                                                         |非必填|
|17|子商户公众号id|reserved_sub_appid|32| 子商户配置多个公众号时必填                                                                                   |非必填|
|18|限制支付|reserved_limit_pay|32| no_credit:不能使用信用卡<br>credit_group:不能使用花呗和信用卡<br>`不参与签名`                                         |非必填|
|19|交易关闭时间|reserved_expire_minute|8| 交易关闭时间（单位分钟，例：60（1小时））,默认填0（2小时）                                                                |非必填|
|20|富友终端号|reserved_fy_term_id|20| 富友终端号(富友终端号与TUSN号二选一),富友采购或自带机入网填此字段                    |非必填|
|21|用户身份证号|reserved_user_creid|64| 用户身份证号码<br>(实名保险类交易必填)                                                                          |非必填|
|22|用户姓名|reserved_user_truename|20| 用户姓名                                                                                            |非必填|
|23|用户手机号|reserved_user_mobile|20| 用户手机号用户手机号<br>(仅支付宝认证)                                                                          |非必填|
|24|保险公司订单号|reserved_bxno_inf|65| 保险公司订单号，认证机构单号<br>(使用英文逗号分隔)                                                                    |非必填|
|25|富友终端类型|reserved_fy_term_type|1| 富友终端类型<br>0：其他<br>1：富友终端<br>2：POS机<br>3：台卡<br>4：PC软件                                            |非必填|
|26|终端序列号|reserved_fy_term_sn|20| 终端序列号                                                                                           |非必填|
|27|设备信息|reserved_device_info|32| 设备信息，托传给微信。用于单品券核销<br>示例参照：[示例字段](/aggregatePay/introduction#设备信息-reserved_device_info-示例字段)    |非必填|
|28|花呗分期信息|reserved_ali_extend_params||JSON串<br>示例参照：[示例字段](/aggregatePay/introduction#花呗分期信息-reserved_ali_extend_params-示例字段)| 非必填                                                                                             |
|29||└ hb_fq_num|3|花呗分期期数：仅支持3、6、12| 非必填                                                                                             |
|30||└ hb_fq_seller_percent|3|花呗分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br>目前该字段未生效，我司会默认当0处理| 非必填                                                                                             |
|31||└ industry_reflux_info|512|行业数据回流信息<br>示例参照：[示例字段](/aggregatePay/introduction#行业数据回流信息-industry_reflux_info-示例字段)| 非必填                                                                                             |
|32||└ dynamic_token_out_biz_|128|条码支付中，目前支持的用法为：sence_no| 非必填                                                                                             |
|33|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ order_type +"\|"<br>+order_amt +"\|"<br>+ mchnt_order_no+"\|"<br>+ txn_begin_ts +"\|"<br>+ goods_des +"\|"<br>+ term_id + "\|"<br>+ term_ip + "\|"<br>+auth_code + "\|"<br>+random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key<br><br>并做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填|
|34|终端信息说明字段| reserved_terminal_info     | json | 见文档中[reserved_terminal_info终端信息说明字段（259号文，终端信息）](/aggregatePay/transform#reserved_terminal_info终端信息说明字段（259号文，终端信息）) |非必填|

#### 条码支付响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填||
|2|中文描述|result_msg|128|代码描述|必填||
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||
|4|终端号|term_id|8|终端号|非必填||
|5|随机字符串|random_str|32|随机字符串|必填||
|6|订单类型|order_type|20|订单类型：ALIPAY，WECHAT，UNIONPAY(银联二维码)等同上|必填||
|7|订单金额|total_amount|16|分为单位的整数|必填||
|8|买家渠道号|buyer_id|32|买家在渠道账号|非必填||
|9|渠道流水号|transaction_id|32|渠道交易流水号|必填||
|10|附加数据|addn_inf||100|非必填|
|11|富友生成的订单号|reserved_fy_order_no|30|富友生成的订单号，需要商户与商户订单号进行关联(预留)|非必填||
|12|商户订单号|reserved_mchnt_order_no|30|商户系统内部的订单号|必填||
|13|富友清算日|reserved_fy_settle_dt|8|富友清算日|必填||
|14|优惠金额|reserved_coupon_fee|10|以分为单位|非必填||
|15|买家登录账号|reserved_buyer_logon_id|128|买家在渠道登录账号|非必填||
|16|渠道信息|reserved_fund_bill_list|不定长|支付宝交易资金渠道，详细渠道|非必填||
|17|富友追踪号|reserved_fy_trace_no|12|富友系统内部追踪号|必填||
|18|银行交易号|reserved_channel_order_id|32|银行返回的交易号|非必填||
|19|信用卡标识|reserved_is_credit|8|1：表示信用卡或者花呗<br>0：表示其他(非信用方式)<br>不填，表示未知|非必填||
|20|付款方式|reserved_bank_type|16||非必填||
|21|应结算订单金额|**`reserved_settlement_amt`**|16|以分为单位的整数<br />只有成功交易才会返回<br /><br />如果使用了商户免充值优惠券，该值为订单金额-商户免充值<br />如果没有使用商户免充值，该值等于订单金额|非必填||
|22|微信营销详情|**`reserved_promotion_detail`**|6000|见文档中[reserved_promotion_detail说明字段](aggregatePay/introduction#reserved_promotion_detail说明字段)|非必填||
|23|签名|sign||拼接<br><br>result_code+"\|"<br>+result_msg+"\|"<br>+mchnt_cd+"\|"<br>+total_amount+"\|"<br>+transaction_id+"\|"<br>+reserved_mchnt_order_no +"\|"<br>+reserved_fy_settle_dt +"\|"<br>+random_str +"\|"<br>+mchnt_key<br><br>并做md5摘要<br>其中mchnt_key为商户密钥，系统分配|||

---


### 2.3 订单查询接口

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/commonQuery`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/commonQuery`

*报文格式：* `JSON`

#### 场景说明

!> 由于不可抗因素导致交易超时，或者状态不明确时，需要商户主动发起交易查询。来确定交易的最终状态。目前系统支持往前查询3日的交易。即 D-3 到 D 日的交易才能查询到记录。

#### 订单查询请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|版本号|version|8|1.0|必填|
|2|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填|
|3|随机字符串|random_str|32|随机字符串|必填|
|4|订单类型|order_type|20|订单类型：<br>ALIPAY(统一下单、条码支付、服务窗支付)<br>WECHAT(统一下单、条码支付，公众号支付)<br>UNIONPAY<br>WXAPP(微信app)<br>ALIAPP(支付宝app)<br>WXH5(微信h5)<br>ALIH5(支付宝h5)<br>WXBX(微信保险类)<br>ALBX(支付宝保险类)<br>WXXS(微信线上所有交易)<br>BESTPAY(翼支付)|必填|
|5|商户订单号|mchnt_order_no|30|商户系统内部的订单号|必填|
|6|终端号|term_id|8|随机8字节数字字母组合|必填|
|7|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ order_type +"\|"<br>+ mchnt_order_no+"\|"<br>+ term_id +"\|"<br>+ random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key<br><br>并做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填|

#### 订单查询响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填|
|2|中文描述|result_msg|128|代码描述|必填|
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填|
|4|终端号|term_id|8|终端号|非必填|
|5|随机字符串|random_str|32|随机字符串|必填|
|6|订单类型|order_type|20|订单类型：<br>ALIPAY (统一下单、条码支付、服务窗支付)<br>WECHAT(统一下单、条码支付，公众号支付)<br>UNIONPAY<br>WXAPP(微信app)<br>ALIAPP(支付宝app)<br>WXH5(微信h5)<br>ALIH5(支付宝h5)<br>WXBX(微信保险类)<br>ALBX(支付宝保险类)<br>【公众号相关的交易为空，其他都有】|必填|
|7|订单金额|order_amt|16|分为单位的整数|必填|
|8|买家渠道号|buyer_id|32|买家在渠道账号|非必填|
|9|渠道流水号|transaction_id|32|渠道交易流水号|必填|
|10|附加数据|addn_inf||100|附加数据|
|11|富友清算日|reserved_fy_settle_dt|8|富友清算日|必填|
|12|商户订单号|mchnt_order_no|30|商户系统内部的订单号|必填|
|13|交易状态|trans_stat|32|SUCCESS—支付成功<br>REFUND—已退款<br>NOTPAY—未支付<br>CLOSED—已关闭<br>REVOKED—已撤销（刷卡支付）<br>USERPAYING--用户支付中<br>PAYERROR--支付失败(其他原因，如银行返回失败)|必填|
|14|优惠金额|reserved_coupon_fee|10|以分为单位|非必填|
|15|买家登录账号|reserved_buyer_logon_id|128|买家在渠道登录账号|非必填|
|16|渠道信息|reserved_fund_bill_list|不定长|支付宝交易资金渠道，详细渠道|非必填|
|17|富友追踪号|reserved_fy_trace_no|12|富友系统内部追踪号|必填|
|18|银行交易号|reserved_channel_order_id|32|银行返回的交易号|非必填|
|19|富友终端号|reserved_fy_term_id|20|富友的POS终端号，如果不是用的富友的POS终端请忽略|非必填|
|20|信用卡标识|reserved_is_credit|8|1：表示信用卡或者花呗<br>0：表示其他(非信用方式)<br>不填，表示未知|非必填|
|21|付款方式|reserved_bank_type|16||非必填|
|22|支付完成时间|reserved_txn_fin_ts|14|订单支付时间<br>格式为：yyyyMMddHHmmss|非必填|
|23|应结算订单金额|**`reserved_settlement_amt`**|16|以分为单位的整数<br />只有成功交易才会返回<br /><br />如果使用了商户免充值优惠券，该值为订单金额-商户免充值<br />如果没有使用商户免充值，该值等于订单金额|非必填|
|24|微信营销详情|**`reserved_promotion_detail`**|6000|见文档中[reserved_promotion_detail说明字段](aggregatePay/introduction#reserved_promotion_detail说明字段)|非必填|
|25|签名|sign||拼接<br><br>result_code+"\|"<br>+ result_msg+"\|"<br>+ mchnt_cd +"\|"<br>+ order_amt +"\|"<br>+ transaction_id+"\|"<br>+ mchnt_order_no+"\|"<br>+ reserved_fy_settle_dt+"\|"<br>+ trans_stat+"\|"<br>+ random_str+"\|"<br>+ mchnt_key<br><br>并做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填|

---


### 2.4 退款申请接口

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/commonRefund`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/commonRefund`

*报文格式：* `JSON`

#### 场景说明

> 退款接口只是一个申请接口向微信、银行等申请退款，钱会在三个工作日内到账，如果这中间失败了目前不能查询，后期会增加一个退款查询接口，当交易发生之后一段时间内，由于买家或者卖家的原因需要退款时，卖家可以通过退款接口将支付款退还给买家，扫码支付平台将在收到退款请求并且验证成功之后，按照退款规则将支付款按原路退到买家帐号上。总退款金额不能超过订单总金额。<br>注：目前支持对180天以内的交易进行退款 ，支持全额或者是多次部分退款

#### 退款申请请求报文

| 序号 | 参数中文名 | 参数名                 |     长度      | 说明                                                                                                                                                                                                    | 必填  |
|:--:|:------|:--------------------|:-----------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:----|
|  1 | 版本号   | version             |      8      | 1.0                                                                                                                                                                                                   | 必填  |
|  2 | 商户号   | mchnt_cd            |     15      | 富友分配的商户号<br>例：0002900F0313432                                                                                                                                                                         | 必填  |
|  3 | 终端号   | term_id             |      8      | 终端号，备用字段，传任意 8 字节字符即可                                                                                                                                                                                 ||
|  4 | 随机字符串 | random_str          |     32      | 随机字符串                                                                                                                                                                                                 | 必填  |
|  5 | 商户订单号 | mchnt_order_no      |     30      | 原正交易商户订单号                                                                                                                                                                                             | 必填  |
|  6 | 退款单号  | refund_order_no     |     30      | 商户退款单号(也要加订单前缀)                                                                                                                                                                                       | 必填  |
|  7 | 订单类型  | order_type          |     20      | 订单类型：<br>ALIPAY(统一下单、条码支付、服务窗支付)<br>WECHAT(统一下单、条码支付、公众号支付、小程序)<br>UNIONPAY<br>WXAPP(微信app)<br>ALIAPP(支付宝app)<br>WXH5(微信h5)<br>ALIH5(支付宝h5)<br>WXBX(微信保险类)<br>ALBX(支付宝保险类)<br>WXXS(微信线上所有交易)(不支持线下类型) | 必填  |
|  8 | 订单金额  | total_amt           |     16      | 分为单位的整数                                                                                                                                                                                               | 必填  |
|  9 | 退款金额  | refund_amt          |     16      | 退款金额以分为单位                                                                                                                                                                                             | 必填  |
| 10 | 操作员   | operator_id         |     32      | 操作员                                                                                                                                                                                                   | 非必填 |
| 11 | 富友终端号 | reserved_fy_term_id |      8      | 富友终端号                                                                                                                                                                                                 | 非必填 |
| 12 | 原交易日期 | reserved_origi_dt   |      8      | 原交易日期（yyyyMMdd）<br>该值必定等于 reserved_fy_settle_dt(富友接收交易时间。理论和合作方下单时间一致。微量跨日交易会不一致)。<br>不填该值支持30天内的交易进行退款。<br>填写该值，支持180天。                                                                              | 非必填 |
| 13 | 附加数据  | reserved_addn_inf   |     100     | 附加数据                                                                                                                                                                                                  | 非必填 |
| 14 |签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ order_type +"\|"<br>+ mchnt_order_no +"\|"<br>+ refund_order_no+"\|"<br>+ total_amt+"\|"<br>+ refund_amt +"\|"<br>+ term_id +"\|"<br>+ random_str +"\|"<br>+ version +"\|"<br>+ mchnt_key <br><br>并做md5摘要<br>其中 mchnt_key 为商户密钥，系统分配|必填|


#### 退款申请响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填||
|2|中文描述|result_msg|128|代码描述|非必填||
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||
|4|终端号|term_id|8|终端号|非必填||
|5|随机字符串|random_str|32|随机字符串|必填||
|6|订单类型|order_type|20|订单类型：<br>ALIPAY (统一下单、条码支付、服务窗支付)<br>WECHAT(统一下单、条码支付，公众号支付)<br>UNIONPAY<br>WXAPP(微信app)<br>ALIAPP(支付宝app)<br>WXH5(微信h5)<br>ALIH5(支付宝h5)<br>WXBX(微信保险类)<br>ALBX(支付宝保险类)<br>【公众号相关的交易为空，其他都有】|必填||
|7|渠道流水号|transaction_id|32|渠道交易流水号|非必填||
|8|退款流水号|refund_id|渠道退款流水号|非必填|||
|9|商户订单号|mchnt_order_no|30|商户系统内部的订单号|必填||
|10|退款单号|refund_order_no|30|商户退款单号|必填||
|11|富友清算日|reserved_fy_settle_dt|8|富友清算日|必填||
|12|退款金额|reserved_refund_amt|16|退款金额|非必填||
|13|富友追踪号|reserved_fy_trace_no|12|富友系统内部追踪号|必填||
|14|签名|sign||拼接<br><br>result_code+"\|"<br>+ mchnt_cd +"\|"<br>+ mchnt_order_no+"\|"<br>+ refund_order_no+"\|"<br>+ reserved_fy_settle_dt+"\|"<br>+ random_str+"\|"<br>+ mchnt_key<br><br>并做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填||

---


### 2.5 支付结果通知

#### 请求地址

*测试地址：* `商户提供`

*生产地址1：*

*报文格式：* `JSON`

#### 场景说明

![支付结果通知](_images/callBack-process.jpg '支付结果通知')

!> 如图中第 6 步中异步回调，下单(主扫)交易的结果是以异步的形式进行回调的。富友在接受到支付宝等支付通道的回调结果以后再回调商户。商户接收回调成功处理成功后返回字符串”1” , 后富友停止回调给商户。最多回调 5 次，每次间隔 30S。

!> （`重要！！！重要！！！重要！！！`不保证通知最终一定能成功，在订单状态不明或者没有收到微信，支付结果通知的情况下，建议商户主动调用【2.3 订单查询】确认订单状态）<br>只有主扫、公众号/服务窗支付会通过此接口发异步通知，条码支付没有异步通知。


#### 支付结果通知响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填|
|2|中文描述|result_msg|128|代码描述|非必填|
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填|
|4|终端号|term_id|8|终端号|非必填|
|5|随机字符串|random_str|32|随机字符串|必填|
|6|订单类型|order_type|20|订单类型：<br>ALIPAY<br>WECHAT<br>UNIONPAY(银联二维码)等|必填|
|7|渠道流水号|transaction_id|32|渠道交易流水号|非必填|
|8|用户ID|user_id|128|用户在商户的ID|非必填|
|9|商户订单号|mchnt_order_no|30||必填|
|10|订单金额|order_amt|16||必填|
|11|应结订单金额|settle_order_amt|16||必填|
|12|货币种类|curr_type|3||非必填|
|13|支付完成时间|txn_fin_ts|14|格式：yyyyMMddHHmmss|必填|
|14|富友清算日|reserved_fy_settle_dt|8||必填|
|15|优惠金额|reserved_coupon_fee|10||非必填|
|16|买家登录账号|reserved_buyer_logon_id|128||非必填|
|17|渠道信息|reserved_fund_bill_list|不定长|支付宝交易资金渠道，详细渠道|非必填|
|18|富友追踪号|reserved_fy_trace_no|12||必填|
|19|银行交易号|reserved_channel_order_id|32||非必填|
|20|信用卡标识|reserved_is_credit|8|1- -表示信用卡或者花呗<br>0- -表示其他(非信用方式)<br>不填- -表示未知|非必填|
|21|应结算订单金额|**`reserved_settlement_amt`**|16|以分为单位的整数<br />只有成功交易才会返回<br /><br />如果使用了商户免充值优惠券，该值为订单金额-商户免充值<br />如果没有使用商户免充值，该值等于订单金额|非必填|
|22|微信营销详情|**`reserved_promotion_detail`**|6000|见文档中[reserved_promotion_detail说明字段](aggregatePay/introduction#reserved_promotion_detail说明字段)|非必填|
|23|付款方式|reserved_bank_type|16||非必填|
|24|附加数据|reserved_addn_inf|100||非必填|
|25|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ mchnt_order_no+"\|"<br>+ settle_order_amt +"\|"<br>+ order_amt +"\|"<br>+ txn_fin_ts +"\|"<br>+ reserved_fy_settle_dt+"\|"<br>+ random_str+ "\|"<br>+ mchnt_key<br><br>并做md5摘要<br>其中 mchnt_key 为商户密钥，系统分配||
|26|全签名|full_sign||拼接<br><br>result_code+"\|"<br>+ result_msg+"\|"<br>+ mchnt_cd+"\|"<br>+ mchnt_order_no+"\|"<br>+ settle_order_amt+"\|"<br>+ order_amt+"\|"<br>+ txn_fin_ts+"\|"<br>+ reserved_fy_settle_dt+"\|"<br>+ random_str+"\|"<br>+ mchnt_key <br><br>并做md5摘要<br>其中mchnt_key 为商户密钥，系统分配||



---



### 2.6 公众号/服务窗统一下单接口

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/wxPreCreate`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/wxPreCreate`

*报文格式：* `JSON`

#### 场景说明

![公众号/服务窗统一下单流程图](_images/wxPreCreate-process.jpg '公众号/服务窗统一下单流程图')

?> 该接口常应用于聚合二维码（静态二维码、统一收款码、台卡等不同叫法），用户扫二维码进入微信公众号/支付宝服务窗/QQJS 页面，页面调此接口生成订单，接受订单参数后调起官方支付接口支付。详见公众号/服务窗对接流程(若是使用富友的appid,需要调用2.16接口获取到该appid对应的openid)

- `步骤 1：`用户通过支付宝(服务窗)、微信(公众号)进入到商户 H5/SDK 页面，或者是通过扫描台卡进入。

  商户H5页面中可以通过user-agent获取用户使用的是哪种支付方式

  * 如果来自微信APP

    * 获取用户openid，传入交易接口sub_openid字段

      获取用户openid有两种模式：

      a: 使用富友appid(即商户没有自有公众号或者合作方没有申请自有渠道号)的情况下，使用接口2.16服务商模式获取openid

      b:使用合作方或者商户自有公众号的情况，公众号appid传入sub_appid字段中，openid需要调用微信官方接口获取 https://pay.weixin.qq.com/doc/v2/merchant/4011939975#14%E3%80%81Openid

  * 如果来自支付宝APP

 ：# (    * 获取用户user_id&#40;即buyer_id&#41;，传入交易接口sub_openid字段,user_id需要调用支付宝官方接口获取https://opendocs.alipay.com/open/02xtl8

- `步骤 2：`用户选择商品、输入支付金额等进行下单支付

- `步骤 3：`商户将订单信息发送给富友，返回支付信息(用于调起支付宝、微信的参数)。

- `步骤 4：`商户拿到支付信息后调起微信或者支付宝进行支付

- `步骤 5：`支付结果以回调(2.5)的方式通知到商户

#### 公众号/服务窗统一下单请求报文

| 序号 |参数中文名| 参数名                        |长度| 说明                                                                                                                                                                                                                                                  |必填|
|:--:|:-----|:---------------------------|:-----:|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-----|
|  1 |版本号| version                    |8| 1.0                                                                                                                                                                                                                                                 |必填|
|  2 |商户号| mchnt_cd                   |15| 富友分配的商户号<br>例：0002900F0313432                                                                                                                                                                                                                       |必填|
|  3 |随机字符串| random_str                 |32|| 必填                                                                                                                                                                                                                                                  |
|  4 |订单总金额| order_amt                  |16| 以分为单位                                                                                                                                                                                                                                               |必填|
|  5 |商户订单号| mchnt_order_no             |30|| 必填                                                                                                                                                                                                                                                  |
|  6 |商品标识| product_id                 |32|| 非必填                                                                                                                                                                                                                                                 |
|  7 |交易起始时间| txn_begin_ts               |14| 格式：yyyyMMddHHmmss                                                                                                                                                                                                                                   |必填|
|  8 |商品描述| goods_des                  |128|| 必填                                                                                                                                                                                                                                                  |
|  9 |商品详情| goods_detail               |600| 商品名称明细                                                                                                                                                                                                                                              |非必填|
| 10 |商品标记| goods_tag                  |32|| 非必填                                                                                                                                                                                                                                                 |
| 11 |终端号| term_id                    |8| 随机 8 字节数字字母组合                                                                                                                                                                                                                                       |必填|
| 12 |终端IP| term_ip                    |16| 终端IP                                                                                                                                                                                                                                                |必填|
| 13 |附加数据| addn_inf                   |100|| 非必填                                                                                                                                                                                                                                                 |
| 14 |货币种类| curr_type                  |3| 默认人民币：CNY                                                                                                                                                                                                                                           |非必填|
| 15 |通知URL| notify_url                 |256| 接收富友异步通知回调地址<br>通知url必须为直接公网可访问的url,只能是域名，内网地址如192.168或者172,10开头的地址不能使用，若需加白使用，需要找我司索要出口IP,不能携带参数主扫                                                                                                                                                                                                          |必填|
| 16 |限制支付| limit_pay                  |32| no_credit：不能使用信用卡<br>credit_group：不能使用花呗和信用卡<br>`不参与签名`                                                                                                                                                                                             |非必填|
| 17 |交易类型| trade_type                 |16| JSAPI--公众号线下支付<br>~~GZXS--公众号线上~~<br>APP--app支付(**暂不可用**)<br>FWC--支付宝服务窗<br>LETPAY-小程序<br>~~LPXS--小程序线上~~<br>WXBXJS(微信公众号保险类)<br>WXBXLET(微信小程序保险类)<br>WXBXAPP(微信APP保险类)<br>ALBXJS(支付宝服务窗保险类)<br>APPLEPAY(相机扫码)<br>UNIONPAY(云闪付扫码)<br>BESTPAY(翼支付js) |必填|
| 18 |用户标识| openid                     |128|| 非必填 `获取方法看2.16接口`                                                                                                                                                                                                                                   |
| 19 |子商户用户标识| sub_openid                 |128| 子商户用户标识<br>支付宝服务窗为用户buyer_id（此场景必填）<br>微信公众号为用户的openid（小程序，公众号，服务窗必填）<br>APPLEPAY(相机扫码)该值不用填<br>UNIONPAY(云闪付扫码)该值必填,`若是用富友的appid，则调用2.16接口获取此参数；若用自己的，则去对应官网获取参数`                                                                                   |非必填|
| 20 |子商户公众号id| sub_appid                  |32| 子商户公众号id<br>sub_appid 填商户或者是服务商的 appid<br>微信交易为商户的appid（小程序，公众号必填）    `此接口若客户自己没有appid，则使用富友的appid，反之可以用自己的`                                                                                                                                        |非必填|
| 21 |富友终端号| reserved_fy_term_id        |20| 富友终端号`(如果不是用的富友的POS 终端，此字段千万不要填，不然会影响清算)`                                                                                                                                                                                                           |非必填|
| 22 |交易关闭时间| reserved_expire_minute     |8| 交易关闭时间（单位分钟，例：60（1小时））,默认填0（2小时）                                                                                                                                                                                                                    |非必填|
| 23 |用户身份证号| reserved_user_creid        |64| 用户身份证号码(实名保险类交易必填)                                                                                                                                                                                                                                  |非必填|
| 24 |用户姓名| reserved_user_truename     |20|| 非必填                                                                                                                                                                                                                                                 |
| 25 |用户手机号| reserved_user_mobile       |20|| 非必填                                                                                                                                                                                                                                                 |
| 26 |保险公司订单号| reserved_bxno_inf          |65| 保险公司订单号，认证机构单号<br>(使用英文逗号分隔)<br><br>深圳模式示例：SZ01,单号1,单号2<br>`通过前缀SZ01识别深圳模式`                                                                                                                                                                         |非必填|
| 27 |富友终端类型| reserved_fy_term_type      |1| 富友终端类型<br>0:其他<br>1:富友终端<br>2:POS机<br>3:台卡<br>4:PC软件                                                                                                                                                                                                |非必填|
| 28 |终端序列号| reserved_fy_term_sn        |20|| 非必填                                                                                                                                                                                                                                                 |
| 29 |设备信息| reserved_device_info       |32| 设备信息，托传给微信。用于单品券核销<br>示例参照：[示例字段](/aggregatePay/introduction#设备信息-reserved_device_info-示例字段)                                                                                                                                                        |非必填|
| 30 |花呗分期信息| reserved_ali_extend_params ||JSON串<br>示例参照：[示例字段](/aggregatePay/introduction#花呗分期信息-reserved_ali_extend_params-示例字段)||
| 31 ||└ hb_fq_num| 3                          |花呗分期期数：仅支持3、6、12| 非必填                                                                                                                                                                                                                                                 |
| 32 ||└ hb_fq_seller_percent| 3                          |花呗分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br>目前该字段未生效，我司会默认当0处理| 非必填                                                                                                                                                                                                                                                 |
| 33 ||└ industry_reflux_info| 512                        |行业数据回流信息<br>示例参照：[示例字段](/aggregatePay/introduction#行业数据回流信息-industry_reflux_info-示例字段)| 非必填                                                                                                                                                                                                                                                 |
| 34 ||└ food_order_type| 32                         |点餐场景类型：<br>qr_order(店内扫码点餐)<br>pre_order(预点到店自提)<br>home_delivery(外送到家)<br>direct_payment(直接付款)<br>other(其他)| 非必填                                                                                                                                                                                                                                                 |
| 35 |签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ trade_type +"\|"<br>+ order_amt +"\|"<br>+ mchnt_order_no+"\|"<br>+ txn_begin_ts+"\|"<br>+ goods_des +"\|"<br>+ term_id +"\|"<br>+ term_ip +"\|"<br>+ notify_url +"\|"<br>+ random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key <br><br>并做md5摘要<br>其中 mchnt_key 为商户密钥，系统分配|必填|
| 36 |终端信息说明字段| reserved_terminal_info     |json| 见文档中[reserved_terminal_info终端信息说明字段（259号文，终端信息）](/aggregatePay/transform#reserved_terminal_info终端信息说明字段（259号文，终端信息）)                                                                                                                                |非必填|


#### 公众号/服务窗统一下单响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填|
|2|中文描述|result_msg|128|代码描述|必填|
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填|
|4|终端号|term_id|8|终端号|非必填|
|5|随机字符串|random_str|32|随机字符串|必填|
|6|子商户识别码|sub_mer_id||支付通道对应的子商户识别码|非必填|
|7|会话标识|session_id|64|预支付交易会话标识，富友返回支付宝生成的预支付回话标识，用于后续接口调用中使用，该值有效期为2小时|非必填|
|8|二维码链接|qr_code|64|支付链接(直接重定向调起支付)|非必填|
|9|子商户公众号|sub_appid|32|子商户公众号id|非必填|
|10|子商户标识|sub_openid|128|子商户用户标识|非必填|
|11|公众号id|sdk_appid|16|公众号id|微信支付时返回|
|12|时间戳|sdk_timestamp|32|时间戳，自1970年1月1日0点0分0秒以来的秒数|微信支付时返回|
|13|随机字符串|sdk_noncestr|32|随机字符串|微信支付时返回|
|14|扩展字段|sdk_package|128|订单详情扩展字符串|微信支付时返回|
|15|签名方式|sdk_signtype|32|签名方式，trade_type为JSAPI、LETPAY时才返回|微信支付时返回|
|16|签名|sdk_paysign|64|签名|微信支付时返回|
|17||sdk_partnerid|32|trade_type为APP时才返回|非必填|
|18|富友生成的订单号|reserved_fy_order_no|30|需要商户与商户订单号进行关联|非必填|
|19|富友清算日|reserved_fy_settle_dt|8||非必填|
|20|渠道交易流水号|reserved_transaction_id|32|trade_type为FWC时返回|非必填|
|21|追踪号|reserved_fy_trace_no|12||必填|
|22|支付参数|reserved_pay_info|不定长||非必填|
|23|银行交易号|reserved_channel_order_id|32||非必填|
|24|附加数据|reserved_addn_inf|50||非必填|
|25|签名|sign||拼接<br><br>result_code+"\|"<br>+ result_msg+"\|"<br>+ mchnt_cd+"\|"<br>+ reserved_fy_trace_no+"\|"<br>+ random_str +"\|"<br>+ mchnt_key<br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填|



---



[//]: # "### 2.7 简版微信小程序下单接口（`暂停使用`）"

[//]: #
[//]: # "#### 请求地址"

[//]: # "*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/xcxPreCreate`"

[//]: #
[//]: # "*生产地址1：* `https://aipay-cloud.fuioupay.com/aggregatePay/xcxPreCreate`"

[//]: #
[//]: # "*生产地址2：* `https://aipay-xs.fuioupay.com/aggregatePay/xcxPreCreate`"

[//]: #
[//]: # "*报文格式：* `JSON`"

[//]: #
[//]: # "#### 场景说明"

[//]: #
[//]: # "![简版微信小程序下单]&#40;_images/xcxPreCreate-process.jpg '简版微信小程序下单'&#41;"

[//]: #
[//]: # "?> 该接口常应用于商户 APP 内微信支付。"

[//]: #
[//]: # "- `步骤 1：`用户通过商户 app 进入。"

[//]: # "- `步骤 2：`用户选择商品、输入支付金额等进行下单支付"

[//]: # "- `步骤 3：`商户将订单信息发送给富友小程序，返回支付信息&#40;用于、微信的参数&#41;。"

[//]: # "- `步骤 4：`小程序拿到支付信息后调起微信进行支付，支付完成后返回 app。"

[//]: # "- `步骤 5：`支付结果以回调&#40;2.5&#41;的方式通知到商户"

[//]: # "#### 简版微信小程序下单请求报文"

[//]: #
[//]: # "|序号|参数中文名|参数名|长度|说明|必填|"

[//]: # "|:-----:|:-----|:-----|:-----:|:-----|:-----|"

[//]: # "|1|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填|"

[//]: # "|2|订单总金额|order_amt|16|以分为单位|必填|"

[//]: # "|3|商户订单号|mchnt_order_no|30||必填|"

[//]: # "|4|通知URL|notify_url|256|接收富友异步通知回调地址，通知url必须为直接可访问的url，不能携带参数主扫时必填|必填|"

[//]: # "|5|子商户用户标识|sub_openid|128|子商户用户标识<br>支付宝服务窗为用户buyer_id（此场景必填）<br>微信公众号为用户的openid（小程序，公众号，服务窗必填）|非必填|"

[//]: # "|6|子商户公众号id|sub_appid|32|子商户公众号id<br>sub_appid 填商户或者是服务商的 appid<br>微信交易为商户的appid（小程序，公众号必填）|非必填|"

[//]: # "|7|交易类型|reserved_trade_type|16|JSAPI--公众号支付`&#40;默认&#41;`<br>LPXS--小程序线上|选填|"


[//]: # "|9|终端信息说明字段|reserved_terminal_info|json|见文档中[reserved_terminal_info终端信息说明字段（259号文，终端信息）]&#40;/aggregatePay/transform#reserved_terminal_info终端信息说明字段（259号文，终端信息）&#41;|非必填|"


[//]: # "#### 简版微信小程序下单响应报文"

[//]: #
[//]: # "|序号|参数中文名|参数名|长度|说明|必填|"

[//]: # "|:-----:|:-----|:-----|:-----:|:-----|:-----|"

[//]: # "|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填||"

[//]: # "|2|中文描述|result_msg|128|代码描述|必填||"

[//]: # "|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||"

[//]: # "|4|终端号|term_id|8|终端号|非必填||"

[//]: # "|5|随机字符串|random_str|32|随机字符串|必填||"

[//]: # "|6|子商户识别码|sub_mer_id||支付通道对应的子商户识别码|非必填||"

[//]: # "|7|会话标识|session_id|64|预支付交易会话标识，富友返回支付宝生成的预支付回话标识，用于后续接口调用中使用，该值有效期为2小时|非必填||"

[//]: # "|8|二维码链接|qr_code|64|支付链接&#40;直接重定向调起支付&#41;|非必填||"

[//]: # "|9|子商户公众号|sub_appid|32|子商户公众号id|非必填||"

[//]: # "|10|子商户标识|sub_openid|128|子商户用户标识|非必填||"

[//]: # "|11|公众号id|sdk_appid|16|公众号id|微信支付时返回||"

[//]: # "|12|时间戳|sdk_timestamp|32|时间戳，自1970年1月1日0点0分0秒以来的秒数|微信支付时返回||"

[//]: # "|13|随机字符串|sdk_noncestr|32|随机字符串|微信支付时返回||"

[//]: # "|14|扩展字段|sdk_package|128|订单性情扩展字符串|微信支付时返回||"

[//]: # "|15|签名方式|sdk_signtype|32|签名方式，trade_type为JSAPI、LETPAY时才返回|微信支付时返回||"

[//]: # "|16|签名|sdk_paysign|64|签名|微信支付时返回||"

[//]: # "|17||sdk_partnerid|32|trade_type为APP时才返回|非必填||"

[//]: # "|18|富友生成的订单号|reserved_fy_order_no|30|需要商户与商户订单号进行关联|非必填||"

[//]: # "|19|富友清算日|reserved_fy_settle_dt|8||非必填||"

[//]: # "|20|渠道交易流水号|reserved_transaction_id|32|trade_type为FWC时返回|非必填||"

[//]: # "|21|追踪号|reserved_fy_trace_no|12||必填||"

[//]: # "|22|支付参数|reserved_pay_info|不定长||非必填||"

[//]: # "|23|银行交易号|reserved_channel_order_id|32||非必填||"

---


### 2.8 查询可提现资金

#### 请求地址：

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/queryWithdrawAmt`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/queryWithdrawAmt`

*报文格式：* `JSON`

#### 场景说明

?> 查询商户在富友账户中的已结算资金、未结算资金、账户总资金

#### 查询可提现资金请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填||
|2|随机字符串|random_str|32|随机字符串|必填||
|3|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ random_str +"\|"<br>+ mchnt_key<br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填||

#### 查询可提现资金响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|状态码|return_code|16|返回状态码，SUCCESS/FAIL<br>此字段通信标识，非交易标识，交易是否成功需要查看result_code来判断|必填||
|2|中文描述|return_msg|128|返回错误原因|必填||
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||
|4|随机字符串|random_str|32|随机字符串|必填||
|5|业务结果|result_code|16|业务结果, SUCCESS/FAIL|必填||
|6|错误代码|err_code|32|错误代码，0000成功<br>其他详细参见错误列表|非必填||
|7|错误代码描述|err_code_des|128||非必填||
|8|已结算金额|settled_amt|16||必填||
|9|未结算金额|not_settle_amt|16||必填||
|10|账面余额|reserved_book_balance|16||必填||
|11|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ result_code +"\|"<br>+ settled_amt +"\|"<br>+ not_settle_amt +"\|"<br>+ random_str +"\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填||



---


### 2.9 查询提现手续费信息

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/queryFeeAmt`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/queryFeeAmt`

*报文格式：* `JSON`

#### 场景说明

?> 提现金额的手续费查询，根据需提现金额计算相应手续费

#### 查询提现手续费信息请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填||
|2|待提现金额|amt|12||必填||
|3|随机字符串|random_str|32|随机字符串|必填||
|4|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ amt +"\|"<br>+ random_str +"\|"<br>+ mchnt_key<br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填||

#### 查询提现手续费信息响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|状态码|return_code|16|返回状态码，SUCCESS/FAIL<br>此字段通信标识，非交易标识，交易是否成功需要查看result_code来判断|必填||
|2|中文描述|return_msg|128|返回错误原因|必填||
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||
|4|随机字符串|random_str|32|随机字符串|必填||
|5|业务结果|result_code|16|业务结果, SUCCESS/FAIL|必填||
|6|错误代码|err_code|32|错误代码，0000成功<br>其他详细参见错误列表|非必填||
|7|错误代码描述|err_code_des|128||非必填||
|8|手续费金额|fee_amt|16||必填||
|9|手续费金额描述|fee_desc|100||非必填||
|10|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ result_code +"\|"<br>+ fee_amt +"\|"<br>+ random_str +"\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填||

---


### 2.10 发起提现

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/withdraw`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/withdraw`

*报文格式：* `JSON`

#### 场景说明

?> 将商户在富友虚拟户中的资金提现到商户在富友绑定的银行卡中

#### 发起提现请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填||
|2|提现金额|amt|12||必填||
|3|手续费|fee_amt|12||必填||
|4|提现类型|txn_type|1|1：未结金额提现<br>2：已结金额提现|必填||
|5|随机字符串|random_str|32|随机字符串|必填||
|6|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ amt +"\|"<br>+ fee_amt +"\|"<br>+ txn_type +"\|"<br>+ random_str +"\|"<br>+ mchnt_key<br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填||

#### 发起提现响应报文

|序号|参数中文名|参数名|长度|说明|必填|备注|
|:-----:|:-----|:-----|:-----:|:-----|:-----|:-----|
|1|状态码|return_code|16|返回状态码，SUCCESS/FAIL<br>此字段通信标识，非交易标识，交易是否成功需要查看result_code来判断|必填||
|2|中文描述|return_msg|128|返回错误原因|必填||
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||
|4|随机字符串|random_str|32|随机字符串|必填||
|5|业务结果|result_code|16|业务结果, SUCCESS/FAIL|必填||
|6|错误代码|err_code|32|错误代码，0000成功<br>其他详细参见错误列表|非必填||
|7|错误代码描述|err_code_des|128||非必填||
|8|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ result_code +"\|"<br>+ random_str +"\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填||

---


### 2.11 资金划拨查询

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/queryChnlPayAmt`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/queryChnlPayAmt`

*报文格式：* `JSON`

#### 场景说明

?> 查询商户在某段时间内自动结算的笔数及金额信息

#### 资金划拨查询请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填||
|2|查询开始日期|start_date|8||必填||
|3|查询结束日期|end_date|8||必填||
|4|开始笔数|start_index|8||必填||
|5|结束笔数|end_index|8||必填||
|6|随机字符串|random_str|32||必填||
|7|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ start_date +"\|"<br>+ end_date +"\|"<br>+ start_index +"\|"<br>+ end_index +"\|"<br>+ random_str +"\|"<br>+ mchnt_key<br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|||

#### 资金划拨查询响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----|:-----|:-----|
|6|划拨明细<br>(以下带"└"标记的为本字段的子字段)|**`mchntList`**||示例：[划拨明细示例](/aggregatePay/introduction#划拨明细-mchntList-示例字段)|||
|1|商户号|**`└ mchnt_cd`**|15|富友分配的商户号|必填||
|2|金额|**`└ pay_amt`**|20||必填||
|3|划拨状态|**`└ pay_st`**|2||必填||
|4|划拨状态描述|**`└ pay_st_desc`**|64|1：成功<br>2：失败<br>3：划款中<br>4：已划款,状态未知<br>富友已经调用了划款接口，但是通道没有返回应答或返回了超时<br>无法判断是否成功，只能次日通过对账明确结果<br>5：已转入小宝金库|必填||
|5|清算日期|**`└ settle_dt`**|8||必填||
|7|笔数|count|10||必填||
|8|随机字符串|random_str|32||||
|9|签名|sign|128|拼接<br><br>mchnt_cd+"\|"<br>+ count +"\|"<br>+ random_str +"\|"<br>+ mchnt_key<br><br>做md5摘要<br>其中mchnt_key 为商户密钥，系统分配|必填||

---
<!--

### 2.12 扫码 wap/app 下单

#### 请求地址

*测试地址：* `http://jfzwtest.fuiou.com/aggregatePay/wapPreCreate`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/wapPreCreate`

#### 场景说明

![扫码 wap/app 下单](_images/wapPreCreate-process.jpg '扫码 wap/app 下单')

- `步骤 1：`用户在移动端通过非微信浏览器访问商户 H5 页面或 APP
- `步骤 2：`用户选择商品、输入支付金额等进行下单支付
- `步骤 3：`商户将订单信息发送给富友，返回支付信息。
- `步骤 4：`商户拿到支付信息后调起微信或者支付宝进行支付
- `步骤 5：`支付结果以回调(2.5)的方式通知到商户

#### 扫码 wap/app 下单请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|版本号|version|8|1.0|必填|
|2|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填|
|3|随机字符串|random_str|32|随机字符串|必填|
|4|订单类型|order_type|20|订单类型：<br>ALIPAY<br>WECHAT<br>WXBX(微信保险类)<br>ALBX(支付宝保险类)|必填|
|5|订单总金额|order_amt|16|以`分`为单位|必填|
|6|商户订单号|mchnt_order_no|30|商户系统内部的订单号|
|7|交易起始时间|txn_begin_ts|14|订单生成时间<br>格式：<br>yyyyMMddHHmmss|必填|
|8|商品描述|goods_des|128|商品或支付单简要描述|必填|
|9|商品详情|goods_detail|600|商品名称明细|非必填|
|10|商品标记|goods_tag|32|商品标记|非必填|
|11|终端号|term_id|8|随机8字节数字字母组合|必填|
|12|终端IP|term_ip|16|终端IP(H5必须传真实IP)(与微信官方获取的IP一致)<br>参考地址：https://pay.weixin.qq.com/wiki/doc/api/H5.php?chapter=15_5<br>如果IP有变化，会出现：网络环境未能通过安全验证|必填|
|13|附加数据|addn_inf|100|附加数据|非必填|
|14|货币类型|curr_type|3|默认人民币：CNY|非必填|
|15|设备信息|reserved_device_info|512|`H5必填`字段用于上报支付的场景信息，针对 H5 支付有以下三种场景，请根据对应场景上报<br>示例参照：[示例字段](/aggregatePay/introduction#设备信息-reserved_device_info-示例字段)||
|16|通知URL|notify_url|256|接收富友异步通知回调地址，通知 url 必须为直接可访问的 url，不能携带参数主扫时必填|必填|
|17|前台通知地址|front_notify_url|200|前台通知URL|非必填|
|18|交易类型|trade_type|16|APP- -app支付|必填|
|19|子商户公众号id|reserved_sub_appid|32|子商户配置多个公众号时必填|非必填|
|20|限制支付|reserved_limit_pay|32|no_credit：不能使用信用卡<br>credit_group：不能使用花呗和信用卡<br>`不参与签名`|非必填|
|21|富友终端号|reserved_fy_term_id|20||非必填|
|22|交易关闭时间|reserved_expire_minute|8|分钟|非必填|
|23|用户身份证号|reserved_user_creid|64|用户身份证号码(实名保险类交易必填)|非必填|
|24|用户姓名|reserved_user_truename|20||非必填|
|25|用户手机号|reserved_user_mobile|20||非必填|
|26|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ order_type +"\|"<br>+ trade_type +"\|"<br>+ order_amt +"\|"<br>+ mchnt_order_no+"\|"<br>+ txn_begin_ts+"\|"<br>+ goods_des +"\|"<br>+ term_id +"\|"<br>+ term_ip +"\|"<br>+ notify_url +"\|"<br>+ random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中 mchnt_key 为商户密钥，系统分配|必填|

#### 扫码 wap/app 下单响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填||
|2|中文描述|result_msg|128|代码描述|必填||
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||
|4|终端号|term_id|8|终端号|非必填||
|5|随机字符串|random_str|32|随机字符串|必填||
|6|订单类型|order_type|20|订单类型：ALIPAY，WECHAT|必填||
|7|会话标识|session_id|64|预支付交易会话标识<br>富友返回支付宝生成的预支付回话标识<br>用于后续接口调用中使用，该值有效期为2小时|非必填||
|8|支付类型|trade_type|16|APP--app支付<br>H5--H5支付|必填||
|9|二维码链接|qr_code|64|二维码链接|非必填||
|10|商户订单号|dest_trade_no|32|目标通道商户订单号|非必填||
|11|支付参数|pay_info|不定长||非必填||
|12|富友生成的订单号|reserved_fy_order_no|30|富友生成的订单号，需要商户与商户订单号进行关联|必填||
|13|银行交易号|reserved_channel_order_id|32|银行返回的交易号|非必填||
|14|签名|sign||拼接<br><br>result_code+"\|"<br>+ result_msg+"\|"<br>+ mchnt_cd+"\|"<br>+ trade_type +"\|"<br>+ order_type +"\|"<br>+ reserved_fy_order_no+"\|"<br>+ random_str +"\|"<br>+ mchnt_key<br><br>并做md5摘要<br>其中mchnt_key为商户密钥，系统分配|||

---


-->

### 2.12 历史订单查询接口

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/hisTradeQuery`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/hisTradeQuery`

*报文格式：* `JSON`

#### 历史订单查询请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|版本号|version|8|1.0|必填|
|2|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填|
|3|随机字符串|random_str|32|随机字符串|必填|
|4|订单类型|order_type|20|订单类型：<br>ALIPAY(统一下单、条码支付、服务窗支付)<br>WECHAT(统一下单、条码支付，公众号支付)<br>JD(京东钱包)<br>QQ(QQ钱包)<br>UNIONPAY(银联二维码)<br>WXAPP(微信app)<br>ALIAPP(支付宝app)<br>WXH5(微信h5)<br>ALIH5(支付宝h5)|必填|
|5|终端号|term_id|8|随机8字节数字字母组合|必填|
|6|交易日期|trade_dt|8|交易日期，非必填，默认当天。查询往日交易时，必填，支持查询90天内交易<br>格式：YYYYMMDD|非必填|
|7|商户订单号|mchnt_order_no|30|mchnt_order_no、channel_order_id、transaction_id 三选填一|非必填|
|8|条码号|channel_order_id|32|条码流水<br>mchnt_order_no、channel_order_id、transaction_id 三选填一|非必填|
|9|渠道订单号|transaction_id|32|mchnt_order_no、channel_order_id、transaction_id 三选填一|非必填|
|10|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ order_type +"\|"<br>+ term_id +"\|"<br>+ random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中 mchnt_key 为商户密钥，系统分配||

#### 历史订单查询响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填||
|2|中文描述|result_msg|128|代码描述|必填||
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||
|4|终端号|term_id|8|终端号|非必填||
|5|随机字符串|random_str|32|随机字符串|必填||
|6|订单类型|order_type|20|订单类型：ALIPAY，WECHAT，JD(京东钱包)，QQ(QQ 钱包)，UNIONPAY(银联二维码)等|必填||
|7|订单金额|order_amt|16||必填|
|8|买家渠道号|buyer_id|32||非必填|
|9|渠道流水号|transaction_id|32||必填|
|10|附加数据|addn_inf|100||非必填|
|11|富友清算日|reserved_fy_settle_dt|8||必填|
|12|商户订单号|mchnt_order_no|30||必填|
|13|交易状态|trans_stat|32|SUCCESS—支付成功<br>REFUND—转入退款<br>NOTPAY—未支付<br>CLOSED—已关闭<br>REVOKED—已撤销(刷卡支付)<br>USERPAYING--用户支付中<br>PAYERROR--支付失败(其他原因，如银行返回失败)|必填|
|14|优惠金额|reserved_coupon_fee|10||非必填|
|15|买家登录账号|reserved_buyer_logon_id|128|买家在渠道登录账号|非必填||
|16|渠道信息|reserved_fund_bill_list|不定长|支付宝交易资金渠道，详细渠道|非必填||
|17|富友追踪号|reserved_fy_trace_no|12|富友系统内部追踪号|必填||
|18|银行交易号|reserved_channel_order_id|32|银行返回的交易号|非必填||
|19|富友终端号|reserved_fy_term_id|20|富友的POS终端号，如果不是用的富友的POS终端请忽略|非必填||
|20|签名|sign||拼接<br><br>result_code+"\|"<br>+ result_msg+"\|"<br>+ mchnt_cd +"\|"<br>+ order_amt +"\|"<br>+ transaction_id+"\|"<br>+ mchnt_order_no+"\|"<br>+ reserved_fy_settle_dt+"\|"<br>+ trans_stat+"\|"<br>+ random_str+"\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填|

---


### 2.13 退款查询接口
#### 场景说明

?> 本接口支持所有退款交易查询(时间限制：`3日内`)

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/refundQuery`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/refundQuery`

*报文格式：* `JSON`

#### 退款查询请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|版本号|version|8|1.0|必填|
|2|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填|
|3|随机字符串|random_str|32|随机字符串|必填|
|4|终端号|term_id|8|随机8字节数字字母组合|必填|
|5|退款订单号|refund_order_no|30||必填|
|6|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ refund_order_no +"\|"<br>+ term_id +"\|"<br>+ random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key <br><br>做 md5 摘要<br>其中mchnt_key为商户密钥，系统分配|必填|

#### 退款查询响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填||
|2|中文描述|result_msg|128|代码描述|必填||
|3|交易状态|trans_stat|32|交易状态<br>SUCCESS—退款成功<br>PAYERROR--退款失败<br>USERPAYING-退款已受理(超时或状态未知，等T+1富友处理)|必填|
|4|机构号|ins_cd|20|机构号,接入机构在富友的唯一代码|必填|
|5|商户号|mchnt_cd|15|富友分配的商户号|必填|
|6|终端号|term_id|8||必填|
|7|随机字符串|random_str|32|随机字符串|必填|
|8|商户订单号|mchnt_order_no|30||必填|
|9|订单类型|order_type|20|订单类型：<br>ALIPAY、WECHAT、UNIONPAY(银联二维码)|必填|
|10|退款订单号|refund_order_no|30||必填|
|11|渠道流水号|transaction_id|32||选填|
|12|渠道退款流水号|refund_id|32||选填|
|13|富友清算日|reserved_fy_settle_dt|8||必填|
|14|富友追踪号|reserved_fy_trace_no|12|富友系统内部追踪号|选填|
|15|退款金额|reserved_refund_amt|16||选填|
|16|签名|sign||拼接<br><br>result_code+ "\|" <br>+ result_msg + "\|"<br>+ trans_stat+"\|" <br>+ ins_cd+"\|" <br>+ mchnt_cd+ "\|"<br>+ term_id+"\|" <br>+ random_str + "\|" <br>+ order_type + "\|" <br>+ mchnt_order_no+"\|"<br>+ refund_order_no+"\|"<br>+ transaction_id+"\|"<br>+ refund_id+"\|"<br>+ reserved_fy_settle_dt+ "\|" <br>+ reserved_fy_trace_no + "\|"<br>+reserved_refund_amt+"\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中mchnt_key 为商户密钥，系统分配|必填|

---


### 2.14 关闭订单接口

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/closeOrder`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/closeOrder`

*报文格式：* `JSON`

#### 场景说明

!> 接口支持微信、支付宝各种下单类交易(order_type)的关闭。只有未支付的订单才能发起关闭。

[//]: # "!> 注意：订单生成后不能马上调用关单接口，最短调用时间间隔为 5 分钟"

?> **支付宝主扫调用关单接口**<br><br>如果未扫码直接关单的话会关单不成功的，一定要扫码后未支付才能调用关单接口。支付宝动态码的交易，下单成功（返回动态码链接只是对应支付宝的预下单，并不是下单），因此在查询、关单的时候支付宝会返回“交易不存在”， 用户扫码以后才会去下单。<br><br>即，调用了统一下单接口后，产生的qr_code二维码链接，必须扫一下那个链接，支付宝那边才算生成了订单，不付款，才能调用关单接口。

?> **微信主扫调用关单接口**<br><br>微信主扫，调用了统一下单接口，就可以直接关单，不需要去扫qr_code产生的二维码链接


#### 关闭订单请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|版本号|version|8|1.0|必填|
|2|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填|
|3|随机字符串|random_str|32|随机字符串|必填|
|4|终端号|term_id|8|随机 8 字节数字字母组合|必填|
|5|商户订单号|mchnt_order_no|30|商户系统内部的订单号|必填|
|6|订单类型|order_type|20|订单类型：<br>ALIPAY(统一下单、服务窗支付)<br>WECHAT(统一下单、公众号支付、小程序)<br>WXAPP(微信app)<br>WXH5(微信h5)|必填|
|7|子商户公众号|sub_appid|32|子商户公众号id，子商户配置多个公众号时必填|非必填|
|8|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ mchnt_order_no +"\|"<br>+ order_type +"\|"<br>+ term_id +"\|"<br>+ random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填|

#### 关闭订单响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填|
|2|中文描述|result_msg|128|代码描述|必填|
|3|机构号|ins_cd|20|机构号,接入机构在富友的唯一代码|必填|
|4|商户号|mchnt_cd|15|富友分配的商户号|必填|
|5|终端号|term_id|8||必填|
|6|随机字符串|random_str|32|随机字符串|必填|
|7|订单类型|order_type|20||非必填|
|8|商户订单号|mchnt_order_no|30||必填|
|9|签名|sign||拼接<br><br>result_code+"\|"<br>+ result_msg+"\|"<br>+ mchnt_cd+"\|"<br>+ mchnt_order_no+"\|"<br>+ random_str +"\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填|

---


### 2.15 撤销订单接口

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/cancelOrder`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/cancelOrder`

*报文格式：* `JSON`

#### 场景说明

?> 本接口支持微信、支付宝条码支付撤销。建议先发起查询确认订单状态未支付（或者支付中）后发起撤销。如果**原交易成功，建议使用退款接口**

?> 建议：原交易请求完至少 15S 后发起撤销。如果原交易是未支付，目前仅支持撤销当日交易

#### 撤销订单请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|版本号|version|8|1.0|必填|
|2|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填|
|3|随机字符串|random_str|32|随机字符串|必填|
|4|终端号|term_id|8|随机8字节数字字母组合|必填|
|5|商户订单号|mchnt_order_no|30||必填|
|6|商户撤销单号|cancel_order_no|30||必填|
|7|操作员|operator_id|32||非必填|
|8|富友终端号|reserved_fy_term_id|20||非必填|
|9|订单类型|order_type|20|ALIPAY(条码支付)， WECHAT(条码支付)|必填|
|10|签名|sign||mchnt_cd+"\|"<br>+ mchnt_order_no +"\|"<br>+ order_type+"\|"<br>+ cancel_order_no+"\|"<br>+ term_id +"\|"<br>+ random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key <br><br>做 md5 摘要<br>其中 mchnt_key 为商户密钥，系统分配|必填|

#### 撤销订单响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填|
|2|中文描述|result_msg|128|代码描述|必填|
|3|机构号|ins_cd|20|机构号,接入机构在富友的唯一代码|必填|
|4|商户号|mchnt_cd|15|富友分配的商户号|必填|
|5|终端号|term_id|8||必填|
|6|随机字符串|random_str|32|随机字符串|必填|
|7|订单类型|order_type|20||非必填|
|8|商户订单号|mchnt_order_no|30||必填|
|9|商户撤销单号|cancel_order_no|30||必填|
|10|渠道交易流水号|transaction_id|32||非必填|
|11|渠道撤销流水号|cancel_id|32||非必填|
|12|是否有资金流向|fund_change|8|是否有资金流向(只有支付宝有该字段)|非必填|
|13|是否需要新调用撤销|recall|8|是否需要新调用撤销(当为Y时，需要重新调用撤销接口)|非必填|
|14|富友清算日|reserved_fy_settle_dt|8||非必填|
|15|富友追踪号|reserved_fy_trace_no|12||非必填|
|16|签名|sign||拼接<br><br>result_code+"\|"<br>+ result_msg+"\|"<br>+ mchnt_cd+"\|"<br>+ mchnt_order_no+"\|"<br>+ cancel_order_no+"\|"<br>+ random_str +"\|"<br>+ mchnt_key <br><br>做 md5 摘要<br>其中mchnt_key 为商户密钥，系统分配|必填|

---


### 2.16 服务商模式获取 openid 接口

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/auth2Openid`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/auth2Openid`

*报文格式：* `GET.QUERY 请求参数拼接在URL后`

#### 场景说明

?> 合作方下的商户使用富友渠道号的 appid 发起公众号交易，需要获取该 appid 下的 openid。<br>合作方组装页面重定向 url 请求富友，富友验证参数并封装 URL 请求微信，获取 openid 之后页面重定向到合作方指定的收银台 URL

#### 服务商模式获取 openid请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|商户号|mchnt_cd|15|富友分配的商户号|必填|
|2|商户重定向地址|redirect_uri|32|收银台地址<br>例如：<br>https://m.baidu.com/from=844b/s?b=2&a=1&word=%D6%D0%CE%C4<br><br>接受 appid/openid 的收银台地址，若无特殊场景需求，不要出现特殊字符、中文<br>如果出现中文，需要对 url 后面的参数做 encode 处理<br>收银台地址至少需要一个参数，否则会重定向失败|必填|
|3|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ redirect_uri +"\|"<br>+ mchnt_key <br><br>做 md5 摘要<br>中 mchnt_key 为商户密钥，系统分配||

#### 重定向响应

?> redirect_url + appid + openid + state + access_token

*示例*
```text
https://m.baidu.com/from=844b/s?b=2&a=1&word=%D6%D0%CE%C4&state=wxfa089da95020ba1a&openid=ooIeqs4nrFiC634N2Spt3caubrUM&appid=wxfa089da95020ba1a&access_token=ACCESS_TOKEN
```

!> 说明：为了兼容众多合作方的代码，appid 和 state 均填写的是 appid 的值，openid 为 appid 对应的，公众号交易下单时，使用重定向返回的 appid+openid 的值填入到公众号下单接口中的 sub_appid 和 sub_openid。商户通过重定向地址解析参数来获取 openid.

---


### 2.17 银联授权码获取 user_id 接口

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/getOpenidByAuthCode`

*生产地址：* `https://aipay-cloud.fuioupay.com/aggregatePay/getOpenidByAuthCode`

*报文格式：* `JSON`

#### 银联授权码获取 user_id 请求报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|版本号|version|8|1.0|必填|
|2|商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填|
|3|随机字符串|random_str|32|随机字符串|必填|
|4|订单类型|order_type|20|订单类型：UNIONPAY|必填|
|5|终端IP|term_ip|16||必填|
|6|请求代理头|sub_appid|32|填空或填固定值：UnionPay/1.0CloudPay<br>填错获取不到 user_id<br>目前仅支持云闪付，待后期其他APP 支持后，填写其他银行 APP 的User Agent|非必填|
|7|商户订单号|auth_code|128|扫码支付授权码|必填|
|8|订单金额|order_amt|16|暂时无用，后期拓展字段|非必填|
|9|签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ order_type +"\|"<br>+ order_amt +"\|"<br>+ auth_code +"\|"<br>+ sub_appid +"\|"<br>+ term_ip +"\|"<br>+ random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key <br><br>做 md5 摘要<br>其中 mchnt_key 为商户密钥，系统分配|必填|


#### 银联授权码获取 user_id 响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填||
|2|中文描述|result_msg|128|代码描述|必填||
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填||
|4|终端号|appid|32|微信分配的公众账号ID|非必填||
|5|买家渠道号|session_id|32||非必填|
|6|渠道流水号|openid|128|用户在商户 appid 下的唯一标识|非必填|
|7|附加数据|sub_openid|128|用户在商户 appid 下的唯一标识|非必填|
|8|随机字符串|random_str|32|随机字符串|必填||
|9|签名|sign||拼接<br><br>result_code+"\|"<br>+ result_msg+"\|"<br>+ mchnt_cd +"\|"<br>+ appid +"\|"<br>+ sub_appid+"\|"<br>+ session_id+"\|"<br>+ openid+"\|"<br>+ sub_openid+"\|"<br>+ random_str+"\|"<br>+ mchnt_key <br><br>做md5摘要<br>其中mchnt_key为商户密钥，系统分配|必填|

---


### 2.18 安卓调用富友小程序样例代码

#### 注意事项

1.app传过来的参数需要 encodeUrl 转下，防止微信报错

2.传入的金额不能带小数点(我这边接收的是以分为单位的)

3.url参数不能太长防止微信报错

#### 代码

```java
/**
* android 客户端调用富友小程序样例代码
*
* @param amt 订单总金额 以分为单位 必填
* @param mchntCd 商户号 0005840F2026315
* @param mchntOrderNo 商户订单号（最大 30 位） 格式：1077+随机
* @param notifyUrl 通知 URL (随便填)，样例：www.baidu.com
* @param req 调用微信 sdk 的 req 实例
*/

private void handleReqPath(String amt, String mchntCd, String mchntOrderNo, String notifyUrl,String tradeType, WXLaunchMiniProgram.Req req) {
String notifyUrlEncode = "";
//notifyUrl 要 encode传输
try {
    notify UrlEncode = URLEncoder.encode(notifyUrl, "UTF-8");
    Log.i("notifyUrl", notifyUrl);
} catch (UnsupportedEncodingException e) {
    e.printStackTrace();
}

//参数处理
String mchnt_cd = mchntCd;//商户号 0005840F2026315
String order_amt = amt;//订单总金额 以分为单位 必填
String mchnt_order_no = mchntOrderNo;//商户订单号 1077+随机
String sign = "";//签名 必填
String notify_url_encode = notifyUrlEncode;//通知 URL (随便填)-notifyUrl 要 encode 传输
String mchnt_key = "cc5182d0769811e73647e27170fac2c5";//签名使用
String reserved_trade_type=tradeType;

//签名时 notifyUrl 字段使用明文
//md5 字符串
String md5Str = mchnt_cd + "|"
+ order_amt + "|"
+ mchnt_order_no + "|"
+ notifyUrl + "|"
+ mchnt_key;
//传递给小程序时 notifyUrl 要 encode 传输
sign = MD5Utils.MD5Encode(md5Str, "utf-8");

StringBuilder params = new StringBuilder();
params
.append("mchnt_cd=" + mchnt_cd)
.append("&order_amt=" + order_amt)
.append("&mchnt_order_no=" + mchnt_order_no)
.append("&notify_url=" +notify_url_encode)
.append("&reserved_trade_type=" + reserved_trade_type)
.append("&sign=" + sign);

req.path = "/pages/pay/index?" + params;

}

```

---


### 2.19 服务商模式获取 openid 接口样例代码

#### 示例代码(java)

*示例代码(java)*

```java
//商家参数
Map<String, String> mchntParamMap = new HashMap<String, String>();
mchntParamMap.put("a", "1");
mchntParamMap.put("b", "2");
mchntParamMap.put("word", "中文");

//参数需要进行 encode
String mchnt_redirect_url = getRequestUrl("https://m.baidu.com/from=844b/s", mchntParamMap);

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
            if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                if (hasParam) {
                    redirectOpenidThirdUrl.append("&");
                } else {
                    hasParam = true;
                }
                redirectOpenidThirdUrl.append(name).append("=").append(URLEncoder.encode(value, "GBK"));
            }
        }
        return redirectOpenidThirdUrl.toString();
    } catch (Exception e) {
        e.printStackTrace();
    }
    return redirectOpenidThirdUrl.toString();
}
```

#### 示例代码(form表单)

```xml
<html>
<head>

<meta http-equiv="Content-Type" content="text/html; charset=utf8" pageencoding="utf8" />
<title>loading</title>
<style type="text/css">
body{
    margin:200px auto;
    font-family:"宋体",Arial;
    font-size:12px;
    color:#369;
    text-align:center;
}
#1{
    height:auto;
    width:78px;
    margin:0 auto;
}
#2{
    height:auto;
    width:153px;
    margin:0 auto;
}
vertical-align:bottom;
}
</style>

</head>

<body onload="OnLoadEvent();">
<div id="3">
    Loading...
</div>
<form name="forwardForm" action="https://aipaytest.fuioupay.com/aggregatePay/auth2Openid" method="POST">
    <input type="hidden" name="sign" value="此处填写签名" />
    <input type="hidden" name="redirect_uri" value="此处填写重定向地址" />
    <input type="hidden" name="mchnt_cd" value="此处填写商户号" />
</form>

<script language="Javascript">
function OnLoadEvent(){
    document.forwardForm.submit();
}
</script>

</body>
</html>
```

---


### 2.20 银联授权码获取 user_id 合作方注意事项

#### 注意事项

**`前置条件：域名加白 台卡域名需要通过富友到银联总行进行加白`**

1. 判断 UA 来源，如果是 safari 浏览器，则不需要获取 userid，可以直接下单(trade_type=APPLEPAY sub_appid,sub_openid,openid 填空)

2. 如果是来自云闪付 APP(UnionPay/1.0 目前只支持云闪付。需要合作方重定向https://qr.95516.com/qrcGtwWeb-web/api/userAuth?version=1.0.0&redirectUrl=合作方银联二维码授权码接收地址 (该地址需要进行 urlencode)

3. 通过2中的 redirectUrl 重定向获取到【授权码】

4. 通过上述的【授权码】调用【2.18 银联授权码获取 user_id 接口】获取 user_id

5. 使用上述的【user_id】调用2.6公众号下单接口进行下单
(trade_type=UNIONPAY sub_appid,openid 填空。sub_openid = user_id)
Trade_type 使用说明：由于 APPLE 和云闪付付款方式不一样，后期费率、补贴会不一样。

合作方获取授权码流程如下所示：

![银联授权码获取 user_id流程](_images/getOpenidByAuthCode.jpg '银联授权码获取 user_id流程')







<!-- 2.21 wap或app下单(APP/H5/PC支付方式)

#### 请求地址

*测试地址：* `https://aipaytest.fuioupay.com/aggregatePay/wapPreCreate`

*生产地址1：* `https://aipay-fzg.fuioupay.com/aggregatePay/wapPreCreate`

*生产地址2：* `https://aipay-xs-fzg.fuioupay.com/aggregatePay/wapPreCreate`

*报文格式：* `JSON`

#### 场景说明

![公众号/服务窗统一下单流程图](_images/wxPreCreate-process.jpg 'wap/app统一下单流程图')

?> 该接口常应用于APP/H5/PC端支付，用户在商户商品页进行商品选购，页面调此接口生成订单，接受订单参数后调起官方支付接口支付。详见公众号/服务窗对接流程

- `步骤 1：`用户进入商户 APP/H5/PC端 页面
- `步骤 2：`用户选择商品、输入支付金额等进行下单支付
- `步骤 3：`商户将订单信息发送给富友，返回支付信息(用于调起支付宝、微信的参数)。
- `步骤 4：`商户拿到支付信息后调起微信或者支付宝进行支付
- `步骤 5：`支付结果以回调(2.5)的方式通知到商户

#### wap/app统一下单请求报文

| 序号 |参数中文名|参数名|长度|说明|必填|
|:--:|:-----|:-----|:-----:|:-----|:-----|
| 1  |版本号|version|8|1.0|必填|
| 2  |商户号|mchnt_cd|15|富友分配的商户号<br>例：0002900F0313432|必填|
| 3  |随机字符串|random_str|32||必填|
| 4  |订单总金额|order_amt|16|以分为单位|必填|
| 5  |商户订单号|mchnt_order_no|30||必填|
| 6  |商品标识|product_id|32||非必填|
| 7  |交易起始时间|txn_begin_ts|14|格式：yyyyMMddHHmmss|必填|
| 8  |商品描述|goods_des|128||必填|
| 9  |商品详情|goods_detail|600|商品名称明细|非必填|
| 10 |商品标记|goods_tag|32||非必填|
| 11 |终端号|term_id|8|随机 8 字节数字字母组合|必填|
| 12 |终端IP|term_ip|16|终端IP|必填|
| 13 |附加数据|addn_inf|100||非必填|
| 14 |货币种类|curr_type|3|默认人民币：CNY|非必填|
| 15 |通知URL|notify_url|256|接收富友异步通知回调地址，通知url必须为直接可访问的url，不能携带参数主扫时必填|必填|
| 16 |订单类型|order_type|20|订单类型：ALIPAY(支付宝支付)|必填|
| 17 |交易类型|trade_type|16|JSAPI--公众号线下支付<br>GZXS--公众号线上<br>APP--app支付<br>H5支付<br>PC支付<br>FWC--支付宝服务窗<br>LETPAY-小程序<br>LPXS--小程序线上<br>WXBXJS(微信公众号保险类)<br>WXBXLET(微信小程序保险类)<br>WXBXAPP(微信APP保险类)<br>ALBXJS(支付宝服务窗保险类)<br>APPLEPAY(相机扫码)<br>UNIONPAY(云闪付扫码)|必填|
| 18 |子商户用户标识|sub_openid|128|子商户用户标识<br>支付宝服务窗为用户buyer_id（此场景必填）<br>微信公众号为用户的openid（小程序，公众号，服务窗必填）<br>APPLEPAY(相机扫码)该值不用填<br>UNIONPAY(云闪付扫码)该值必填|非必填|
| 19 |子商户公众号id|reserved_sub_appid|32|子商户公众号id<br>sub_appid 填商户或者是服务商的 appid<br>微信交易为商户的appid（小程序，公众号必填）|非必填|
| 20 |限制支付|reserved_limit_pay|32|no_credit：不能使用信用卡<br>credit_group：不能使用花呗和信用卡<br>`不参与签名`|非必填|
| 21 |富友终端号|reserved_fy_term_id|20|富友终端号`(如果不是用的富友的POS 终端，此字段千万不要填，不然会影响清算)`|非必填|
| 22 |交易关闭时间|reserved_expire_minute|8|交易关闭时间,默认填0（2小时）|非必填|
| 23 |用户身份证号|reserved_user_creid|64|用户身份证号码(实名保险类交易必填)|非必填|
| 24 |用户姓名|reserved_user_truename|20|用户姓名(实名保险类交易必填)|非必填|
| 25 |用户手机号|reserved_user_mobile|20|用户手机号(实名保险类交易必填)|非必填|
| 26 |保险商户敏感信息|reserved_identityinfo|65|加密传递时必填，传递用户敏感信息时使用|非必填|
| 27 |保险商户敏感信息密钥|reserved_identityinfo_key|65|加密传递时必填，传递用户敏感信息时使用|非必填|
| 28 |门店id|reserved_store_code|65|门店id（目前仅支持支付宝）|非必填|
| 29 |业务信息|reserved_business_params|65|商户传入业务信息，应用于安全，营销等参数直传场景，json字符串|非必填|
| 30 |保险公司订单号|reserved_bxno_inf|65|保险公司订单号，认证机构单号<br>(使用英文逗号分隔)<br>|非必填|
| 31 |富友终端类型|reserved_fy_term_type|1|富友终端类型<br>0:其他<br>1:富友终端<br>2:POS机<br>3:台卡<br>4:PC软件|非必填|
| 32 |终端序列号|reserved_fy_term_sn|20||非必填|
| 33 |设备信息|reserved_device_info|32|设备信息，托传给微信。用于单品券核销<br>示例参照：[示例字段](/aggregatePay/introduction#设备信息-reserved_device_info-示例字段)|非必填|
| 34 |花呗分期信息|reserved_ali_extend_params||JSON串<br>示例参照：[示例字段](/aggregatePay/introduction#花呗分期信息-reserved_ali_extend_params-示例字段)||
| 35 ||└ hb_fq_num|3|花呗分期期数：仅支持3、6、12|非必填|
| 36 ||└ hb_fq_seller_percent|3|花呗分期商家手续费比例，目前仅支持用户出资，如需使用，请填写0！<br>目前该字段未生效，我司会默认当0处理|非必填|
| 37 ||└ industry_reflux_info|512|行业数据回流信息<br>示例参照：[示例字段](/aggregatePay/introduction#行业数据回流信息-industry_reflux_info-示例字段)|非必填|
| 38 ||└ food_order_type|32|点餐场景类型：<br>qr_order(店内扫码点餐)<br>pre_order(预点到店自提)<br>home_delivery(外送到家)<br>direct_payment(直接付款)<br>other(其他)|非必填|
| 39 |签名|sign||拼接<br><br>mchnt_cd+"\|"<br>+ trade_type +"\|"<br>+ order_amt +"\|"<br>+ mchnt_order_no+"\|"<br>+ txn_begin_ts+"\|"<br>+ goods_des +"\|"<br>+ term_id +"\|"<br>+ term_ip +"\|"<br>+ notify_url +"\|"<br>+ random_str +"\|"<br>+ version + "\|"<br>+ mchnt_key <br><br>并做 md5 摘要<br>其中 mchnt_key 为商户密钥，系统分配|必填|
| 40 |终端信息说明字段|reserved_terminal_info|json|见文档中[reserved_terminal_info终端信息说明字段（259号文，终端信息）](/aggregatePay/transform#reserved_terminal_info终端信息说明字段（259号文，终端信息）)|非必填|


#### 扫码 wap/app 下单响应报文

|序号|参数中文名|参数名|长度|说明|必填|
|:-----:|:-----|:-----|:-----:|:-----|:-----|
|1|响应代码|result_code|16|000000 成功,其他详细参见错误列表|必填|
|2|中文描述|result_msg|128|代码描述|必填|
|3|商户代码|mchnt_cd|15|富友分配的商户号|必填|
|4|终端号|term_id|8|终端号|非必填|
|5|随机字符串|random_str|32|随机字符串|必填|
|6|订单类型|order_type|20|订单类型：ALIPAY，WECHAT|必填|
|7|会话标识|session_id|64|预支付交易会话标识<br>富友返回支付宝生成的预支付回话标识<br>用于后续接口调用中使用，该值有效期为2小时|非必填|
|8|支付类型|trade_type|16|APP--app支付<br>H5--H5支付<br>PC-PC支付|必填|
|9|二维码链接|qr_code|64|二维码链接|非必填|
|10|商户订单号|dest_trade_no|32|目标通道商户订单号|非必填|
|11|支付参数|pay_info|不定长||非必填|
|12|富友生成的订单号|reserved_fy_order_no|30|富友生成的订单号，需要商户与商户订单号进行关联|必填|
|13|银行交易号|reserved_channel_order_id|32|银行返回的交易号|非必填|
|14|签名|sign||拼接<br><br>result_code+"\|"<br>+ result_msg+"\|"<br>+ mchnt_cd+"\|"<br>+ trade_type +"\|"<br>+ order_type +"\|"<br>+ reserved_fy_order_no+"\|"<br>+ random_str +"\|"<br>+ mchnt_key<br><br>并做md5摘要<br>其中mchnt_key为商户密钥，系统分配||

-->


---
