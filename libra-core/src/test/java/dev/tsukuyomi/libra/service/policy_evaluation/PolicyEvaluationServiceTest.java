package dev.tsukuyomi.libra.service.policy_evaluation;

import dev.tsukuyomi.libra.dao.policy_evaluation.PolicyEvaluationDAO;
import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationDTO;
import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationResultDTO;
import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.entity.PolicyEvaluation;
import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import dev.tsukuyomi.libra.domain.enums.JsIdenfier;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.engine.GraalJSPolicyEngine;
import dev.tsukuyomi.libra.engine.IPolicyEngine;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import dev.tsukuyomi.libra.exeception.LibraEvaluationException;
import dev.tsukuyomi.libra.exeception.LibraServiceException;
import dev.tsukuyomi.libra.service.policy.IPolicyService;
import dev.tsukuyomi.libra.utils.JsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyEvaluationServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long SCOPE_ID = 3L;
    private static final String ENTITY_ID = "user-1";
    private static final Map<String, Object> INPUT_ATTRS = Map.of("amount", 50L, "limit", 100L);
    private static final String INPUT_JSON = JsonUtil.toJson(INPUT_ATTRS);
    // the engine is mocked, so the script body is never run; it only needs to be non-blank
    private static final String APPLIED_JS = "function evaluate(input) { return true; }";

    @Mock
    private IPolicyService policyService;

    @Mock
    private IPolicyEngine policyEngine;

    @Mock
    private PolicyEvaluationDAO policyEvaluationDAO;

    private ExecutorService executor;
    private PolicyEvaluationService evaluationService;

    private static Policy policy(Long id, String code) {
        return policy(id, code, APPLIED_JS);
    }

    private static Policy policy(Long id, String code, String appliedJs) {
        return Policy.builder()
                .id(id)
                .tenantID(TENANT_ID)
                .scopeID(SCOPE_ID)
                .entityID(ENTITY_ID)
                .code(code)
                .appliedJs(appliedJs)
                .status(PolicyStatus.ACTIVE)
                .validFrom(100L)
                .version(1L)
                .build();
    }

    private static PolicyEvaluationDTO request() {
        return PolicyEvaluationDTO.builder()
                .tenantID(TENANT_ID)
                .scopeID(SCOPE_ID)
                .entityID(ENTITY_ID)
                .inputAttrs(INPUT_ATTRS)
                .build();
    }

    private void givenApplicablePolicies(Policy... policies) {
        when(policyService.getApplicablePolicies(eq(TENANT_ID), eq(SCOPE_ID), eq(ENTITY_ID), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(List.of(policies)));
    }

    private void givenEngine(Policy policy, JsIdenfier identifier, boolean result) {
        when(policyEngine.evaluate(policy, identifier, INPUT_JSON)).thenReturn(result);
    }

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        evaluationService = new PolicyEvaluationService(policyService, policyEngine, policyEvaluationDAO, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void looksUpApplicablePoliciesForRequestAtCurrentTime() {
        givenApplicablePolicies();
        ArgumentCaptor<Long> at = ArgumentCaptor.forClass(Long.class);

        long before = Instant.now().toEpochMilli();
        evaluationService.evaluatePoliciesForUser(request()).join();
        long after = Instant.now().toEpochMilli();

        verify(policyService).getApplicablePolicies(eq(TENANT_ID), eq(SCOPE_ID), eq(ENTITY_ID), at.capture());
        assertThat(at.getValue()).isBetween(before, after);
    }

    @Test
    void returnsEmptyWhenNoPoliciesAreApplicable() {
        givenApplicablePolicies();

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join()).isEmpty();
        verifyNoInteractions(policyEngine);
    }

    @Test
    void allowsWhenPolicyAppliesAndEvaluatesTrue() {
        Policy p = policy(11L, "credit-limit");
        givenApplicablePolicies(p);
        givenEngine(p, JsIdenfier.APPLIES, true);
        givenEngine(p, JsIdenfier.POLICY, true);

        List<PolicyEvaluationResultDTO> results = evaluationService.evaluatePoliciesForUser(request()).join();

        assertThat(results).singleElement().satisfies(r -> {
            assertThat(r.getTenantID()).isEqualTo(TENANT_ID);
            assertThat(r.getPolicyID()).isEqualTo(11L);
            assertThat(r.getScopeID()).isEqualTo(SCOPE_ID);
            assertThat(r.getEntityID()).isEqualTo(ENTITY_ID);
            assertThat(r.getPolicyCode()).isEqualTo("credit-limit");
            assertThat(r.getResult()).isEqualTo(EvaluationResult.ALLOW);
            assertThat(r.getErrors()).isNull();
        });
    }

    @Test
    void deniesWhenPolicyAppliesAndEvaluatesFalse() {
        Policy p = policy(11L, "credit-limit");
        givenApplicablePolicies(p);
        givenEngine(p, JsIdenfier.APPLIES, true);
        givenEngine(p, JsIdenfier.POLICY, false);

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                .extracting(PolicyEvaluationResultDTO::getResult)
                .containsExactly(EvaluationResult.DENY);
    }

    @Test
    void skipsPoliciesThatDoNotApply() {
        Policy applies = policy(11L, "credit-limit");
        Policy skipped = policy(12L, "no-sms-after-22");
        givenApplicablePolicies(applies, skipped);
        givenEngine(applies, JsIdenfier.APPLIES, true);
        givenEngine(skipped, JsIdenfier.APPLIES, false);
        givenEngine(applies, JsIdenfier.POLICY, true);

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                .extracting(PolicyEvaluationResultDTO::getPolicyCode)
                .containsExactly("credit-limit");
        verify(policyEngine, never()).evaluate(skipped, JsIdenfier.POLICY, INPUT_JSON);
    }

    @Test
    void returnsEmptyWhenNoPolicyApplies() {
        Policy p1 = policy(11L, "credit-limit");
        Policy p2 = policy(12L, "no-sms-after-22");
        givenApplicablePolicies(p1, p2);
        givenEngine(p1, JsIdenfier.APPLIES, false);
        givenEngine(p2, JsIdenfier.APPLIES, false);

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join()).isEmpty();
        verify(policyEngine, never()).evaluate(any(), eq(JsIdenfier.POLICY), anyString());
    }

    @Test
    void preservesPolicyOrderAndMixesResults() {
        Policy p1 = policy(11L, "a");
        Policy p2 = policy(12L, "b");
        Policy p3 = policy(13L, "c");
        givenApplicablePolicies(p1, p2, p3);
        givenEngine(p1, JsIdenfier.APPLIES, true);
        givenEngine(p2, JsIdenfier.APPLIES, true);
        givenEngine(p3, JsIdenfier.APPLIES, true);
        givenEngine(p1, JsIdenfier.POLICY, false);
        givenEngine(p2, JsIdenfier.POLICY, true);
        givenEngine(p3, JsIdenfier.POLICY, false);

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                .extracting(PolicyEvaluationResultDTO::getPolicyCode, PolicyEvaluationResultDTO::getResult)
                .containsExactly(
                        tuple("a", EvaluationResult.DENY),
                        tuple("b", EvaluationResult.ALLOW),
                        tuple("c", EvaluationResult.DENY));
    }

    @Test
    void resultIdentifiersComeFromPolicyNotRequest() {
        Policy tenantWide = Policy.builder()
                .id(21L).tenantID(TENANT_ID).scopeID(SCOPE_ID).entityID(null).code("tenant-default")
                .appliedJs(APPLIED_JS).status(PolicyStatus.ACTIVE).version(1L).build();
        givenApplicablePolicies(tenantWide);
        givenEngine(tenantWide, JsIdenfier.APPLIES, true);
        givenEngine(tenantWide, JsIdenfier.POLICY, true);

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                .singleElement()
                .satisfies(r -> assertThat(r.getEntityID()).isNull());
    }

    @Test
    void passesSerializedInputAttrsToEngine() {
        Policy p = policy(11L, "credit-limit");
        givenApplicablePolicies(p);
        ArgumentCaptor<String> input = ArgumentCaptor.forClass(String.class);
        when(policyEngine.evaluate(eq(p), any(), input.capture())).thenReturn(true);

        evaluationService.evaluatePoliciesForUser(request()).join();

        assertThat(input.getAllValues()).hasSize(2);
        assertThat(input.getAllValues()).allSatisfy(json -> assertThat(JsonUtil.toMap(json)).isEqualTo(INPUT_ATTRS));
    }

    @Test
    void wrapsPolicyLookupFailure() {
        when(policyService.getApplicablePolicies(eq(TENANT_ID), eq(SCOPE_ID), eq(ENTITY_ID), anyLong()))
                .thenReturn(CompletableFuture.failedFuture(new LibraServiceException("error getting applicable policies")));

        assertThatThrownBy(() -> evaluationService.evaluatePoliciesForUser(request()).join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraEvaluationException.class)
                .hasMessage("error when evaluating policy")
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error getting applicable policies");
        verifyNoInteractions(policyEngine);
    }

    @Test
    void deniesWithErrorWhenEngineFailsDuringAppliesCheck() {
        Policy p = policy(11L, "credit-limit");
        givenApplicablePolicies(p);
        when(policyEngine.evaluate(p, JsIdenfier.APPLIES, INPUT_JSON))
                .thenThrow(new LibraEvaluationException("error evaluating policy"));

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getPolicyID()).isEqualTo(11L);
                    assertThat(r.getPolicyCode()).isEqualTo("credit-limit");
                    assertThat(r.getResult()).isEqualTo(EvaluationResult.DENY);
                    assertThat(r.getErrors()).containsExactly(Map.entry("msg", "error evaluating policy"));
                });
        verify(policyEngine, never()).evaluate(p, JsIdenfier.POLICY, INPUT_JSON);
    }

    @Test
    void deniesWithErrorWhenEngineFailsDuringPolicyEvaluation() {
        Policy p = policy(11L, "credit-limit");
        givenApplicablePolicies(p);
        givenEngine(p, JsIdenfier.APPLIES, true);
        when(policyEngine.evaluate(p, JsIdenfier.POLICY, INPUT_JSON))
                .thenThrow(new LibraEvaluationException("js execution was cancelled"));

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getResult()).isEqualTo(EvaluationResult.DENY);
                    assertThat(r.getErrors()).containsExactly(Map.entry("msg", "js execution was cancelled"));
                });
    }

    @Test
    void usesExceptionClassNameWhenEngineErrorHasNoMessage() {
        Policy p = policy(11L, "credit-limit");
        givenApplicablePolicies(p);
        when(policyEngine.evaluate(p, JsIdenfier.APPLIES, INPUT_JSON)).thenThrow(new IllegalStateException());

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                .singleElement()
                .satisfies(r -> assertThat(r.getErrors()).containsExactly(Map.entry("msg", "IllegalStateException")));
    }

    @Test
    void engineFailureOnOnePolicyDoesNotAffectOthers() {
        Policy broken = policy(11L, "broken");
        Policy ok = policy(12L, "ok");
        givenApplicablePolicies(broken, ok);
        when(policyEngine.evaluate(broken, JsIdenfier.APPLIES, INPUT_JSON))
                .thenThrow(new LibraEvaluationException("boom"));
        givenEngine(ok, JsIdenfier.APPLIES, true);
        givenEngine(ok, JsIdenfier.POLICY, true);

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                .extracting(PolicyEvaluationResultDTO::getPolicyCode, PolicyEvaluationResultDTO::getResult)
                .containsExactly(
                        tuple("broken", EvaluationResult.DENY),
                        tuple("ok", EvaluationResult.ALLOW));
    }

    @Test
    void treatsNullEngineResultAsNotApplicable() {
        Policy p = policy(11L, "credit-limit");
        givenApplicablePolicies(p);
        when(policyEngine.evaluate(p, JsIdenfier.APPLIES, INPUT_JSON)).thenReturn(null);

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join()).isEmpty();
        verify(policyEngine, never()).evaluate(p, JsIdenfier.POLICY, INPUT_JSON);
    }

    @Test
    void treatsNullPolicyResultAsDeny() {
        Policy p = policy(11L, "credit-limit");
        givenApplicablePolicies(p);
        givenEngine(p, JsIdenfier.APPLIES, true);
        when(policyEngine.evaluate(p, JsIdenfier.POLICY, INPUT_JSON)).thenReturn(null);

        assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                .extracting(PolicyEvaluationResultDTO::getResult)
                .containsExactly(EvaluationResult.DENY);
    }

    @Nested
    class WithoutAppliedJs {

        @ParameterizedTest(name = "appliedJs = [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n  "})
        void skipsAppliesCheckAndAllows(String appliedJs) {
            Policy p = policy(11L, "credit-limit", appliedJs);
            givenApplicablePolicies(p);
            givenEngine(p, JsIdenfier.POLICY, true);

            assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                    .extracting(PolicyEvaluationResultDTO::getPolicyCode, PolicyEvaluationResultDTO::getResult)
                    .containsExactly(tuple("credit-limit", EvaluationResult.ALLOW));
            verify(policyEngine, never()).evaluate(any(), eq(JsIdenfier.APPLIES), anyString());
        }

        @ParameterizedTest(name = "appliedJs = [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void skipsAppliesCheckAndDenies(String appliedJs) {
            Policy p = policy(11L, "credit-limit", appliedJs);
            givenApplicablePolicies(p);
            givenEngine(p, JsIdenfier.POLICY, false);

            assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                    .extracting(PolicyEvaluationResultDTO::getResult)
                    .containsExactly(EvaluationResult.DENY);
            verify(policyEngine, never()).evaluate(any(), eq(JsIdenfier.APPLIES), anyString());
        }

        @Test
        void deniesWithErrorWhenPolicyEvaluationFails() {
            Policy p = policy(11L, "credit-limit", null);
            givenApplicablePolicies(p);
            when(policyEngine.evaluate(p, JsIdenfier.POLICY, INPUT_JSON))
                    .thenThrow(new LibraEvaluationException("js execution was cancelled"));

            assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.getResult()).isEqualTo(EvaluationResult.DENY);
                        assertThat(r.getErrors()).containsExactly(Map.entry("msg", "js execution was cancelled"));
                    });
        }

        @Test
        void mixesWithPoliciesThatHaveAppliedJs() {
            Policy noApplies = policy(11L, "no-applies", null);
            Policy applies = policy(12L, "applies");
            Policy skipped = policy(13L, "skipped");
            givenApplicablePolicies(noApplies, applies, skipped);
            givenEngine(noApplies, JsIdenfier.POLICY, true);
            givenEngine(applies, JsIdenfier.APPLIES, true);
            givenEngine(applies, JsIdenfier.POLICY, false);
            givenEngine(skipped, JsIdenfier.APPLIES, false);

            assertThat(evaluationService.evaluatePoliciesForUser(request()).join())
                    .extracting(PolicyEvaluationResultDTO::getPolicyCode, PolicyEvaluationResultDTO::getResult)
                    .containsExactly(
                            tuple("no-applies", EvaluationResult.ALLOW),
                            tuple("applies", EvaluationResult.DENY));
            verify(policyEngine, never()).evaluate(noApplies, JsIdenfier.APPLIES, INPUT_JSON);
            verify(policyEngine, never()).evaluate(skipped, JsIdenfier.POLICY, INPUT_JSON);
        }

        @Test
        void passesSerializedInputAttrsOnlyToPolicyScript() {
            Policy p = policy(11L, "credit-limit", null);
            givenApplicablePolicies(p);
            ArgumentCaptor<String> input = ArgumentCaptor.forClass(String.class);
            when(policyEngine.evaluate(eq(p), eq(JsIdenfier.POLICY), input.capture())).thenReturn(true);

            evaluationService.evaluatePoliciesForUser(request()).join();

            assertThat(input.getAllValues()).singleElement()
                    .satisfies(json -> assertThat(JsonUtil.toMap(json)).isEqualTo(INPUT_ATTRS));
        }
    }

    @Test
    void runsEvaluationOnProvidedExecutor() {
        Set<String> engineThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger counter = new AtomicInteger();
        try (ExecutorService named = Executors.newFixedThreadPool(2,
                r -> new Thread(r, "libra-eval-" + counter.incrementAndGet()))) {
            PolicyEvaluationService service = new PolicyEvaluationService(policyService, policyEngine, policyEvaluationDAO, named);
            Policy p = policy(11L, "credit-limit");
            givenApplicablePolicies(p);
            when(policyEngine.evaluate(eq(p), any(), eq(INPUT_JSON))).thenAnswer(inv -> {
                engineThreads.add(Thread.currentThread().getName());
                return true;
            });

            service.evaluatePoliciesForUser(request()).join();
        }

        assertThat(engineThreads).isNotEmpty().allSatisfy(name -> assertThat(name).startsWith("libra-eval-"));
    }

    @Nested
    class EvaluatePolicies {

        @Test
        void assignsRandomRequestID() {
            givenApplicablePolicies();

            PolicyEvaluationDTO result = evaluationService.evaluatePolicies(request()).join();

            assertThat(result.getRequestID()).isNotBlank();
            assertThat(UUID.fromString(result.getRequestID())).isNotNull();
        }

        @Test
        void assignsDistinctRequestIDPerCall() {
            when(policyService.getApplicablePolicies(eq(TENANT_ID), eq(SCOPE_ID), eq(ENTITY_ID), anyLong()))
                    .thenReturn(CompletableFuture.completedFuture(List.of()));

            String first = evaluationService.evaluatePolicies(request()).join().getRequestID();
            String second = evaluationService.evaluatePolicies(request()).join().getRequestID();

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        void returnsSameDtoPopulatedWithResults() {
            Policy p = policy(11L, "credit-limit");
            givenApplicablePolicies(p);
            givenEngine(p, JsIdenfier.APPLIES, true);
            givenEngine(p, JsIdenfier.POLICY, true);
            PolicyEvaluationDTO request = request();

            PolicyEvaluationDTO result = evaluationService.evaluatePolicies(request).join();

            assertThat(result).isSameAs(request);
            assertThat(result.getEvaluationResults())
                    .extracting(PolicyEvaluationResultDTO::getPolicyCode)
                    .containsExactly("credit-limit");
        }

        @Test
        void allowsWhenNoPolicyApplies() {
            givenApplicablePolicies();

            PolicyEvaluationDTO result = evaluationService.evaluatePolicies(request()).join();

            assertThat(result.getEvaluationResult()).isEqualTo(EvaluationResult.ALLOW);
            assertThat(result.getEvaluationResults()).isEmpty();
            assertThat(result.getDeniedPolicies()).isNull();
        }

        @Test
        void allowsWhenEveryApplicablePolicyAllows() {
            Policy p1 = policy(11L, "a");
            Policy p2 = policy(12L, "b");
            givenApplicablePolicies(p1, p2);
            givenEngine(p1, JsIdenfier.APPLIES, true);
            givenEngine(p2, JsIdenfier.APPLIES, true);
            givenEngine(p1, JsIdenfier.POLICY, true);
            givenEngine(p2, JsIdenfier.POLICY, true);

            PolicyEvaluationDTO result = evaluationService.evaluatePolicies(request()).join();

            assertThat(result.getEvaluationResult()).isEqualTo(EvaluationResult.ALLOW);
            assertThat(result.getDeniedPolicies()).isNull();
        }

        @Test
        void deniesAndListsDeniedPoliciesInOrder() {
            Policy p1 = policy(11L, "a");
            Policy p2 = policy(12L, "b");
            Policy p3 = policy(13L, "c");
            givenApplicablePolicies(p1, p2, p3);
            givenEngine(p1, JsIdenfier.APPLIES, true);
            givenEngine(p2, JsIdenfier.APPLIES, true);
            givenEngine(p3, JsIdenfier.APPLIES, true);
            givenEngine(p1, JsIdenfier.POLICY, false);
            givenEngine(p2, JsIdenfier.POLICY, true);
            givenEngine(p3, JsIdenfier.POLICY, false);

            PolicyEvaluationDTO result = evaluationService.evaluatePolicies(request()).join();

            assertThat(result.getEvaluationResult()).isEqualTo(EvaluationResult.DENY);
            assertThat(result.getDeniedPolicies()).containsExactly("a", "c");
        }

        @Test
        void deniesWhenAPolicyErrors() {
            Policy p = policy(11L, "broken");
            givenApplicablePolicies(p);
            when(policyEngine.evaluate(p, JsIdenfier.APPLIES, INPUT_JSON))
                    .thenThrow(new LibraEvaluationException("boom"));

            PolicyEvaluationDTO result = evaluationService.evaluatePolicies(request()).join();

            assertThat(result.getEvaluationResult()).isEqualTo(EvaluationResult.DENY);
            assertThat(result.getDeniedPolicies()).containsExactly("broken");
        }

        @Test
        @SuppressWarnings("unchecked")
        void persistsOneAuditRecordPerEvaluatedPolicy() {
            Policy allowed = policy(11L, "a");
            Policy broken = policy(12L, "b");
            Policy skipped = policy(13L, "c");
            givenApplicablePolicies(allowed, broken, skipped);
            givenEngine(allowed, JsIdenfier.APPLIES, true);
            givenEngine(allowed, JsIdenfier.POLICY, true);
            when(policyEngine.evaluate(broken, JsIdenfier.APPLIES, INPUT_JSON))
                    .thenThrow(new LibraEvaluationException("boom"));
            givenEngine(skipped, JsIdenfier.APPLIES, false);
            ArgumentCaptor<List<PolicyEvaluation>> saved = ArgumentCaptor.forClass(List.class);

            PolicyEvaluationDTO result = evaluationService.evaluatePolicies(request()).join();

            verify(policyEvaluationDAO).createPolicyEvaluations(saved.capture());
            assertThat(saved.getValue())
                    .extracting(PolicyEvaluation::getRequestID, PolicyEvaluation::getEntityID,
                            PolicyEvaluation::getPolicyID, PolicyEvaluation::getInputAttrs,
                            PolicyEvaluation::getResult, PolicyEvaluation::getErrors)
                    .containsExactly(
                            tuple(result.getRequestID(), ENTITY_ID, 11L, INPUT_ATTRS, EvaluationResult.ALLOW, null),
                            tuple(result.getRequestID(), ENTITY_ID, 12L, INPUT_ATTRS, EvaluationResult.DENY, Map.of("msg", "boom")));
        }

        @Test
        @SuppressWarnings("unchecked")
        void persistsEmptyBatchWhenNothingApplies() {
            givenApplicablePolicies();
            ArgumentCaptor<List<PolicyEvaluation>> saved = ArgumentCaptor.forClass(List.class);

            evaluationService.evaluatePolicies(request()).join();

            verify(policyEvaluationDAO).createPolicyEvaluations(saved.capture());
            assertThat(saved.getValue()).isEmpty();
        }

        @Test
        void propagatesPolicyLookupFailureWithoutPersisting() {
            when(policyService.getApplicablePolicies(eq(TENANT_ID), eq(SCOPE_ID), eq(ENTITY_ID), anyLong()))
                    .thenReturn(CompletableFuture.failedFuture(new LibraServiceException("error getting applicable policies")));

            assertThatThrownBy(() -> evaluationService.evaluatePolicies(request()).join())
                    .isInstanceOf(CompletionException.class)
                    .cause()
                    .isInstanceOf(LibraEvaluationException.class)
                    .hasMessage("error when evaluating policy")
                    .cause()
                    .isInstanceOf(LibraServiceException.class)
                    .hasMessage("error getting applicable policies");
            verifyNoInteractions(policyEngine, policyEvaluationDAO);
        }

        @Test
        void failsWhenAuditPersistenceFails() {
            Policy p = policy(11L, "credit-limit");
            givenApplicablePolicies(p);
            givenEngine(p, JsIdenfier.APPLIES, true);
            givenEngine(p, JsIdenfier.POLICY, true);
            when(policyEvaluationDAO.createPolicyEvaluations(anyList()))
                    .thenThrow(new LibraDAOException("error creating policy evaluations"));
            PolicyEvaluationDTO request = request();

            assertThatThrownBy(() -> evaluationService.evaluatePolicies(request).join())
                    .isInstanceOf(CompletionException.class)
                    .cause()
                    .isInstanceOf(LibraEvaluationException.class)
                    .hasMessage("error when persisting evals for audit")
                    .cause()
                    .isInstanceOf(LibraDAOException.class)
                    .hasMessage("error creating policy evaluations");
            assertThat(request.getEvaluationResult()).isNull();
        }
    }

    @Nested
    class WithGraalJSEngine {

        // NOTE: never close this engine — it closes the static, shared ENGINE.
        private final GraalJSPolicyEngine graalEngine = new GraalJSPolicyEngine();

        private Policy jsPolicy(Long id, String code, String appliesJs, String policyJs) {
            return Policy.builder()
                    .id(id).tenantID(TENANT_ID).scopeID(SCOPE_ID).entityID(ENTITY_ID).code(code)
                    .appliedJs(appliesJs).evaluatedJs(policyJs)
                    .status(PolicyStatus.ACTIVE).version(1L).build();
        }

        @Test
        void evaluatesRealJsPolicies() {
            PolicyEvaluationService service = new PolicyEvaluationService(policyService, graalEngine, policyEvaluationDAO, executor);
            Policy withinLimit = jsPolicy(11L, "credit-limit",
                    "function evaluate(input) { return input.amount !== undefined; }",
                    "function evaluate(input) { return input.amount <= input.limit; }");
            Policy tooSmall = jsPolicy(12L, "min-amount",
                    "function evaluate(input) { return true; }",
                    "function evaluate(input) { return input.amount >= 1000; }");
            Policy notApplicable = jsPolicy(13L, "sms-only",
                    "function evaluate(input) { return input.channel === 'sms'; }",
                    "function evaluate(input) { return true; }");
            givenApplicablePolicies(withinLimit, tooSmall, notApplicable);

            assertThat(service.evaluatePoliciesForUser(request()).join())
                    .extracting(PolicyEvaluationResultDTO::getPolicyCode, PolicyEvaluationResultDTO::getResult)
                    .containsExactly(
                            tuple("credit-limit", EvaluationResult.ALLOW),
                            tuple("min-amount", EvaluationResult.DENY));
        }

        @ParameterizedTest(name = "appliedJs = [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void evaluatesRealJsPolicyWithoutAppliedJs(String appliedJs) {
            PolicyEvaluationService service = new PolicyEvaluationService(policyService, graalEngine, policyEvaluationDAO, executor);
            Policy withinLimit = jsPolicy(11L, "credit-limit", appliedJs,
                    "function evaluate(input) { return input.amount <= input.limit; }");
            Policy tooSmall = jsPolicy(12L, "min-amount", appliedJs,
                    "function evaluate(input) { return input.amount >= 1000; }");
            givenApplicablePolicies(withinLimit, tooSmall);

            assertThat(service.evaluatePoliciesForUser(request()).join())
                    .extracting(PolicyEvaluationResultDTO::getPolicyCode, PolicyEvaluationResultDTO::getResult,
                            PolicyEvaluationResultDTO::getErrors)
                    .containsExactly(
                            tuple("credit-limit", EvaluationResult.ALLOW, null),
                            tuple("min-amount", EvaluationResult.DENY, null));
        }

        @Test
        void deniesWithErrorWhenPolicyReturnsNonBoolean() {
            PolicyEvaluationService service = new PolicyEvaluationService(policyService, graalEngine, policyEvaluationDAO, executor);
            Policy bad = jsPolicy(11L, "bad", "function evaluate(input) { return true; }",
                    "function evaluate(input) { return 'yes'; }");
            givenApplicablePolicies(bad);

            assertThat(service.evaluatePoliciesForUser(request()).join())
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.getResult()).isEqualTo(EvaluationResult.DENY);
                        assertThat(r.getErrors()).containsEntry("msg", "error evaluating policy evaluator");
                    });
        }
    }

}
