package dev.tsukuyomi.libra.domain.entity;

import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

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

}
