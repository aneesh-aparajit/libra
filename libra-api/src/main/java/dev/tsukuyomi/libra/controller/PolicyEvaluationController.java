package dev.tsukuyomi.libra.controller;

import dev.tsukuyomi.libra.dto.request.EvaluatePolicyRequest;
import dev.tsukuyomi.libra.dto.response.PolicyEvaluationResponse;
import dev.tsukuyomi.libra.service.policy_evaluation.IPolicyEvaluationService;
import dev.tsukuyomi.libra.transformer.policy_evaluation.EvaluatePolicyRequestTransformer;
import dev.tsukuyomi.libra.transformer.policy_evaluation.PolicyEvaluationResponseTransformer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/evaluate")
@RequiredArgsConstructor
public class PolicyEvaluationController {

    private final IPolicyEvaluationService policyEvaluationService;
    private final EvaluatePolicyRequestTransformer requestTransformer;
    private final PolicyEvaluationResponseTransformer responseTransformer;

    /**
     * Evaluates every policy applicable to the entity right now. Always {@code 200} when evaluation ran:
     * a deny is a valid answer, not an error. Each call is recorded for audit under the returned {@code requestID}.
     */
    @PostMapping
    public CompletableFuture<PolicyEvaluationResponse> evaluate(@Valid @RequestBody EvaluatePolicyRequest request) {
        return policyEvaluationService.evaluatePolicies(requestTransformer.transform(request))
                .thenApply(responseTransformer::transform);
    }

}
