package cn.keking.utils;

import cn.keking.spi.FileDownloaders;
import jakarta.servlet.ServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 判断一次预览请求是否走「进程内取源」：请求带 {@code fileId} 且本进程注册了直取实现。
 *
 * <p>这类请求的身份由短期取源票据承担，没有可供站点校验的网络地址，因此不做 host 校验；
 * 票据是否有效由注册的下载器在门处只读校验（不取流、不消费票据）。无效票据直接拦在门外，
 * 不进入引擎的解包、转换与落盘。{@code url} 形态的请求仍按原规则校验 host 与目录。
 */
public final class PreviewSourceRequests {

    public static final String FILE_ID_PARAM = "fileId";

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewSourceRequests.class);

    private static final int VISIBLE_PREFIX = 6;

    private PreviewSourceRequests() {
    }

    public static boolean isInternalSource(ServletRequest request) {
        if (request == null) {
            return false;
        }
        String sourceId = request.getParameter(FILE_ID_PARAM);
        if (StringUtils.isBlank(sourceId)) {
            return false;
        }
        boolean valid = FileDownloaders.verifySource(sourceId);
        if (!valid) {
            LOGGER.debug("预览取源票据不可用，拒绝放行：{}", masked(sourceId));
        }
        return valid;
    }

    private static String masked(String sourceId) {
        if (sourceId.length() <= VISIBLE_PREFIX) {
            return "***(len=" + sourceId.length() + ")";
        }
        return sourceId.substring(0, VISIBLE_PREFIX) + "***(len=" + sourceId.length() + ")";
    }
}
