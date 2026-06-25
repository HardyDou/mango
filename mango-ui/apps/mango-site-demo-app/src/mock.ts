/**
 * 演示站点页面自有结构化内容（产品能力、解决方案、数据亮点、客户）。
 *
 * 站点信息、导航、Banner、广告、新闻等内容均来自 CMS 公开接口，
 * 不在此处维护仿真数据；本文件只承载官网视觉区块的自有展示结构。
 */

/**
 * 核心能力（产品矩阵）。用于首页能力卡片与产品列表页。
 */
export interface Capability {
  code: string;
  name: string;
  tagline: string;
  description: string;
  features: string[];
  icon: string;
}

export const CAPABILITIES: Capability[] = [
  {
    code: 'publish',
    name: '内容发布',
    tagline: '可视化 · 秒级生效',
    description: '富文本、图文、单页、附件、视频多类型内容在线编辑与发布，支持定时发布与即时生效。',
    features: ['秒级发布生效', '多内容类型覆盖', '版本与回滚', '7×24 小时不间断'],
    icon: 'shield',
  },
  {
    code: 'review',
    name: '智能审核',
    tagline: '审核 · 风控 · 实时监控',
    description: '基于规则与图谱的内容审核模型，对违规内容实时拦截，审核准确率达 99.2%。',
    features: ['审核规则可配置', '违规拦截 99.2%', '实时风险预警', '审核留痕可溯'],
    icon: 'radar',
  },
  {
    code: 'data',
    name: '数据穿透',
    tagline: '可信 · 可溯 · 可验',
    description: '内容全生命周期数据穿透，从创建、审核、发布到下线全链路可追溯，杜绝违规内容。',
    features: ['全链路溯源', '多方实时核验', '操作日志存证', '监管报表一键生成'],
    icon: 'link',
  },
  {
    code: 'multisite',
    name: '多站点管理',
    tagline: '官网 · 帮助 · 门户',
    description: '一套平台支撑多个站点，按域名解析站点，独立栏目、导航、Banner 与广告位配置。',
    features: ['按域名解析站点', '独立栏目与导航', '多站点广告投放', '开放 API 生态'],
    icon: 'network',
  },
];

/**
 * 解决方案（行业）。用于首页解决方案卡片与解决方案列表页。
 */
export interface Solution {
  code: string;
  industry: string;
  title: string;
  description: string;
  highlights: string[];
}

export const SOLUTIONS: Solution[] = [
  {
    code: 'enterprise',
    industry: '企业官网',
    title: '企业官网站点',
    description: '为企业提供从站点搭建、栏目规划、内容发布到 SEO 优化的全流程官网解决方案。',
    highlights: ['多栏目内容管理', '导航与 Banner 配置', 'SEO 自动优化', '响应式主题'],
  },
  {
    code: 'help',
    industry: '帮助中心',
    title: '帮助中心站点',
    description: '面向产品用户的帮助中心，支持文档栏目、全文检索、FAQ 与内容详情阅读。',
    highlights: ['文档栏目树', '全文检索', 'FAQ 知识库', '内容详情阅读'],
  },
  {
    code: 'portal',
    industry: '行业门户',
    title: '行业门户平台',
    description: '为行业公共服务平台建设统一的内容发布、核验与风控体系，支撑海量内容运营。',
    highlights: ['统一发布入口', '内容真伪核验', '全流程电子化', '监管数据上报'],
  },
  {
    code: 'gov',
    industry: '政务公开',
    title: '政务公开站点',
    description: '覆盖政务公开、通知公告、政策解读等内容类型，助力政务信息透明化与可追溯。',
    highlights: ['多内容类型覆盖', '通知公告专区的发布', '政策解读联动', '风险预警联动'],
  },
];

/**
 * 数据亮点。用于首页数字滚动区块。
 */
export interface Highlight {
  value: number;
  suffix: string;
  label: string;
}

export const HIGHLIGHTS: Highlight[] = [
  { value: 200, suffix: '+', label: '服务站点' },
  { value: 1200, suffix: '万+', label: '年发布内容' },
  { value: 992, suffix: '‰', label: '审核拦截准确率' },
  { value: 60, suffix: '%', label: '发布时效提升' },
];

/**
 * 客户与合作机构。用于首页 logo 墙。
 */
export const CLIENTS: string[] = [
  '示例科技',
  '示例传媒',
  '示例金服',
  '示例银行',
  '示例担保',
  '示例招投标',
  '示例公共服务平台',
  '示例交易中心',
  '示例建设集团',
  '示例研究院',
];
