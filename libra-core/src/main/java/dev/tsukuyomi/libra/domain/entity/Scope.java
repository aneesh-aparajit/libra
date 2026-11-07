package dev.tsukuyomi.libra.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Scope {

    private Long id;
    private Long tenantID;
    private String ScopeName;
    private Long createdAt;
    private Long updatedAt;

}
