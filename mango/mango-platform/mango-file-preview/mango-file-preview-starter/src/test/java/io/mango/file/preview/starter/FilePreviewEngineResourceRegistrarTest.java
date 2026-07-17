package io.mango.file.preview.starter;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.command.ApiResourceRegisterRequest;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewEngineResourceRegistrarTest {

    @Test
    void run_registersArchiveDirectoryEndpointAsPublicResource() {
        CapturingApiResourceApi api = new CapturingApiResourceApi();

        new FilePreviewEngineResourceRegistrar(api).run(null);

        assertThat(api.resources)
                .anySatisfy(resource -> {
                    assertThat(resource.getHttpMethod()).isEqualTo("GET");
                    assertThat(resource.getPathPattern()).isEqualTo("/directory");
                    assertThat(resource.getResourceCode()).isEqualTo("GET:/directory");
                    assertThat(resource.getAccessMode()).isEqualTo(ApiResourceAccessMode.PUBLIC);
                })
                .anySatisfy(resource -> {
                    assertThat(resource.getHttpMethod()).isEqualTo("GET");
                    assertThat(resource.getPathPattern()).isEqualTo("/compressed-file");
                    assertThat(resource.getResourceCode()).isEqualTo("GET:/compressed-file");
                    assertThat(resource.getAccessMode()).isEqualTo(ApiResourceAccessMode.PUBLIC);
                })
                .anySatisfy(resource -> {
                    assertThat(resource.getHttpMethod()).isEqualTo("GET");
                    assertThat(resource.getPathPattern()).isEqualTo("/file-preview/sources");
                    assertThat(resource.getResourceCode()).isEqualTo("GET:/file-preview/sources");
                    assertThat(resource.getAccessMode()).isEqualTo(ApiResourceAccessMode.PUBLIC);
                });
    }

    private static class CapturingApiResourceApi implements ApiResourceApi {

        private final List<ApiResourceRegisterCommand> resources = new ArrayList<>();

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(ApiResourceRegisterRequest request) {
            this.resources.addAll(request.getResources());
            return R.ok(new ApiResourceRegisterResultVO(
                    request.getResources().size(), request.getResources().size(), 0));
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            return R.ok(ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN));
        }

        @Override
        public R<Void> refreshApiResourceCache() {
            return R.ok();
        }
    }
}
