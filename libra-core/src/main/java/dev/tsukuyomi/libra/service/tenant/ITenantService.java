package dev.tsukuyomi.libra.service.tenant;

import dev.tsukuyomi.libra.domain.entity.Tenant;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ITenantService {

    CompletableFuture<Tenant> createTenant(Tenant tenant);

    CompletableFuture<Optional<Tenant>> getTenantByID(Long id);

    CompletableFuture<Optional<Tenant>> getTenantByCode(String tenantCode);

}
