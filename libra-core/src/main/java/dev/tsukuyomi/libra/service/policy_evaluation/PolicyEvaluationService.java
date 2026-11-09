package dev.tsukuyomi.libra.service.policy_evaluation;

import dev.tsukuyomi.libra.dao.policy_evaluation.PolicyEvaluationDAO;
import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationDTO;
import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationResultDTO;
import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.entity.PolicyEvaluation;
import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import dev.tsukuyomi.libra.domain.enums.JsIdenfier;
import dev.tsukuyomi.libra.engine.IPolicyEngine;
import dev.tsukuyomi.libra.exeception.LibraEvaluationException;
import dev.tsukuyomi.libra.service.policy.IPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEvaluationService implements IPolicyEvaluationService {

    private final IPolicyService policyService;
    private final IPolicyEngine policyEngine;
    private final PolicyEvaluationDAO policyEvaluationDAO;
    private final ExecutorService libraExecutorService;

    @Override
    public CompletableFuture<List<PolicyEvaluationResultDTO>> evaluatePoliciesForUser(PolicyEvaluationDTO dto) {
        String inputAttrs = dto.getStringifiedInputAttrs();
        return policyService.getApplicablePolicies(dto.getTenantID(),
                        dto.getScopeID(), dto.getEntityID(), Instant.now().toEpochMilli())
                .thenComposeAsync(policies -> {
                    List<CompletableFuture<PolicyEvaluationResultDTO>> futures = policies.stream()
                            .map(policy -> evaluatePolicy(policy, inputAttrs))
                            .toList();
                    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                            .thenApply(v -> futures.stream()
                                    .map(CompletableFuture::join)
                                    .filter(Objects::nonNull)   // null = policy did not apply
                                    .toList());
                }, libraExecutorService)
                .exceptionally(th -> {
                    log.error("error when evaluating policy", th.getCause());
                    throw new LibraEvaluationException("error when evaluating policy", th.getCause());
                });
    }

    @Override
    public CompletableFuture<PolicyEvaluationDTO> evaluatePolicies(PolicyEvaluationDTO dto) {
        dto.setRequestID(UUID.randomUUID().toString());
        return evaluatePoliciesForUser(dto)
                .thenComposeAsync(results -> {
                    dto.setEvaluationResults(results);
                    return persistForAudit(dto)
                            .exceptionally(th -> {
                                Throwable cause = th instanceof CompletionException && th.getCause() != null ? th.getCause() : th;
                                log.error("error when persisting evals for audit", cause);
                                throw new LibraEvaluationException("error when persisting evals for audit", cause);
                            });
                }, libraExecutorService)
                .thenApplyAsync(__ -> {
                    List<PolicyEvaluationResultDTO> results = dto.getEvaluationResults();
                    List<PolicyEvaluationResultDTO> resultDTOs = results.stream()
                            .filter(result -> result.getResult().equals(EvaluationResult.DENY))
                            .toList();
                    if (resultDTOs.isEmpty()) {
                        dto.setEvaluationResult(EvaluationResult.ALLOW);
                        return dto;
                    }
                    dto.setEvaluationResult(EvaluationResult.DENY);
                    dto.setDeniedPolicies(resultDTOs.stream().map(PolicyEvaluationResultDTO::getPolicyCode).toList());
                    return dto;
                }, libraExecutorService);
    }

    private CompletableFuture<PolicyEvaluationResultDTO> evaluatePolicy(Policy policy, String inputAttrs) {
        return CompletableFuture.supplyAsync(() -> {
                    if (!Boolean.TRUE.equals(policyEngine.evaluate(policy, JsIdenfier.APPLIES, inputAttrs))) {
                        return null;
                    }
                    boolean allowed = Boolean.TRUE.equals(policyEngine.evaluate(policy, JsIdenfier.POLICY, inputAttrs));
                    log.info("{} result: {}", policy.getCode(), allowed);
                    return resultFor(policy, allowed ? EvaluationResult.ALLOW : EvaluationResult.DENY, null);
                }, libraExecutorService)
                .exceptionally(th -> {
                    Throwable cause = th instanceof CompletionException && th.getCause() != null ? th.getCause() : th;
                    log.error("error evaluating policy [{}]", policy.getCode(), cause);
                    return resultFor(policy, EvaluationResult.DENY,
                            Map.of("msg", Objects.toString(cause.getMessage(), cause.getClass().getSimpleName())));
                });
    }

    private static PolicyEvaluationResultDTO resultFor(Policy policy, EvaluationResult result, Map<String, Object> errors) {
        return PolicyEvaluationResultDTO.builder()
                .tenantID(policy.getTenantID())
                .policyID(policy.getId())
                .scopeID(policy.getScopeID())
                .entityID(policy.getEntityID())
                .policyCode(policy.getCode())
                .result(result)
                .errors(errors)
                .build();
    }

    private CompletableFuture<Void> persistForAudit(PolicyEvaluationDTO dto) {
        return CompletableFuture.completedFuture(dto.getEvaluationResults())
                .thenApplyAsync(results -> results.stream()
                        .map(result -> PolicyEvaluation.builder()
                                .requestID(dto.getRequestID())
                                .entityID(dto.getEntityID())
                                .policyID(result.getPolicyID())
                                .inputAttrs(dto.getInputAttrs())
                                .errors(result.getErrors())
                                .result(result.getResult())
                                .build()).toList(), libraExecutorService)
                .thenAccept(policyEvaluationDAO::createPolicyEvaluations);
    }

}
