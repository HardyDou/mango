package io.mango.template.starter.controller;

import io.mango.template.api.command.CreateTemplateCommand;
import io.mango.template.api.command.TemplateRenderCommand;
import io.mango.template.api.vo.TemplateRenderResultVO;
import io.mango.template.core.service.ITemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TemplateControllerHttpTest {

    private ITemplateService templateService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        templateService = mock(ITemplateService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new TemplateController(templateService))
                .setValidator(validator)
                .build();
    }

    @Test
    void createKeepsRoutePayloadAndResponseEnvelope() throws Exception {
        when(templateService.create(any(CreateTemplateCommand.class))).thenReturn(1001L);

        mockMvc.perform(post("/template/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateCode":"contract.notice","templateName":"合同通知",
                                 "domainCode":"CONTRACT","sourceFormat":"TEXT","draftVariables":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1001));

        ArgumentCaptor<CreateTemplateCommand> captor = ArgumentCaptor.forClass(CreateTemplateCommand.class);
        verify(templateService).create(captor.capture());
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("contract.notice");
        assertThat(captor.getValue().getSourceFormat()).isEqualTo("TEXT");
    }

    @Test
    void createRejectsMissingRequiredDomainBeforeCallingService() throws Exception {
        mockMvc.perform(post("/template/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateCode\":\"contract.notice\",\"templateName\":\"合同通知\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renderKeepsVariablesAsJsonObject() throws Exception {
        TemplateRenderResultVO result = new TemplateRenderResultVO();
        result.setRecordId(2001L);
        result.setStatus("SUCCESS");
        result.setContent("合同编号：C-001");
        when(templateService.render(any(TemplateRenderCommand.class))).thenReturn(result);

        mockMvc.perform(post("/template/templates/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateCode":"contract.notice","outputFormat":"TEXT",
                                 "variables":{"contractNo":"C-001"},"async":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(2001))
                .andExpect(jsonPath("$.data.content").value("合同编号：C-001"));

        ArgumentCaptor<TemplateRenderCommand> captor = ArgumentCaptor.forClass(TemplateRenderCommand.class);
        verify(templateService).render(captor.capture());
        assertThat(captor.getValue().getVariables().toMap()).containsEntry("contractNo", "C-001");
    }
}
