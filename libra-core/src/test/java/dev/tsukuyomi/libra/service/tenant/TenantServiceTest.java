package dev.tsukuyomi.libra.service.tenant;

import dev.tsukuyomi.libra.dao.tenant.TenantDAO;
import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import dev.tsukuyomi.libra.exeception.LibraServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantDAO tenantDAO;

    private ExecutorService executor;
    private TenantService tenantService;

    private final Tenant tenant = Tenant.builder().id(1L).tenantCode("acme").tenantName("Acme").build();

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        tenantService = new TenantService(tenantDAO, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void createTenantReturnsDAOResult() {
        when(tenantDAO.createTenant(tenant)).thenReturn(tenant);

        assertThat(tenantService.createTenant(tenant).join()).isEqualTo(tenant);
        verify(tenantDAO).createTenant(tenant);
    }

    @Test
    void createTenantWrapsDAOFailure() {
        when(tenantDAO.createTenant(tenant)).thenThrow(new LibraDAOException("error creating tenant"));

        assertThatThrownBy(() -> tenantService.createTenant(tenant).join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error creating tenant")
                .hasRootCauseInstanceOf(LibraDAOException.class);
    }

    @Test
    void getTenantByIDReturnsDAOResult() {
        when(tenantDAO.getTenantByID(1L)).thenReturn(Optional.of(tenant));
        when(tenantDAO.getTenantByID(2L)).thenReturn(Optional.empty());

        assertThat(tenantService.getTenantByID(1L).join()).contains(tenant);
        assertThat(tenantService.getTenantByID(2L).join()).isEmpty();
    }

    @Test
    void getTenantByIDWrapsDAOFailure() {
        when(tenantDAO.getTenantByID(1L)).thenThrow(new LibraDAOException("error getting tenant by ID"));

        assertThatThrownBy(() -> tenantService.getTenantByID(1L).join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error getting tenant by ID")
                .hasRootCauseInstanceOf(LibraDAOException.class);
    }

    @Test
    void getTenantByCodeReturnsDAOResult() {
        when(tenantDAO.getTenantByCode("acme")).thenReturn(Optional.of(tenant));
        when(tenantDAO.getTenantByCode("missing")).thenReturn(Optional.empty());

        assertThat(tenantService.getTenantByCode("acme").join()).contains(tenant);
        assertThat(tenantService.getTenantByCode("missing").join()).isEmpty();
    }

    @Test
    void getTenantByCodeWrapsDAOFailure() {
        when(tenantDAO.getTenantByCode("acme")).thenThrow(new LibraDAOException("error getting tenant by code"));

        assertThatThrownBy(() -> tenantService.getTenantByCode("acme").join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error getting tenant by code")
                .hasRootCauseInstanceOf(LibraDAOException.class);
    }

}
