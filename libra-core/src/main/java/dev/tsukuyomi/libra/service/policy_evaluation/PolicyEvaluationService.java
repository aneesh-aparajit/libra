package dev.tsukuyomi.libra.service.policy_evaluation;

import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationDTO;
import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationResultDTO;
import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import dev.tsukuyomi.libra.domain.enums.JsIdenfier;
import dev.tsukuyomi.libra.engine.IPolicyEngine;
import dev.tsukuyomi.libra.service.policy.IPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEvaluationService implements IPolicyEvaluationService {

    private final IPolicyService policyService;
    private final IPolicyEngine policyEngine;
    private final ExecutorService libraExecutorService;

    @Override
    public CompletableFuture<List<PolicyEvaluationResultDTO>> evaluatePoliciesForUser(PolicyEvaluationDTO dto) {
        String inputAttrs = dto.getStringifiedInputAttrs();
        return policyService.getApplicablePolicies(dto.getTenantID(),
                        dto.getScopeID(), dto.getEntityID(), Instant.now().toEpochMilli())
                .thenApplyAsync(policies -> policies.stream()
                        .filter(policy -> policyEngine.evaluate(policy, JsIdenfier.APPLIES, inputAttrs))
                        .toList(), libraExecutorService)
                .thenApplyAsync(policies -> policies.stream()
                                .<PolicyEvaluationResultDTO>map(policy -> {
                                    boolean allowed = policyEngine.evaluate(policy, JsIdenfier.POLICY, inputAttrs);
                                    log.info("{} result: {}", policy.getCode(), allowed);
                                    return PolicyEvaluationResultDTO.builder()
                                            .tenantID(policy.getTenantID())
                                            .scopeID(policy.getScopeID())
                                            .entityID(policy.getEntityID())
                                            .policyCode(policy.getCode())
                                            .result(allowed ? EvaluationResult.ALLOW : EvaluationResult.DENY)
                                            .build();
                                }).toList()
                        , libraExecutorService);
    }

}
