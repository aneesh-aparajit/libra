package dev.tsukuyomi.libra.transformer.scope;

import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.dto.request.CreateScopeRequest;
import dev.tsukuyomi.libra.transformer.Transformer;
import org.springframework.stereotype.Component;

@Component
public class ScopeRequestTransformer implements Transformer<CreateScopeRequest, Scope> {

    @Override
    public Scope transform(CreateScopeRequest request) {
        return Scope.builder()
                .tenantID(request.tenantID())
                .ScopeName(request.scopeName().trim())
                .build();
    }

}
