package io.mango.infra.fileproc.fixture;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "fileproc", contextId = "localFileProcessorFeignClient", path = "/fileproc")
public interface LocalFileProcessorFeignClient extends LocalFileProcessorApi {
}
