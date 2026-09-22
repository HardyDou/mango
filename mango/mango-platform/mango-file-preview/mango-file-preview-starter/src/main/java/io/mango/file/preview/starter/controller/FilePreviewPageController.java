package io.mango.file.preview.starter.controller;

import io.mango.file.api.vo.FileDownloadVO;
import io.mango.common.contract.NativeHttpAdapter;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.task.IFilePreviewTaskService;
import io.mango.file.preview.api.vo.FilePreviewTaskVO;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件预览页面适配器。
 */
@Validated
@RestController
@NativeHttpAdapter
@RequestMapping("/file-preview")
@RequiredArgsConstructor
@Tag(name = "文件预览页面", description = "文件预览页面跳转接口")
public class FilePreviewPageController {

    private static final int HTTP_FOUND = 302;

    private final IFilePreviewService filePreviewService;
    private final IFilePreviewTaskService previewTaskService;

    @GetMapping("/files/preview")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "跳转文件预览页", description = "登录接口。按文件ID跳转到当前租户可见文件的在线预览页面")
    public ModelAndView redirectPreview(
            @Parameter(description = "文件ID", required = true)
            @RequestParam("fileId") @NotNull(message = "文件ID不能为空") Long fileId,
            @Parameter(description = "是否确认继续转换大文件")
            @RequestParam(value = "allowLargeFile", defaultValue = "false") boolean allowLargeFile) {
        previewTaskService.submit(fileId, allowLargeFile);
        FilePreviewLinkVO link = filePreviewService.createPreview(fileId);
        return redirectView(link.getPreviewUrl());
    }

    @GetMapping("/files/preview-entry")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "文件预览临时入口")
    @Operation(summary = "跳转临时文件预览页", description = "公开接口。使用已鉴权接口签发的短期令牌跳转到在线预览页面")
    public ModelAndView redirectPreviewEntry(
            @Parameter(description = "预览入口临时令牌", required = true)
            @RequestParam("token") @NotBlank(message = "预览入口临时令牌不能为空") String token) {
        return redirectPreviewEntry(token, false, false);
    }

    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "文件预览临时入口")
    public ModelAndView redirectPreviewEntry(
            String token,
            @Parameter(description = "是否直接打开已生成产物")
            @RequestParam(value = "ready", defaultValue = "false") boolean ready,
            @Parameter(description = "是否确认继续转换大文件")
            @RequestParam(value = "allowLargeFile", defaultValue = "false") boolean allowLargeFile) {
        FilePreviewTaskVO task = filePreviewService.previewTaskByToken(token, allowLargeFile);
        if (!ready) {
            if (task.getStatus() != io.mango.file.preview.api.enums.FilePreviewTaskStatus.SUCCEEDED) {
                return progressPage(token, task);
            }
        }
        // Office 转换成功后优先使用已经持久化的 PDF，避免再次进入 LibreOffice。
        // 仍然通过 PDF.js 页面展示，避免浏览器直接打开 PDF 产物时使用原生 PDF 查看器。
        if (task.getStatus() == io.mango.file.preview.api.enums.FilePreviewTaskStatus.SUCCEEDED
                && task.getPreviewFileId() != null) {
            return pdfViewerPage(token, true);
        }
        // 原生 PDF 不需要转换，直接按入口令牌读取文件内容并交给 PDF.js。
        if (task.getStatus() == io.mango.file.preview.api.enums.FilePreviewTaskStatus.SUCCEEDED
                && filePreviewService.isPdfPreview(token)) {
            return pdfViewerPage(token, false);
        }
        FilePreviewLinkVO link = filePreviewService.createEnginePreviewByToken(token);
        return redirectView(link.getPreviewUrl());
    }

    @GetMapping(value = "/files/preview-artifact")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "文件预览已生成产物")
    @Operation(summary = "读取预览产物", description = "公开接口。按预览入口令牌读取已经生成的 PDF 产物")
    public void previewArtifact(
            @Parameter(description = "预览入口临时令牌", required = true)
            @RequestParam("token") @NotBlank(message = "预览入口临时令牌不能为空") String token,
            HttpServletResponse response) throws IOException {
        FileDownloadVO download;
        try {
            download = filePreviewService.downloadPreviewArtifact(token);
        } catch (RuntimeException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (download.inputStream() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(download.contentType() == null ? "application/pdf" : download.contentType());
        if (download.contentLength() >= 0) {
            response.setContentLengthLong(download.contentLength());
        }
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''"
                + urlEncode(download.fileName() == null ? "preview.pdf" : download.fileName()));
        try (var input = download.inputStream()) {
            input.transferTo(response.getOutputStream());
        }
    }

    @GetMapping(value = "/files/preview-content")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "文件预览原始内容")
    @Operation(summary = "读取原始预览内容", description = "公开接口。按预览入口令牌读取无需转换的原始 PDF 内容")
    public void previewContent(
            @Parameter(description = "预览入口临时令牌", required = true)
            @RequestParam("token") @NotBlank(message = "预览入口临时令牌不能为空") String token,
            HttpServletResponse response) throws IOException {
        FileDownloadVO download;
        try {
            download = filePreviewService.downloadPreviewSource(token);
        } catch (RuntimeException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (download.inputStream() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(download.contentType() == null ? "application/octet-stream" : download.contentType());
        if (download.contentLength() >= 0) {
            response.setContentLengthLong(download.contentLength());
        }
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''"
                + urlEncode(download.fileName() == null ? "preview-file" : download.fileName()));
        try (var input = download.inputStream()) {
            input.transferTo(response.getOutputStream());
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private ModelAndView pdfViewerPage(String token, boolean artifact) {
        String artifactPath = "/api/file-preview/files/"
                + (artifact ? "preview-artifact" : "preview-content")
                + "?token=" + urlEncode(token);
        String safeArtifactPath = artifactPath.replace("\\", "\\\\").replace("'", "\\'");
        String htmlTemplate = """
                <!doctype html><html lang='zh-CN'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
                <title>文件预览</title><style>html,body{width:100%%;height:100%%;margin:0;overflow:hidden;background:#2b2b2b}iframe{width:100%%;height:100%%;border:0}</style>
                </head><body><iframe id='pdf-viewer' title='文件预览'></iframe><script>
                const artifactPath='%s';
                const artifactUrl=new URL(artifactPath,window.location.href).href;
                const viewer=document.getElementById('pdf-viewer');
                viewer.src='/pdfjs/web/viewer.html?file='+encodeURIComponent(artifactUrl)+'#pagemode=none';
                function applyDefaultView(){
                  const app=viewer.contentWindow && viewer.contentWindow.PDFViewerApplication;
                  if(!app || !app.pdfViewer){setTimeout(applyDefaultView,200);return;}
                  app.pdfViewer.scrollMode=2;
                  app.pdfViewer.spreadMode=0;
                }
                viewer.addEventListener('load',()=>{applyDefaultView();setTimeout(applyDefaultView,500);});
                </script></body></html>
                """;
        String html = htmlTemplate.replace("\n", "%n").formatted(safeArtifactPath);
        View view = (model, request, response) -> {
            response.setContentType(MediaType.TEXT_HTML_VALUE + ";charset=UTF-8");
            response.getWriter().write(html);
        };
        return new ModelAndView(view);
    }

    @GetMapping(value = "/files/preview-progress", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "文件预览转换状态")
    @Operation(summary = "查询预览转换状态", description = "公开接口。按预览入口令牌查询排队、转换或完成状态")
    public R<FilePreviewTaskVO> previewProgress(
            @Parameter(description = "预览入口临时令牌", required = true)
            @RequestParam("token") @NotBlank(message = "预览入口临时令牌不能为空") String token,
            @Parameter(description = "是否确认继续转换大文件")
            @RequestParam(value = "allowLargeFile", defaultValue = "false") boolean allowLargeFile) {
        return R.ok(filePreviewService.previewTaskByToken(token, allowLargeFile));
    }

    private ModelAndView progressPage(String token, FilePreviewTaskVO task) {
        String safeToken = token.replace("\\", "\\\\").replace("'", "\\'");
        String message = task.getMessage() == null ? "文件正在转换，请稍候" : task.getMessage();
        String safeMessage = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String status = switch (task.getStatus()) {
            case CONFIRM_REQUIRED -> "等待确认";
            case QUEUED -> "排队中";
            case PROCESSING -> "转换中";
            case SUCCEEDED -> "已完成";
            case FAILED -> "转换失败";
        };
        String htmlTemplate = """
                <!doctype html><html lang='zh-CN'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
                <title>文件预览准备中</title><style>
                *{box-sizing:border-box}body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:#f5f7fb;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Microsoft YaHei',sans-serif;color:#1f2937}
                .card{width:min(560px,calc(100%% - 32px));padding:40px;border-radius:18px;background:#fff;box-shadow:0 12px 40px rgba(31,41,55,.12);text-align:center}.spinner{width:52px;height:52px;margin:0 auto 24px;border:5px solid #dbeafe;border-top-color:#2563eb;border-radius:50%%;animation:spin 1s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
                h1{font-size:24px;margin:0 0 12px}.message{color:#6b7280;margin:0 0 12px}.status{color:#2563eb;font-weight:600}.button{margin-top:24px;padding:10px 24px;border:0;border-radius:8px;background:#2563eb;color:white;cursor:pointer;font-size:15px}.secondary{background:#e5e7eb;color:#1f2937;text-decoration:none;margin-right:8px}.tip{margin-top:24px;padding:12px;border-radius:10px;background:#eff6ff;color:#1d4ed8;font-size:14px}
                </style></head><body><main class='card'><div class='spinner' id='spinner'></div><h1 id='title'>文件正在转换</h1><p class='message' id='message'>%s</p><div class='status' id='status'>%s</div><div id='actions'><button class='button' onclick='location.reload()'>立即刷新</button></div><div class='tip' id='tip'>大文件转换需要一些时间，请不要关闭此页面。</div></main>
                <script>const token='%s';let allowLargeFile=false;const labels={CONFIRM_REQUIRED:'等待确认',QUEUED:'排队中',PROCESSING:'转换中',SUCCEEDED:'已完成',FAILED:'转换失败'};async function poll(){try{const r=await fetch('/api/file-preview/files/preview-progress?token='+encodeURIComponent(token)+'&allowLargeFile='+allowLargeFile);const j=await r.json();const t=j.data||{};document.getElementById('message').textContent=t.message||'文件正在转换，请稍候';document.getElementById('status').textContent=labels[t.status]||'处理中';if(t.status==='QUEUED'&&t.queueAhead!==undefined){document.getElementById('tip').textContent='前面还有 '+t.queueAhead+' 个任务'+(t.workerCount?'，使用 '+t.workerCount+' 个转换进程':'');}if(t.status==='CONFIRM_REQUIRED'){document.getElementById('spinner').style.display='none';document.getElementById('title').textContent='文件较大';document.getElementById('tip').textContent='建议下载查看，也可以继续等待在线转换';document.getElementById('actions').innerHTML='<a class="button secondary" href="/api/file/files/download?id='+encodeURIComponent(t.fileId)+'" target="_blank">下载查看</a><button class="button" onclick="continuePreview()">继续等待预览</button>';return}if(t.status==='SUCCEEDED'){location.href='/api/file-preview/files/preview-entry?token='+encodeURIComponent(token)+'&ready=true';return}if(t.status==='FAILED'){document.getElementById('spinner').style.display='none';document.getElementById('tip').textContent=t.message||'预览生成失败，请下载原文件查看';return}}catch(e){document.getElementById('tip').textContent='正在检查转换状态，请稍候...'}setTimeout(poll,2000)}async function continuePreview(){allowLargeFile=true;document.getElementById('actions').innerHTML='<button class="button" onclick="location.reload()">立即刷新</button>';document.getElementById('spinner').style.display='block';document.getElementById('title').textContent='文件正在转换';await poll()}poll();</script></body></html>
                """;
        String html = htmlTemplate.replace("\n", "%n").formatted(safeMessage, status, safeToken);
        View view = (model, request, response) -> {
            response.setContentType(MediaType.TEXT_HTML_VALUE + ";charset=UTF-8");
            response.getWriter().write(html);
        };
        return new ModelAndView(view);
    }

    private ModelAndView redirectView(String location) {
        View view = (model, request, response) -> {
            response.setStatus(HTTP_FOUND);
            response.setHeader("Location", location);
        };
        return new ModelAndView(view);
    }
}
