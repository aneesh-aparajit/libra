package dev.tsukuyomi.libra.controller;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.dto.request.CreatePolicyRequest;
import dev.tsukuyomi.libra.dto.request.UpdatePolicyRequest;
import dev.tsukuyomi.libra.dto.request.UpdatePolicyStatusRequest;
import dev.tsukuyomi.libra.dto.response.PolicyResponse;
import dev.tsukuyomi.libra.exception.ApiException;
import dev.tsukuyomi.libra.service.policy.IPolicyService;
import dev.tsukuyomi.libra.transformer.policy.CreatePolicyRequestTransformer;
import dev.tsukuyomi.libra.transformer.policy.PolicyResponseTransformer;
import dev.tsukuyomi.libra.transformer.policy.UpdatePolicyRequestTransformer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final IPolicyService policyService;
    private final CreatePolicyRequestTransformer createTransformer;
    private final UpdatePolicyRequestTransformer updateTransformer;
    private final PolicyResponseTransformer responseTransformer;

    @PostMapping
    public CompletableFuture<ResponseEntity<PolicyResponse>> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        return policyService.createPolicy(createTransformer.transform(request))
                .thenApply(policy -> ResponseEntity
                        .created(URI.create("/v1/policies/" + policy.getId()))
                        .body(responseTransformer.transform(policy)));
    }

    @GetMapping("/{id}")
    public CompletableFuture<PolicyResponse> getPolicyByID(@PathVariable Long id) {
        return policyService.getPolicyByID(id)
                .thenApply(policy -> policy
                        .map(responseTransformer::transform)
                        .orElseThrow(() -> notFound(id)));
    }

    /**
     * Lists policies by one of: (tenantID, scopeID), (entityID, scopeID) or entityID alone.
     */
    @GetMapping
    public CompletableFuture<List<PolicyResponse>> listPolicies(@RequestParam(required = false) Long tenantID,
                                                                @RequestParam(required = false) Long scopeID,
                                                                @RequestParam(required = false) String entityID) {
        CompletableFuture<List<Policy>> policies;
        if (tenantID != null && scopeID != null) {
            policies = policyService.getPoliciesByTenantIDAndScope(tenantID, scopeID);
        } else if (entityID != null && scopeID != null) {
            policies = policyService.getPoliciesByEntityIDAndScope(entityID, scopeID);
        } else if (entityID != null) {
            policies = policyService.getPoliciesByEntityID(entityID);
        } else {
            return CompletableFuture.failedFuture(ApiException.badRequest(
                    "provide tenantID+scopeID, entityID+scopeID, or entityID"));
        }
        return policies.thenApply(this::toResponses);
    }

    @GetMapping("/applicable")
    public CompletableFuture<List<PolicyResponse>> getApplicablePolicies(@RequestParam Long tenantID,
                                                                         @RequestParam Long scopeID,
                                                                         @RequestParam String entityID,
                                                                         @RequestParam(required = false) Long at) {
        long atEpochMillis = Objects.requireNonNullElseGet(at, System::currentTimeMillis);
        return policyService.getApplicablePolicies(tenantID, scopeID, entityID, atEpochMillis)
                .thenApply(this::toResponses);
    }

    /**
     * Full update guarded by optimistic locking on {@code version}.
     */
    @PutMapping("/{id}")
    public CompletableFuture<PolicyResponse> updatePolicy(@PathVariable Long id,
                                                          @Valid @RequestBody UpdatePolicyRequest request) {
        Policy policy = updateTransformer.transform(request);
        policy.setId(id);
        return policyService.updatePolicy(policy)
                .thenCompose(updated -> // no row matched (id, version): tell apart a missing policy from a stale version
                        updated.map(CompletableFuture::completedFuture)
                                .orElseGet(() -> policyService.getPolicyByID(id)
                                        .thenApply(existing -> {
                                            throw existing.map(value -> ApiException.conflict("policy " + id + " version mismatch, current version is "
                                                    + value.getVersion())).orElseGet(() -> notFound(id));
                                        })))
                .thenApply(responseTransformer::transform);
    }

    @PatchMapping("/{id}/status")
    public CompletableFuture<PolicyResponse> updatePolicyStatus(@PathVariable Long id,
                                                                @Valid @RequestBody UpdatePolicyStatusRequest request) {
        return policyService.getPolicyByID(id)
                .thenCompose(existing -> {
                    Policy policy = existing.orElseThrow(() -> notFound(id));
                    if (policy.getStatus() == PolicyStatus.EXPIRED) {
                        throw ApiException.conflict("policy " + id + " is expired and cannot change status");
                    }
                    return policyService.updatePolicyStatus(id, request.status());
                })
                .thenApply(updated -> updated
                        .map(responseTransformer::transform)
                        .orElseThrow(() -> notFound(id)));
    }

    private List<PolicyResponse> toResponses(List<Policy> policies) {
        return policies.stream().map(responseTransformer::transform).toList();
    }

    private static ApiException notFound(Long id) {
        return ApiException.notFound("policy " + id + " not found");
    }

}
