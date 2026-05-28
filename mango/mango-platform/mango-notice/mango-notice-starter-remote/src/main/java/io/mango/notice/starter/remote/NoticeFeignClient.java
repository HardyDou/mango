package io.mango.notice.starter.remote;

import io.mango.notice.api.NoticeApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "mango-notice", path = "/notice")
public interface NoticeFeignClient extends NoticeApi {
}
