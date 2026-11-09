package dev.tsukuyomi.libra.controller;

import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationDTO;
import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationResultDTO;
import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import dev.tsukuyomi.libra.exeception.LibraEvaluationException;
import dev.tsukuyomi.libra.service.policy_evaluation.IPolicyEvaluationService;
import dev.tsukuyomi.libra.transformer.policy_evaluation.EvaluatePolicyRequestTransformer;
import dev.tsukuyomi.libra.transformer.policy_evaluation.PolicyEvaluationResponseTransformer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static dev.tsukuyomi.libra.controller.Futures.done;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(PolicyEvaluationController.class)
@Import({EvaluatePolicyRequestTransformer.class, PolicyEvaluationResponseTransformer.class})
class PolicyEvaluationControllerTest {

    private static final String VALID_REQUEST = """
            {"tenantID": 1, "scopeID": 2, "entityID": " user-1 ", "inputAttrs": {"amount": 500}}""";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IPolicyEvaluationService policyEvaluationService;

    private MvcTestResult evaluate(String body) {
        return mvc.post().uri("/v1/evaluate").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private static PolicyEvaluationResultDTO result(long policyID, String code, EvaluationResult result,
                                                    Map<String, Object> errors) {
        return PolicyEvaluationResultDTO.builder()
                .tenantID(1L).scopeID(2L).policyID(policyID).entityID("*")
                .policyCode(code).result(result).errors(errors)
                .build();
    }

    /**
     * Answers like the real service: fills in the request id and decision on the DTO it was given.
     */
    private void givenDecision(EvaluationResult decision, List<String> denied, PolicyEvaluationResultDTO... results) {
        when(policyEvaluationService.evaluatePolicies(any())).thenAnswer(invocation -> {
            PolicyEvaluationDTO dto = invocation.getArgument(0);
            dto.setRequestID("req-1");
            dto.setEvaluationResults(List.of(results));
            dto.setEvaluationResult(decision);
            dto.setDeniedPolicies(denied);
            return done(dto);
        });
    }

    @Test
    void returns200WithAllow() {
        givenDecision(EvaluationResult.ALLOW, null, result(10L, "limit", EvaluationResult.ALLOW, null));

        MvcTestResult result = evaluate(VALID_REQUEST);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.requestID").isEqualTo("req-1");
        assertThat(result).bodyJson().extractingPath("$.result").isEqualTo("ALLOW");
        assertThat(result).bodyJson().extractingPath("$.entityID").isEqualTo("user-1");
        assertThat(result).bodyJson().extractingPath("$.deniedPolicies").asArray().isEmpty();
        assertThat(result).bodyJson().extractingPath("$.evaluations[0].policyID").isEqualTo(10);
        assertThat(result).bodyJson().extractingPath("$.evaluations[0].policyCode").isEqualTo("limit");
        assertThat(result).bodyJson().extractingPath("$.evaluations[0].result").isEqualTo("ALLOW");

        ArgumentCaptor<PolicyEvaluationDTO> captor = ArgumentCaptor.forClass(PolicyEvaluationDTO.class);
        verify(policyEvaluationService).evaluatePolicies(captor.capture());
        PolicyEvaluationDTO sent = captor.getValue();
        assertThat(sent.getTenantID()).isEqualTo(1L);
        assertThat(sent.getScopeID()).isEqualTo(2L);
        assertThat(sent.getEntityID()).isEqualTo("user-1");
        assertThat(sent.getInputAttrs()).containsEntry("amount", 500);
    }

    @Test
    void returns200WithDenyAndDeniedPolicies() {
        givenDecision(EvaluationResult.DENY, List.of("quiet-hours", "broken"),
                result(10L, "limit", EvaluationResult.ALLOW, null),
                result(11L, "quiet-hours", EvaluationResult.DENY, null),
                result(12L, "broken", EvaluationResult.DENY, Map.of("msg", "boom")));

        MvcTestResult result = evaluate(VALID_REQUEST);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.result").isEqualTo("DENY");
        assertThat(result).bodyJson().extractingPath("$.deniedPolicies").asArray()
                .containsExactly("quiet-hours", "broken");
        assertThat(result).bodyJson().extractingPath("$.evaluations").asArray().hasSize(3);
        assertThat(result).bodyJson().extractingPath("$.evaluations[2].errors.msg").isEqualTo("boom");
    }

    @Test
    void allowsWhenNoPolicyApplies() {
        givenDecision(EvaluationResult.ALLOW, null);

        MvcTestResult result = evaluate("""
                {"tenantID": 1, "scopeID": 2, "entityID": "user-1"}""");

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.result").isEqualTo("ALLOW");
        assertThat(result).bodyJson().extractingPath("$.evaluations").asArray().isEmpty();

        ArgumentCaptor<PolicyEvaluationDTO> captor = ArgumentCaptor.forClass(PolicyEvaluationDTO.class);
        verify(policyEvaluationService).evaluatePolicies(captor.capture());
        assertThat(captor.getValue().getInputAttrs()).isEmpty();
    }

    @Test
    void rejectsMissingTenant() {
        assertThat(evaluate("""
                {"scopeID": 2, "entityID": "user-1"}""")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(policyEvaluationService);
    }

    @Test
    void rejectsNonPositiveScope() {
        assertThat(evaluate("""
                {"tenantID": 1, "scopeID": 0, "entityID": "user-1"}""")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(policyEvaluationService);
    }

    @Test
    void rejectsBlankEntity() {
        assertThat(evaluate("""
                {"tenantID": 1, "scopeID": 2, "entityID": "  "}""")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(policyEvaluationService);
    }

    @Test
    void rejectsNonObjectInputAttrs() {
        assertThat(evaluate("""
                {"tenantID": 1, "scopeID": 2, "entityID": "user-1", "inputAttrs": [1, 2]}"""))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(policyEvaluationService);
    }

    @Test
    void returns500WhenEvaluationFails() {
        when(policyEvaluationService.evaluatePolicies(any())).thenReturn(CompletableFuture.failedFuture(
                new CompletionException(new LibraEvaluationException("error when persisting evals for audit",
                        new RuntimeException("db down")))));

        MvcTestResult result = evaluate(VALID_REQUEST);

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("internal server error");
    }

}
