## Page 1

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
状态码 说明 schema
200 OK  
401 Unauthorized  
粤商通 - 厦门国际银行服务系统接口文档
简介：
HOST:localhost:7540
联系人: 中数智创
Version:v1.0
接口路径： /v2/api-docs
captcha-controller  
getCode  
接口描述:
接口地址:/system/captchaImage
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 2

状态码 说明 schema
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
sys-login-controller  
getInfo  
接口描述:
接口地址:/system/getInfo
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
getRouters  
 

## Page 3

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
接口描述:
接口地址:/system/getRouters
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
首页统计  
接口描述:
接口地址:/system/homePageStatistics
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
 

## Page 4

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
elgPlatformId elgPlatformId query false string  
endTime endTime query false string  
startTime startTime query false string  
type type query false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
login  
接口描述:
接口地址:/system/login
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
请求参数：
 
{
    "password": "",
    "username": ""
}

## Page 5

参数名称 参数说明 in 是否必须 数据类型 schema参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
loginBody loginBody body true LoginBody LoginBody
参数名称 参数说明 in 是否必须 数据类型 schema
password  body false string  
username  body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
schema 属性说明
 
LoginBody
响应示例:
响应参数:
暂无
 
 
响应状态:
sys-menu-controller  
add  
接口描述:
接口地址:/system/menu
请求方式：POST
consumes:["application/json"]
 

## Page 6

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
menu 菜单权限表 body true SysMenuPO 对象 SysMenuPO 对象
produces:["*/*"]
请求示例：
请求参数：
schema 属性说明
 
{
    "children": [
        {
            "children": [],
            "component": "",
            "createBy": "",
            "createTime": "",
            "icon": "",
            "isCache": 0,
            "isFrame": 0,
            "menuId": 0,
            "menuName": "",
            "menuType": "",
            "orderNum": 0,
            "params": {},
            "parentId": 0,
            "path": "",
            "perms": "",
            "remark": "",
            "status": "",
            "updateBy": "",
            "updateTime": "",
            "visible": ""
        }
    ],
    "component": "",
    "createBy": "",
    "createTime": "",
    "icon": "",
    "isCache": 0,
    "isFrame": 0,
    "menuId": 0,
    "menuName": "",
    "menuType": "",
    "orderNum": 0,
    "params": {},
    "parentId": 0,
    "path": "",
    "perms": "",
    "remark": "",
    "status": "",
    "updateBy": "",
    "updateTime": "",
    "visible": ""
}

## Page 7

参数名称 参数说明 in 是否必须 数据类型 schema
children  body false array SysMenuPO 对象
component 组件路径 body false string  
createBy 创建者 body false string  
createTime 创建时间 body false string(date-time)  
icon 菜单图标 body false string  
isCache 是否缓存（ 0 缓存  1 不缓存） body false integer(int32)  
isFrame 是否为外链（ 0 是  1 否） body false integer(int32)  
menuId 菜单 ID body false integer(int64)  
menuName 菜单名称 body false string  
menuType 菜单类型（ M 目录  C 菜单  F 按钮） body false string  
orderNum 显示顺序 body false integer(int32)  
params  body false object  
parentId 父菜单 ID body false integer(int64)  
path 路由地址 body false string  
perms 权限标识 body false string  
remark 备注 body false string  
status 菜单状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
updateTime 更新时间 body false string(date-time)  
visible 菜单状态（ 0 显示  1 隐藏） body false string  
状态码 说明 schema
200 OK  
SysMenuPO 对象
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 8

状态码 说明 schema
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
edit  
接口描述:
接口地址:/system/menu
请求方式：PUT
consumes:["application/json"]
produces:["*/*"]
请求示例：
{
    "children": [
        {
            "children": [],
            "component": "",
            "createBy": "",
            "createTime": "",
            "icon": "",
            "isCache": 0,
            "isFrame": 0,
            "menuId": 0,
            "menuName": "",
            "menuType": "",
            "orderNum": 0,
            "params": {},
            "parentId": 0,
            "path": "",
            "perms": "",
            "remark": "",
            "status": "",
            "updateBy": "",
            "updateTime": "",
            "visible": ""
        }
    ],
    "component": "",
    "createBy": "",
    "createTime": "",
    "icon": "",
    "isCache": 0,
    "isFrame": 0,
    "menuId": 0,
    "menuName": "",
    "menuType": "",
    "orderNum": 0,
    "params": {},

## Page 9

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
menu 菜单权限表 body true SysMenuPO 对象 SysMenuPO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
children  body false array SysMenuPO 对象
component 组件路径 body false string  
createBy 创建者 body false string  
createTime 创建时间 body false string(date-time)  
icon 菜单图标 body false string  
isCache 是否缓存（ 0 缓存  1 不缓存） body false integer(int32)  
isFrame 是否为外链（ 0 是  1 否） body false integer(int32)  
menuId 菜单 ID body false integer(int64)  
menuName 菜单名称 body false string  
menuType 菜单类型（ M 目录  C 菜单  F 按钮） body false string  
orderNum 显示顺序 body false integer(int32)  
params  body false object  
parentId 父菜单 ID body false integer(int64)  
path 路由地址 body false string  
perms 权限标识 body false string  
remark 备注 body false string  
status 菜单状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
请求参数：
schema 属性说明
 
SysMenuPO 对象
    "parentId": 0,
    "path": "",
    "perms": "",
    "remark": "",
    "status": "",
    "updateBy": "",
    "updateTime": "",
    "visible": ""
}

## Page 10

参数名称 参数说明 in 是否必须 数据类型 schema
updateTime 更新时间 body false string(date-time)  
visible 菜单状态（ 0 显示  1 隐藏） body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
component 组件路径 query false string  
createBy 创建者 query false string  
createTime 创建时间 query false string  
icon 菜单图标 query false string  
响应示例:
响应参数:
暂无
 
 
响应状态:
list  
接口描述:
接口地址:/system/menu/list
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
 

## Page 11

参数名称 参数说明 in 是否必须 数据类型 schema
isCache 是否缓存（ 0 缓存  1 不缓存） query false integer  
isFrame 是否为外链（ 0 是  1 否） query false integer  
menuId 菜单 ID query false integer  
menuName 菜单名称 query false string  
menuType 菜单类型（ M 目录  C 菜单  F 按钮） query false string  
orderNum 显示顺序 query false integer  
params  query false object  
parentId 父菜单 ID query false integer  
path 路由地址 query false string  
perms 权限标识 query false string  
remark 备注 query false string  
status 菜单状态（ 0 正常  1 停用） query false string  
updateBy 更新者 query false string  
updateTime 更新时间 query false string  
visible 菜单状态（ 0 显示  1 隐藏） query false string  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
roleMenuTreeselect  
 

## Page 12

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
roleId roleId path true integer  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
接口描述:
接口地址:/system/menu/roleMenuTreeselect/{roleId}
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
treeselect  
接口描述:
接口地址:/system/menu/treeselect
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
 

## Page 13

参数名称 参数说明 in 是否必须 数据类型 schema参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
component 组件路径 query false string  
createBy 创建者 query false string  
createTime 创建时间 query false string  
icon 菜单图标 query false string  
isCache 是否缓存（ 0 缓存  1 不缓存） query false integer  
isFrame 是否为外链（ 0 是  1 否） query false integer  
menuId 菜单 ID query false integer  
menuName 菜单名称 query false string  
menuType 菜单类型（ M 目录  C 菜单  F 按钮） query false string  
orderNum 显示顺序 query false integer  
params  query false object  
parentId 父菜单 ID query false integer  
path 路由地址 query false string  
perms 权限标识 query false string  
remark 备注 query false string  
status 菜单状态（ 0 正常  1 停用） query false string  
updateBy 更新者 query false string  
updateTime 更新时间 query false string  
visible 菜单状态（ 0 显示  1 隐藏） query false string  
状态码 说明 schema
200 OK  
401 Unauthorized  
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 14

状态码 说明 schema
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
menuId menuId path true integer  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
getInfo  
接口描述:
接口地址:/system/menu/{menuId}
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
remove  
接口描述:
 

## Page 15

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
menuId menuId path true integer  
状态码 说明 schema
200 OK  
204 No Content  
401 Unauthorized  
403 Forbidden  
接口地址:/system/menu/{menuId}
请求方式：DELETE
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
sys-operlog-controller  
clean  
接口描述:
接口地址:/system/operlog/clean
请求方式：DELETE
consumes:``
produces:["*/*"]
 
 

## Page 16

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
状态码 说明 schema
200 OK  
204 No Content  
401 Unauthorized  
403 Forbidden  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
businessType 业务类型（ 0 其它  1 新增  2 修改  3 删除） query false integer  
errorMsg 错误消息 query false string  
jsonResult 返回参数 query false string  
method 方法名称 query false string  
operId 日志主键 query false integer  
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
list  
接口描述:
接口地址:/system/operlog/list
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
 

## Page 17

参数名称 参数说明 in 是否必须 数据类型 schema
operIp 主机地址 query false string  
operLocation 操作地点 query false string  
operName 操作人员 query false string  
operParam 请求参数 query false string  
operTime 操作时间 query false string  
operUrl 请求 URL query false string  
operatorType 操作类别（ 0 其它  1 后台用户  2 手机端用户） query false integer  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
params  query false object  
requestMethod 请求方式 query false string  
status 操作状态（ 0 正常  1 异常） query false integer  
title 模块标题 query false string  
参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  object  
total 总记录数 integer(int64) integer(int64)
状态码 说明 schema
200 OK TableDataInfo
401 Unauthorized  
响应示例:
响应参数:
 
 
响应状态:
{
    "code": 0,
    "msg": "",
    "rows": {},
    "total": 0
}

## Page 18

状态码 说明 schema
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
operIds operIds path true string  
状态码 说明 schema
200 OK  
204 No Content  
401 Unauthorized  
403 Forbidden  
remove  
接口描述:
接口地址:/system/operlog/{operIds}
请求方式：DELETE
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 19

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
sys-profile-controller  
profile  
接口描述:
接口地址:/system/user/profile
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
updateProfile  
接口描述:
接口地址:/system/user/profile
请求方式：PUT
consumes:["application/json"]
produces:["*/*"]
 

## Page 20

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
user 用户信息表 body true SysUserPO 对象 SysUserPO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
admin  body false boolean  
avatar 头像地址 body false string  
companyAcronym承保机构名称缩写 body false string  
companyId 承保机构 id body false string  
请求示例：
请求参数：
schema 属性说明
 
SysUserPO 对象
{
    "admin": true,
    "avatar": "",
    "companyAcronym": "",
    "companyId": "",
    "companyName": "",
    "createBy": "",
    "createTime": "",
    "delFlag": "",
    "email": "",
    "loginDate": "",
    "loginIp": "",
    "nickName": "",
    "params": {},
    "password": "",
    "phonenumber": "",
    "platIds": [],
    "platformIds": "",
    "platformSelect": "",
    "remark": "",
    "roleIds": [],
    "sex": "",
    "status": "",
    "updateBy": "",
    "updateTime": "",
    "userId": 0,
    "userName": "",
    "userPlatformList": [
        {
            "elgPlatformId": "",
            "platformName": ""
        }
    ],
    "userType": ""
}

## Page 21

