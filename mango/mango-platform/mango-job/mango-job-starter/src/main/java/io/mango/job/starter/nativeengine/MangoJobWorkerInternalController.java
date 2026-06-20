package io.mango.job.starter.nativeengine;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.web.api.Inner;
import io.mango.job.api.MangoJobWorkerApi;
import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import io.mango.job.support.nativeengine.MangoJobWorkerExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Mango Job Worker 内部执行接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/job")
@Tag(name = "Job Worker 内部接口", description = "Mango Job Worker 内部执行接口，仅允许 Mango 内部调用")
public class MangoJobWorkerInternalController implements MangoJobWorkerApi {

    private final MangoJobWorkerExecutor workerExecutor;

    @Value("${mango.job.native.worker-address:}")
    private String configuredWorkerAddress;

    @PostMapping("/internal/workers/execute")
    @Inner
    @ApiAccess(mode = ApiResourceAccessMode.INTERNAL)
    @Operation(summary = "执行 Worker 任务", description = "仅内部调用。JobCenter 通过 Mango 内部调用安全机制派发任务到远程 Worker。")
    @Override
    public R<MangoJobWorkerExecuteResultVO> execute(@Valid @RequestBody MangoJobWorkerExecuteCommand command) {
        return R.ok(workerExecutor.execute(command, workerAddress()));
    }

    private String workerAddress() {
        if (org.springframework.util.StringUtils.hasText(configuredWorkerAddress)) {
            return configuredWorkerAddress.trim();
        }
        return "http://" + hostName() + "/" + ManagementFactory.getRuntimeMXBean().getName();
    }

    private String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown-host";
        }
    }
}
