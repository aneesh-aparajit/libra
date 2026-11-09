package dev.tsukuyomi.libra.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
public class BaseDTO {

    private Long tenantID;
    private Long scopeID;
    private Long policyID;
    private String entityID;

    public Map<String, Object> logAttrs() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tenantID", tenantID.toString());
        attrs.put("scopeID", scopeID.toString());
        attrs.put("entityID", entityID);
        return attrs;
    }

}
