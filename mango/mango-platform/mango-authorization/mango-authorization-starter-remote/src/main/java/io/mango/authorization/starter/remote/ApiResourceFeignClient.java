package io.mango.authorization.starter.remote;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * API 资源远程客户端。
 *
 * @author hardy
 */
@FeignClient(name = "mango-authorization", path = "/authorization")
public interface ApiResourceFeignClient extends ApiResourceApi {

    @Override
    @PostMapping("/api-resources/register")
    R<ApiResourceRegisterResultVO> registerApiResources(
            @RequestBody List<ApiResourceRegisterCommand> resources);

    @Override
    @GetMapping("/api-resources/access-decision")
    R<ApiResourceAccessDecisionVO> resolveAccessDecision(
            @RequestParam String httpMethod,
            @RequestParam String path);
}
