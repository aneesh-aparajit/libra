package dev.tsukuyomi.libra.service.scope;

import dev.tsukuyomi.libra.dao.scope.ScopeDAO;
import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.exeception.LibraServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScopeService implements IScopeService {

    private final ScopeDAO scopeDAO;
    private final ExecutorService libraServiceExecutor;

    @Override
    public CompletableFuture<Scope> createScope(Scope scope) {
        return CompletableFuture.supplyAsync(() -> scopeDAO.createScope(scope), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error creating scope", ex);
                    throw new LibraServiceException("error creating scope", ex);
                });
    }

    @Override
    public CompletableFuture<Optional<Scope>> getScopeByIDAndScopeName(Long id, String scopeName) {
        return CompletableFuture.supplyAsync(() -> scopeDAO.getScopeByIDAndScopeName(id, scopeName), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error getting scope by ID and scope name", ex);
                    throw new LibraServiceException("error getting scope by ID and scope name", ex);
                });
    }

    @Override
    public CompletableFuture<List<Scope>> getScopesByTenantID(Long tenantID) {
        return CompletableFuture.supplyAsync(() -> scopeDAO.getScopesByTenantID(tenantID), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error getting scopes by tenant ID", ex);
                    throw new LibraServiceException("error getting scopes by tenant ID", ex);
                });
    }

    @Override
    public CompletableFuture<Optional<Scope>> getScopeByID(Long id) {
        return CompletableFuture.supplyAsync(() -> scopeDAO.getScopeByID(id), libraServiceExecutor)
                .exceptionally(ex -> {
                    log.error("error getting scope by ID", ex);
                    throw new LibraServiceException("error getting scope by ID", ex);
                });
    }

}
