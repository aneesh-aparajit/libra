package dev.tsukuyomi.libra.controller;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.service.policy.IPolicyService;
import dev.tsukuyomi.libra.transformer.policy.CreatePolicyRequestTransformer;
import dev.tsukuyomi.libra.transformer.policy.PolicyResponseTransformer;
import dev.tsukuyomi.libra.transformer.policy.UpdatePolicyRequestTransformer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;
import java.util.Optional;

import static dev.tsukuyomi.libra.controller.Futures.daoFailure;
import static dev.tsukuyomi.libra.controller.Futures.done;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(PolicyController.class)
@Import({CreatePolicyRequestTransformer.class, UpdatePolicyRequestTransformer.class, PolicyResponseTransformer.class})
class PolicyControllerTest {

    private static final String VALID_CREATE = """
            {"tenantID": 1, "scopeID": 2, "entityID": " user-1 ", "code": " limit ",
             "appliedJs": "applies()", "evaluatedJs": "evaluate()", "validFrom": 1000, "validTo": 2000}""";

    private static final String VALID_UPDATE = """
            {"appliedJs": "a2()", "evaluatedJs": "e2()", "status": "INACTIVE",
             "validFrom": 1000, "validTo": 3000, "version": 4}""";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IPolicyService policyService;

    private static Policy policy(long id, PolicyStatus status, long version) {
        return Policy.builder()
                .id(id).tenantID(1L).scopeID(2L).entityID("user-1").code("limit")
                .appliedJs("applies()").evaluatedJs("evaluate()").status(status)
                .validFrom(1000L).validTo(2000L).version(version).createdAt(10L).updatedAt(20L)
                .build();
    }

