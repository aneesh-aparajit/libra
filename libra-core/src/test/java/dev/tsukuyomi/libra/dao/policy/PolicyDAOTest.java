package dev.tsukuyomi.libra.dao.policy;

import dev.tsukuyomi.libra.dao.PostgresDAOTestBase;
import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.ALL_ENTITIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyDAOTest extends PostgresDAOTestBase {

    private static final long NOW = 1_000_000L;

    private Long tenantID;
    private Long scopeID;

    @BeforeEach
    void setUp() {
        tenantID = newTenant().getId();
        scopeID = newScope(tenantID, "payment.credit").getId();
    }

    private static Policy.PolicyBuilder copyOf(Policy p) {
        return Policy.builder()
                .id(p.getId())
                .tenantID(p.getTenantID())
                .scopeID(p.getScopeID())
                .entityID(p.getEntityID())
                .code(p.getCode())
                .appliedJs(p.getAppliedJs())
                .evaluatedJs(p.getEvaluatedJs())
                .status(p.getStatus())
                .validFrom(p.getValidFrom())
                .validTo(p.getValidTo())
                .version(p.getVersion())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt());
    }

    @Nested
    class Create {

        @Test
        void returnsPersistedRow() {
            Policy created = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "user-1")
                    .validFrom(100L)
                    .validTo(200L)
                    .build());

            assertThat(created.getId()).isNotNull();
            assertThat(created.getTenantID()).isEqualTo(tenantID);
            assertThat(created.getScopeID()).isEqualTo(scopeID);
            assertThat(created.getEntityID()).isEqualTo("user-1");
            assertThat(created.getCode()).isEqualTo("policy-user-1");
            assertThat(created.getAppliedJs()).contains("applies");
            assertThat(created.getEvaluatedJs()).contains("evaluate");
            assertThat(created.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
            assertThat(created.getValidFrom()).isEqualTo(100L);
            assertThat(created.getValidTo()).isEqualTo(200L);
            assertThat(created.getCreatedAt()).isNotNull();
            assertThat(created.getUpdatedAt()).isNotNull();
        }

        @Test
        void defaultsVersionToOne() {
            assertThat(newPolicy(tenantID, scopeID, "user-1").getVersion()).isEqualTo(1L);
        }

        @Test
        void keepsExplicitVersion() {
            Policy created = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "user-1").version(5L).build());

            assertThat(created.getVersion()).isEqualTo(5L);
        }

        @Test
        void allowsNullOptionalColumns() {
            Policy created = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "user-1")
                    .appliedJs(null)
                    .validTo(null)
                    .build());

            assertThat(created.getAppliedJs()).isNull();
            assertThat(created.getValidTo()).isNull();
        }

        @Test
        void missingEvaluateJsThrows() {
            assertThatThrownBy(() -> policyDAO.createPolicy(
                    policyBuilder(tenantID, scopeID, "user-1").evaluatedJs(null).build()))
                    .isInstanceOf(LibraDAOException.class)
                    .hasMessage("error creating policy");
        }

        @Test
        void missingStatusThrows() {
            assertThatThrownBy(() -> policyDAO.createPolicy(
                    policyBuilder(tenantID, scopeID, "user-1").status(null).build()))
                    .isInstanceOf(LibraDAOException.class);
        }

        @Test
        void unknownScopeThrows() {
            assertThatThrownBy(() -> newPolicy(tenantID, 999L, "user-1"))
                    .isInstanceOf(LibraDAOException.class);
        }

        @Test
        void entityIsUniquePerTenantAndScope() {
            newPolicy(tenantID, scopeID, "user-1");

            assertThatThrownBy(() -> newPolicy(tenantID, scopeID, "user-1"))
                    .isInstanceOf(LibraDAOException.class);
        }

    }

    @Nested
    class Reads {

        @Test
        void getPolicyByID() {
            Policy created = newPolicy(tenantID, scopeID, "user-1");

            assertThat(policyDAO.getPolicyByID(created.getId())).contains(created);
            assertThat(policyDAO.getPolicyByID(created.getId() + 1)).isEmpty();
        }

        @Test
        void getPoliciesByEntityID() {
            Long otherScope = newScope(tenantID, "payment.debit").getId();
            Policy a = newPolicy(tenantID, scopeID, "user-1");
            Policy b = newPolicy(tenantID, otherScope, "user-1");
            newPolicy(tenantID, scopeID, "user-2");

            assertThat(policyDAO.getPoliciesByEntityID("user-1")).containsExactlyInAnyOrder(a, b);
            assertThat(policyDAO.getPoliciesByEntityID("nobody")).isEmpty();
        }

        @Test
        void getPoliciesByEntityIDAndScope() {
            Long otherScope = newScope(tenantID, "payment.debit").getId();
            Policy a = newPolicy(tenantID, scopeID, "user-1");
            newPolicy(tenantID, otherScope, "user-1");

            assertThat(policyDAO.getPoliciesByEntityIDAndScope("user-1", scopeID)).containsExactly(a);
        }

        @Test
        void getPoliciesByTenantIDAndScope() {
            Long otherTenant = newTenant().getId();
            Long otherTenantScope = newScope(otherTenant, "payment.credit").getId();
            Policy a = newPolicy(tenantID, scopeID, "user-1");
            Policy b = newPolicy(tenantID, scopeID, ALL_ENTITIES);
            newPolicy(otherTenant, otherTenantScope, "user-1");

            assertThat(policyDAO.getPoliciesByTenantIDAndScope(tenantID, scopeID)).containsExactlyInAnyOrder(a, b);
        }

    }

    @Nested
    class ApplicablePolicies {

        private List<Policy> applicable(String entityID) {
            return policyDAO.getApplicablePolicies(tenantID, scopeID, entityID, NOW);
        }

        @Test
        void includesEntityAndScopeLevelPolicies() {
            Policy entityLevel = newPolicy(tenantID, scopeID, "user-1");
            Policy scopeLevel = newPolicy(tenantID, scopeID, ALL_ENTITIES);
            newPolicy(tenantID, scopeID, "user-2");

            assertThat(applicable("user-1")).containsExactlyInAnyOrder(entityLevel, scopeLevel);
        }

        @Test
        void excludesOtherScopesAndTenants() {
            Long otherScope = newScope(tenantID, "payment.debit").getId();
            Long otherTenant = newTenant().getId();
            Long otherTenantScope = newScope(otherTenant, "payment.credit").getId();
            newPolicy(tenantID, otherScope, "user-1");
            newPolicy(otherTenant, otherTenantScope, "user-1");

            assertThat(applicable("user-1")).isEmpty();
        }

        @Test
        void excludesNonActivePolicies() {
            policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "user-1").status(PolicyStatus.INACTIVE).build());
            policyDAO.createPolicy(policyBuilder(tenantID, scopeID, ALL_ENTITIES).status(PolicyStatus.EXPIRED).build());

            assertThat(applicable("user-1")).isEmpty();
        }

        @Test
        void excludesPoliciesNotYetValid() {
            policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "user-1").validFrom(NOW + 1).build());

            assertThat(applicable("user-1")).isEmpty();
        }

        @Test
        void includesPolicyStartingExactlyNow() {
            Policy policy = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "user-1").validFrom(NOW).build());

            assertThat(applicable("user-1")).containsExactly(policy);
        }

        @Test
        void excludesPoliciesPastValidTill() {
            policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "user-1").validTo(NOW - 1).build());
            // valid_till is exclusive
            policyDAO.createPolicy(policyBuilder(tenantID, scopeID, ALL_ENTITIES).validTo(NOW).build());

            assertThat(applicable("user-1")).isEmpty();
        }

        @Test
        void includesPoliciesWithoutValidTill() {
            Policy open = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "user-1").validTo(null).build());
            Policy future = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, ALL_ENTITIES).validTo(NOW + 1).build());

            assertThat(applicable("user-1")).containsExactlyInAnyOrder(open, future);
        }

    }

    @Nested
    class Update {

        @Test
        void updatesMutableFieldsAndBumpsVersion() {
            Policy created = newPolicy(tenantID, scopeID, "user-1");

            Optional<Policy> updated = policyDAO.updatePolicy(copyOf(created)
                    .appliedJs(null)
                    .evaluatedJs("function evaluate() { return false; }")
                    .status(PolicyStatus.INACTIVE)
                    .validFrom(10L)
                    .validTo(20L)
                    .build());

            assertThat(updated).hasValueSatisfying(p -> {
                assertThat(p.getId()).isEqualTo(created.getId());
                assertThat(p.getAppliedJs()).isNull();
                assertThat(p.getEvaluatedJs()).isEqualTo("function evaluate() { return false; }");
                assertThat(p.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
                assertThat(p.getValidFrom()).isEqualTo(10L);
                assertThat(p.getValidTo()).isEqualTo(20L);
                assertThat(p.getVersion()).isEqualTo(created.getVersion() + 1);
                assertThat(p.getCreatedAt()).isEqualTo(created.getCreatedAt());
                assertThat(p.getUpdatedAt()).isGreaterThanOrEqualTo(created.getUpdatedAt());
            });
            assertThat(policyDAO.getPolicyByID(created.getId())).isEqualTo(updated);
        }

        @Test
        void doesNotChangeIdentityColumns() {
            Policy created = newPolicy(tenantID, scopeID, "user-1");

            Policy updated = policyDAO.updatePolicy(copyOf(created)
                    .tenantID(999L)
                    .scopeID(999L)
                    .entityID("someone-else")
                    .code("renamed")
                    .build()).orElseThrow();

            assertThat(updated.getTenantID()).isEqualTo(tenantID);
            assertThat(updated.getScopeID()).isEqualTo(scopeID);
            assertThat(updated.getEntityID()).isEqualTo("user-1");
            assertThat(updated.getCode()).isEqualTo(created.getCode());
        }

        @Test
        void staleVersionIsRejected() {
            Policy created = newPolicy(tenantID, scopeID, "user-1");
            policyDAO.updatePolicy(copyOf(created).status(PolicyStatus.INACTIVE).build()).orElseThrow();

            // second writer still holds version 1
            Optional<Policy> result = policyDAO.updatePolicy(copyOf(created).validTo(5L).build());

            assertThat(result).isEmpty();
            Policy current = policyDAO.getPolicyByID(created.getId()).orElseThrow();
            assertThat(current.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
            assertThat(current.getValidTo()).isNull();
            assertThat(current.getVersion()).isEqualTo(2L);
        }

        @Test
        void unknownPolicyReturnsEmpty() {
            Policy ghost = policyBuilder(tenantID, scopeID, "user-1").id(999L).version(1L).build();

            assertThat(policyDAO.updatePolicy(ghost)).isEmpty();
        }

        @Test
        void updatePolicyStatusBumpsVersion() {
            Policy created = newPolicy(tenantID, scopeID, "user-1");

            Optional<Policy> updated = policyDAO.updatePolicyStatus(created.getId(), PolicyStatus.INACTIVE);

            assertThat(updated).hasValueSatisfying(p -> {
                assertThat(p.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
                assertThat(p.getVersion()).isEqualTo(2L);
                assertThat(p.getEvaluatedJs()).isEqualTo(created.getEvaluatedJs());
            });
        }

        @Test
        void updatePolicyStatusForUnknownPolicyReturnsEmpty() {
            assertThat(policyDAO.updatePolicyStatus(999L, PolicyStatus.INACTIVE)).isEmpty();
        }

    }

    @Nested
    class Expire {

        @Test
        void expiresOnlyActivePoliciesPastValidTill() {
            Long otherScope = newScope(tenantID, "payment.debit").getId();
            Policy past = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "past").validTo(NOW - 1).build());
            Policy edge = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "edge").validTo(NOW).build());
            Policy future = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "future").validTo(NOW + 1).build());
            Policy open = policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "open").validTo(null).build());
            Policy inactive = policyDAO.createPolicy(policyBuilder(tenantID, otherScope, "past")
                    .status(PolicyStatus.INACTIVE).validTo(NOW - 1).build());

            assertThat(policyDAO.expirePolicies(NOW)).isEqualTo(2);

            assertStatus(past, PolicyStatus.EXPIRED, 2L);
            assertStatus(edge, PolicyStatus.EXPIRED, 2L);
            assertStatus(future, PolicyStatus.ACTIVE, 1L);
            assertStatus(open, PolicyStatus.ACTIVE, 1L);
            assertStatus(inactive, PolicyStatus.INACTIVE, 1L);
        }

        @Test
        void isIdempotent() {
            policyDAO.createPolicy(policyBuilder(tenantID, scopeID, "user-1").validTo(NOW - 1).build());

            assertThat(policyDAO.expirePolicies(NOW)).isEqualTo(1);
            assertThat(policyDAO.expirePolicies(NOW)).isZero();
        }

        private void assertStatus(Policy policy, PolicyStatus status, Long version) {
            Policy current = policyDAO.getPolicyByID(policy.getId()).orElseThrow();
            assertThat(current.getStatus()).as(policy.getEntityID()).isEqualTo(status);
            assertThat(current.getVersion()).as(policy.getEntityID()).isEqualTo(version);
        }

    }

}
