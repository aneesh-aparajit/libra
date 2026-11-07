package dev.tsukuyomi.libra.transformer.tenant;

import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.dto.response.TenantResponse;
import dev.tsukuyomi.libra.transformer.Transformer;
import org.springframework.stereotype.Component;

@Component
public class TenantResponseTransformer implements Transformer<Tenant, TenantResponse> {

    @Override
    public TenantResponse transform(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getTenantCode(),
                tenant.getTenantName(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }

}
