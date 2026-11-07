package dev.tsukuyomi.libra.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Tenant {

    private Long id;
    private String tenantCode;
    private String tenantName;
    private Long createdAt;
    private Long updatedAt;

}
