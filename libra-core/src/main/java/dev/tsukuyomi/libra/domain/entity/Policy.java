package dev.tsukuyomi.libra.domain.entity;

import dev.tsukuyomi.libra.domain.enums.JsIdenfier;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import static dev.tsukuyomi.libra.domain.enums.JsIdenfier.APPLIES;

@Data
@Builder
@AllArgsConstructor
public class Policy {

    private Long id;
    private Long tenantID;
    private Long scopeID;
    private String entityID;
    private String code;
    private String applied_js;
    private String evaluated_js;
    private PolicyStatus status;
    private Long validFrom;
    private Long validTo;
    private Long version;
    private Long createdAt;
    private Long updatedAt;

    public String getName(JsIdenfier identifier) {
        if (identifier == APPLIES) {
            return String.format("%s-%s-%s-%s-%s", tenantID, scopeID, entityID, code, APPLIES.name());
        }
        return  String.format("%s-%s-%s-%s-%s", tenantID, scopeID, entityID, code, identifier.name());
    }

    public String getJS(JsIdenfier identifier) {
        return identifier == APPLIES ? applied_js : evaluated_js;
    }

}
