package dev.tsukuyomi.libra.service.policy_evaluation;

import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationDTO;
import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationResultDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IPolicyEvaluationService {

    CompletableFuture<List<PolicyEvaluationResultDTO>> evaluatePoliciesForUser(PolicyEvaluationDTO dto);

    CompletableFuture<PolicyEvaluationDTO> evaluatePolicies(PolicyEvaluationDTO dto);

}
