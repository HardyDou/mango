#!/bin/sh
# Mango 通用 Java 应用镜像的构建期中文字体校验。
#
# 由 scripts/docker/mango-app.Dockerfile 在 root 阶段、USER mango 之前执行；
# 任何断言失败都会让镜像构建失败，避免发布“fc-list :lang=zh 为 0”的镜像。
#
# 校验三类事实：
# 1. 别名生效：每个登记族名 fc-match 命中预期族，且命中的字体文件存在、可读、可解析；
# 2. 汉字覆盖：族名 + charset=9e64（鹤）仍命中同类 CJK 族，排除“名称命中但无汉字”；
# 3. 字重与楷体：小标宋系族名权重高于常规字重，AR PL UKai CN 真实存在且覆盖 U+6977（楷）。
#
# 映射来源：/etc/fonts/conf.d/65-mango-office-font-aliases.conf。
set -eu

conf_path=/etc/fonts/conf.d/65-mango-office-font-aliases.conf
hanzi_codepoint=9e64
kai_codepoint=6977

fail() {
  printf 'FONT-VERIFY FAILED: %s\n' "$*" >&2
  exit 1
}

pass() {
  printf 'FONT-VERIFY OK: %s\n' "$*"
}

first_family() {
  fc-match -f '%{family}\n' "$1" | sed -n '1{s/,.*//;p;}'
}

first_file() {
  fc-match -f '%{file}\n' "$1" | sed -n '1p'
}

first_weight() {
  fc-match -f '%{weight}\n' "$1" | sed -n '1p'
}

require_font_file() {
  matched_file=$(first_file "$1")
  [ -n "${matched_file}" ] || fail "$1 未命中任何字体文件"
  [ -r "${matched_file}" ] || fail "$1 命中的字体文件不可读：${matched_file}"
  fc-query "${matched_file}" >/dev/null 2>&1 || fail "$1 命中的文件不是有效字体：${matched_file}"
}

assert_family() {
  actual=$(first_family "$1")
  [ "${actual}" = "$2" ] || fail "$1 -> ${actual}（期望 $2）"
  require_font_file "$1"
  pass "$1 -> ${actual}"
}

assert_hanzi_family() {
  actual=$(first_family "$1:charset=${hanzi_codepoint}")
  [ "${actual}" = "$2" ] || fail "$1 + 汉字 ${hanzi_codepoint} -> ${actual}（期望 $2）"
  pass "$1 + 汉字 ${hanzi_codepoint} -> ${actual}"
}

assert_group() {
  group_expected=$1
  shift
  for group_name in "$@"; do
    assert_family "${group_name}" "${group_expected}"
    assert_hanzi_family "${group_name}" "${group_expected}"
  done
}

assert_bold_group() {
  bold_expected=$1
  shift
  regular_weight=$(first_weight "${bold_expected}")
  [ -n "${regular_weight}" ] || fail "${bold_expected} 常规字重不可读"
  for bold_name in "$@"; do
    assert_family "${bold_name}" "${bold_expected}"
    assert_hanzi_family "${bold_name}" "${bold_expected}"
    bold_weight=$(first_weight "${bold_name}")
    case "${bold_weight}" in
      ''|*[!0-9]*) fail "${bold_name} 字重不是数值：${bold_weight}" ;;
    esac
    [ "${bold_weight}" -gt "${regular_weight}" ] \
      || fail "${bold_name} 字重 ${bold_weight} 不大于 ${bold_expected} 常规字重 ${regular_weight}"
    pass "${bold_name} 字重 ${bold_weight} > 常规 ${regular_weight}"
  done
}

[ -r "${conf_path}" ] || fail "缺少字体映射文件：${conf_path}"
command -v fc-match >/dev/null 2>&1 || fail "缺少 fontconfig（fc-match）"

# ---- 1. 字体映射必须已安装且被 fontconfig 识别 ----
for required_family in 'Noto Sans CJK SC' 'Noto Serif CJK SC' 'Noto Color Emoji' 'AR PL UKai CN'; do
  fc-list : family | tr ',' '\n' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | grep -Fqx "${required_family}" \
    || fail "内置字体缺失：${required_family}"
  pass "内置字体存在：${required_family}"
done

zh_font_count=$(fc-list :lang=zh | wc -l)
[ "${zh_font_count}" -ge 20 ] || fail "中文字体面数量异常：${zh_font_count}"
pass "fc-list :lang=zh 字体面数量：${zh_font_count}"

# ---- 2. 衬线类 -> Noto Serif CJK SC ----
assert_group 'Noto Serif CJK SC' \
  '宋体' 'SimSun' 'NSimSun' '新宋体' '中易宋体' 'SimSun-ExtB' \
  '仿宋' 'FangSong' '仿宋_GB2312' 'FangSong_GB2312' \
  '华文仿宋' 'STFangsong' '华文宋体' 'STSong' '华文中宋' 'STZhongsong' \
  '明体' '明朝' 'MingLiU' 'PMingLiU' 'Songti SC' 'AR PL UMing CN' \
  'Source Han Serif SC' 'Source Han Serif CN' '思源宋体' \
  '方正书宋简体' '方正兰亭宋' '方正仿宋简体'

