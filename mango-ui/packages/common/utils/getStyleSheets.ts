/**
 * 获取所有样式表
 */
export function getStyleSheets(): CSSStyleSheet[] {
  const styleSheets: CSSStyleSheet[] = [];

  try {
    for (let i = 0; i < document.styleSheets.length; i++) {
      const sheet = document.styleSheets[i];
      styleSheets.push(sheet as CSSStyleSheet);
    }
  } catch (error) {
    console.error('获取样式表失败:', error);
  }

  return styleSheets;
}

/**
 * 遍历样式表中的规则
 * @param callback 回调函数
 */
export function forEachStyleSheet(
  callback: (cssRule: CSSStyleRule, styleSheet: CSSStyleSheet) => void
): void {
  const styleSheets = getStyleSheets();

  styleSheets.forEach((styleSheet) => {
    try {
      const rules = styleSheet.cssRules;
      if (!rules) return;

      for (let i = 0; i < rules.length; i++) {
        const rule = rules[i];
        if (rule instanceof CSSStyleRule) {
          callback(rule, styleSheet);
        }
      }
    } catch (error) {
      // 跨域样式表可能会抛出异常
    }
  });
}

/**
 * 根据选择器查找 CSS 规则
 * @param selector 选择器
 */
export function findCssRule(selector: string): CSSStyleRule | null {
  let result: CSSStyleRule | null = null;

  forEachStyleSheet((cssRule) => {
    if (cssRule.selectorText === selector) {
      result = cssRule;
    }
  });

  return result;
}

/**
 * 获取 link 标签的 href 列表
 */
export function getLinkHrefs(): string[] {
  const links = document.querySelectorAll('link[rel="stylesheet"]');
  return Array.from(links)
    .map((link) => (link as HTMLLinkElement).href)
    .filter((href) => href && !href.includes('node_modules'));
}
