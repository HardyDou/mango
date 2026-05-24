<#setting classic_compatible=true>
<#assign watermarkTxtValue=watermarkTxt!''>
<#assign watermarkXSpaceValue=watermarkXSpace!'10'>
<#assign watermarkYSpaceValue=watermarkYSpace!'10'>
<#assign watermarkFontValue=watermarkFont!'微软雅黑'>
<#assign watermarkFontsizeValue=watermarkFontsize!'18px'>
<#assign watermarkColorValue=watermarkColor!'black'>
<#assign watermarkAlphaValue=watermarkAlpha!'0.2'>
<#assign watermarkWidthValue=watermarkWidth!'240'>
<#assign watermarkHeightValue=watermarkHeight!'80'>
<#assign watermarkAngleValue=watermarkAngle!'10'>
<#assign kkResourceBaseUrl=(baseUrl!'')>
<link rel="icon" href="${kkResourceBaseUrl}favicon.ico" type="image/x-icon">
<script src="${kkResourceBaseUrl}js/watermark.js" type="text/javascript"></script>

<script>
    /**
     * 初始化水印
     */
    function initWaterMark() {
        let watermarkTxt = '${watermarkTxtValue?js_string}';
        if (watermarkTxt === '') {
            return;
        }
        let lastWidth = 0;
        let lastHeight = 0;
        const checkResize = () => {
            const currentWidth = document.documentElement.scrollWidth;
            const currentHeight = document.documentElement.scrollHeight;
            // 检测尺寸是否变化
            if (currentWidth === lastWidth && currentHeight === lastHeight) {
                return;
            }
            // 如果变化了, 重新初始化水印
            watermark.init({
                watermark_txt: '${watermarkTxtValue?js_string}',
                watermark_x: 0,
                watermark_y: 0,
                watermark_rows: 0,
                watermark_cols: 0,
                watermark_x_space: ${watermarkXSpaceValue},
                watermark_y_space: ${watermarkYSpaceValue},
                watermark_font: '${watermarkFontValue?js_string}',
                watermark_fontsize: '${watermarkFontsizeValue?js_string}',
                watermark_color: '${watermarkColorValue?js_string}',
                watermark_alpha: ${watermarkAlphaValue},
                watermark_width: ${watermarkWidthValue},
                watermark_height: ${watermarkHeightValue},
                watermark_angle: ${watermarkAngleValue},
            });
            // 更新存储的宽口大小
            lastWidth = currentWidth;
            lastHeight = currentHeight;
        };
        setInterval(checkResize, 1000);
    }
</script>

<style>
    * {
        margin: 0;
        padding: 0;
    }

    html, body {
        height: 100%;
        width: 100%;
    }
</style>
