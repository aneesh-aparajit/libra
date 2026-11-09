package dev.tsukuyomi.libra.service.policy;

import dev.tsukuyomi.libra.dao.policy.PolicyDAO;
import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.exeception.LibraServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService implements IPolicyService {

    private final PolicyDAO policyDAO;
    private final ExecutorService libraServiceExecutor;

    @Override
    public CompletableFuture<Policy> createPolicy(Policy policy) {
        return CompletableFuture.supplyAsync(() -> policyDAO.createPolicy(policy), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error creating policy", ex);
                    throw new LibraServiceException("error creating policy", ex);
                });
    }

    @Override
    public CompletableFuture<Optional<Policy>> getPolicyByID(Long id) {
        return CompletableFuture.supplyAsync(() -> policyDAO.getPolicyByID(id), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error getting policy by ID", ex);
                    throw new LibraServiceException("error getting policy by ID", ex);
                });
    }

    @Override
    public CompletableFuture<List<Policy>> getPoliciesByEntityID(String entityID) {
        return CompletableFuture.supplyAsync(() -> policyDAO.getPoliciesByEntityID(entityID), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error getting policies by entity ID", ex);
                    throw new LibraServiceException("error getting policies by entity ID", ex);
                });
    }

    @Override
    public CompletableFuture<List<Policy>> getPoliciesByEntityIDAndScope(String entityID, Long scopeID) {
        return CompletableFuture.supplyAsync(() -> policyDAO.getPoliciesByEntityIDAndScope(entityID, scopeID), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error getting policies by entity ID and scope", ex);
                    throw new LibraServiceException("error getting policies by entity ID and scope", ex);
                });
    }

    @Override
    public CompletableFuture<List<Policy>> getPoliciesByTenantIDAndScope(Long tenantID, Long scopeID) {
        return CompletableFuture.supplyAsync(() -> policyDAO.getPoliciesByTenantIDAndScope(tenantID, scopeID), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error getting policies by tenant ID and scope", ex);
                    throw new LibraServiceException("error getting policies by tenant ID and scope", ex);
                });
    }

    @Override
    public CompletableFuture<List<Policy>> getApplicablePolicies(Long tenantID, Long scopeID, String entityID, Long atEpochMillis) {
        return CompletableFuture.supplyAsync(() -> policyDAO.getApplicablePolicies(tenantID, scopeID, entityID, atEpochMillis), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error getting applicable policies", ex);
                    throw new LibraServiceException("error getting applicable policies", ex);
                });
    }

    @Override
    public CompletableFuture<Optional<Policy>> updatePolicy(Policy policy) {
        return CompletableFuture.supplyAsync(() -> policyDAO.updatePolicy(policy), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error updating policy", ex);
                    throw new LibraServiceException("error updating policy", ex);
                });
    }

    @Override
    public CompletableFuture<Optional<Policy>> updatePolicyStatus(Long id, PolicyStatus status) {
        return CompletableFuture.completedFuture(policyDAO.getPolicyByID(id))
                .thenAccept(policy -> {
                    if (policy.isEmpty()) {
                        log.error("policy is empty");
                        throw new LibraServiceException("policy is empty");
                    }
                    if (policy.get().getStatus() == PolicyStatus.EXPIRED) {
                        log.error("policy is expired");
                        throw new LibraServiceException("policy is expired");
                    }
                })
                .thenApplyAsync(__ -> policyDAO.updatePolicyStatus(id, status), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error updating policy status", ex);
                    throw new LibraServiceException("error updating policy status", ex);
                });
    }

    @Override
    public CompletableFuture<Integer> expirePolicies(Long atEpochMillis) {
        return CompletableFuture.supplyAsync(() -> policyDAO.expirePolicies(atEpochMillis), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error expiring policies", ex);
                    throw new LibraServiceException("error expiring policies", ex);
                });
    }

}
