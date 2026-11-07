package dev.tsukuyomi.libra.transformer.scope;

import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.dto.response.ScopeResponse;
import dev.tsukuyomi.libra.transformer.Transformer;
import org.springframework.stereotype.Component;

@Component
public class ScopeResponseTransformer implements Transformer<Scope, ScopeResponse> {

    @Override
    public ScopeResponse transform(Scope scope) {
        return new ScopeResponse(
                scope.getId(),
                scope.getTenantID(),
                scope.getScopeName(),
                scope.getCreatedAt(),
                scope.getUpdatedAt()
        );
    }

}
