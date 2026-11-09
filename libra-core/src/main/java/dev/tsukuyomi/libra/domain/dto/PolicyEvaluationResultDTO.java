package dev.tsukuyomi.libra.domain.dto;

import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import dev.tsukuyomi.libra.utils.JsonUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PolicyEvaluationResultDTO extends BaseDTO {

    private String policyCode;
    private EvaluationResult result;
    private Map<String, Object> errors;

    @Override
    public Map<String, Object> logAttrs() {
        Map<String, Object> attrs = super.logAttrs();
        attrs.put("policyCode", policyCode);
        attrs.put("result", result);
        attrs.put("errors", JsonUtil.toJson(errors));
        return attrs;
    }

}
