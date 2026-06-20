package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.resource.api.ResourceTargetApi;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.model.ResourceSyncResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.Map;

/**
 * 资源目标模块远程执行适配。
 */
@FeignClient(name = "mango-resource", contextId = "resourceTargetFeignClient", path = "/_resource/targets",
        url = "${mango.resource.registry.target-feign-url:http://127.0.0.1}")
public interface ResourceTargetFeignClient extends ResourceTargetApi {

    @Override
    @PostMapping("/upsert-batch")
    R<Map<String, ResourceSyncResult>> upsertBatch(@RequestBody ExecuteResourceTargetCommand command);

    /**
     * 向指定目标服务批量创建或更新资源。
     *
     * @param targetUri 目标服务基础地址。
     * @param command 目标执行命令。
     * @return 同步结果。
     */
    @PostMapping("/_resource/targets/upsert-batch")
    R<Map<String, ResourceSyncResult>> upsertBatch(URI targetUri, @RequestBody ExecuteResourceTargetCommand command);

    @Override
    @PostMapping("/disable")
    R<ResourceSyncResult> disable(@RequestBody ExecuteResourceTargetCommand command);

    /**
     * 向指定目标服务禁用资源。
     *
     * @param targetUri 目标服务基础地址。
     * @param command 目标执行命令。
     * @return 同步结果。
     */
    @PostMapping("/_resource/targets/disable")
    R<ResourceSyncResult> disable(URI targetUri, @RequestBody ExecuteResourceTargetCommand command);

    @Override
    @PostMapping("/delete")
    R<ResourceSyncResult> delete(@RequestBody ExecuteResourceTargetCommand command);

    /**
     * 向指定目标服务删除资源。
     *
     * @param targetUri 目标服务基础地址。
     * @param command 目标执行命令。
     * @return 同步结果。
     */
    @PostMapping("/_resource/targets/delete")
    R<ResourceSyncResult> delete(URI targetUri, @RequestBody ExecuteResourceTargetCommand command);
}
