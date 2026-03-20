澳门国际银行电子保函综合服务平台

非融资性分离式投标保函接入手册

版本V1\.0

文档修订订历史

__文档修订版本号__

__修订内容__

20231204

创建接口初始版本

20231205

保函申请订单推送增加字段，增加发票开立通知、发票信息推送、密文保函解密还原接口

20241025

申请接口修改，增加退保申请和退保通知等

目  录<a id="_Toc20893_WPSOffice_Type3"></a>

<a id="_Toc7094_WPSOffice_Level1"></a><a id="_Toc3473_WPSOffice_Level1"></a><a id="_Toc13130_WPSOffice_Level1"></a>[一、	概述	3](#_Toc180762540)

[1\.1 目的	3](#_Toc180762541)

[1\.2 使用对象	3](#_Toc180762542)

[二、	API接口	3](#_Toc180762543)

[1\. 接口数据规范说明	3](#_Toc180762544)

[1\.1 交互方式	3](#_Toc180762545)

[1\.2 公共请求参数	3](#_Toc180762546)

[1\.3 安全规范	4](#_Toc180762547)

[1\.4 报文结构	4](#_Toc180762548)

[1\.5数据加密及报文加签	5](#_Toc180762549)

[2\. 公共资源交易中心电子投标保函	6](#_Toc180762550)

[2\.1保函申请订单推送	6](#_Toc180762551)

[2\.2保函申请订单状态更新	11](#_Toc180762552)

[2\.3推送保函申请订单处理结果	12](#_Toc180762553)

[2\.4推送电子保函文件	14](#_Toc180762554)

[2\.5密文保函解密还原（项目信息方式）通知	15](#_Toc180762555)

[2\.6保函退保申请	17](#_Toc180762556)

[2\.7保函退保通知	18](#_Toc180762557)

[3\. 附录	19](#_Toc180762558)

[3\.1 接口说明	19](#_Toc180762559)

[3\.2 国密SM生成密钥对	20](#_Toc180762560)

# <a id="_Toc18247_WPSOffice_Level1"></a><a id="_Toc119940337"></a><a id="_Toc180762540"></a><a id="_Toc15120_WPSOffice_Level2"></a>概述

本对接手册是提供电子保函渠道端与澳门国际银行对接使用。

## <a id="_Toc119940338"></a><a id="_Toc7700_WPSOffice_Level3"></a><a id="_Toc180762541"></a>1\.1 目的

本手册的主要目的是帮助和指导技术人员开发使用。

## <a id="_Toc119940339"></a><a id="_Toc3527_WPSOffice_Level3"></a><a id="_Toc180762542"></a>1\.2 使用对象

本手册的使用对象是对接电子保函平台的开发人员、维护人员和管理人员。 

# <a id="_Toc8470_WPSOffice_Level1"></a><a id="_Toc28464_WPSOffice_Level1"></a><a id="_Toc11580_WPSOffice_Level1"></a><a id="_Toc23355_WPSOffice_Level1"></a><a id="_Toc119940340"></a><a id="_Toc180762543"></a>API接口

## <a id="_Toc119940341"></a><a id="_Toc180762544"></a>接口数据规范说明

### <a id="_Toc68628080"></a><a id="_Toc119940342"></a><a id="_Toc180762545"></a>1\.1 交互方式 

消息请求基于HTTP的POST方式，业务数据组织成JSON格式的字符串来交互。

Content\-Type设置”application/json;charset=UTF\-8”

开发测试URL: [https://xib03\.test\.xib\.com\.cn:51443/ifspeesi/gm/api/xxx/xxx](https://xib03.test.xib.com.cn:51443/ifspeesi/gm/api/xxx/xxx)

\(标黄部分有具体项目对接提供\)

### <a id="_Toc68628081"></a><a id="_Toc119940343"></a><a id="_Toc180762546"></a>1\.2 公共请求参数 

<a id="_Toc68628082"></a>__请求或返回公共参数__

序号

参数名

参数类型

是否必填

备注

1

data

String

是

业务场景的json数据，需参考encrypt值决定是否需SM2全报文加密字段，接收方公钥加密 

2

encrypt

String\(1\)

是

data是否加密 1\-加密，0\-不加密

3

nonceStr

String\(32\)

是

本次通信报文随机数，接收方__响应报文使用__发送方的随机数，__无需重新生成__

4

timeStamp

String\(64\)

是

本次通信报文时间戳，接收方__响应报文使用__发送方的时间戳，可利用时间撮判断报文是否已失效、作为流水等，__无需重新生成__

5

sign

String\(1024\)

是

SM3WithSM2加签，请见加密规则，发起方私钥加签

<a id="_Toc68628083"></a>__返回参数中data数据格式__

返回报文data为业务场景的json数据，返回报文中status 字段：1表示成功、 0表示失败，失败的情况下ackMsg（失败原因）必需返回；

名称

是否必须

字段

类型

说明

接收状态

M

status

String\(2\)

Hex String，1\-成功  0\-失败

失败原因

O

ackMsg

String\(256\)

失败情况下必须返回

业务参数1

O

xxxx

业务参数，根据场景需要

业务参数2

O

xxxx

业务参数，根据场景需要

### <a id="_Toc119940344"></a><a id="_Toc68628084"></a><a id="_Toc180762547"></a>1\.3 安全规范

接收方需要验证场景平台上送的签名是否正确，场景平台收到应答，也需要验证签名是否正确。如场景平台未正确验证签名，存在潜在风险，场景平台自行承担因此而产生的所有损失。

### <a id="_Toc68628085"></a><a id="_Toc119940345"></a><a id="_Toc180762548"></a>1\.4 报文结构

请求与返回报文统一为json格式字符串，格式如下：

\{

"data": "SM2使用对方公钥进行全报文加密Hex String",

"encrypt": "data是否加密 1\-加密，0\-不加密",

"nonceStr": "随机数",

"timeStamp"：“时间戳”，

    "sign": “按SM3\(data原文 \+ encrypt \+ nonceStr \+ timeStamp 组合原文\)计算摘要后，采用SM2签名”

\}

- __示例公私钥对__

// 行方公私钥对  第一个为私钥，第二个为公钥

__static __String\[\] *xibGenerateKeyPairs *= __new __String\[\] \{   \(标黄部分有具体项目对接提供\)

  
   __"58a56fe06e81724a88fc3e2282428e2773af14b9d720032cfa5ebfdadcd9d23e"__,  
__"04c7457f9972b25722d1b7144fd27bd470befafaeb1c812db71fc4fc653f5e4d87a76d1069b2661b9feddfbc967a29cb23c223de2d5564159b9d7e33abd4b77eac"  
__\};

// 合作方公私钥对  第一个为私钥，第二个为公钥  
__static __String\[\] *partnerGenerateKeyPairs *= __new __String\[\] \{  
__"041838FF0965F17C9D8C69AA1640523C2AD83ED5AD482A1F59774E75980A6190"__, __"0435B0F3DD1BEEE5497EFD806968B8EB78D95EF56B29984CAAFEFC75B4C1014807FF39E94AD86E1202C5779BFFDA57CA5EEAE61689524ECDEC697A7EC7EDDD6E6D"  
__\};

- __请求示例报文：__

 \{

    "timeStamp":"1617699916653",

"data":"046A7C4B653CFB7B0820581DA7B393706E94562ECD7FE9B98AEA20DC78B42A31A5DAAB9479358EF7F335D75C50E4492F9563EA0AB1DCD2D3C742818ECF1C234D4DF3E859A7C702AFC56AF86F3EBBE27B4175D54BF7614B689125842AEE0A840E449CA64F48CF39025868668069C58F043DCFD28D228D6B77C8C210AF46301CDEF72F54A609F582DCD92A379E5A41DE6C",

    "encrypt":"1",

"sign":"MEQCIAU\+oNjMVKYtn2G2B6TfSHPnTpLbWnO2Pny73U\+O\+BWpAiBar8D3vpEY2wkwFDdAUx0/MZU/VxoAIJ\+K/ezbo3gDfg==",

    "nonceStr":"qkdq1dte"

\}

其中data原始明文为：\{"creditApplicationCode":"0120210402772141087"\}

- __返回示例报文__

 \{

    "timeStamp":"1617699916653", // 该值为请求方的数据，不是使用当前时间

"data":"047da9ae370b5b6723edee53d5c437f082c1af87b4a10bb3641f196ca95e0f3a35eb7f0fcf21cec9f46472a7992b89f190e67ae23019e871fe0248b92b92642bbe7008af0938b9096684824e206002ee77ca1d5026ac1b3438412c5100881c2089e38a0e33a8615401e171f99bdcc4ba3005fcd59314b4d22c5401af76f05908f70972a04839f3641cf0474373e28cf343f07a96332e121b8f2a8434364497bd5ee8025e9231fbb1c97b911faf73ec36c3b110909b575602d297bdbd73942d56edb166ff3328e04ecb4fb97c20af57f11aad1b65786c77f0190a9955c7af9d22f0c2bc9699e61b7dfe5aba5bbbee249522713ab3a1343f10206ccbec8fd0ba24d49a9265f9ac3954c2efca106a2a2cc3245458ea173d06efa71f61772fa39abdb0530b81c97b55b03f312169e88168eb524cdc2bb548104a4a22e425a2e7568c842a7f659f83",

    "encrypt":"1",

"sign":"MEUCICJx1fTjuTn5Q31bvG1RxdCbG1/SJEAAFfxA/ZOQ4/eAAiEA/EOplMH56lBN7WsY\+19LZ3iyoakVBWfQ4cN\+\+jNbB5k=",

    "nonceStr":"qkdq1dte" // 该值为请求方的数据，不是使用当前随机生成

\}

__	__其中data原始明文为：\{"url":"https://xib01\.test\.xib\.com\.cn:34443/loan/gwsg\#/idcard\-verify?token=50cfbc40d9abccacac1014eb357b533b8c60e7a41ab5d31ae158d652df139950","token":"50cfbc40d9abccacac1014eb357b533b8c60e7a41ab5d31ae158d652df139950","status":"1"\} 

\(标黄部分有具体项目对接提供\)

### <a id="_Toc119940346"></a><a id="_Toc68628086"></a><a id="_Toc180762549"></a>1\.5数据加密及报文加签

基于互联网交互，报文发起方需要对整体报文（包括公共部分）做签名。报文接收方应该先验签，验签通过后再进入实际业务流程。

#### <a id="_Toc68628087"></a>1\.5\.1 加密、解密方案

1、data数据采用国密SM2加密

2、报文中encrypt设置为1（即采用全报文加密）

3、使用接收方公钥进行SM2加密，报文发送到接收方时，使用自身私钥进行解密，得到data原文

#### <a id="_Toc68628088"></a>1\.5\.2 签名方案

1、	除去sign之外的域，按照域名key的字典顺序升序排列，获取对应value值拼接成一个字符串，例如val1val2val3val4

2、	用SM3算法计算上一步得到的串得到摘要

3、	用发起方的SM2私钥对上一步得到的结果生成签名（算法：SM3WithSM2）。

#### <a id="_Toc68628089"></a>1\.5\.3 密钥交换

行方和电子保函平台渠道端各持有一对公私钥对，双方交换公钥（可通过邮件）

## <a id="_Toc180762550"></a><a id="_Toc23360_WPSOffice_Level2"></a><a id="_Toc21255_WPSOffice_Level2"></a>公共资源交易中心电子投标保函

### <a id="_Toc25292_WPSOffice_Level3"></a><a id="_Toc10449334"></a><a id="_Toc16071_WPSOffice_Level3"></a><a id="_Toc180762551"></a><a id="_2.1开函申请接口（异步推送）"></a><a id="_Toc11530_WPSOffice_Level3"></a><a id="_Toc20909_WPSOffice_Level3"></a>2\.1保函申请订单推送

- __接收数据URL地址__

[https://xib03\.test\.xib\.com\.cn:38080](https://xib03.test.xib.com.cn:38080)/ifspeesi/gm/api/dlglcn004/DlgOrderApply

- __请求方式__

post  application/json

- __功能说明__

合作方平台向银行提交保函申请订单信息。

- __请求数据\(解密后的数据\)__

__名称__

__类型__

__必须__

__描述__

channel\_no

string

是

渠道号，不同的交易中心不同，由银行分配，请联系银行工作人员获取。

dlglcn004\-担保公司获客\(永鸿兴\-广东、浙江地区项目\)

dlglcn005\-担保公司获客\(永鸿兴\-其他地区项目\)

center\_name

string

是

交易中心名称

order\_no

string

是

订单编号

zx\_order\_no

string

可选

中心订单编号，如果订单编号采用的是中心的编号，则两则一致即可。

order\_status

string

是

订单状态

project\_number

string

否

招标项目编号，若没有项目编号则按照标段填写

project\_name

string

否

招标项目名称，若没有项目编号则按照标段填写

block\_number

string

是

标段编号，若只有招标项目编号，则此处填充招标项目编号。明文保函必输。

block\_name

string

否

标段名称，若只有招标项目编号，则此处填充招标项目编号。明文保函必输。

address

string

否

项目地址，格式:省市,例如"广东省深圳市"

project\_type

string

否

项目类型

1：工程建设；2：政府采购；

bid\_deadline

string

否

开标时间，投递投标文件截止时间

valid\_day

string

否

投标有效期（单位：天）

project\_amount

decimal\(16,6\)

否

项目金额（单位：元）。最多保留小数点后两位

contract\_reckon\_amount

decimal\(16,6\)

否

合同估算价（单位：元）。最多保留小数点后两位

guarantee\_amount

decimal\(16,6\)

是

担保金额（单位：元）。最多保留小数点后两位

guarantee\_end\_date

string

否

担保结束日期（格式：YYYY\-MM\-DD）

由合作方计算后传输，作为保函到期日。

guarantee\_type

string

是

保函类型。2\-明文，1\-密文

creditor

string

可选

招标人（建设单位、受益人）名称

creditor\_scc

string

可选

招标人（建设单位、受益人）统一社会信用代码

creditor\_name

string

否

招标人联系人

creditor\_phone

string

否

招标人联系人联系方式

creditor\_account\_bank\_no

string

否

招标人银行账户

creditor\_account\_bank\_name

string

否

招标人银行开户行

tender\_publish\_date

string

否

招标公告发布时间（格式：YYYY\-MM\-DD HH:mm:ss）

creditor\_bank\_account\_name

string

否

招标人银行账户名称

principal

string

是

被保证人名称

principal\_scc

string

是

被保证人统一社会信用代码

principal\_address

string

否

被保证人地址

principal\_email

string

否

被保证人邮箱

principal\_basic\_account

string

否

被保证人企业基本账户

principal\_basic\_account\_name

string

否

被保证人企业基本账户名称

principal\_name

string

否

被保证人联系人

principal\_cert\_type

string

否

被保证人联系人身份证件类型

principal\_id\_card

string

否

被保证人联系人身份证件号码

principal\_phone

string

否

被保证人联系人联系方式

principal\_legal\_person

string

是

被保证人法定代表人

principal\_legal\_cert\_type

string

是

被保证人法定代表人身份证件类型

principal\_legal\_id\_card

string

是

被保证人法定代表人身份证件号码

principal\_legal\_phone

string

是

被保证人法定代表人联系方式

principal\_qualf\_name

string

否

被保证人最高资质名称，如：“建筑装修装饰工程专业承包”

guarantor

string

是

担保公司名称

apply\_company\_social\_credit\_code

string

是

担保公司统一社会信用代码

guarantee\_file\_type

string

是

保函文件格式。值域:PDF、OFD

guarantee\_template\_file\_id

string

是

保函模板ID

guarantee\_template\_file\_name

string

是

保函模板名称

fileList

JSON数组

否

投保申请的各类附件信息，包括但不限于营业执照、法人身份证明、招标文件、出具保函承诺书等。

- __附件信息__

__名称__

__类型__

__必须__

__描述__

fileNm

string

是

文件名称

fileTp

string

是

文件类型

1\-营业执照

2\-法人身份证明

3\-招标文件

4\-电子保单（合同）原文件

5\-电子保函（合同）PDF文件

6\-电子保函申请单原文件

7\-电子保函申请单PDF文件

8\-出具保函承诺书

9\-企业资质登记证书

10\-理赔文件

0\-其他

可根据情况增加文件类型，红色部分在订单传输环节是必须的。

fileUrl

string

是

文件路径

md5Value

string

否

文件大写MD5码

- __返回结果\(解密后的数据\)__

__名称__

__类型__

__必须__

__描述__

status

int

是

状态码

- __返回示例__

\{

	"status": “1”

\}

### <a id="_Toc173158117"></a><a id="_Toc180762552"></a>2\.2保函申请订单状态更新

- __接收数据URL地址__

https://xib03\.test\.xib\.com\.cn:38080/ifspeesi/gm/api/dlglcn004/DlgOrderStUpdate

- __请求方式__

post  application/json

- __功能说明__

在未发起开函申请之前，保函申请订单取消时，合作方平台向银行推送保函申请订单状态信息。

- __请求数据\(解密后的数据\)__

__名称__

__类型__

__必须__

__描述__

order\_no

string

是

订单编号

block\_number

string

否

标段编号，项目信息为空的时候不传，其他要传输，为密码串的传输密码串

block\_name

string

否

标段名称，项目信息为空的时候不传，其他要传输，为密码串的传输密码串

principal

string

是

被保证人

creditor

string

否

受益人，招标为空的时候不传，其他要传输，为密码串的传输密码串

applicant

string

是

申请人。分离式保函申请人为担保公司。

order\_status\_old

string

是

旧订单状态

order\_status

string

是

订单状态

- __返回结果__

__名称__

__类型__

__必须__

__描述__

status

int

是

状态码

### <a id="_Toc180762553"></a>2\.3推送保函申请订单处理结果

- __接收数据URL地址__

由交易中心等合作平台提供URL地址

- __请求方式__

post  application/json

- __功能说明__

银行返回保函申请订单处理结果。

- __请求数据\(解密后的数据\)__

__名称__

__类型__

__必须__

__描述__

order\_no

string

是

订单编号

block\_number

string

否

标段（包）编号，项目信息为空的时候不传，其他要传输，为密码串的传输密码串

block\_name

string

否

标段（包）名称，项目信息为空的时候不传，其他要传输，为密码串的传输密码串

status

int

是

保函申请处理结果

1审核通过2审核不通过

guarantee\_no

string

否

保函编号，当status=1的时候必输。

guarantor

string

否

出函机构，当status=1的时候必输。

out\_guarantee\_time

string

否

出函时间，当status=1的时候必输。格式：yyyy\-MM\-dd HH:mm:ss

guarantee\_start\_date

string

否

保函起始日（格式：yyyy\-MM\-dd），当status=1的时候必输。

guarantee\_end\_date

string

否

保函终止日期（格式：yyyy\-MM\-dd），当status=1的时候必输。

note

string

否

保函申请审批结果备注，当status=2的时候必输。

- __返回结果__

__名称__

__类型__

__必须__

__描述__

status

int

是

状态码

- __返回示例__

\{

	"status": "1",

\}

### <a id="_Toc180762554"></a><a id="_Toc75523374"></a>2\.4推送电子保函文件

- __接收数据URL地址__

由交易中心等合作平台提供URL地址

- __功能说明__

澳门国际银行出具电子保函后，将电子保函文件推送到合作方。

- __请求数据\(报文原文\)__

__名称__

__类型__

__必须__

__描述__

data

string

是

SM2全报文加密密文

nonceStr

string

是

随机数

encrypt

string

是

data是否加密 1\-加密，0\-不加密

timeStamp

string

是

时间戳

sign

string

是

签名值

guarantee\_file

file

是

文件流，不加密

- __请求数据\(data解密后的数据\)__

__名称__

__类型__

__必须__

__描述__

order\_no

string

是

订单编号

guarantee\_no

string

是

保函编号

service\_begin\_time

string

是

保函起始日（格式：yyyy\-MM\-dd）

service\_end\_time

string

是

保函终止日（格式：yyyy\-MM\-dd）

guarantee\_file\_type

string

是

保函文件类型，PDF、OFD

guarantee\_type

string

是

保函类型，2\-明文，1\-密文

guarantee\_file\_md5

string

是

保函文件MD5码

guarantee\_file\_hash

string

是

电子保函哈希码：电子保函原件的hash值,采用sha256算法。值例如:

66cf2a99d894d908f6bcf01c4a3d9d248d05ad31fa0b0dce5b0914a09b4555a4

- __返回结果__

__名称__

__类型__

__必须__

__描述__

status

int

是

状态码

- __返回示例__

\{

	"status": “1”

\}

### <a id="_Toc180762555"></a>2\.5密文保函解密还原（项目信息方式）通知

- __接收数据URL地址__

https://xib03\.test\.xib\.com\.cn:38080/ifspeesi/gm/api/dlglcn004/DlgDecryptionNotice

- __请求方式__

post  application/json

- __功能说明__

对于密文保函，当项目开标后，合作方平台通知银行解密还原保函信息。接口输入均为Json数组格式。单次推送包含单个或多个订单。

- __请求数据\(解密后的数据\)__

__名称__

__类型__

__必须__

__描述__

guaranteeList

JSON数组

是

json数组内容见下表

- guaranteeList__数据__

__名称__

__类型__

__必须__

__描述__

order\_no

string

是

订单编号

guarantee\_no

string

是

保函编号

bid\_time

string

是

开标时间（格式：yyyy\-MM\-dd）

key

string

可选

加密的key（密钥）

project\_name

string

可选

项目名称

project\_number

string

可选

项目编号

block\_name

string

是

标段（包件）名称

block\_number

string

是

标段（包件）编号

creditor

string

是

招标人

creditor\_scc

string

是

招标人统一信用社会代码

creditor\_phone

string

否

招标人联系人电话

creditor\_agency

string

否

代理机构

creditor\_region

string

否

招标（采购）人地址 区域码

国家标准6位行政区划

project\_region

string

否

项目地区 区域码

国家标准6位行政区划

bid\_type

string

否

招标类型（1：服务；2：货物；3：工程）

project\_type

string

否

项目类型（1：工程建设；2：政府采购）

- __返回结果__

__名称__

__类型__

__必须__

__描述__

status

int

是

状态码

- __返回示例__

\{

	"status": “1”

\}

### <a id="_Toc173158121"></a><a id="_Toc180762556"></a>2\.6保函退保申请

- __接收数据URL地址__

https://xib03\.test\.xib\.com\.cn:38080/ifspeesi/gm/api/dlglcn004/DlgSurrenderApply

- __请求方式__

post  application/json

- __功能说明__

向银行推送保函退保申请。

- __请求数据\(解密后的数据\)__

__名称__

__类型__

__必须__

__描述__

order\_no

string

是

订单编号

guarantee\_no

string

是

保函编号

reason

string

是

退保原因

apply\_time

string

是

申请时间，格式yyyy\-MM\-dd HH:mm:ss

principal

string

否

投标人名称

principal\_scc

string

否

投标人统一社会信用代码

principal\_phone

string

否

联系人电话

principal\_bank\_name

string

否

银行开户行

principal\_bank\_account

string

否

银行账号

- __返回示例__

\{

	"status": "1"

\}

### <a id="_Toc173158122"></a><a id="_Toc180762557"></a>2\.7保函退保通知

- __接收数据URL地址__

由交易中心等合作平台提供URL地址

- __功能说明__

银行将退保申请结果通知合作平台

- __请求数据\(解密后的数据\)__

__名称__

__类型__

__必须__

__描述__

guarantee\_no

string

是

保函编号

order\_no

string

是

订单编号

result

string

是

退保结果。1\-退保成功，0\-退保失败

remark

string

否

备注

- __返回结果__

__名称__

__类型__

__必须__

__描述__

status

int

是

状态码

- __返回示例__

\{

	"status": "1"

\}

## <a id="_Toc180762558"></a>附录

### <a id="_Toc68628091"></a><a id="_Toc180762559"></a>3\.1 接口说明

#### <a id="_Toc51929328"></a><a id="_Toc53592716"></a><a id="_Toc68624641"></a><a id="_Toc53123069"></a><a id="_Toc53210521"></a><a id="_Toc68624643"></a><a id="_Toc53211231"></a><a id="_Toc53603912"></a><a id="_Toc53121832"></a><a id="_Toc53121956"></a><a id="_Toc68628094"></a><a id="_Toc68628092"></a><a id="_Toc68628095"></a>3\.1\.1 国密算法示例交易

##### <a id="_Toc68628096"></a>3\.1\.1\.1 接口访问地址

[https://xib03\.test\.xib\.com\.cn:51443/ifspeesi/gm/api/test/getToken](https://xib03.test.xib.com.cn:51443/ifspeesi/gm/api/test/getToken)

\(标黄部分有具体项目对接提供\)

##### <a id="_Toc68628097"></a>3\.1\.1\.2 接口功能描述

示例交易，获取token

##### <a id="_Toc68628098"></a>3\.1\.1\.3 请求参数列表

请求数据

名称

说明

字段

类型

必输项

授信申请编码

向客户展示行方跳转二维码时，先调用该接口获取token，并作为url参数放置在二维码内容中

creditApplicationCode

String\(32\)

Y

##### <a id="_Toc68628099"></a>3\.1\.1\.4 返回参数列表

名称

说明

字段

类型

与入参授信申请编码关联的token

token具有一定时效性，超过规定时间（如10分钟）后会自动失效。

Token

String\(64\)

接收状态

成功   失败

status

String\(2\)

失败原因

失败情况下必须返回

ackMsg

String\(256\)

### <a id="_Toc68628100"></a><a id="_Toc75523375"></a><a id="_Toc53603923"></a><a id="_Toc68624653"></a><a id="_Toc68628104"></a><a id="_Toc75771035"></a><a id="_Toc68628101"></a><a id="_Toc53603920"></a><a id="_Toc68624650"></a><a id="_Toc75771038"></a><a id="_Toc68628106"></a><a id="_Toc68628111"></a><a id="_Toc180762560"></a>3\.2 国密SM生成密钥对

打开行方提供的Demo，执行generateKeyPairs方法

