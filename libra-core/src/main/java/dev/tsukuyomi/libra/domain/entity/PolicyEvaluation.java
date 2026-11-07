package dev.tsukuyomi.libra.domain.entity;

import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class PolicyEvaluation {

    private Long id;
    private String requestID;
    private String entityID;
    private Long policyID;
    private Map<String, Object> inputAttrs;
    private Map<String, Object> errors;
    private EvaluationResult result;
    private Long createdAt;
    private Long updatedAt;

}
