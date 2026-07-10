INSERT INTO link_category (
    id, tenant_id, scope, owner_user_id, name, sort_no, status, remark, created_by, updated_by
)
SELECT seed.id, 1, 'COMPANY', 0, seed.name, seed.sort_no, 'ENABLED', seed.remark, 1, 1
FROM (
    SELECT 202607100101000001 AS id, '业务相关' AS name, 10 AS sort_no, '保函业务人员常用查询与核验入口' AS remark
    UNION ALL
    SELECT 202607100101000002, '工具相关', 20, '保函业务人员常用工具入口'
    UNION ALL
    SELECT 202607100101000003, '其他', 30, '保函业务辅助知识入口'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM link_category
    WHERE tenant_id = 1 AND scope = 'COMPANY' AND owner_user_id = 0 AND name = seed.name
);

INSERT INTO link_item (
    id, tenant_id, category_id, name, url, summary, icon_url, tags,
    visibility_scope, owner_user_id, open_mode, recommended, sort_no, status,
    remark, created_by, updated_by
)
SELECT
    seed.id,
    1,
    category.id,
    seed.name,
    seed.url,
    seed.summary,
    NULL,
    seed.tags,
    'PUBLIC',
    0,
    'NEW_WINDOW',
    seed.recommended,
    seed.sort_no,
    'ENABLED',
    seed.remark,
    1,
    1
FROM (
    SELECT
        202607100102000001 AS id,
        '业务相关' AS category_name,
        '建设银行保函查询' AS name,
        'https://www2.ccb.com/tran/WCCMainPlatV5?CCB_IBSVersion=V5&SERVLET_NAME=WCCMainPlatV5&TXCODE=NBH001' AS url,
        '用于查询建设银行保函相关业务信息。' AS summary,
        '建行,建设银行,保函,保函查询' AS tags,
        1 AS recommended,
        10 AS sort_no,
        '系统内置保函业务导航' AS remark
    UNION ALL
    SELECT 202607100102000002, '业务相关', '中信银行保函查询', 'https://corp.bank.ecitic.com/cotb/onLineLOGVerify_new.html', '用于查询中信银行保函真伪和业务信息。', '中信,中信银行,保函,保函查询', 1, 20, '系统内置保函业务导航'
    UNION ALL
    SELECT 202607100102000003, '业务相关', '邮储银行 U 函通', 'https://elgs.psbc.com/', '邮储银行电子保函业务查询入口。', '邮储,邮储银行,U函通,保函', 1, 30, '系统内置保函业务导航'
    UNION ALL
    SELECT 202607100102000004, '业务相关', '沧州银行保函查询', 'https://bankczbhcx.com/', '沧州银行保函业务查询入口，地址待业务最终确认。', '沧州银行,保函,保函查询', 0, 40, '系统内置保函业务导航，地址待业务确认'
    UNION ALL
    SELECT 202607100102000005, '业务相关', '中国执行信息公开网', 'https://zxgk.court.gov.cn/', '查询被执行人、失信被执行人等公开信息。', '执行信息,法院,失信,风险核验', 0, 50, '系统内置保函业务导航'
    UNION ALL
    SELECT 202607100102000006, '业务相关', '全国建筑市场监管公共服务平台', 'https://jzsc.mohurd.gov.cn/home', '查询建筑企业、人员、项目和诚信信息。', '建筑市场,住建部,企业资质,项目核验', 0, 60, '系统内置保函业务导航'
    UNION ALL
    SELECT 202607100102000007, '业务相关', '国家企业信用信息公示系统', 'https://www.gsxt.gov.cn/', '查询企业登记、公示和经营异常等信息。', '企业信用,工商,企业核验', 0, 70, '系统内置保函业务导航'
    UNION ALL
    SELECT 202607100102000008, '业务相关', '信用中国', 'https://www.creditchina.gov.cn/', '查询信用信息、行政处罚和联合惩戒信息。', '信用中国,信用,风险核验', 0, 80, '系统内置保函业务导航'
    UNION ALL
    SELECT 202607100102000009, '业务相关', '全国公共资源交易平台', 'https://www.ggzy.gov.cn/', '查询公共资源交易公告和项目信息。', '公共资源,招投标,交易平台', 0, 90, '系统内置保函业务导航'
    UNION ALL
    SELECT 202607100102000010, '业务相关', '中国政府采购网', 'https://www.ccgp.gov.cn/', '查询政府采购公告、中标和成交信息。', '政府采购,采购公告,中标', 0, 100, '系统内置保函业务导航'
    UNION ALL
    SELECT 202607100102000011, '工具相关', '保费测算', '/tools/premium-calculator', '系统内置保函保费测算工具入口。', '保费,测算,内置工具', 1, 10, '系统内置工具导航，路由待宿主确认'
    UNION ALL
    SELECT 202607100102000012, '工具相关', '快递 H5', '/tools/express-h5', '系统内置快递 H5 工具入口。', '快递,H5,内置工具', 1, 20, '系统内置工具导航，路由待宿主确认'
    UNION ALL
    SELECT 202607100102000013, '工具相关', 'e签宝', 'https://h5.esign.cn/usercenterFront/login/web', '电子签署和合同签章服务入口。', 'e签宝,电子签章,签署', 0, 30, '系统内置工具导航'
    UNION ALL
    SELECT 202607100102000014, '工具相关', '豆包', 'https://www.doubao.com/chat/', 'AI 问答和内容辅助工具。', '豆包,AI,工具', 0, 40, '系统内置工具导航'
    UNION ALL
    SELECT 202607100102000015, '工具相关', '百度', 'https://www.baidu.com/', '常用中文搜索引擎。', '百度,搜索', 0, 50, '系统内置工具导航'
    UNION ALL
    SELECT 202607100102000016, '其他', '中国法律服务网', 'https://www.12348.gov.cn/', '法律服务、法规和咨询辅助入口。', '法律,法规,法律服务', 0, 10, '系统内置辅助导航'
    UNION ALL
    SELECT 202607100102000017, '其他', '知乎', 'https://www.zhihu.com/', '知识问答和行业信息检索入口。', '知乎,知识,问答', 0, 20, '系统内置辅助导航'
) seed
JOIN link_category category
    ON category.tenant_id = 1
    AND category.scope = 'COMPANY'
    AND category.owner_user_id = 0
    AND category.name = seed.category_name
WHERE NOT EXISTS (
    SELECT 1 FROM link_item
    WHERE tenant_id = 1
      AND (
          id = seed.id
          OR (name = seed.name AND url = seed.url)
      )
);
