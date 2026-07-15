package io.mango.numgen.starter.controller;

import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.NumgenBatchCommand;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.numgen.api.command.NumgenValidateRuleCommand;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.UpdateNumgenGeneratorStatusCommand;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;
import io.mango.numgen.api.vo.NumgenRuleValidationVO;
import io.mango.numgen.core.service.INumgenGeneratorService;
import io.mango.numgen.core.service.INumgenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NumgenHttpContractTest {

    private CapturingNumgenService numgenService;
    private CapturingGeneratorService generatorService;
    private MockMvc numgenMvc;
    private MockMvc generatorMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        numgenService = new CapturingNumgenService();
        generatorService = new CapturingGeneratorService();
        numgenMvc = MockMvcBuilders.standaloneSetup(new NumgenController(numgenService))
                .setValidator(validator)
                .build();
        generatorMvc = MockMvcBuilders.standaloneSetup(new NumgenGeneratorController(generatorService))
                .setValidator(validator)
                .build();
    }

    @Test
    void nextBindsObjectParamsAndKeepsResponseEnvelope() throws Exception {
        numgenMvc.perform(post("/numgen/next")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"genKey":"ORDER_NO","params":{"bizKey":"BIZ-1001","shop":"SH-A"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("ORDER-0001"));

        assertThat(numgenService.nextCommand.getGenKey()).isEqualTo("ORDER_NO");
        assertThat(numgenService.nextCommand.getParams())
                .containsEntry("bizKey", "BIZ-1001")
                .containsEntry("shop", "SH-A");
        numgenService.nextCommand.getParams().put("channel", "APP");
        assertThat(numgenService.nextCommand.getParams()).containsEntry("channel", "APP");
    }

    @Test
    void nextRejectsMissingGeneratorKeyBeforeCallingService() throws Exception {
        numgenMvc.perform(post("/numgen/next")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"params\":{}}"))
                .andExpect(status().isBadRequest());

        assertThat(numgenService.nextCommand).isNull();
    }

    @Test
    void generatorDetailBindsExplicitIdAndWrapsServiceResult() throws Exception {
        generatorMvc.perform(get("/numgen/generators/detail").param("id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.genKey").value("ORDER_NO"));

        assertThat(generatorService.detailId).isEqualTo(42L);
    }

    @Test
    void createGeneratorBindsBodyAndPreservesPostRoute() throws Exception {
        generatorMvc.perform(post("/numgen/generators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"genKey":"ORDER_NO","genName":"订单号","domainCode":"NUMGEN","status":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1001));

        assertThat(generatorService.createCommand.getGenKey()).isEqualTo("ORDER_NO");
        assertThat(generatorService.createCommand.getDomainCode()).isEqualTo("NUMGEN");
    }

    private static final class CapturingNumgenService implements INumgenService {

        private NumgenNextCommand nextCommand;

        @Override
        public String nextValue(NumgenNextCommand command) {
            nextCommand = command;
            return "ORDER-0001";
        }

        @Override
        public List<String> batchValue(NumgenBatchCommand command) {
            return List.of("ORDER-0001");
        }

        @Override
        public NumgenRuleValidationVO validateRule(NumgenValidateRuleCommand command) {
            NumgenRuleValidationVO result = new NumgenRuleValidationVO();
            result.setValid(true);
            return result;
        }
    }

    private static final class CapturingGeneratorService implements INumgenGeneratorService {

        private Long detailId;
        private SaveNumgenGeneratorCommand createCommand;

        @Override
        public PageResult<NumgenGeneratorVO> pageGenerators(NumgenGeneratorPageQuery query) {
            return PageResult.of(List.of(), 0, 1, 20);
        }

        @Override
        public NumgenGeneratorVO detailGenerator(Long id) {
            detailId = id;
            NumgenGeneratorVO result = new NumgenGeneratorVO();
            result.setId(id);
            result.setGenKey("ORDER_NO");
            return result;
        }

        @Override
        public Long createGenerator(SaveNumgenGeneratorCommand command) {
            createCommand = command;
            return 1001L;
        }

        @Override
        public Boolean updateGenerator(SaveNumgenGeneratorCommand command) {
            return Boolean.TRUE;
        }

        @Override
        public Boolean updateGeneratorStatus(UpdateNumgenGeneratorStatusCommand command) {
            return Boolean.TRUE;
        }

        @Override
        public Boolean deleteGenerator(Long id) {
            return Boolean.TRUE;
        }
    }
}
