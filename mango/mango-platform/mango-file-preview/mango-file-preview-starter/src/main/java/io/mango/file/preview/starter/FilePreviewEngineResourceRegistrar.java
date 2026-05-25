package io.mango.file.preview.starter;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

/**
 * 注册预览引擎渲染页所需的公开资源。
 */
@RequiredArgsConstructor
public class FilePreviewEngineResourceRegistrar implements ApplicationRunner {

    private static final String MODULE_NAME = "mango-file-preview";
    private static final String HANDLER_CLASS = "cn.keking.web.controller.OnlinePreviewController";

    private final ApiResourceApi apiResourceApi;

    @Override
    public void run(ApplicationArguments args) {
        apiResourceApi.registerApiResources(List.of(
                publicGet("/onlinePreview", "在线预览页面"),
                publicGet("/picturesPreview", "图片预览页面"),
                publicGet("/getCorsFile", "读取跨域预览文件"),
                publicGet("/file-preview/files/preview-entry", "文件预览临时入口"),
                publicGet("/pdfjs/**", "PDF 预览静态资源"),
                publicGet("/js/**", "预览脚本资源"),
                publicGet("/css/**", "预览样式资源"),
                publicGet("/images/**", "预览图片资源"),
                publicGet("/bootstrap/**", "预览 Bootstrap 资源"),
                publicGet("/highlight/**", "预览代码高亮资源"),
                publicGet("/xlsx/**", "Excel 预览静态资源"),
                publicGet("/static/**", "预览扩展静态资源"),
                publicGet("/favicon.ico", "预览站点图标")
        ));
    }

    private static ApiResourceRegisterCommand publicGet(String pathPattern, String description) {
        ApiResourceRegisterCommand command = new ApiResourceRegisterCommand();
        command.setModuleName(MODULE_NAME);
        command.setHttpMethod("GET");
        command.setPathPattern(pathPattern);
        command.setResourceCode("GET:" + pathPattern);
        command.setAccessMode(ApiResourceAccessMode.PUBLIC);
        command.setHandlerClass(HANDLER_CLASS);
        command.setHandlerMethod("mango.file-preview.engine");
        command.setDescription(description);
        return command;
    }
}
