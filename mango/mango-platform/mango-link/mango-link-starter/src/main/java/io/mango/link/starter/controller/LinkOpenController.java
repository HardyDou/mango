package io.mango.link.starter.controller;

import io.mango.common.result.R;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.link.api.LinkOpenApi;
import io.mango.link.api.query.LinkPublicItemQuery;
import io.mango.link.api.vo.LinkPublicItemVO;
import io.mango.link.core.service.ILinkOpenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/link/open")
@RequiredArgsConstructor
@Validated
@Tag(name = "网址公开接口", description = "公开只读网址接口")
public class LinkOpenController implements LinkOpenApi {

    private final ILinkOpenService linkOpenService;

    @Override
    @GetMapping("/public-links/list")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "查询公开网址")
    @Operation(summary = "查询公开网址")
    public R<List<LinkPublicItemVO>> listPublicItems(@Valid @ParameterObject LinkPublicItemQuery query) {
        return R.ok(linkOpenService.listPublicItems(query));
    }

    @GetMapping("/redirect/{id}")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "跳转网址并记录访问")
    @Operation(summary = "跳转网址并记录访问")
    public ResponseEntity<Void> redirect(
            @PathVariable @NotNull(message = "网址 ID 不能为空") Long id,
            @RequestParam(required = false) String source,
            HttpServletRequest request) {
        String targetUrl = linkOpenService.resolveRedirectUrl(id, source, clientIp(request),
                request.getHeader(HttpHeaders.USER_AGENT), request.getHeader(HttpHeaders.REFERER));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(targetUrl)).build();
    }

    @GetMapping("/jump")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "按 URL 跳转网址并记录访问")
    @Operation(summary = "按 URL 跳转网址并记录访问")
    public ResponseEntity<Void> jump(
            @RequestParam String url,
            @RequestParam(required = false) String uid,
            @RequestParam(required = false) String source,
            HttpServletRequest request) {
        String targetUrl = linkOpenService.resolveJumpUrl(url, uid, source, extraParams(request),
                clientIp(request), request.getHeader(HttpHeaders.USER_AGENT), request.getHeader(HttpHeaders.REFERER));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(targetUrl)).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extraParams(HttpServletRequest request) {
        Set<String> excluded = Set.of("url", "uid", "source", "token", "accessToken", "password", "secret");
        return request.getParameterMap().entrySet().stream()
                .filter(entry -> !excluded.contains(entry.getKey()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                .collect(Collectors.joining("&"));
    }
}
