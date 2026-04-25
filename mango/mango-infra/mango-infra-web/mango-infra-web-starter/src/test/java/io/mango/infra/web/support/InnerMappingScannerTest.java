package io.mango.infra.web.support;

import io.mango.infra.web.api.Inner;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InnerMappingScannerTest {

    @Test
    void scanShouldDiscoverInnerAnnotationOnControllerMethods() throws Exception {
        RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
        mapping.setApplicationContext(new org.springframework.context.support.StaticApplicationContext());
        mapping.afterPropertiesSet();
        mapping.registerMapping(
                org.springframework.web.servlet.mvc.method.RequestMappingInfo.paths("/demo/inner").build(),
                new DemoController(),
                DemoController.class.getMethod("inner"));

        InnerMappingInternalPathProvider provider = new InnerMappingInternalPathProvider();
        new InnerMappingScanner(mapping, provider).scan();

        assertTrue(provider.getInternalPaths().contains("/demo/inner"));
    }

    @Test
    void scanShouldDiscoverInnerAnnotationOnApiInterfaceMethods() throws Exception {
        RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
        mapping.setApplicationContext(new org.springframework.context.support.StaticApplicationContext());
        mapping.afterPropertiesSet();
        mapping.registerMapping(
                org.springframework.web.servlet.mvc.method.RequestMappingInfo.paths("/demo/api-inner").build(),
                new ApiBackedController(),
                ApiBackedController.class.getMethod("apiInner"));

        InnerMappingInternalPathProvider provider = new InnerMappingInternalPathProvider();
        new InnerMappingScanner(mapping, provider).scan();

        assertTrue(provider.getInternalPaths().contains("/demo/api-inner"));
    }

    @Controller
    @RequestMapping("/demo")
    static class DemoController {
        @Inner
        @PostMapping("/inner")
        public void inner() {
        }

        @GetMapping("/outer")
        public void outer() {
        }
    }

    interface DemoApi {
        @Inner
        void apiInner();
    }

    @Controller
    static class ApiBackedController implements DemoApi {
        @Override
        public void apiInner() {
        }
    }
}
