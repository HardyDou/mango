package io.mango.org.starter.remote;

import io.mango.org.api.PostApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "mango-org", contextId = "postFeignClient", path = "/post")
public interface PostFeignClient extends PostApi {
}
