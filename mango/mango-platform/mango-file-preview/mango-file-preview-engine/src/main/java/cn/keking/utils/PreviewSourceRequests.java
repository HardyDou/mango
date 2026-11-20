package cn.keking.utils;

import cn.keking.spi.FileDownloaders;
import jakarta.servlet.ServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * 判断一次预览请求是否走「进程内取源」：请求带 {@code fileId} 且本进程注册了直取实现。
 *
 * <p>这类请求的身份由短期取源票据承担，没有可供站点校验的网络地址，因此不做 host 校验；
 * 票据是否有效由取源环节校验（无效时取源失败）。{@code url} 形态的请求仍按原规则校验。
 */
public final class PreviewSourceRequests {

    public static final String FILE_ID_PARAM = "fileId";

    private PreviewSourceRequests() {
    }

    public static boolean isInternalSource(ServletRequest request) {
        if (request == null) {
            return false;
        }
        return StringUtils.isNotBlank(request.getParameter(FILE_ID_PARAM)) && FileDownloaders.hasProvider();
    }
}