    private MvcTestResult send(org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder request,
                               String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Nested
    class CreatePolicy {

        @Test
        void returns201AndDefaultsStatusToActive() {
            when(policyService.createPolicy(any())).thenReturn(done(policy(5L, PolicyStatus.ACTIVE, 1L)));

            MvcTestResult result = send(mvc.post().uri("/v1/policies"), VALID_CREATE);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).headers().hasValue("Location", "/v1/policies/5");
            assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(5);
            assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("ACTIVE");
            assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(1);

            ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
            verify(policyService).createPolicy(captor.capture());
            Policy sent = captor.getValue();
            assertThat(sent.getId()).isNull();
            assertThat(sent.getVersion()).isNull();
            assertThat(sent.getTenantID()).isEqualTo(1L);
            assertThat(sent.getScopeID()).isEqualTo(2L);
            assertThat(sent.getEntityID()).isEqualTo("user-1");
            assertThat(sent.getCode()).isEqualTo("limit");
            assertThat(sent.getAppliedJs()).isEqualTo("applies()");
            assertThat(sent.getEvaluatedJs()).isEqualTo("evaluate()");
            assertThat(sent.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
            assertThat(sent.getValidFrom()).isEqualTo(1000L);
            assertThat(sent.getValidTo()).isEqualTo(2000L);
        }

        @Test
        void keepsExplicitStatusAndAllowsOpenEndedWindow() {
            when(policyService.createPolicy(any())).thenReturn(done(policy(5L, PolicyStatus.INACTIVE, 1L)));

            MvcTestResult result = send(mvc.post().uri("/v1/policies"), """
                    {"tenantID": 1, "scopeID": 2, "entityID": "*", "code": "limit",
                     "evaluatedJs": "evaluate()", "status": "INACTIVE", "validFrom": 1000}""");

            assertThat(result).hasStatus(HttpStatus.CREATED);
            ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
            verify(policyService).createPolicy(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PolicyStatus.INACTIVE);
            assertThat(captor.getValue().getValidTo()).isNull();
            assertThat(captor.getValue().getAppliedJs()).isNull();
        }

        @Test
        void rejectsValidToNotAfterValidFrom() {
            assertThat(send(mvc.post().uri("/v1/policies"), """
                    {"tenantID": 1, "scopeID": 2, "entityID": "*", "code": "c",
                     "evaluatedJs": "e()", "validFrom": 2000, "validTo": 2000}""")).hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

        @Test
        void rejectsExpiredStatus() {
            assertThat(send(mvc.post().uri("/v1/policies"), """
                    {"tenantID": 1, "scopeID": 2, "entityID": "*", "code": "c",
                     "evaluatedJs": "e()", "status": "EXPIRED", "validFrom": 1000}""")).hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

        @Test
        void rejectsMissingRequiredFields() {
            assertThat(send(mvc.post().uri("/v1/policies"), """
                    {"tenantID": 1, "scopeID": 2, "entityID": "*", "code": "c"}""")).hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

        @Test
        void rejectsUnknownStatusValue() {
            assertThat(send(mvc.post().uri("/v1/policies"), """
                    {"tenantID": 1, "scopeID": 2, "entityID": "*", "code": "c",
                     "evaluatedJs": "e()", "status": "BOGUS", "validFrom": 1000}""")).hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

        @Test
        void returns409OnDuplicate() {
            when(policyService.createPolicy(any())).thenReturn(daoFailure(new DuplicateKeyException("unq__tenant_scope_entity")));

            assertThat(send(mvc.post().uri("/v1/policies"), VALID_CREATE)).hasStatus(HttpStatus.CONFLICT);
        }

        @Test
        void returns400OnUnknownTenantOrScope() {
            when(policyService.createPolicy(any())).thenReturn(daoFailure(new DataIntegrityViolationException("fk")));

            assertThat(send(mvc.post().uri("/v1/policies"), VALID_CREATE)).hasStatus(HttpStatus.BAD_REQUEST);
        }

    }

    @Nested
    class GetPolicy {

        @Test
        void returns200() {
            when(policyService.getPolicyByID(5L)).thenReturn(done(Optional.of(policy(5L, PolicyStatus.ACTIVE, 3L))));

            MvcTestResult result = mvc.get().uri("/v1/policies/5").exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().extractingPath("$.entityID").isEqualTo("user-1");
            assertThat(result).bodyJson().extractingPath("$.evaluatedJs").isEqualTo("evaluate()");
            assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(3);
        }

        @Test
        void returns404WhenMissing() {
            when(policyService.getPolicyByID(9L)).thenReturn(done(Optional.empty()));

            MvcTestResult result = mvc.get().uri("/v1/policies/9").exchange();

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("policy 9 not found");
        }

        @Test
        void rejectsNonNumericID() {
            assertThat(mvc.get().uri("/v1/policies/abc").exchange()).hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

    }

    @Nested
    class ListPolicies {

        @Test
        void byTenantAndScope() {
            when(policyService.getPoliciesByTenantIDAndScope(1L, 2L))
                    .thenReturn(done(List.of(policy(5L, PolicyStatus.ACTIVE, 1L), policy(6L, PolicyStatus.INACTIVE, 1L))));

            MvcTestResult result = mvc.get().uri("/v1/policies").param("tenantID", "1").param("scopeID", "2").exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(2);
            assertThat(result).bodyJson().extractingPath("$[1].id").isEqualTo(6);
        }

        @Test
        void byEntityAndScope() {
            when(policyService.getPoliciesByEntityIDAndScope("user-1", 2L))
                    .thenReturn(done(List.of(policy(5L, PolicyStatus.ACTIVE, 1L))));

            MvcTestResult result = mvc.get().uri("/v1/policies").param("entityID", "user-1").param("scopeID", "2").exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(1);
            verify(policyService).getPoliciesByEntityIDAndScope("user-1", 2L);
        }

        @Test
        void byEntityOnly() {
            when(policyService.getPoliciesByEntityID("user-1")).thenReturn(done(List.of()));

            MvcTestResult result = mvc.get().uri("/v1/policies").param("entityID", "user-1").exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().isEqualTo("[]");
            verify(policyService).getPoliciesByEntityID("user-1");
        }

        @Test
        void rejectsMissingFilters() {
            MvcTestResult result = mvc.get().uri("/v1/policies").exchange();

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.detail")
                    .isEqualTo("provide tenantID+scopeID, entityID+scopeID, or entityID");
            verifyNoInteractions(policyService);
        }

        @Test
        void rejectsTenantWithoutScope() {
            assertThat(mvc.get().uri("/v1/policies").param("tenantID", "1").exchange())
                    .hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

    }

    @Nested
    class ApplicablePolicies {

        @Test
        void usesGivenTimestamp() {
            when(policyService.getApplicablePolicies(1L, 2L, "user-1", 1500L))
                    .thenReturn(done(List.of(policy(5L, PolicyStatus.ACTIVE, 1L))));

            MvcTestResult result = mvc.get().uri("/v1/policies/applicable")
                    .param("tenantID", "1").param("scopeID", "2").param("entityID", "user-1").param("at", "1500")
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().extractingPath("$[0].id").isEqualTo(5);
        }

        @Test
        void defaultsTimestampToNow() {
            when(policyService.getApplicablePolicies(eq(1L), eq(2L), eq("user-1"), anyLong())).thenReturn(done(List.of()));
            long before = System.currentTimeMillis();

            MvcTestResult result = mvc.get().uri("/v1/policies/applicable")
                    .param("tenantID", "1").param("scopeID", "2").param("entityID", "user-1").exchange();

            long after = System.currentTimeMillis();
            assertThat(result).hasStatus(HttpStatus.OK);
            ArgumentCaptor<Long> at = ArgumentCaptor.forClass(Long.class);
            verify(policyService).getApplicablePolicies(eq(1L), eq(2L), eq("user-1"), at.capture());
            assertThat(at.getValue()).isBetween(before, after);
        }

        @Test
        void requiresTenantScopeAndEntity() {
            assertThat(mvc.get().uri("/v1/policies/applicable").param("tenantID", "1").param("scopeID", "2").exchange())
                    .hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

    }

    @Nested
    class UpdatePolicy {

        @Test
        void returns200AndPassesPathIDAndVersion() {
            when(policyService.updatePolicy(any())).thenReturn(done(Optional.of(policy(5L, PolicyStatus.INACTIVE, 5L))));

            MvcTestResult result = send(mvc.put().uri("/v1/policies/5"), VALID_UPDATE);

            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(5);

            ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
            verify(policyService).updatePolicy(captor.capture());
            Policy sent = captor.getValue();
            assertThat(sent.getId()).isEqualTo(5L);
            assertThat(sent.getVersion()).isEqualTo(4L);
            assertThat(sent.getAppliedJs()).isEqualTo("a2()");
            assertThat(sent.getEvaluatedJs()).isEqualTo("e2()");
            assertThat(sent.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
            assertThat(sent.getValidFrom()).isEqualTo(1000L);
            assertThat(sent.getValidTo()).isEqualTo(3000L);
        }

        @Test
        void returns409OnStaleVersion() {
            when(policyService.updatePolicy(any())).thenReturn(done(Optional.empty()));
            when(policyService.getPolicyByID(5L)).thenReturn(done(Optional.of(policy(5L, PolicyStatus.ACTIVE, 7L))));

            MvcTestResult result = send(mvc.put().uri("/v1/policies/5"), VALID_UPDATE);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.detail")
                    .isEqualTo("policy 5 version mismatch, current version is 7");
        }

        @Test
        void returns404WhenMissing() {
            when(policyService.updatePolicy(any())).thenReturn(done(Optional.empty()));
            when(policyService.getPolicyByID(5L)).thenReturn(done(Optional.empty()));

            assertThat(send(mvc.put().uri("/v1/policies/5"), VALID_UPDATE)).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void rejectsMissingVersion() {
            assertThat(send(mvc.put().uri("/v1/policies/5"), """
                    {"evaluatedJs": "e()", "status": "ACTIVE", "validFrom": 1000}""")).hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

        @Test
        void rejectsInvertedWindow() {
            assertThat(send(mvc.put().uri("/v1/policies/5"), """
                    {"evaluatedJs": "e()", "status": "ACTIVE", "validFrom": 3000, "validTo": 1000, "version": 1}"""))
                    .hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

        @Test
        void returns500OnServiceFailure() {
            when(policyService.updatePolicy(any())).thenReturn(daoFailure(new RuntimeException("db down")));

            assertThat(send(mvc.put().uri("/v1/policies/5"), VALID_UPDATE)).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @Nested
    class UpdatePolicyStatus {

        @Test
        void returns200() {
            when(policyService.getPolicyByID(5L)).thenReturn(done(Optional.of(policy(5L, PolicyStatus.ACTIVE, 1L))));
            when(policyService.updatePolicyStatus(5L, PolicyStatus.INACTIVE))
                    .thenReturn(done(Optional.of(policy(5L, PolicyStatus.INACTIVE, 2L))));

            MvcTestResult result = send(mvc.patch().uri("/v1/policies/5/status"), """
                    {"status": "INACTIVE"}""");

            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("INACTIVE");
            assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(2);
        }

        @Test
        void returns404WhenMissing() {
            when(policyService.getPolicyByID(9L)).thenReturn(done(Optional.empty()));

            assertThat(send(mvc.patch().uri("/v1/policies/9/status"), """
                    {"status": "ACTIVE"}""")).hasStatus(HttpStatus.NOT_FOUND);
            verify(policyService, never()).updatePolicyStatus(anyLong(), any());
        }

        @Test
        void returns409WhenExpired() {
            when(policyService.getPolicyByID(5L)).thenReturn(done(Optional.of(policy(5L, PolicyStatus.EXPIRED, 1L))));

            MvcTestResult result = send(mvc.patch().uri("/v1/policies/5/status"), """
                    {"status": "ACTIVE"}""");

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.detail")
                    .isEqualTo("policy 5 is expired and cannot change status");
            verify(policyService, never()).updatePolicyStatus(anyLong(), any());
        }

        @Test
        void returns404WhenDeletedBetweenReadAndUpdate() {
            when(policyService.getPolicyByID(5L)).thenReturn(done(Optional.of(policy(5L, PolicyStatus.ACTIVE, 1L))));
            when(policyService.updatePolicyStatus(5L, PolicyStatus.INACTIVE)).thenReturn(done(Optional.empty()));

            assertThat(send(mvc.patch().uri("/v1/policies/5/status"), """
                    {"status": "INACTIVE"}""")).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void rejectsMissingStatus() {
            assertThat(send(mvc.patch().uri("/v1/policies/5/status"), "{}")).hasStatus(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(policyService);
        }

    }

}
