package io.mango.notice.starter.remote;

import io.mango.common.result.R;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-notice", path = "/notice")
public interface NoticeFeignClient extends NoticeApi {

    @Override
    @GetMapping("/internal/wecom-login-config")
    R<NoticeWecomLoginConfigVO> getWecomLoginConfig(@RequestParam(required = false) Long channelConfigId);
}
