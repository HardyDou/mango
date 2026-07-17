package io.mango.link.starter.controller;

import io.mango.link.core.service.ILinkAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LinkAdminController.class)
@ContextConfiguration(classes = {
        LinkControllerValidationIntegrationTest.TestApplication.class,
        LinkAdminController.class
})
class LinkControllerValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILinkAdminService linkAdminService;

    @Test
    void apiOwnedBodyConstraintsAreAppliedWithoutControllerConstraintConflict() throws Exception {
        mockMvc.perform(post("/link/categories/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apiOwnedScalarConstraintsRejectMissingIdentifier() throws Exception {
        mockMvc.perform(delete("/link/categories/delete"))
                .andExpect(status().isBadRequest());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
