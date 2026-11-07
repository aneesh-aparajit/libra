package dev.tsukuyomi.libra.service.scope;

import dev.tsukuyomi.libra.domain.entity.Scope;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IScopeService {

    CompletableFuture<Scope> createScope(Scope scope);

    CompletableFuture<Optional<Scope>> getScopeByIDAndScopeName(Long id, String scopeName);

    CompletableFuture<List<Scope>> getScopesByTenantID(Long tenantID);

    CompletableFuture<Optional<Scope>> getScopeByID(Long id);

}
