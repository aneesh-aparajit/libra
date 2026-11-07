package dev.tsukuyomi.libra.domain.dto;

import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PolicyEvalDTO {

    private Long tenantID;
    private Long scopeID;
    private String entityID;
    private Map<String, Object> inputAttrs;
    private EvaluationResult result;

}