参数名称 参数说明 in 是否必须 数据类型 schema
companyName 承保机构名称 body false string  
createBy 创建者 body false string  
createTime 创建时间 body false string(date-time) 
delFlag 删除标志（ 0 代表存在  2 代表删除） body false string  
email 用户邮箱 body false string  
loginDate 最后登录时间 body false string(date-time) 
loginIp 最后登录 IP body false string  
nickName 用户昵称 body false string  
params  body false object  
password  body false string  
phonenumber 手机号码 body false string  
platIds  body false array  
platformIds 平台集合 body false string  
platformSelect 是否显示平台下拉： 1 显示， 2 不显示 body false string  
remark 备注 body false string  
roleIds  body false array  
sex 用户性别（ 0 男  1 女  2 未知） body false string  
status 帐号状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
updateTime 更新时间 body false string(date-time) 
userId 用户 ID body false integer(int64)  
userName 用户账号 body false string  
userPlatformList 平台下拉集合 body false array UserPlatformBO
userType 用户类型： 1 管理员， 2 担保公司 body false string  
参数名称 参数说明 in 是否必须 数据类型 schema
elgPlatformId  body false string  
platformName  body false string  
UserPlatformBO
响应示例:

## Page 22

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
avatarfile avatarfile formData true file  
响应参数:
暂无
 
 
响应状态:
avatar  
接口描述:
接口地址:/system/user/profile/avatar
请求方式：POST
consumes:["multipart/form-data"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 
 

## Page 23

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
newPassword newPassword query false string  
oldPassword oldPassword query false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
updatePwd  
接口描述:
接口地址:/system/user/profile/updatePwd
请求方式：PUT
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 24

状态码 说明 schema
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
role 角色信息表 body true SysRolePO 对象 SysRolePO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
admin  body false boolean  
createBy 创建者 body false string  
sys-role-controller  
add  
接口描述:
接口地址:/system/role
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
请求参数：
schema 属性说明
 
SysRolePO 对象
{
    "admin": true,
    "createBy": "",
    "createTime": "",
    "delFlag": "",
    "menuIds": [],
    "params": {},
    "remark": "",
    "roleId": 0,
    "roleName": "",
    "roleSort": 0,
    "status": "",
    "updateBy": "",
    "updateTime": ""
}

## Page 25

参数名称 参数说明 in 是否必须 数据类型 schema
createTime 创建时间 body false string(date-time)  
delFlag 删除标志（ 0 代表存在  2 代表删除） body false string  
menuIds  body false array  
params  body false object  
remark 备注 body false string  
roleId 角色 ID body false integer(int64)  
roleName 角色名称 body false string  
roleSort 显示顺序 body false integer(int32)  
status 角色状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
updateTime 更新时间 body false string(date-time)  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
edit  
接口描述:
接口地址:/system/role
请求方式：PUT
consumes:["application/json"]
 

## Page 26

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
role 角色信息表 body true SysRolePO 对象 SysRolePO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
admin  body false boolean  
createBy 创建者 body false string  
createTime 创建时间 body false string(date-time)  
delFlag 删除标志（ 0 代表存在  2 代表删除） body false string  
menuIds  body false array  
params  body false object  
remark 备注 body false string  
roleId 角色 ID body false integer(int64)  
roleName 角色名称 body false string  
roleSort 显示顺序 body false integer(int32)  
status 角色状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
updateTime 更新时间 body false string(date-time)  
produces:["*/*"]
请求示例：
请求参数：
schema 属性说明
 
SysRolePO 对象
{
    "admin": true,
    "createBy": "",
    "createTime": "",
    "delFlag": "",
    "menuIds": [],
    "params": {},
    "remark": "",
    "roleId": 0,
    "roleName": "",
    "roleSort": 0,
    "status": "",
    "updateBy": "",
    "updateTime": ""
}

## Page 27

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
changeStatus  
接口描述:
接口地址:/system/role/changeStatus
请求方式：PUT
consumes:["application/json"]
produces:["*/*"]
请求示例：
请求参数：
 
{
    "admin": true,
    "createBy": "",
    "createTime": "",
    "delFlag": "",
    "menuIds": [],
    "params": {},
    "remark": "",
    "roleId": 0,
    "roleName": "",
    "roleSort": 0,
    "status": "",
    "updateBy": "",
    "updateTime": ""
}

## Page 28

参数名称 参数说明 in 是否必须 数据类型 schema参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
role 角色信息表 body true SysRolePO 对象 SysRolePO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
admin  body false boolean  
createBy 创建者 body false string  
createTime 创建时间 body false string(date-time)  
delFlag 删除标志（ 0 代表存在  2 代表删除） body false string  
menuIds  body false array  
params  body false object  
remark 备注 body false string  
roleId 角色 ID body false integer(int64)  
roleName 角色名称 body false string  
roleSort 显示顺序 body false integer(int32)  
status 角色状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
updateTime 更新时间 body false string(date-time)  
状态码 说明 schema
200 OK  
201 Created  
schema 属性说明
 
SysRolePO 对象
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 29

状态码 说明 schema
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
admin  query false boolean  
createBy 创建者 query false string  
createTime 创建时间 query false string  
delFlag 删除标志（ 0 代表存在  2 代表删除） query false string  
menuIds  query false array integer
pageNum pageNum query false integer  
pageSize pageSize query false integer  
params  query false object  
remark 备注 query false string  
roleId 角色 ID query false integer  
roleName 角色名称 query false string  
roleSort 显示顺序 query false integer  
status 角色状态（ 0 正常  1 停用） query false string  
updateBy 更新者 query false string  
updateTime 更新时间 query false string  
list  
接口描述:
接口地址:/system/role/list
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:

## Page 30

参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  object  
total 总记录数 integer(int64) integer(int64)
状态码 说明 schema
200 OK TableDataInfo
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
roleIds roleIds path true string  
响应参数:
 
 
响应状态:
remove  
接口描述:
接口地址:/system/role/{roleIds}
请求方式：DELETE
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
{
    "code": 0,
    "msg": "",
    "rows": {},
    "total": 0
}

## Page 31

状态码 说明 schema
200 OK  
204 No Content  
401 Unauthorized  
403 Forbidden  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
roleId roleId path true integer  
响应参数:
暂无
 
 
响应状态:
getInfo  
接口描述:
接口地址:/system/role/{roleId}
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 
 

## Page 32

状态码 说明 schema状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
sys-user-controller  
add  
接口描述:
接口地址:/system/user
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
{
    "admin": true,
    "avatar": "",
    "companyAcronym": "",
    "companyId": "",
    "companyName": "",
    "createBy": "",
    "createTime": "",
    "delFlag": "",
    "email": "",
    "loginDate": "",
    "loginIp": "",
    "nickName": "",
    "params": {},
    "password": "",
    "phonenumber": "",
    "platIds": [],
    "platformIds": "",
    "platformSelect": "",
    "remark": "",
    "roleIds": [],
    "sex": "",
    "status": "",
    "updateBy": "",
    "updateTime": "",
    "userId": 0,
    "userName": "",
    "userPlatformList": [
        {
            "elgPlatformId": "",
            "platformName": ""

## Page 33

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
user 用户信息表 body true SysUserPO 对象 SysUserPO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
admin  body false boolean  
avatar 头像地址 body false string  
companyAcronym承保机构名称缩写 body false string  
companyId 承保机构 id body false string  
companyName 承保机构名称 body false string  
createBy 创建者 body false string  
createTime 创建时间 body false string(date-time) 
delFlag 删除标志（ 0 代表存在  2 代表删除） body false string  
email 用户邮箱 body false string  
loginDate 最后登录时间 body false string(date-time) 
loginIp 最后登录 IP body false string  
nickName 用户昵称 body false string  
params  body false object  
password  body false string  
phonenumber 手机号码 body false string  
platIds  body false array  
platformIds 平台集合 body false string  
platformSelect 是否显示平台下拉： 1 显示， 2 不显示 body false string  
remark 备注 body false string  
roleIds  body false array  
请求参数：
schema 属性说明
 
SysUserPO 对象
        }
    ],
    "userType": ""
}

## Page 34

参数名称 参数说明 in 是否必须 数据类型 schema
sex 用户性别（ 0 男  1 女  2 未知） body false string  
status 帐号状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
updateTime 更新时间 body false string(date-time) 
userId 用户 ID body false integer(int64)  
userName 用户账号 body false string  
userPlatformList 平台下拉集合 body false array UserPlatformBO
userType 用户类型： 1 管理员， 2 担保公司 body false string  
参数名称 参数说明 in 是否必须 数据类型 schema
elgPlatformId  body false string  
platformName  body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
UserPlatformBO
响应示例:
响应参数:
暂无
 
 
响应状态:
edit  
接口描述:
接口地址:/system/user
 

## Page 35

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
user 用户信息表 body true SysUserPO 对象 SysUserPO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
admin  body false boolean  
请求方式：PUT
consumes:["application/json"]
produces:["*/*"]
请求示例：
请求参数：
schema 属性说明
 
SysUserPO 对象
{
    "admin": true,
    "avatar": "",
    "companyAcronym": "",
    "companyId": "",
    "companyName": "",
    "createBy": "",
    "createTime": "",
    "delFlag": "",
    "email": "",
    "loginDate": "",
    "loginIp": "",
    "nickName": "",
    "params": {},
    "password": "",
    "phonenumber": "",
    "platIds": [],
    "platformIds": "",
    "platformSelect": "",
    "remark": "",
    "roleIds": [],
    "sex": "",
    "status": "",
    "updateBy": "",
    "updateTime": "",
    "userId": 0,
    "userName": "",
    "userPlatformList": [
        {
            "elgPlatformId": "",
            "platformName": ""
        }
    ],
    "userType": ""
}

## Page 36

参数名称 参数说明 in 是否必须 数据类型 schema
avatar 头像地址 body false string  
companyAcronym承保机构名称缩写 body false string  
companyId 承保机构 id body false string  
companyName 承保机构名称 body false string  
createBy 创建者 body false string  
createTime 创建时间 body false string(date-time) 
delFlag 删除标志（ 0 代表存在  2 代表删除） body false string  
email 用户邮箱 body false string  
loginDate 最后登录时间 body false string(date-time) 
loginIp 最后登录 IP body false string  
nickName 用户昵称 body false string  
params  body false object  
password  body false string  
phonenumber 手机号码 body false string  
platIds  body false array  
platformIds 平台集合 body false string  
platformSelect 是否显示平台下拉： 1 显示， 2 不显示 body false string  
remark 备注 body false string  
roleIds  body false array  
sex 用户性别（ 0 男  1 女  2 未知） body false string  
status 帐号状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
updateTime 更新时间 body false string(date-time) 
userId 用户 ID body false integer(int64)  
userName 用户账号 body false string  
userPlatformList 平台下拉集合 body false array UserPlatformBO
userType 用户类型： 1 管理员， 2 担保公司 body false string  
参数名称 参数说明 in 是否必须 数据类型 schema
elgPlatformId  body false string  
UserPlatformBO

## Page 37

参数名称 参数说明 in 是否必须 数据类型 schema
platformName  body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
changeStatus  
接口描述:
接口地址:/system/user/changeStatus
请求方式：PUT
consumes:["application/json"]
produces:["*/*"]
请求示例：
 
{
    "admin": true,
    "avatar": "",
    "companyAcronym": "",
    "companyId": "",
    "companyName": "",
    "createBy": "",
    "createTime": "",
    "delFlag": "",
    "email": "",
    "loginDate": "",
    "loginIp": "",
    "nickName": "",
    "params": {},
    "password": "",

## Page 38

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
user 用户信息表 body true SysUserPO 对象 SysUserPO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
admin  body false boolean  
avatar 头像地址 body false string  
companyAcronym承保机构名称缩写 body false string  
companyId 承保机构 id body false string  
companyName 承保机构名称 body false string  
createBy 创建者 body false string  
createTime 创建时间 body false string(date-time) 
delFlag 删除标志（ 0 代表存在  2 代表删除） body false string  
email 用户邮箱 body false string  
loginDate 最后登录时间 body false string(date-time) 
loginIp 最后登录 IP body false string  
nickName 用户昵称 body false string  
请求参数：
schema 属性说明
 
SysUserPO 对象
    "phonenumber": "",
    "platIds": [],
    "platformIds": "",
    "platformSelect": "",
    "remark": "",
    "roleIds": [],
    "sex": "",
    "status": "",
    "updateBy": "",
    "updateTime": "",
    "userId": 0,
    "userName": "",
    "userPlatformList": [
        {
            "elgPlatformId": "",
            "platformName": ""
        }
    ],
    "userType": ""
}

## Page 39

参数名称 参数说明 in 是否必须 数据类型 schema
params  body false object  
password  body false string  
phonenumber 手机号码 body false string  
platIds  body false array  
platformIds 平台集合 body false string  
platformSelect 是否显示平台下拉： 1 显示， 2 不显示 body false string  
remark 备注 body false string  
roleIds  body false array  
sex 用户性别（ 0 男  1 女  2 未知） body false string  
status 帐号状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
updateTime 更新时间 body false string(date-time) 
userId 用户 ID body false integer(int64)  
userName 用户账号 body false string  
userPlatformList 平台下拉集合 body false array UserPlatformBO
userType 用户类型： 1 管理员， 2 担保公司 body false string  
参数名称 参数说明 in 是否必须 数据类型 schema
elgPlatformId  body false string  
platformName  body false string  
状态码 说明 schema
200 OK  
UserPlatformBO
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 40

状态码 说明 schema
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
userId userId query false integer  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
getInfo  
接口描述:
接口地址:/system/user/info
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 41

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
admin  query false boolean  
avatar 头像地址 query false string  
companyAcronym 承保机构名称缩写 query false string  
companyId 承保机构 id query false string  
companyName 承保机构名称 query false string  
createBy 创建者 query false string  
createTime 创建时间 query false string  
delFlag 删除标志（ 0 代表存在  2 代表删除） query false string  
email 用户邮箱 query false string  
loginDate 最后登录时间 query false string  
loginIp 最后登录 IP query false string  
nickName 用户昵称 query false string  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
params  query false object  
password 密码 query false string  
phonenumber 手机号码 query false string  
platIds  query false array string
platformIds 平台集合 query false string  
platformSelect 是否显示平台下拉： 1 显示， 2 不显示 query false string  
list  
接口描述:
接口地址:/system/user/list
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：

## Page 42

参数名称 参数说明 in 是否必须 数据类型 schemaremark 备注 query false string  
roleIds  query false array integer
sex 用户性别（ 0 男  1 女  2 未知） query false string  
status 帐号状态（ 0 正常  1 停用） query false string  
updateBy 更新者 query false string  
updateTime 更新时间 query false string  
userId 用户 ID query false integer  
userName 用户账号 query false string  
userPlatformList[0].elgPlatformId query false string  
userPlatformList[0].platformName query false string  
userType 用户类型： 1 管理员， 2 担保公司 query false string  
参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  object  
total 总记录数 integer(int64) integer(int64)
状态码 说明 schema
200 OK TableDataInfo
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
 
 
响应状态:
{
    "code": 0,
    "msg": "",
    "rows": {},
    "total": 0
}

## Page 43

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
user 用户信息表 body true SysUserPO 对象 SysUserPO 对象
resetPwd  
接口描述:
接口地址:/system/user/resetPwd
请求方式：PUT
consumes:["application/json"]
produces:["*/*"]
请求示例：
请求参数：
schema 属性说明
 
{
    "admin": true,
    "avatar": "",
    "companyAcronym": "",
    "companyId": "",
    "companyName": "",
    "createBy": "",
    "createTime": "",
    "delFlag": "",
    "email": "",
    "loginDate": "",
    "loginIp": "",
    "nickName": "",
    "params": {},
    "password": "",
    "phonenumber": "",
    "platIds": [],
    "platformIds": "",
    "platformSelect": "",
    "remark": "",
    "roleIds": [],
    "sex": "",
    "status": "",
    "updateBy": "",
    "updateTime": "",
    "userId": 0,
    "userName": "",
    "userPlatformList": [
        {
            "elgPlatformId": "",
            "platformName": ""
        }
    ],
    "userType": ""
}

## Page 44

参数名称 参数说明 in 是否必须 数据类型 schema
admin  body false boolean  
avatar 头像地址 body false string  
companyAcronym承保机构名称缩写 body false string  
companyId 承保机构 id body false string  
companyName 承保机构名称 body false string  
createBy 创建者 body false string  
createTime 创建时间 body false string(date-time) 
delFlag 删除标志（ 0 代表存在  2 代表删除） body false string  
email 用户邮箱 body false string  
loginDate 最后登录时间 body false string(date-time) 
loginIp 最后登录 IP body false string  
nickName 用户昵称 body false string  
params  body false object  
password  body false string  
phonenumber 手机号码 body false string  
platIds  body false array  
platformIds 平台集合 body false string  
platformSelect 是否显示平台下拉： 1 显示， 2 不显示 body false string  
remark 备注 body false string  
roleIds  body false array  
sex 用户性别（ 0 男  1 女  2 未知） body false string  
status 帐号状态（ 0 正常  1 停用） body false string  
updateBy 更新者 body false string  
updateTime 更新时间 body false string(date-time) 
userId 用户 ID body false integer(int64)  
userName 用户账号 body false string  
userPlatformList 平台下拉集合 body false array UserPlatformBO
userType 用户类型： 1 管理员， 2 担保公司 body false string  
SysUserPO 对象
UserPlatformBO

## Page 45

参数名称 参数说明 in 是否必须 数据类型 schema参数名称 参数说明 in 是否必须 数据类型 schema
elgPlatformId  body false string  
platformName  body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
userIds userIds path true string  
响应示例:
响应参数:
暂无
 
 
响应状态:
remove  
接口描述:
接口地址:/system/user/{userIds}
请求方式：DELETE
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
 
 

## Page 46

状态码 说明 schema
200 OK  
204 No Content  
401 Unauthorized  
403 Forbidden  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
ipaddr ipaddr query false string  
userName userName query false string  
响应参数:
暂无
 
 
响应状态:
sys-user-online-controller  
list  
接口描述:
接口地址:/system/monitor/online/list
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
{
    "code": 0,
    "msg": "",
    "rows": {},
    "total": 0
}

## Page 47

参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  object  
total 总记录数 integer(int64) integer(int64)
状态码 说明 schema
200 OK TableDataInfo
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
tokenId tokenId path true string  
 
 
响应状态:
forceLogout  
接口描述:
接口地址:/system/monitor/online/{tokenId}
请求方式：DELETE
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
 

## Page 48

状态码 说明 schema
200 OK  
204 No Content  
401 Unauthorized  
403 Forbidden  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
下载文件路径 url body true string  
状态码 说明 schema
200 OK  
401 Unauthorized  
响应状态:
公共文件接口  
下载文件  
接口描述:
接口地址:/system/common/file/down
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 49

状态码 说明 schema
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
查看文件路径 url body true string  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
展示文件  
接口描述:
接口地址:/system/common/file/show
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
上传文件  
接口描述:
 

## Page 50

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
file file formData true file  
type type query false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
接口地址:/system/common/file/upload
请求方式：POST
consumes:["multipart/form-data"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
发票后台相关功能操作接口 
开具蓝票普票 - 调用接口  
接口描述:
接口地址:/system/invoice/blueInvoiceApply
请求方式：POST
 

## Page 51

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
id 发票 id body false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
开具蓝票专票 - 邮寄  
接口描述:
接口地址:/system/invoice/blueInvoiceSpecial
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
 

## Page 52

参数名称 参数说明 in 是否必须 数据类型 schema
expressCode expressCode query false string  
id 发票 id body false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyId 承保机构 ID query false string  
corpName 企业名称 query false string  
createTimeEnd 发票申请时间 query false string  
createTimeStart 发票申请时间 query false string  
响应示例:
响应参数:
暂无
 
 
响应状态:
发票导出  
接口描述:
接口地址:/system/invoice/export
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
 

## Page 53

参数名称 参数说明 in 是否必须 数据类型 schema
elgPlatformId 平台 ID query false string  
id  query false integer  
invoiceType 发票类型【 0= 电子发票， 1= 增值税发票】 query false string  
orderCode 订单编号 query false string  
receiveMobile 联系电话 query false string  
redStatus 红票发票状态 query false string  
refundStatus 0= 未申请 ,1= 待审核 ,2= 审核不通过 ,3= 审核通过 query false string  
status 蓝票发票状态 query false string  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
响应示例:
响应参数:
暂无
 
 
响应状态:
获取发票详情  
接口描述:
接口地址:/system/invoice/getData
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
 

## Page 54

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
invoiceId 发票 id body false integer  
参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
data  InvoiceInfoDetails InvoiceInfoDetails
msg 消息内容 string  
响应示例:
响应参数:
 
schema 属性说明
 
{
    "code": 0,
    "data": {
        "corpName": "",
        "createTime": "",
        "elgPlatformName": "",
        "expressCode": "",
        "expressCompany": "",
        "fpdm": "",
        "fphm": "",
        "id": 0,
        "invoiceAmount": "",
        "invoiceCode": "",
        "invoiceDesc": "",
        "invoicePath": "",
        "invoiceTitle": "",
        "invoiceType": "",
        "orderCode": "",
        "ratepayerNum": "",
        "receiveAddress": "",
        "receiveCity": "",
        "receiveCountry": "",
        "receiveEmail": "",
        "receiveMobile": "",
        "receiveProvince": "",
        "receiveUserName": "",
        "redInvoiceCode": "",
        "redInvoicePath": "",
        "redStatus": "",
        "region": "",
        "socialCreditCode": "",
        "status": "",
        "underwritingCompanyName": "",
        "updateTime": ""
    },
    "msg": ""
}

## Page 55

参数名称 参数说明 类型 schema
corpName 企业名称 string  
createTime 创建时间 string(date-time)  
elgPlatformName 平台名称 string  
expressCode 快递单号 string  
expressCompany 快递公司 string  
fpdm 发票代码 string  
fphm 发票号码 string  
id id integer(int32)  
invoiceAmount 发票金额 string  
invoiceCode 蓝票发票唯一码 string  
invoiceDesc 发票描述，用户自定义描述 string  
invoicePath 蓝票存储地址 string  
invoiceTitle 发票抬头，可填写单位地址 string  
invoiceType 发票类型【 0= 电子发票， 1= 增值税发票】 string  
orderCode 订单编号 string  
ratepayerNum 纳税人识别号 string  
receiveAddress 详细地址 string  
receiveCity 市 ( 编 ) string  
receiveCountry 区 ( 编码 ) string  
receiveEmail 接收发票用户邮件地址 string  
receiveMobile 接收发票用户电话号码 string  
receiveProvince 省 ( 编码 ) string  
receiveUserName 接收发票用户姓名 string  
redInvoiceCode 红票发票唯一码 string  
redInvoicePath 红票存储地址 string  
redStatus 红票发票状态 string  
region 省市区 ( 名称 ) string  
socialCreditCode 统一社会信用代码 string  
status 蓝票发票状态 string  
InvoiceInfoDetails

## Page 56

参数名称 参数说明 类型 schema
underwritingCompanyName 承保机构名称 string  
updateTime 更新时间 string(date-time)  
状态码 说明 schema
200 OK DataInfo«InvoiceInfoDetails»
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
id 发票 id body false integer  
状态码 说明 schema
响应状态:
查询发票结果  
接口描述:
接口地址:/system/invoice/invoiceQuery
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 57

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyId 承保机构 ID query false string  
corpName 企业名称 query false string  
createTimeEnd 发票申请时间 query false string  
createTimeStart 发票申请时间 query false string  
elgPlatformId 平台 ID query false string  
id  query false integer  
invoiceType 发票类型【 0= 电子发票， 1= 增值税发票】 query false string  
orderCode 订单编号 query false string  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
receiveMobile 联系电话 query false string  
redStatus 红票发票状态 query false string  
refundStatus 0= 未申请 ,1= 待审核 ,2= 审核不通过 ,3= 审核通过 query false string  
status 蓝票发票状态 query false string  
分页查询发票列表  
接口描述:
接口地址:/system/invoice/list
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：

## Page 58

参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  InvoiceInfoPageVO InvoiceInfoPageVO
total 总记录数 integer(int64) integer(int64)
参数名称 参数说明 类型 schema
companyId 承保机构 ID string  
corpName 企业名称 string  
createTime 创建时间 string(date-time) 
elgPlatformId 平台 ID string  
elgPlatformName 平台名称 string  
id  integer(int32)  
invoiceAmount 发票金额 string  
响应示例:
响应参数:
 
schema 属性说明
 
InvoiceInfoPageVO
{
    "code": 0,
    "msg": "",
    "rows": {
        "companyId": "",
        "corpName": "",
        "createTime": "",
        "elgPlatformId": "",
        "elgPlatformName": "",
        "id": 0,
        "invoiceAmount": "",
        "invoiceChannel": "",
        "invoiceType": "",
        "orderCode": "",
        "receiveMobile": "",
        "redStatus": "",
        "refundStatus": "",
        "status": "",
        "underwritingCompanyName": ""
    },
    "total": 0
}

## Page 59

参数名称 参数说明 类型 schema
invoiceChannel 普票开票渠道： 1 线下上传， 2 外部接口， 3 票点点 string  
invoiceType 发票类型【 0= 电子发票， 1= 增值税发票】 string  
orderCode 订单编号 string  
receiveMobile 联系电话 string  
redStatus 红票状态 string  
refundStatus 0= 未申请 ,1= 待审核 ,2= 审核不通过 ,3= 审核通过 string  
status 蓝票状态 string  
underwritingCompanyName承保机构名称 string  
状态码 说明 schema
200 OK TableDataInfo«InvoiceInfoPageVO»
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
id 发票 id body false integer  
响应状态:
普票冲红  
接口描述:
接口地址:/system/invoice/redInvoiceApply
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
 

## Page 60

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
id 发票 id body false integer  
状态码 说明 schema
响应参数:
暂无
 
 
响应状态:
专票冲红  
接口描述:
接口地址:/system/invoice/redInvoiceSpecial
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 61

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
file 支付凭证 formData false file  
id 发票 id body false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
上传发票  
接口描述:
接口地址:/system/invoice/uploadPayCertificate
请求方式：POST
consumes:["multipart/form-data"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 62

状态码 说明 schema
404 Not Found  
平台与担保公司配置后台相关功能操作接
口
 
添加  
接口描述:
接口地址:/system/platformCompany/add
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
{
    "accountName": "",
    "bankAccount": "",
    "bankName": "",
    "commitmentFilePath": "",
    "companyId": "",
    "createTime": "",
    "createTimeEnd": "",
    "createTimeStart": "",
    "dbAppId": "",
    "dbMyPrivateKey": "",
    "dbMyPublicKey": "",
    "dbPublicKey": "",
    "dbRequestUrl": "",
    "elgPlatformId": "",
    "entrustFilePath": "",
    "id": 0,
    "invoiceAutoSwitch": "",
    "invoiceChannel": "",
    "invoiceConfigJson": "",
    "orderAuditSwitch": "",
    "payChannelSwitch": "",
    "payWay": "",
    "premiumMin": "",
    "premiumRate": "",
    "productId": "",
    "projectExpireDate": "",
    "pushBankSwitch": "",
    "pushDbResult": "",
    "specialInvoiceChannel": "",
    "updateTime": "",
    "wxAppId": "",
    "wxKeyPath": "",

## Page 63

参数名称 参数说明 in
是否
必须 数据类型 schema
platformCompanyConfigPO平台和承保机
构配置表
body true PlatformCompanyConfigPO
对象
PlatformCompanyConfigPO
对象
Authorization Authorization header false string  
参数名称 参数说明 in
是否必
须 数据类型 schema
accountName 收款账户名称 body false string  
bankAccount 收款开户行账号 body false string  
bankName 收款开户行 body false string  
commitmentFilePath出具保函承诺书模板 body false string  
companyId 承保机构 ID body false string  
createTime 创建时间 body false string(date-
time)
 
createTimeEnd 平台创建时间 body false string  
createTimeStart 平台创建时间 body false string  
dbAppId 担保 appId body false string  
dbMyPrivateKey 担保我方私钥 body false string  
dbMyPublicKey 担保我方公钥 body false string  
dbPublicKey 担保方公钥 body false string  
dbRequestUrl 担保请求地址 body false string  
elgPlatformId 平台 ID body false string  
entrustFilePath 委托担保协议模板 body false string  
id 主键 id body false integer(int64) 
请求参数：
schema 属性说明
 
PlatformCompanyConfigPO 对象
    "wxMchId": "",
    "wxMchKey": "",
    "wxPrivateCertPath": "",
    "wxPrivateKeyPath": "",
    "zfbAliPayPublicKey": "",
    "zfbAppId": "",
    "zfbPrivateKey": ""
}

## Page 64

参数名称 参数说明 in
是否必
须 数据类型 schema
invoiceAutoSwitch 出函后自动申请发票开关： 1 开， 2 关 body false string  
invoiceChannel 普票开票渠道： 2 外部接口， 3 票点点 body false string  
invoiceConfigJson 发票参数配置 JSON body false string  
orderAuditSwitch 订单审核类型： 1 自动， 2 人工， 3 外部接口 body false string  
payChannelSwitch 支付渠道： 1 内部支付， 2 外部支付 body false string  
payWay 支付方式（ 1- 微信， 2- 支付宝， 3- 银行转账， 4- 工行支
付  可多选）
body false string  
premiumMin 最低保费 body false string  
premiumRate 保费费率 body false string  
productId 产品 id body false string  
projectExpireDate 保函有效期 ( 天数 ) body false string  
pushBankSwitch 支付后自动推送银行开关： 1 开， 2 关 body false string  
pushDbResult 出函结果通知担保公司 :1 不通知， 2 通知 body false string  
specialInvoiceChannel专票开票渠道： 1 线下邮寄， 2 外部接口 body false string  
updateTime 更新时间 body false string(date-
time)
 
wxAppId 微信 appId body false string  
wxKeyPath 微信 key 证书存储地址 body false string  
wxMchId 微信商户号 body false string  
wxMchKey 微信商户 key body false string  
wxPrivateCertPath 微信 privateCert 证书存储地址 body false string  
wxPrivateKeyPath 微信 privateKey 证书存储地址 body false string  
zfbAliPayPublicKey支付宝公钥 body false string  
zfbAppId 支付宝 appId body false string  
zfbPrivateKey 支付宝应用私钥 body false string  
响应示例:
响应参数:
暂无
 
 
 

## Page 65

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应状态:
修改  
接口描述:
接口地址:/system/platformCompany/edit
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
{
    "accountName": "",
    "bankAccount": "",
    "bankName": "",
    "commitmentFilePath": "",
    "companyId": "",
    "createTime": "",
    "createTimeEnd": "",
    "createTimeStart": "",
    "dbAppId": "",
    "dbMyPrivateKey": "",
    "dbMyPublicKey": "",
    "dbPublicKey": "",
    "dbRequestUrl": "",
    "elgPlatformId": "",
    "entrustFilePath": "",
    "id": 0,
    "invoiceAutoSwitch": "",
    "invoiceChannel": "",
    "invoiceConfigJson": "",
    "orderAuditSwitch": "",
    "payChannelSwitch": "",
    "payWay": "",
    "premiumMin": "",
    "premiumRate": "",
    "productId": "",
    "projectExpireDate": "",
    "pushBankSwitch": "",
    "pushDbResult": "",
    "specialInvoiceChannel": "",
    "updateTime": "",
    "wxAppId": "",

## Page 66

参数名称 参数说明 in
是否
必须 数据类型 schema
Authorization Authorization header false string  
platformCompanyConfigPO平台和承保机
构配置表
body true PlatformCompanyConfigPO
对象
PlatformCompanyConfigPO
对象
参数名称 参数说明 in
是否必
须 数据类型 schema
accountName 收款账户名称 body false string  
bankAccount 收款开户行账号 body false string  
bankName 收款开户行 body false string  
commitmentFilePath出具保函承诺书模板 body false string  
companyId 承保机构 ID body false string  
createTime 创建时间 body false string(date-
time)
 
createTimeEnd 平台创建时间 body false string  
createTimeStart 平台创建时间 body false string  
dbAppId 担保 appId body false string  
dbMyPrivateKey 担保我方私钥 body false string  
dbMyPublicKey 担保我方公钥 body false string  
dbPublicKey 担保方公钥 body false string  
dbRequestUrl 担保请求地址 body false string  
elgPlatformId 平台 ID body false string  
entrustFilePath 委托担保协议模板 body false string  
id 主键 id body false integer(int64) 
请求参数：
schema 属性说明
 
PlatformCompanyConfigPO 对象
    "wxKeyPath": "",
    "wxMchId": "",
    "wxMchKey": "",
    "wxPrivateCertPath": "",
    "wxPrivateKeyPath": "",
    "zfbAliPayPublicKey": "",
    "zfbAppId": "",
    "zfbPrivateKey": ""
}

## Page 67

参数名称 参数说明 in
是否必
须 数据类型 schema
invoiceAutoSwitch 出函后自动申请发票开关： 1 开， 2 关 body false string  
invoiceChannel 普票开票渠道： 2 外部接口， 3 票点点 body false string  
invoiceConfigJson 发票参数配置 JSON body false string  
orderAuditSwitch 订单审核类型： 1 自动， 2 人工， 3 外部接口 body false string  
payChannelSwitch 支付渠道： 1 内部支付， 2 外部支付 body false string  
payWay 支付方式（ 1- 微信， 2- 支付宝， 3- 银行转账， 4- 工行支
付  可多选）
body false string  
premiumMin 最低保费 body false string  
premiumRate 保费费率 body false string  
productId 产品 id body false string  
projectExpireDate 保函有效期 ( 天数 ) body false string  
pushBankSwitch 支付后自动推送银行开关： 1 开， 2 关 body false string  
pushDbResult 出函结果通知担保公司 :1 不通知， 2 通知 body false string  
specialInvoiceChannel专票开票渠道： 1 线下邮寄， 2 外部接口 body false string  
updateTime 更新时间 body false string(date-
time)
 
wxAppId 微信 appId body false string  
wxKeyPath 微信 key 证书存储地址 body false string  
wxMchId 微信商户号 body false string  
wxMchKey 微信商户 key body false string  
wxPrivateCertPath 微信 privateCert 证书存储地址 body false string  
wxPrivateKeyPath 微信 privateKey 证书存储地址 body false string  
zfbAliPayPublicKey支付宝公钥 body false string  
zfbAppId 支付宝 appId body false string  
zfbPrivateKey 支付宝应用私钥 body false string  
响应示例:
响应参数:
暂无
 
 
 

## Page 68

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
id id query false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
响应状态:
查询平台与担保公司配置详情  
接口描述:
接口地址:/system/platformCompany/getData
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 69

状态码 说明 schema
404 Not Found  
参数名称 参数说明 in
是否必
须
数据类
型 schema
Authorization Authorization header false string  
accountName 收款账户名称 query false string  
bankAccount 收款开户行账号 query false string  
bankName 收款开户行 query false string  
commitmentFilePath出具保函承诺书模板 query false string  
companyId 承保机构 ID query false string  
createTime 创建时间 query false string  
createTimeEnd 平台创建时间 query false string  
createTimeStart 平台创建时间 query false string  
dbAppId 担保 appId query false string  
dbMyPrivateKey 担保我方私钥 query false string  
dbMyPublicKey 担保我方公钥 query false string  
dbPublicKey 担保方公钥 query false string  
dbRequestUrl 担保请求地址 query false string  
elgPlatformId 平台 ID query false string  
entrustFilePath 委托担保协议模板 query false string  
id 主键 id query false integer  
invoiceAutoSwitch 出函后自动申请发票开关： 1 开， 2 关 query false string  
invoiceChannel 普票开票渠道： 2 外部接口， 3 票点点 query false string  
分页查询平台与担保公司配置列表  
接口描述:
接口地址:/system/platformCompany/list
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：

## Page 70

参数名称 参数说明 in
是否必
须
数据类
型 schema
invoiceConfigJson 发票参数配置 JSON query false string  
orderAuditSwitch 订单审核类型： 1 自动， 2 人工， 3 外部接口 query false string  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
payChannelSwitch 支付渠道： 1 内部支付， 2 外部支付 query false string  
payWay 支付方式（ 1- 微信， 2- 支付宝， 3- 银行转账， 4- 工行支付
可多选）
query false string  
premiumMin 最低保费 query false string  
premiumRate 保费费率 query false string  
productId 产品 id query false string  
projectExpireDate 保函有效期 ( 天数 ) query false string  
pushBankSwitch 支付后自动推送银行开关： 1 开， 2 关 query false string  
pushDbResult 出函结果通知担保公司 :1 不通知， 2 通知 query false string  
specialInvoiceChannel专票开票渠道： 1 线下邮寄， 2 外部接口 query false string  
updateTime 更新时间 query false string  
wxAppId 微信 appId query false string  
wxKeyPath 微信 key 证书存储地址 query false string  
wxMchId 微信商户号 query false string  
wxMchKey 微信商户 key query false string  
wxPrivateCertPath 微信 privateCert 证书存储地址 query false string  
wxPrivateKeyPath 微信 privateKey 证书存储地址 query false string  
zfbAliPayPublicKey支付宝公钥 query false string  
zfbAppId 支付宝 appId query false string  
zfbPrivateKey 支付宝应用私钥 query false string  
响应示例:

## Page 71

参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  PlatformInfoPageVO PlatformInfoPageVO
total 总记录数 integer(int64) integer(int64)
参数名称 参数说明 类型 schema
agencyName 出函机构名称 string  
createTime 创建时间 string(date-time)  
elgPlatformId 平台 ID string  
id  integer(int32)  
isBasicPay 是否基本户支付（ 0- 否， 1 是） string  
platformName 平台名称 string  
状态码 说明 schema
200 OK TableDataInfo«PlatformInfoPageVO»
201 Created  
401 Unauthorized  
403 Forbidden  
响应参数:
 
schema 属性说明
 
PlatformInfoPageVO
响应状态:
{
    "code": 0,
    "msg": "",
    "rows": {
        "agencyName": "",
        "createTime": "",
        "elgPlatformId": "",
        "id": 0,
        "isBasicPay": "",
        "platformName": ""
    },
    "total": 0
}

## Page 72

状态码 说明 schema
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
id id query false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
删除  
接口描述:
接口地址:/system/platformCompany/remove
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 73

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
platformInfoPO 平台配置信息 body true PlatformInfoPO 对象 PlatformInfoPO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
agencyName 出函机构名称 body false string  
centerName 交易中心名称 body false string  
channelNo 渠道号 body false string  
createTime 创建时间 body false string(date-time)  
平台后台相关功能操作接口 
添加平台  
接口描述:
接口地址:/system/platform/add
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
请求参数：
schema 属性说明
 
PlatformInfoPO 对象
{
    "agencyName": "",
    "centerName": "",
    "channelNo": "",
    "createTime": "",
    "createTimeEnd": "",
    "createTimeStart": "",
    "elgPlatformId": "",
    "guaranteeTemplateFileId": "",
    "guaranteeTemplateFileName": "",
    "id": 0,
    "marginAmountLimit": "",
    "platformName": "",
    "remark": "",
    "updateTime": "",
    "xibCallbackUrl": ""
}

## Page 74

参数名称 参数说明 in 是否必须 数据类型 schema
createTimeEnd 平台创建时间 body false string  
createTimeStart 平台创建时间 body false string  
elgPlatformId 平台 ID （交易中心编码） body false string  
guaranteeTemplateFileId 银行保函模板 ID body false string  
guaranteeTemplateFileName 银行保函模板名称 body false string  
id 主键 id body false integer(int32)  
marginAmountLimit 开函限制金额 body false string  
platformName 平台名称 body false string  
remark 备注 body false string  
updateTime 更新时间 body false string(date-time)  
xibCallbackUrl 银行回调地址 body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
修改平台  
接口描述:
接口地址:/system/platform/edit
请求方式：POST
consumes:["application/json"]
 

## Page 75

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
platformInfoPO 平台配置信息 body true PlatformInfoPO 对象 PlatformInfoPO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
agencyName 出函机构名称 body false string  
centerName 交易中心名称 body false string  
channelNo 渠道号 body false string  
createTime 创建时间 body false string(date-time)  
createTimeEnd 平台创建时间 body false string  
createTimeStart 平台创建时间 body false string  
elgPlatformId 平台 ID （交易中心编码） body false string  
guaranteeTemplateFileId 银行保函模板 ID body false string  
guaranteeTemplateFileName 银行保函模板名称 body false string  
id 主键 id body false integer(int32)  
marginAmountLimit 开函限制金额 body false string  
platformName 平台名称 body false string  
produces:["*/*"]
请求示例：
请求参数：
schema 属性说明
 
PlatformInfoPO 对象
{
    "agencyName": "",
    "centerName": "",
    "channelNo": "",
    "createTime": "",
    "createTimeEnd": "",
    "createTimeStart": "",
    "elgPlatformId": "",
    "guaranteeTemplateFileId": "",
    "guaranteeTemplateFileName": "",
    "id": 0,
    "marginAmountLimit": "",
    "platformName": "",
    "remark": "",
    "updateTime": "",
    "xibCallbackUrl": ""
}

## Page 76

参数名称 参数说明 in 是否必须 数据类型 schema
remark 备注 body false string  
updateTime 更新时间 body false string(date-time)  
xibCallbackUrl 银行回调地址 body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
platformCode id body false integer  
响应示例:
响应参数:
暂无
 
 
响应状态:
查询平台详情  
接口描述:
接口地址:/system/platform/getData
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
 

## Page 77

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
响应参数:
暂无
 
 
响应状态:
查询平台列表  
接口描述:
接口地址:/system/platform/getList
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 
 

## Page 78

状态码 说明 schema状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
agencyName 出函机构名称 query false string  
centerName 交易中心名称 query false string  
channelNo 渠道号 query false string  
createTime 创建时间 query false string  
createTimeEnd 平台创建时间 query false string  
createTimeStart 平台创建时间 query false string  
elgPlatformId 平台 ID （交易中心编码） query false string  
guaranteeTemplateFileId 银行保函模板 ID query false string  
guaranteeTemplateFileName 银行保函模板名称 query false string  
id 主键 id query false integer  
marginAmountLimit 开函限制金额 query false string  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
platformName 平台名称 query false string  
分页查询平台列表  
接口描述:
接口地址:/system/platform/list
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：

## Page 79

参数名称 参数说明 in 是否必须 数据类型 schema
remark 备注 query false string  
updateTime 更新时间 query false string  
xibCallbackUrl 银行回调地址 query false string  
参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  PlatformInfoPageVO PlatformInfoPageVO
total 总记录数 integer(int64) integer(int64)
参数名称 参数说明 类型 schema
agencyName 出函机构名称 string  
createTime 创建时间 string(date-time)  
elgPlatformId 平台 ID string  
id  integer(int32)  
isBasicPay 是否基本户支付（ 0- 否， 1 是） string  
platformName 平台名称 string  
响应示例:
响应参数:
 
schema 属性说明
 
PlatformInfoPageVO
响应状态:
{
    "code": 0,
    "msg": "",
    "rows": {
        "agencyName": "",
        "createTime": "",
        "elgPlatformId": "",
        "id": 0,
        "isBasicPay": "",
        "platformName": ""
    },
    "total": 0
}

## Page 80

状态码 说明 schema状态码 说明 schema
200 OK TableDataInfo«PlatformInfoPageVO»
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
elgPlatformId 平台 ID body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
删除平台  
接口描述:
接口地址:/system/platform/remove
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 81

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
sysWechatMessage微信短信通知表body true SysWechatMessagePO 对象 SysWechatMessagePO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
companyId 承保机构 ID body false string  
companyName 承保机构名称 body false string  
createTime 创建时间 body false string(date-time)  
createTimeEnd 搜索创建结束时间 body false string  
createTimeStart 搜索创建开始时间 body false string  
elgPlatformId 平台 id body false string  
微信通知后台相关功能操作接口 
添加微信通知  
接口描述:
接口地址:/system/wechatmess/add
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
请求参数：
schema 属性说明
 
SysWechatMessagePO 对象
{
    "companyId": "",
    "companyName": "",
    "createTime": "",
    "createTimeEnd": "",
    "createTimeStart": "",
    "elgPlatformId": "",
    "id": 0,
    "openId": "",
    "platformName": "",
    "recipientName": "",
    "type": "",
    "updateTime": ""
}

## Page 82

参数名称 参数说明 in 是否必须 数据类型 schema
id 主键 body false integer(int32)  
openId 微信 openid body false string  
platformName 平台名称 body false string  
recipientName 接收人名称 body false string  
type 通知人员类型： 1 管理员， 2 担保公司 body false string  
updateTime 更新时间 body false string(date-time)  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
修改微信通知  
接口描述:
接口地址:/system/wechatmess/edit
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
 
{
    "companyId": "",
    "companyName": "",
    "createTime": "",
    "createTimeEnd": "",

## Page 83

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
sysWechatMessage微信短信通知表body true SysWechatMessagePO 对象 SysWechatMessagePO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
companyId 承保机构 ID body false string  
companyName 承保机构名称 body false string  
createTime 创建时间 body false string(date-time)  
createTimeEnd 搜索创建结束时间 body false string  
createTimeStart 搜索创建开始时间 body false string  
elgPlatformId 平台 id body false string  
id 主键 body false integer(int32)  
openId 微信 openid body false string  
platformName 平台名称 body false string  
recipientName 接收人名称 body false string  
type 通知人员类型： 1 管理员， 2 担保公司 body false string  
updateTime 更新时间 body false string(date-time)  
请求参数：
schema 属性说明
 
SysWechatMessagePO 对象
响应示例:
响应参数:
暂无
 
 
    "createTimeStart": "",
    "elgPlatformId": "",
    "id": 0,
    "openId": "",
    "platformName": "",
    "recipientName": "",
    "type": "",
    "updateTime": ""
}
 

## Page 84

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
id id query false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
响应状态:
查询微信通知详情  
接口描述:
接口地址:/system/wechatmess/getData
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 85

状态码 说明 schema
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyId 承保机构 ID query false string  
companyName 承保机构名称 query false string  
createTime 创建时间 query false string  
createTimeEnd 搜索创建结束时间 query false string  
createTimeStart 搜索创建开始时间 query false string  
elgPlatformId 平台 id query false string  
id 主键 query false integer  
openId 微信 openid query false string  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
platformName 平台名称 query false string  
recipientName 接收人名称 query false string  
type 通知人员类型： 1 管理员， 2 担保公司 query false string  
updateTime 更新时间 query false string  
分页查询微信通知列表  
接口描述:
接口地址:/system/wechatmess/list
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
{
    "code": 0,
    "msg": "",
    "rows": {
        "companyId": "",

## Page 86

参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  SysWechatMessagePO 对象 SysWechatMessagePO 对象
total 总记录数 integer(int64) integer(int64)
参数名称 参数说明 类型 schema
companyId 承保机构 ID string  
companyName 承保机构名称 string  
createTime 创建时间 string(date-time)  
createTimeEnd 搜索创建结束时间 string  
createTimeStart 搜索创建开始时间 string  
elgPlatformId 平台 id string  
id 主键 integer(int32)  
openId 微信 openid string  
platformName 平台名称 string  
recipientName 接收人名称 string  
type 通知人员类型： 1 管理员， 2 担保公司 string  
updateTime 更新时间 string(date-time)  
响应参数:
 
schema 属性说明
 
SysWechatMessagePO 对象
        "companyName": "",
        "createTime": "",
        "createTimeEnd": "",
        "createTimeStart": "",
        "elgPlatformId": "",
        "id": 0,
        "openId": "",
        "platformName": "",
        "recipientName": "",
        "type": "",
        "updateTime": ""
    },
    "total": 0
}

## Page 87

状态码 说明 schema
200 OK TableDataInfo«SysWechatMessagePO 对象 »
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
id id query false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
响应状态:
删除微信通知  
接口描述:
接口地址:/system/wechatmess/remove
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 88

状态码 说明 schema
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyInfoPO 承保机构信息 body true CompanyInfoPO 对象 CompanyInfoPO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
companyAcronym 企业名称缩写 body false string  
companyCode 企业统一社会信用代码 body false string  
companyId 承保机构 ID body false string  
担保公司后台相关功能操作接口 
添加担保公司  
接口描述:
接口地址:/system/company/add
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
请求参数：
schema 属性说明
 
CompanyInfoPO 对象
{
    "companyAcronym": "",
    "companyCode": "",
    "companyId": "",
    "companyName": "",
    "createTime": "",
    "createTimeEnd": "",
    "createTimeStart": "",
    "id": 0,
    "remark": "",
    "updateTime": ""
}

## Page 89

参数名称 参数说明 in 是否必须 数据类型 schema
companyName 企业名称 body false string  
createTime 创建时间 body false string(date-time)  
createTimeEnd 搜索结束时间 body false string  
createTimeStart 搜索开始时间 body false string  
id 主键 id body false integer(int32)  
remark 备注 body false string  
updateTime 更新时间 body false string(date-time)  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
修改担保公司  
接口描述:
接口地址:/system/company/edit
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
请求示例：
 

## Page 90

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyInfoPO 承保机构信息 body true CompanyInfoPO 对象 CompanyInfoPO 对象
参数名称 参数说明 in 是否必须 数据类型 schema
companyAcronym 企业名称缩写 body false string  
companyCode 企业统一社会信用代码 body false string  
companyId 承保机构 ID body false string  
companyName 企业名称 body false string  
createTime 创建时间 body false string(date-time)  
createTimeEnd 搜索结束时间 body false string  
createTimeStart 搜索开始时间 body false string  
id 主键 id body false integer(int32)  
remark 备注 body false string  
updateTime 更新时间 body false string(date-time)  
请求参数：
schema 属性说明
 
CompanyInfoPO 对象
响应示例:
响应参数:
暂无
 
 
{
    "companyAcronym": "",
    "companyCode": "",
    "companyId": "",
    "companyName": "",
    "createTime": "",
    "createTimeEnd": "",
    "createTimeStart": "",
    "id": 0,
    "remark": "",
    "updateTime": ""
}
 

## Page 91

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyId id body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
响应状态:
查询担保公司详情  
接口描述:
接口地址:/system/company/getCompanyData
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 92

状态码 说明 schema
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
查询担保公司列表  
接口描述:
接口地址:/system/company/getCompanyList
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
分页查询担保公司列表  
接口描述:
接口地址:/system/company/list
 

## Page 93

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyAcronym 企业名称缩写 query false string  
companyCode 企业统一社会信用代码 query false string  
companyId 承保机构 ID query false string  
companyName 企业名称 query false string  
createTime 创建时间 query false string  
createTimeEnd 搜索结束时间 query false string  
createTimeStart 搜索开始时间 query false string  
id 主键 id query false integer  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
remark 备注 query false string  
updateTime 更新时间 query false string  
参数名称 参数说明 类型 schema
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
{
    "code": 0,
    "msg": "",
    "rows": {
        "companyAcronym": "",
        "companyCode": "",
        "companyId": "",
        "companyName": "",
        "createTime": "",
        "createTimeEnd": "",
        "createTimeStart": "",
        "id": 0,
        "remark": "",
        "updateTime": ""
    },
    "total": 0
}

## Page 94

参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  CompanyInfoPO 对象 CompanyInfoPO 对象
total 总记录数 integer(int64) integer(int64)
参数名称 参数说明 类型 schema
companyAcronym 企业名称缩写 string  
companyCode 企业统一社会信用代码 string  
companyId 承保机构 ID string  
companyName 企业名称 string  
createTime 创建时间 string(date-time)  
createTimeEnd 搜索结束时间 string  
createTimeStart 搜索开始时间 string  
id 主键 id integer(int32)  
remark 备注 string  
updateTime 更新时间 string(date-time)  
状态码 说明 schema
200 OK TableDataInfo«CompanyInfoPO 对象 »
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
 
schema 属性说明
 
CompanyInfoPO 对象
响应状态:
删除担保公司  
接口描述:

## Page 95

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyId 承保机构 ID body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
接口地址:/system/company/remove
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
理赔后台相关功能操作接口 
导出  
接口描述:
接口地址:/system/claim/export
请求方式：GET
consumes:``
produces:["*/*"]
 

## Page 96

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
bidName 标段名称 query false string  
bidUnitName 投标单位名称 query false string  
companyId 承保机构 ID query false string  
contactMobile 被保人联系人电话 query false string  
contactName 被保人联系人姓名 query false string  
createTimeEnd 索赔申请时间 query false string  
createTimeStart 索赔申请时间 query false string  
elgPlatformId 平台 ID query false string  
guaranteeCode 保函编号 query false string  
id  query false integer  
orderCode 订单编号 query false string  
status 理赔状态 :1= 已受理， 2= 已推送银行 query false string  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
根据主键 id 查询理赔订单详情  
接口描述:
 

## Page 97

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
claimId 理赔 id body false integer  
参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
data  理赔详情信息 理赔详情信息
接口地址:/system/claim/getClaimData
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
{
    "code": 0,
    "data": {
        "bidName": "",
        "bidNum": "",
        "bidUnitName": "",
        "companyName": "",
        "contactMobile": "",
        "contactName": "",
        "createTime": "",
        "elgPlatformName": "",
        "guaranteeCode": "",
        "id": 0,
        "list": [
            {
                "claimFileKey": "",
                "claimFileNo": 0,
                "claimFilePath": "",
                "claimFileType": "",
                "claimId": 0,
                "createTime": "",
                "id": 0,
                "updateTime": ""
            }
        ],
        "orderCode": "",
        "projectName": "",
        "projectNum": "",
        "status": "",
        "updateTime": ""
    },
    "msg": ""
}

## Page 98

参数名称 参数说明 类型 schema
msg 消息内容 string  
参数名称 参数说明 类型 schema
bidName 标段名称 string  
bidNum 标段编号 string  
bidUnitName 投标单位名称 string  
companyName 承保机构名称 string  
contactMobile 被保人联系人电话 string  
contactName 被保人联系人姓名 string  
createTime 创建时间 string(date-time)  
elgPlatformName 平台名称 string  
guaranteeCode 保函编号 string  
id 理赔 id integer(int32)  
list 理赔文件集合 array ClaimFileInfoPO 对象
orderCode 订单编号 string  
projectName 项目名称 string  
projectNum 项目编号 string  
status 理赔状态 :1= 已受理， 2= 已推送银行 string  
updateTime 更新时间 string(date-time)  
参数名称 参数说明 类型 schema
claimFileKey 文件 key string  
claimFileNo 索赔文件编号 integer(int32)  
claimFilePath 索赔文件存储路径 string  
claimFileType 文件类型， jpg 、 png 、 jpeg 、 pdf 支持大写 string  
claimId 索赔表 id integer(int32)  
createTime 创建时间 string(date-time)  
 
schema 属性说明
 
理赔详情信息
ClaimFileInfoPO 对象

## Page 99

参数名称 参数说明 类型 schema
id 主键 id integer(int32)  
updateTime 更新时间 string(date-time)  
状态码 说明 schema
200 OK DataInfo« 理赔详情信息 »
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
bidName 标段名称 query false string  
bidUnitName 投标单位名称 query false string  
companyId 承保机构 ID query false string  
contactMobile 被保人联系人电话 query false string  
contactName 被保人联系人姓名 query false string  
createTimeEnd 索赔申请时间 query false string  
createTimeStart 索赔申请时间 query false string  
elgPlatformId 平台 ID query false string  
guaranteeCode 保函编号 query false string  
id  query false integer  
响应状态:
分页查询理赔列表  
接口描述:
接口地址:/system/claim/list
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：

## Page 100

参数名称 参数说明 in 是否必须 数据类型 schema
orderCode 订单编号 query false string  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
status 理赔状态 :1= 已受理， 2= 已推送银行 query false string  
参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  ClaimInfoPageVO ClaimInfoPageVO
total 总记录数 integer(int64) integer(int64)
参数名称 参数说明 类型 schema
bidName 标段名称 string  
bidNum 标段编号 string  
响应示例:
响应参数:
 
schema 属性说明
 
ClaimInfoPageVO
{
    "code": 0,
    "msg": "",
    "rows": {
        "bidName": "",
        "bidNum": "",
        "bidUnitName": "",
        "companyName": "",
        "contactMobile": "",
        "contactName": "",
        "createTime": "",
        "elgPlatformName": "",
        "guaranteeCode": "",
        "id": 0,
        "orderCode": "",
        "projectName": "",
        "projectNum": "",
        "status": ""
    },
    "total": 0
}

## Page 101

参数名称 参数说明 类型 schema
bidUnitName 投标单位名称 string  
companyName 承保机构名称 string  
contactMobile 被保人联系人电话 string  
contactName 被保人联系人姓名 string  
createTime 申请时间 string(date-time)  
elgPlatformName 平台名称 string  
guaranteeCode 保函编号 string  
id 理赔 id integer(int32)  
orderCode 订单编号 string  
projectName 项目名称 string  
projectNum 项目编号 string  
status 理赔状态 :1= 已受理， 2= 已推送银行 string  
状态码 说明 schema
200 OK TableDataInfo«ClaimInfoPageVO»
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
响应状态:
推送理赔信息至银行  
接口描述:
接口地址:/system/claim/pushXib
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：

## Page 102

参数名称 参数说明 in 是否必须 数据类型 schema
claimInfoId claimInfoId query false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
orderCode 订单编号 body false string  
reason 审核不通过原因 body false string  
响应示例:
响应参数:
暂无
 
 
响应状态:
订单后台相关功能操作接口 
审核支付凭证  
接口描述:
接口地址:/system/order/auditPayCertificate
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
 

## Page 103

参数名称 参数说明 in 是否必须 数据类型 schema
status 审核状态  0 通过  1 不通过 body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
applicationId 申请 id query false string  
companyId 承保机构 ID query false string  
companyName 企业名称 query false string  
createTimeEnd 提交订单结束时间 query false string  
createTimeStart 提交订单开始时间 query false string  
响应示例:
响应参数:
暂无
 
 
响应状态:
导出  
接口描述:
接口地址:/system/order/export
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
 

## Page 104

参数名称 参数说明 in 是否必须 数据类型 schema
elgPlatformId 平台 id query false string  
guaranteeCode 保函编号 query false string  
guaranteeTimeEnd 检索出函结束时间 query false string  
guaranteeTimeStart 检索出函开始时间 query false string  
isRefund 是否退款（ 0- 否， 1- 是） query false string  
orderCode 订单编号 query false string  
payTimeEnd 检索支付结束时间 query false string  
payTimeStart 检索支付开始时间 query false string  
projectName 项目名称 query false string  
status 订单状态 query false string  
tenderDocEndDateEnd 检索开标结束时间 query false string  
tenderDocEndDateStart 检索开标开始时间 query false string  
状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
获取订单详情  
接口描述:
接口地址:/system/order/getOrderData
请求方式：POST
consumes:["application/json"]
 

## Page 105

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
orderCode 订单编号 body false string  
produces:["*/*"]
 
请求参数：
响应示例:
{
    "code": 0,
    "data": {
        "agentContacts": "",
        "agentName": "",
        "applicationId": "",
        "basicBank": "",
        "bidAddress": "",
        "bidCompanyName": "",
        "bidContacts": "",
        "bidDocumentNum": "",
        "bidDocumentType": "",
        "bidName": "",
        "bidNum": "",
        "biddingDocNo": "",
        "bidpublishstarttime": "",
        "cardNo": "",
        "cardNoName": "",
        "cardNoVerify": "",
        "cashDepositExpireTime": "",
        "ciphertext": "",
        "commitmentFileId": "",
        "commitmentFilePath": "",
        "companyAddress": "",
        "companyName": "",
        "createTime": "",
        "elgPlatformName": "",
        "entrustFileId": "",
        "entrustFilePath": "",
        "guaranteeAmt": "",
        "guaranteeCode": "",
        "guaranteeEndDate": "",
        "guaranteeEndTime": "",
        "guaranteeFilePath": "",
        "guaranteeStartDate": "",
        "guaranteeTime": "",
        "handleContactEmail": "",
        "handleContactName": "",
        "handleContactWay": "",
        "id": 0,
        "insurancePolicy": "",
        "insurancePolicyFile": "",
        "isRefund": "",
        "legalContactIdCard": "",
        "legalContactName": "",
        "legalContactWay": "",
        "linkman": "",
        "linkmanMobile": "",

## Page 106

参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
data  OrderInfoDetailsVO OrderInfoDetailsVO
msg 消息内容 string  
参数名称 参数说明 类型 schema
agentContacts 代理机构联系人 string  
agentName 代理机构名称 string  
applicationId 申请 id string  
basicBank 企业基本帐户开户行 string  
bidAddress 被保人地址，多个时英文逗号分隔 string  
bidCompanyName 被保人企业名称，多个时英文逗号分隔 string  
bidContacts 被保人联系人名称，多个时英文逗号分隔 string  
响应参数:
 
schema 属性说明
 
OrderInfoDetailsVO
        "orderCode": "",
        "payCertificate": "",
        "payExternalUrl": "",
        "payNumber": "",
        "payTime": "",
        "payType": "",
        "premium": "",
        "producType": "",
        "productOne": "",
        "projectExpireTime": "",
        "projectName": "",
        "projectNum": "",
        "projectType": "",
        "purchaseInfo": "",
        "refundPath": "",
        "refundTime": "",
        "rejectReason": "",
        "sameOrderCodes": "",
        "socialCreditCode": "",
        "status": "",
        "tenderDocEndDate": "",
        "underwritingCompanyName": "",
        "updateTime": "",
        "verificationCode": ""
    },
    "msg": ""
}

## Page 107

参数名称 参数说明 类型 schema
bidDocumentNum 被保人证件号码，多个时英文逗号分隔 string  
bidDocumentType 被保人证件类型  个人 (01 ：身份证、 02 ：护照、 03 ：军人证、 04 ：
港澳通行证， 05 ：驾驶证、 06 ：港澳回乡证或台胞证， 07 ：临时
身份证、 08 ：外国人永久居留身份证， 09 ：港澳台居民居住证、
10 ：台湾通行证、 99 ：其他 ) ，团体 (01 ：组织机构代码证、 02 ：税
务登记证、 03 ：统一社会信用代码、 04 ：工商营业执照号、 51 ：
其他 - 政府机关、 52 ：其他 - 事业单位、 53 ：其他 - 社会团体、 54 ：
其他 - 工程项目组、 55 ：其他 - 军事机构、 56 ：其他 - 外国注册企
业、 57 ：其他 - 其他类型、 99 ：其他 ), 多个时英文逗号分隔
string  
bidName 标段名称 string  
bidNum 标段编号 string  
biddingDocNo 招标文件编号 string  
bidpublishstarttime 招标公告公示开始时间 string  
cardNo 对公银行账号 ( 基本户 ) string  
cardNoName 对公银行账号名称 ( 基本户 ) string  
cardNoVerify 是否校验对公银行帐号， 0- 不校验  1- 校验基本户账号  2- 同时校验
基本户账号和户名
string  
cashDepositExpireTime 保证金缴纳截止日期 string  
ciphertext 是否密文函， 0= 明文， 1= 密文 string  
commitmentFileId 出具保函承诺书文件 id string  
commitmentFilePath 出具保函承诺书存储路径 string  
companyAddress 企业地址 string  
companyName 投标方企业名称 string  
createTime 创建时间 string(date-
time)
 
elgPlatformName 平台名称 string  
entrustFileId 委托担保协议文件 id string  
entrustFilePath 委托担保协议存储路径 string  
guaranteeAmt 担保金额【元】 string  
guaranteeCode 保函编号 string  
guaranteeEndDate 保函终止日期（格式： yyyy-MM-dd ）厦门国际银行回调存储string  
guaranteeEndTime 担保结束日期（格式： YYYY-MM-DD ）由合作方计算后传输，作
为保函到期日。
string  
guaranteeFilePath 保函文件存储路径 string  
guaranteeStartDate 保函起始日（格式： yyyy-MM-dd ）厦门国际银行回调存储 string  

## Page 108

参数名称 参数说明 类型 schema
guaranteeTime 保函生成时间 string  
handleContactEmail 经办人邮箱地址作为投保人邮箱 string  
handleContactName 经办人姓名 string  
handleContactWay 经办人联系方式 string  
id id integer(int32) 
insurancePolicy 担保协议附件 ID ，调用 232 接口获取申请材料。 string  
insurancePolicyFile 担保协议附件 string  
isRefund 是否退款（ 0- 否， 1- 是） string  
legalContactIdCard 法人身份证号 string  
legalContactName 法定代表人 string  
legalContactWay 法人联系方式 string  
linkman 提交人用户名称 string  
linkmanMobile 提交人电话号码 string  
orderCode 订单编号 string  
payCertificate 支付凭证 string  
payExternalUrl 外部支付地址 string  
payNumber 支付流水号 string  
payTime 支付时间 string  
payType 支付方式： 1 微信支付 ,2 支付宝支付 ,3 银行转账 ,4 银联 B2B,5 银联
B2C,6 工行支付
string  
premium 保费【元】 string  
producType 保函类型（ 1 分离式、 2 直开式） string  
productOne 产品唯一标识 string  
projectExpireTime 保函有效期，默认是 180 天 string  
projectName 项目名称 string  
projectNum 项目编号 string  
projectType 项目类型 string  
purchaseInfo 采购人信息（联合采购格式：采购单位名称 @ 统一社会信用代码 /
或组织机构代码，采购单位名称 @ 统一社会信用代码、 / 或组织机
构代码）
string  
refundPath 退款凭证 string  
refundTime 退款时间 string  

## Page 109

参数名称 参数说明 类型 schema
rejectReason 拒绝原因（审核未通过时必填） string  
sameOrderCodes 统一企业统一标段的其他订单号 string  
socialCreditCode 统一社会信用代码号 string  
status 订单状态 string  
tenderDocEndDate 投标文件递交截止日期 string  
underwritingCompanyName承保机构名称 string  
updateTime 更新时间 string(date-
time)
 
verificationCode 保函验真码 string  
状态码 说明 schema
200 OK DataInfo«OrderInfoDetailsVO»
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
orderCode 订单编号 body false string  
响应状态:
担保投保申请  
接口描述:
接口地址:/system/order/guaranteeApply
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
 

## Page 110

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
orderCode 订单编号 body false string  
状态码 说明 schema
响应参数:
暂无
 
 
响应状态:
担保出函通知  
接口描述:
接口地址:/system/order/guaranteeNotice
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 111

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
applicationId 申请 id query false string  
companyId 承保机构 ID query false string  
companyName 企业名称 query false string  
createTimeEnd 提交订单结束时间 query false string  
createTimeStart 提交订单开始时间 query false string  
elgPlatformId 平台 id query false string  
guaranteeCode 保函编号 query false string  
guaranteeTimeEnd 检索出函结束时间 query false string  
guaranteeTimeStart 检索出函开始时间 query false string  
isRefund 是否退款（ 0- 否， 1- 是） query false string  
orderCode 订单编号 query false string  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
payTimeEnd 检索支付结束时间 query false string  
分页查询订单列表  
接口描述:
接口地址:/system/order/list
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：

## Page 112

参数名称 参数说明 in 是否必须 数据类型 schema
payTimeStart 检索支付开始时间 query false string  
projectName 项目名称 query false string  
status 订单状态 query false string  
tenderDocEndDateEnd 检索开标结束时间 query false string  
tenderDocEndDateStart 检索开标开始时间 query false string  
参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  OrderInfoPageVO OrderInfoPageVO
total 总记录数 integer(int64) integer(int64)
响应示例:
响应参数:
 
schema 属性说明
 
{
    "code": 0,
    "msg": "",
    "rows": {
        "bidName": "",
        "companyName": "",
        "createTime": "",
        "elgPlatformName": "",
        "guaranteeAmt": "",
        "guaranteeCode": "",
        "guaranteeTime": "",
        "id": 0,
        "isRefund": "",
        "orderCode": "",
        "payChannelSwitch": "",
        "payTime": "",
        "payType": "",
        "premium": "",
        "projectName": "",
        "pushExternalCh": "",
        "pushExternalTb": "",
        "status": "",
        "tenderDocEndDate": "",
        "underwritingCompanyName": "",
        "upStatus": ""
    },
    "total": 0
}

## Page 113

参数名称 参数说明 类型 schema
bidName 标段名称 string  
companyName 企业名称 string  
createTime 提交订单时间 string(date-
time)
 
elgPlatformName 平台名称 string  
guaranteeAmt 担保金额 string  
guaranteeCode 保函编号 string  
guaranteeTime 保函生成时间 string  
id id integer(int32) 
isRefund 是否退款（ 0- 否， 1- 是） string  
orderCode 订单编号 string  
payChannelSwitch 支付渠道： 1 内部支付， 2 外部支付 string  
payTime 支付时间 string  
payType 支付方式： 1 微信支付 ,2 支付宝支付 ,3 银行转账 ,4 银联 B2B,5 银联
B2C,6 工行支付
string  
premium 保费【元】 string  
projectName 项目名称 string  
pushExternalCh 出函通知调用外部接口： 1 不调用， 2 调用失败， 3 调用成功 string  
pushExternalTb 保函申请调用外部接口： 1 不调用， 2 调用失败， 3 调用成功 string  
status 订单状态 string  
tenderDocEndDate 投标文件递交截止日期 string  
underwritingCompanyName承保机构名称 string  
upStatus 支付完成回调 - 推送上游状态： 1 待推送  2 成功  3 失败 string  
状态码 说明 schema
200 OK TableDataInfo«OrderInfoPageVO»
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
OrderInfoPageVO
响应状态:

## Page 114

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
orderCode 订单编号 body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
预览保函  
接口描述:
接口地址:/system/order/previewLetter
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
推送银行  
接口描述:
接口地址:/system/order/pushXib
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 

## Page 115

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
orderCode 订单编号 body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
orderCode 订单编号 body false string  
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
推送粤商通支付结果通知  
接口描述:
接口地址:/system/order/pushYstPayResult
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
 

## Page 116

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
file 退款凭证 formData false file  
orderCode 订单编号 body false string  
响应参数:
暂无
 
 
响应状态:
订单退款  
接口描述:
接口地址:/system/order/refundMoney
请求方式：POST
consumes:["multipart/form-data"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
 
 

## Page 117

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
orderCode 订单编号 body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
响应状态:
查看同企业同标段是否有重复订单  
接口描述:
接口地址:/system/order/selectRepetitionOrder
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 118

状态码 说明 schema
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
orderCode 订单编号 body false string  
reason 审核不通过原因 body false string  
status 审核状态  0 通过  1 不通过 body false string  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
订单担保审核  
接口描述:
接口地址:/system/order/warrantyReview
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 119

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyId 承保机构 ID query false string  
companyName 企业名称 query false string  
createTimeEnd 退保申请时间 - 止 query false string  
createTimeStart 退保申请时间 - 起 query false string  
elgPlatformId 平台 id query false string  
guaranteeCode 保函编号 query false string  
handleContactName经办人姓名 query false string  
handleContactWay 经办人电话 query false string  
id id query false integer  
isRefund 是否退款（ 0- 否， 1- 是） query false string  
orderCode 订单编号 query false string  
status 退保状态 :1= 待审核 ,2= 审核不通过 ,3= 审核通过 query false string  
退保后台相关功能操作接口 
导出  
接口描述:
接口地址:/system/refund/export
请求方式：GET
consumes:``
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
 

## Page 120

状态码 说明 schema状态码 说明 schema
200 OK  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
refundId 退保表 id body false integer  
根据主键 id 查询退保订单详情  
接口描述:
接口地址:/system/refund/getRefundData
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
{
    "code": 0,
    "data": {
        "bankAuditStatus": "",
        "bankRemark": "",
        "companyName": "",
        "createTime": "",
        "elgPlatformName": "",
        "guaranteeCode": "",
        "handleContactName": "",
        "handleContactWay": "",
        "id": 0,
        "initiator": "",
        "isRefund": "",
        "orderCode": "",
        "premium": "",
        "projectName": "",
        "reason": "",
        "refundAuditTime": "",
        "refundPath": "",
        "refundTime": "",
        "rejectReason": "",
        "socialCreditCode": "",
        "status": "",
        "underwritingCompanyName": "",

## Page 121

参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
data  RefundInfoDetailsVO RefundInfoDetailsVO
msg 消息内容 string  
参数名称 参数说明 类型 schema
bankAuditStatus 银行审核状态 :1 未推送， 2 银行待审核， 3 审核不通过， 4 审核通
过
string  
bankRemark 银行退保审核原因 string  
companyName 企业名称 string  
createTime 创建时间 string(date-
time)
 
elgPlatformName 平台名称 string  
guaranteeCode 保函编号 string  
handleContactName 申请人姓名 string  
handleContactWay 申请人电话 string  
id 退保 id integer(int32)  
initiator 发起人： 1= 保函平台， 2= 交易中心 string  
isRefund 是否退款（ 0- 否， 1- 是） string  
orderCode 订单编号 string  
premium 保费【元】 string  
projectName 项目名称 string  
reason 退费原因 string  
refundAuditTime 退保审核时间 string  
refundPath 退款凭证地址 string  
响应参数:
 
schema 属性说明
 
RefundInfoDetailsVO
        "updateTime": ""
    },
    "msg": ""
}

## Page 122

参数名称 参数说明 类型 schema
refundTime 退款时间 string  
rejectReason 拒绝原因 string  
socialCreditCode 统一社会信用代码 string  
status 退保状态 :1= 待审核 ,2= 审核不通过 ,3= 审核通过 string  
underwritingCompanyName承保机构名称 string  
updateTime 更新时间 string(date-
time)
 
状态码 说明 schema
200 OK DataInfo«RefundInfoDetailsVO»
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
companyId 承保机构 ID query false string  
companyName 企业名称 query false string  
createTimeEnd 退保申请时间 - 止 query false string  
createTimeStart 退保申请时间 - 起 query false string  
elgPlatformId 平台 id query false string  
响应状态:
分页查询退保列表  
接口描述:
接口地址:/system/refund/list
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：

## Page 123

参数名称 参数说明 in 是否必须 数据类型 schema
guaranteeCode 保函编号 query false string  
handleContactName经办人姓名 query false string  
handleContactWay 经办人电话 query false string  
id id query false integer  
isRefund 是否退款（ 0- 否， 1- 是） query false string  
orderCode 订单编号 query false string  
pageNum pageNum query false integer  
pageSize pageSize query false integer  
status 退保状态 :1= 待审核 ,2= 审核不通过 ,3= 审核通过 query false string  
参数名称 参数说明 类型 schema
code 消息状态码 integer(int32) integer(int32)
msg 消息内容 string  
rows  RefundInfoPageVO RefundInfoPageVO
total 总记录数 integer(int64) integer(int64)
响应示例:
响应参数:
{
    "code": 0,
    "msg": "",
    "rows": {
        "bankAuditStatus": "",
        "companyName": "",
        "createTime": "",
        "elgPlatformName": "",
        "guaranteeCode": "",
        "guaranteeStatus": "",
        "handleContactName": "",
        "handleContactWay": "",
        "id": 0,
        "isRefund": "",
        "orderCode": "",
        "payChannelSwitch": "",
        "payType": "",
        "premium": "",
        "projectName": "",
        "refundAuditTime": "",
        "status": "",
        "underwritingCompanyName": "",
        "upStatus": ""
    },
    "total": 0
}

## Page 124

参数名称 参数说明 类型 schema
bankAuditStatus 银行审核状态 :1 未推送， 2 银行待审核， 3 审核不通过， 4 审核通过 string  
companyName 企业名称 string  
createTime 申请时间 string(date-
time)
 
elgPlatformName 平台名称 string  
guaranteeCode 保函编号 string  
guaranteeStatus 出函状态 :1= 未出函， 2 已出函 string  
handleContactName 申请人姓名 string  
handleContactWay 申请人电话 string  
id 退保 id integer(int32) 
isRefund 是否退款（ 0- 否， 1- 是） string  
orderCode 订单编号 string  
payChannelSwitch 支付渠道： 1 内部支付， 2 外部支付 string  
payType 支付方式： 1 微信支付 ,2 支付宝支付 ,3 银行转账 ,4 银联 B2B,5 银联
B2C,6 工行支付
string  
premium 保费【元】 string  
projectName 项目名称 string  
refundAuditTime 退保审核时间 string  
status 退保状态 string  
underwritingCompanyName承保机构名称 string  
upStatus 退保结果回调 - 推送上游状态： 1 待推送  2 成功  3 失败 string  
状态码 说明 schema
200 OK TableDataInfo«RefundInfoPageVO»
201 Created  
401 Unauthorized  
403 Forbidden  
 
schema 属性说明
 
RefundInfoPageVO
响应状态:

## Page 125

状态码 说明 schema
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
refundId 退保 id body false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
退保推送银行  
接口描述:
接口地址:/system/refund/pushXib
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
推送退保信息至粤商通  
接口描述:
 

## Page 126

参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
refundId 退保 id body false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
接口地址:/system/refund/pushYst
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
响应示例:
响应参数:
暂无
 
 
响应状态:
退保退款  
接口描述:
接口地址:/system/refund/refundMoney
请求方式：POST
consumes:["multipart/form-data"]
produces:["*/*"]
 
请求参数：
 

## Page 127

参数名称 参数说明 in 是否必须 数据类型 schema参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
file 退款凭证 formData false file  
refundId 退保 id body false integer  
状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
参数名称 参数说明 in 是否必须 数据类型 schema
Authorization Authorization header false string  
reason 审核不通过原因 body false string  
refundId 退保表 id body false integer  
status 审核状态： 0 通过  1 不通过 body false string  
响应示例:
响应参数:
暂无
 
 
响应状态:
退保担保审核  
接口描述:
接口地址:/system/refund/warrantyReview
请求方式：POST
consumes:["application/json"]
produces:["*/*"]
 
请求参数：
 

## Page 128

状态码 说明 schema
200 OK  
201 Created  
401 Unauthorized  
403 Forbidden  
404 Not Found  
响应示例:
响应参数:
暂无
 
 
响应状态:
 

