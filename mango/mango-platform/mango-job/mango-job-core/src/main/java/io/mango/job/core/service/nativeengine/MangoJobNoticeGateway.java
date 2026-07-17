package io.mango.job.core.service.nativeengine;

import io.mango.common.result.R;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.vo.NoticeSendResultVO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * mango-notice HTTP/API 返回契约适配器。
 */
@Component
public class MangoJobNoticeGateway {

    private final ObjectProvider<NoticeApi> noticeApiProvider;

    public MangoJobNoticeGateway(ObjectProvider<NoticeApi> noticeApiProvider) {
        this.noticeApiProvider = noticeApiProvider;
    }

    public boolean isAvailable() {
        return noticeApiProvider.getIfAvailable() != null;
    }

    public MangoJobNoticeDelivery send(SendNoticeCommand command) {
        NoticeApi noticeApi = noticeApiProvider.getIfAvailable();
        if (noticeApi == null) {
            return new MangoJobNoticeDelivery(false, "mango-notice 未启用");
        }
        R<NoticeSendResultVO> response = noticeApi.send(command);
        if (response == null) {
            return new MangoJobNoticeDelivery(false, "通知接口无响应");
        }
        return new MangoJobNoticeDelivery(response.isSuccess(), response.getMsg());
    }

    public record MangoJobNoticeDelivery(boolean success, String message) {
    }
}
