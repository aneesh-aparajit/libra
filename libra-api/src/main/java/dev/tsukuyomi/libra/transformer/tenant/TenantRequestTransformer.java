package dev.tsukuyomi.libra.transformer.tenant;

import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.dto.request.CreateTenantRequest;
import dev.tsukuyomi.libra.transformer.Transformer;
import org.springframework.stereotype.Component;

@Component
public class TenantRequestTransformer implements Transformer<CreateTenantRequest, Tenant> {

    @Override
    public Tenant transform(CreateTenantRequest request) {
        return Tenant.builder()
                .tenantCode(request.tenantCode().trim())
                .tenantName(request.tenantName().trim())
                .build();
    }

}
