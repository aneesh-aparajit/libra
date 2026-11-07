package dev.tsukuyomi.libra.service.tenant;

import dev.tsukuyomi.libra.dao.tenant.TenantDAO;
import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.exeception.LibraServiceException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class TenantService implements ITenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

    private final TenantDAO tenantDAO;
    private final ExecutorService libraServiceExecutor;

    @Override
    public CompletableFuture<Tenant> createTenant(Tenant tenant) {
        return CompletableFuture.supplyAsync(() -> tenantDAO.createTenant(tenant), libraServiceExecutor)
                .exceptionally(ex -> {
                    logger.error("error creating tenant", ex);
                    throw new LibraServiceException("error creating tenant", ex);
                });
    }

    @Override
    public CompletableFuture<Optional<Tenant>> getTenantByID(Long id) {
        return CompletableFuture.supplyAsync(() -> tenantDAO.getTenantByID(id), libraServiceExecutor)
                .exceptionally(ex -> {
                    logger.error("error getting tenant by ID", ex);
                    throw new LibraServiceException("error getting tenant by ID", ex);
                });
    }

    @Override
    public CompletableFuture<Optional<Tenant>> getTenantByCode(String tenantCode) {
        return CompletableFuture.supplyAsync(() -> tenantDAO.getTenantByCode(tenantCode), libraServiceExecutor)
                .exceptionally(ex -> {
                    logger.error("error getting tenant by code", ex);
                    throw new LibraServiceException("error getting tenant by code", ex);
                });
    }

}