# ---- 3. 楷体类 -> AR PL UKai CN（真楷体） ----
assert_group 'AR PL UKai CN' \
  '楷体' 'KaiTi' '楷体_GB2312' 'KaiTi_GB2312' '楷书' \
  '华文楷体' 'STKaiti' 'Kaiti SC' '标楷体' 'DFKai-SB' \
  '方正楷体简体'

kai_file=$(first_file 'AR PL UKai CN')
case "${kai_file}" in
  *ukai*) ;;
  *) fail "楷体命中的不是文鼎楷体文件：${kai_file}" ;;
esac
pass "楷体命中真楷体文件：${kai_file}"
assert_hanzi_family 'AR PL UKai CN' 'AR PL UKai CN'
kai_family_6977=$(first_family "AR PL UKai CN:charset=${kai_codepoint}")
[ "${kai_family_6977}" = 'AR PL UKai CN' ] \
  || fail "AR PL UKai CN 不覆盖 U+${kai_codepoint}（楷）：${kai_family_6977}"
pass "AR PL UKai CN 覆盖 U+${kai_codepoint}（楷）"

# ---- 4. 黑体/无衬线类 -> Noto Sans CJK SC ----
assert_group 'Noto Sans CJK SC' \
  '黑体' 'SimHei' '中易黑体' '微软雅黑' 'Microsoft YaHei' '微软雅黑 Light' \
  '等线' 'DengXian' '等线体' '苹方' 'PingFang SC' 'PingFang TC' \
  '华文黑体' 'STHeiti' '华文细黑' 'STXihei' 'Heiti SC' 'Hiragino Sans GB' \
  '冬青黑体' '冬青黑体简体中文' 'Yuanti SC' \
  '思源黑体' 'Source Han Sans SC' 'Source Han Sans CN' \
  '文泉驿正黑' 'WenQuanYi Zen Hei' '文泉驿微米黑' 'WenQuanYi Micro Hei' \
  '兰亭黑' '方正兰亭黑' '方正黑体简体' '汉仪旗黑' '幼圆' 'YouYuan' '隶书' 'LiSu'

# ---- 5. 艺术字：无开源等价字体，显式降级到 Noto Sans CJK SC ----
assert_group 'Noto Sans CJK SC' \
  'STHupo' 'STCaiyun' 'STXinwei' 'STXingkai' 'STLiti'

# ---- 6. 拉丁族名 -> Liberation / Carlito / Caladea ----
for latin_pair in 'Arial:Liberation Sans' 'Helvetica:Liberation Sans' \
                  'Times New Roman:Liberation Serif' \
                  'Courier New:Liberation Mono' \
                  'Calibri:Carlito' 'Cambria:Caladea'; do
  assert_family "${latin_pair%%:*}" "${latin_pair#*:}"
done

# 拉丁族名本身不含汉字，带汉字时必须落到 CJK 兜底族（等宽族可能落到 CJK 等宽字体）。
is_cjk_family() {
  case "$1" in
    'Noto Sans CJK SC'|'Noto Serif CJK SC'|'Noto Sans Mono CJK SC') return 0 ;;
    'WenQuanYi Zen Hei'*|'WenQuanYi Micro Hei'*) return 0 ;;
    'AR PL UKai CN'|'AR PL UMing CN') return 0 ;;
    *) return 1 ;;
  esac
}

for latin_name in 'Arial' 'Helvetica' 'Times New Roman' 'Courier New' 'Calibri' 'Cambria'; do
  latin_hanzi_family=$(first_family "${latin_name}:charset=${hanzi_codepoint}")
  is_cjk_family "${latin_hanzi_family}" \
    || fail "${latin_name} 带汉字未落到 CJK 字体：${latin_hanzi_family}"
  pass "${latin_name} + 汉字 ${hanzi_codepoint} -> ${latin_hanzi_family}"
done

# ---- 7. 公文标题加粗：小标宋系 -> Noto Serif CJK SC 且 weight=bold ----
assert_bold_group 'Noto Serif CJK SC' '方正小标宋简体' '方正小标宋_GBK' '小标宋'

# ---- 8. 未登记族名兜底：不得退回无汉字的 DejaVu ----
for unknown_family in 'MangoUnknownFamily' 'Some Legacy Office Font'; do
  fallback_family=$(first_family "${unknown_family}:charset=${hanzi_codepoint}")
  case "${fallback_family}" in
    'Noto Sans CJK SC'|'Noto Serif CJK SC') ;;
    *) fail "未登记族名 ${unknown_family} 汉字兜底异常：${fallback_family}" ;;
  esac
  pass "未登记族名 ${unknown_family} -> ${fallback_family}"
done

emoji_family=$(first_family 'emoji:charset=1f600')
[ "${emoji_family}" = 'Noto Color Emoji' ] || fail "emoji 兜底异常：${emoji_family}"
pass "emoji -> ${emoji_family}"

pass '中文字体覆盖校验全部通过'
