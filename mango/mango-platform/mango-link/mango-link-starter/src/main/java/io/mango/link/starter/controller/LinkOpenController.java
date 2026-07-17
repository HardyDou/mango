package io.mango.link.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.link.api.LinkOpenApi;
import io.mango.link.api.query.LinkPublicItemQuery;
import io.mango.link.api.vo.LinkPublicItemVO;
import io.mango.link.core.service.ILinkOpenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    @Operation(summary = "查询公开网址", description = "匿名查询指定租户已启用且公开的网址列表")
    public R<List<LinkPublicItemVO>> listPublicItems(@ParameterObject LinkPublicItemQuery query) {
        return R.ok(linkOpenService.listPublicItems(query));
    }
}
