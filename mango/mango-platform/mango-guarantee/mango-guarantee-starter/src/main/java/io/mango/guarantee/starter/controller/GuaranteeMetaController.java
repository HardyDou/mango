package io.mango.guarantee.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.guarantee.api.vo.GuaranteeModuleMetaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/guarantee")
@Tag(name = "保函协同", description = "保函业务跨机构协同接口")
public class GuaranteeMetaController {

    @GetMapping("/meta")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "获取保函协同模块元信息", description = "登录接口。用于确认保函协同模块已装配到当前服务")
    public R<GuaranteeModuleMetaVO> meta() {
        return R.ok(GuaranteeModuleMetaVO.builder()
                .moduleName("mango-guarantee")
                .modulePath("/guarantee")
                .stage("collaboration-foundation")
                .capabilities(List.of("MODULE_ASSEMBLY", "FLYWAY_MIGRATION", "OPENAPI_GROUP"))
                .build());
    }
}
