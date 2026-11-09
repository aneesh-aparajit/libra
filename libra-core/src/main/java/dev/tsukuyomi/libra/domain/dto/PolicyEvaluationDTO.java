package dev.tsukuyomi.libra.domain.dto;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import dev.tsukuyomi.libra.utils.JsonUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PolicyEvaluationDTO extends BaseDTO {

    private String requestID;
    private List<Policy> policies;
    private Map<String, Object> inputAttrs;
    private List<PolicyEvaluationResultDTO> evaluationResults;
    private EvaluationResult evaluationResult;
    private List<String> deniedPolicies;

    public String getStringifiedInputAttrs() {
        return JsonUtil.toJson(inputAttrs);
    }

}
