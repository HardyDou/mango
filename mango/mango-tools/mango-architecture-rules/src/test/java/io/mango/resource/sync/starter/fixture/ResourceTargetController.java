package io.mango.resource.sync.starter.fixture;

import io.mango.resource.api.fixture.ResourceTargetApi;
import io.mango.resource.support.fixture.ResourceTargetExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resource/targets")
public final class ResourceTargetController implements ResourceTargetApi {

    private final ResourceTargetExecutor executor;

    public ResourceTargetController(ResourceTargetExecutor executor) {
        this.executor = executor;
    }

    @Override
    @GetMapping("/execute")
    public String execute() {
        return executor.execute();
    }
}
