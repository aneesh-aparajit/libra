package dev.tsukuyomi.libra.service.policy;

import dev.tsukuyomi.libra.dao.policy.PolicyDAO;
import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import dev.tsukuyomi.libra.exeception.LibraServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyDAO policyDAO;

    private ExecutorService executor;
    private PolicyService policyService;

    private final Policy policy = newPolicy(PolicyStatus.ACTIVE, 1L);

    private static Policy newPolicy(PolicyStatus status, Long version) {
        return Policy.builder()
                .id(11L)
                .tenantID(1L)
                .scopeID(3L)
                .entityID("user-1")
                .code("no-sms-after-22")
                .status(status)
                .validFrom(100L)
                .version(version)
                .build();
    }

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        policyService = new PolicyService(policyDAO, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    private static void assertWrapsDAOFailure(CompletableFuture<?> future, String message) {
        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage(message)
                .hasRootCauseInstanceOf(LibraDAOException.class);
    }

    @Test
    void createPolicyReturnsDAOResult() {
        when(policyDAO.createPolicy(policy)).thenReturn(policy);

        assertThat(policyService.createPolicy(policy).join()).isEqualTo(policy);
        verify(policyDAO).createPolicy(policy);
    }

    @Test
    void createPolicyWrapsDAOFailure() {
        when(policyDAO.createPolicy(policy)).thenThrow(new LibraDAOException("error creating policy"));

        assertWrapsDAOFailure(policyService.createPolicy(policy), "error creating policy");
    }

    @Test
    void getPolicyByIDReturnsDAOResult() {
        when(policyDAO.getPolicyByID(11L)).thenReturn(Optional.of(policy));
        when(policyDAO.getPolicyByID(12L)).thenReturn(Optional.empty());

        assertThat(policyService.getPolicyByID(11L).join()).contains(policy);
        assertThat(policyService.getPolicyByID(12L).join()).isEmpty();
    }

    @Test
    void getPolicyByIDWrapsDAOFailure() {
        when(policyDAO.getPolicyByID(11L)).thenThrow(new LibraDAOException("error getting policy by ID"));

        assertWrapsDAOFailure(policyService.getPolicyByID(11L), "error getting policy by ID");
    }

    @Test
    void getPoliciesByEntityIDReturnsDAOResult() {
        when(policyDAO.getPoliciesByEntityID("user-1")).thenReturn(List.of(policy));

        assertThat(policyService.getPoliciesByEntityID("user-1").join()).containsExactly(policy);
    }

    @Test
    void getPoliciesByEntityIDWrapsDAOFailure() {
        when(policyDAO.getPoliciesByEntityID("user-1")).thenThrow(new LibraDAOException("error getting policies by entity ID"));

        assertWrapsDAOFailure(policyService.getPoliciesByEntityID("user-1"), "error getting policies by entity ID");
    }

    @Test
    void getPoliciesByEntityIDAndScopeReturnsDAOResult() {
        when(policyDAO.getPoliciesByEntityIDAndScope("user-1", 3L)).thenReturn(List.of(policy));

        assertThat(policyService.getPoliciesByEntityIDAndScope("user-1", 3L).join()).containsExactly(policy);
    }

    @Test
    void getPoliciesByEntityIDAndScopeWrapsDAOFailure() {
        when(policyDAO.getPoliciesByEntityIDAndScope("user-1", 3L)).thenThrow(new LibraDAOException("error getting policies by entity ID and scope"));

        assertWrapsDAOFailure(policyService.getPoliciesByEntityIDAndScope("user-1", 3L), "error getting policies by entity ID and scope");
    }

    @Test
    void getPoliciesByTenantIDAndScopeReturnsDAOResult() {
        when(policyDAO.getPoliciesByTenantIDAndScope(1L, 3L)).thenReturn(List.of(policy));

        assertThat(policyService.getPoliciesByTenantIDAndScope(1L, 3L).join()).containsExactly(policy);
    }

    @Test
    void getPoliciesByTenantIDAndScopeWrapsDAOFailure() {
        when(policyDAO.getPoliciesByTenantIDAndScope(1L, 3L)).thenThrow(new LibraDAOException("error getting policies by tenant ID and scope"));

        assertWrapsDAOFailure(policyService.getPoliciesByTenantIDAndScope(1L, 3L), "error getting policies by tenant ID and scope");
    }

    @Test
    void getApplicablePoliciesReturnsDAOResult() {
        when(policyDAO.getApplicablePolicies(1L, 3L, "user-1", 150L)).thenReturn(List.of(policy));

        assertThat(policyService.getApplicablePolicies(1L, 3L, "user-1", 150L).join()).containsExactly(policy);
    }

    @Test
    void getApplicablePoliciesWrapsDAOFailure() {
        when(policyDAO.getApplicablePolicies(1L, 3L, "user-1", 150L)).thenThrow(new LibraDAOException("error getting applicable policies"));

        assertWrapsDAOFailure(policyService.getApplicablePolicies(1L, 3L, "user-1", 150L), "error getting applicable policies");
    }

    @Test
    void updatePolicyReturnsDAOResult() {
        Policy updated = newPolicy(PolicyStatus.ACTIVE, 2L);
        when(policyDAO.updatePolicy(policy)).thenReturn(Optional.of(updated));

        assertThat(policyService.updatePolicy(policy).join()).contains(updated);
    }

    @Test
    void updatePolicyReturnsEmptyOnStaleVersion() {
        when(policyDAO.updatePolicy(policy)).thenReturn(Optional.empty());

        assertThat(policyService.updatePolicy(policy).join()).isEmpty();
    }

    @Test
    void updatePolicyWrapsDAOFailure() {
        when(policyDAO.updatePolicy(policy)).thenThrow(new LibraDAOException("error updating policy"));

        assertWrapsDAOFailure(policyService.updatePolicy(policy), "error updating policy");
    }

    @Test
    void updatePolicyStatusReturnsDAOResult() {
        Policy inactive = newPolicy(PolicyStatus.INACTIVE, 2L);
        when(policyDAO.getPolicyByID(11L)).thenReturn(Optional.of(policy));
        when(policyDAO.updatePolicyStatus(11L, PolicyStatus.INACTIVE)).thenReturn(Optional.of(inactive));

        assertThat(policyService.updatePolicyStatus(11L, PolicyStatus.INACTIVE).join()).contains(inactive);
        verify(policyDAO).getPolicyByID(11L);
        verify(policyDAO).updatePolicyStatus(11L, PolicyStatus.INACTIVE);
    }

    @Test
    void updatePolicyStatusReturnsEmptyWhenDAOUpdatesNothing() {
        when(policyDAO.getPolicyByID(11L)).thenReturn(Optional.of(policy));
        when(policyDAO.updatePolicyStatus(11L, PolicyStatus.INACTIVE)).thenReturn(Optional.empty());

        assertThat(policyService.updatePolicyStatus(11L, PolicyStatus.INACTIVE).join()).isEmpty();
    }

    @Test
    void updatePolicyStatusFailsWhenPolicyMissing() {
        when(policyDAO.getPolicyByID(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.updatePolicyStatus(11L, PolicyStatus.INACTIVE).join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error updating policy status")
                .hasRootCauseInstanceOf(LibraServiceException.class)
                .hasRootCauseMessage("policy is empty");
        verify(policyDAO, never()).updatePolicyStatus(any(), any());
    }

    @Test
    void updatePolicyStatusFailsWhenPolicyExpired() {
        when(policyDAO.getPolicyByID(11L)).thenReturn(Optional.of(newPolicy(PolicyStatus.EXPIRED, 1L)));

        assertThatThrownBy(() -> policyService.updatePolicyStatus(11L, PolicyStatus.ACTIVE).join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error updating policy status")
                .hasRootCauseInstanceOf(LibraServiceException.class)
                .hasRootCauseMessage("policy is expired");
        verify(policyDAO, never()).updatePolicyStatus(any(), any());
    }

    @Test
    void updatePolicyStatusWrapsDAOFailure() {
        when(policyDAO.getPolicyByID(11L)).thenReturn(Optional.of(policy));
        when(policyDAO.updatePolicyStatus(11L, PolicyStatus.INACTIVE)).thenThrow(new LibraDAOException("error updating policy status"));

        assertWrapsDAOFailure(policyService.updatePolicyStatus(11L, PolicyStatus.INACTIVE), "error updating policy status");
    }

    @Test
    void expirePoliciesReturnsDAOResult() {
        when(policyDAO.expirePolicies(200L)).thenReturn(4);

        assertThat(policyService.expirePolicies(200L).join()).isEqualTo(4);
    }

    @Test
    void expirePoliciesWrapsDAOFailure() {
        when(policyDAO.expirePolicies(200L)).thenThrow(new LibraDAOException("error expiring policies"));

        assertWrapsDAOFailure(policyService.expirePolicies(200L), "error expiring policies");
    }

}
