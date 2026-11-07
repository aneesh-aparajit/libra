package dev.tsukuyomi.libra.service.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IPolicyService {

    CompletableFuture<Policy> createPolicy(Policy policy);

    CompletableFuture<Optional<Policy>> getPolicyByID(Long id);

    CompletableFuture<List<Policy>> getPoliciesByEntityID(String entityID);

    CompletableFuture<List<Policy>> getPoliciesByEntityIDAndScope(String entityID, Long scopeID);

    CompletableFuture<List<Policy>> getPoliciesByTenantIDAndScope(Long tenantID, Long scopeID);

    CompletableFuture<List<Policy>> getApplicablePolicies(Long tenantID, Long scopeID, String entityID, Long atEpochMillis);

    CompletableFuture<Optional<Policy>> updatePolicy(Policy policy);

    CompletableFuture<Optional<Policy>> updatePolicyStatus(Long id, PolicyStatus status);

    CompletableFuture<Integer> expirePolicies(Long atEpochMillis);

}
