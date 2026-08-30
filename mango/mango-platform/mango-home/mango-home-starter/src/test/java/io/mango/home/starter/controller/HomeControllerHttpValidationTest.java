package io.mango.home.starter.controller;

import io.mango.home.core.service.IHomePageService;
import io.mango.home.core.service.IHomeTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 首页 HTTP 参数校验合同测试。 */
@WebMvcTest({HomePageController.class, HomeTemplateController.class, HomeOptionController.class})
@ContextConfiguration(classes = {
        HomeControllerHttpValidationTest.TestApplication.class,
        HomePageController.class,
        HomeTemplateController.class,
        HomeOptionController.class
})
class HomeControllerHttpValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IHomePageService homePageService;

    @MockitoBean
    private IHomeTemplateService homeTemplateService;

    @MockitoBean
    private io.mango.home.core.service.IHomeOptionService homeOptionService;

    @Test
    void createPageRejectsMissingNameThroughApiOwnedValidation() throws Exception {
        mockMvc.perform(post("/home/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void templateDetailRejectsMissingIdentifierThroughApiOwnedValidation() throws Exception {
        mockMvc.perform(get("/home/templates/detail"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveAuthorizationsRejectsNullListButAllowsEmptyListContract() throws Exception {
        mockMvc.perform(put("/home/templates/authorizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":1,\"authorizations\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userOptionsRejectsOversizedResultRequest() throws Exception {
        mockMvc.perform(get("/home/options/page-users").param("size", "201"))
                .andExpect(status().isBadRequest());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
