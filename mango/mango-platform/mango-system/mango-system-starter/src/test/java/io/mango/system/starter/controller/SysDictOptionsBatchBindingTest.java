package io.mango.system.starter.controller;

import io.mango.infra.web.starter.MangoWebProperties;
import io.mango.infra.web.starter.WebAutoConfiguration;
import io.mango.system.api.vo.DictOptionGroupVO;
import io.mango.system.core.service.IDictService;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysDictOptionsBatchBindingTest {

    @Test
    void commaSeparatedTypeCodesBindToCollection() throws Exception {
        IDictService service = mock(IDictService.class);
        Collection<String>[] captured = capturedCodes(service);
        MockMvc mvc = mockMvc(new SysDictController(service));

        mvc.perform(get("/system/dict/data/options/batch").param("typeCodes", "order_status,after_sale"))
                .andExpect(status().isOk());

        assertThat(captured[0]).containsExactly("order_status", "after_sale");
    }

    @Test
    void singleTypeCodeKeepsWorkingForCompatibleCallers() throws Exception {
        IDictService service = mock(IDictService.class);
        Collection<String>[] captured = capturedCodes(service);
        MockMvc mvc = mockMvc(new SysDictController(service));

        mvc.perform(get("/system/dict/data/options/batch").param("typeCodes", "order_status"))
                .andExpect(status().isOk());

        assertThat(captured[0]).containsExactly("order_status");
    }

    @Test
    void repeatedTypeCodeParametersAreMerged() throws Exception {
        IDictService service = mock(IDictService.class);
        Collection<String>[] captured = capturedCodes(service);
        MockMvc mvc = mockMvc(new SysDictController(service));

        mvc.perform(get("/system/dict/data/options/batch")
                        .param("typeCodes", "order_status", "after_sale"))
                .andExpect(status().isOk());

        assertThat(captured[0]).containsExactly("order_status", "after_sale");
    }

    @SuppressWarnings("unchecked")
    private Collection<String>[] capturedCodes(IDictService service) {
        Collection<String>[] captured = new Collection[1];
        when(service.getOptionsBatch(any())).thenAnswer(invocation -> {
            captured[0] = invocation.getArgument(0);
            return List.<DictOptionGroupVO>of();
        });
        return captured;
    }

    private MockMvc mockMvc(Object controller) {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        new WebAutoConfiguration(new MangoWebProperties()).addFormatters(conversionService);
        return MockMvcBuilders.standaloneSetup(controller)
                .setConversionService(conversionService)
                .build();
    }
}
