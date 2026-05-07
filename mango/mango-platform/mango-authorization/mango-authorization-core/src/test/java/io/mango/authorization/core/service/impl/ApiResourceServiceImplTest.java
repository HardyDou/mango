package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.core.entity.ApiResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@DisplayName("ApiResourceServiceImpl Tests")
class ApiResourceServiceImplTest {

    @Test
    @DisplayName("registerApiResources should disable stale controller paths")
    void registerApiResources_withStaleControllerPath_disablesStaleResource() {
        ApiResourceServiceImpl service = spy(new ApiResourceServiceImpl());
        ApiResource current = resource(1L, "mango-system", "GET", "/system/area/tree",
                "io.mango.area.core.controller.SysAreaController", 1);
        ApiResource stale = resource(2L, "mango-system", "GET", "/area/tree",
                "io.mango.area.core.controller.SysAreaController", 1);
        doReturn(List.of(current, stale)).when(service).list(any(Wrapper.class));
        doReturn(true).when(service).updateBatchById(any(Collection.class));
        doReturn(true).when(service).saveBatch(any(Collection.class));

        ApiResourceRegisterCommand command = command("mango-system", "GET", "/system/area/tree",
                "io.mango.area.core.controller.SysAreaController");
        ApiResourceRegisterResultVO result = service.registerApiResources(List.of(command));

        assertEquals(1, result.scanned());
        assertEquals(0, result.created());
        assertEquals(2, result.updated());
        ArgumentCaptor<Collection<ApiResource>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(service).updateBatchById(captor.capture());
        Collection<ApiResource> updatedResources = captor.getValue();
        ApiResource staleUpdate = updatedResources.stream()
                .filter(resource -> Long.valueOf(2L).equals(resource.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, staleUpdate.getStatus());
    }

    private ApiResourceRegisterCommand command(String moduleName, String httpMethod, String path, String handlerClass) {
        ApiResourceRegisterCommand command = new ApiResourceRegisterCommand();
        command.setModuleName(moduleName);
        command.setHttpMethod(httpMethod);
        command.setPathPattern(path);
        command.setHandlerClass(handlerClass);
        command.setHandlerMethod("tree");
        command.setAccessMode(ApiResourceAccessMode.LOGIN);
        command.setResourceCode(httpMethod + ":" + path);
        return command;
    }

    private ApiResource resource(Long id, String moduleName, String httpMethod, String path, String handlerClass, int status) {
        ApiResource resource = new ApiResource();
        resource.setId(id);
        resource.setModuleName(moduleName);
        resource.setHttpMethod(httpMethod);
        resource.setPathPattern(path);
        resource.setHandlerClass(handlerClass);
        resource.setHandlerMethod("tree");
        resource.setAccessMode(ApiResourceAccessMode.LOGIN.name());
        resource.setResourceCode(httpMethod + ":" + path);
        resource.setStatus(status);
        resource.setDeleted(0);
        return resource;
    }
}
