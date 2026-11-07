package dev.tsukuyomi.libra.service.scope;

import dev.tsukuyomi.libra.dao.scope.ScopeDAO;
import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import dev.tsukuyomi.libra.exeception.LibraServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScopeServiceTest {

    @Mock
    private ScopeDAO scopeDAO;

    private ExecutorService executor;
    private ScopeService scopeService;

    private final Scope scope = Scope.builder().id(3L).tenantID(1L).ScopeName("sms").build();

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        scopeService = new ScopeService(scopeDAO, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void createScopeReturnsDAOResult() {
        when(scopeDAO.createScope(scope)).thenReturn(scope);

        assertThat(scopeService.createScope(scope).join()).isEqualTo(scope);
        verify(scopeDAO).createScope(scope);
    }

    @Test
    void createScopeWrapsDAOFailure() {
        when(scopeDAO.createScope(scope)).thenThrow(new LibraDAOException("error creating scope"));

        assertThatThrownBy(() -> scopeService.createScope(scope).join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error creating scope")
                .hasRootCauseInstanceOf(LibraDAOException.class);
    }

    @Test
    void getScopeByIDAndScopeNameReturnsDAOResult() {
        when(scopeDAO.getScopeByIDAndScopeName(3L, "sms")).thenReturn(Optional.of(scope));
        when(scopeDAO.getScopeByIDAndScopeName(3L, "email")).thenReturn(Optional.empty());

        assertThat(scopeService.getScopeByIDAndScopeName(3L, "sms").join()).contains(scope);
        assertThat(scopeService.getScopeByIDAndScopeName(3L, "email").join()).isEmpty();
    }

    @Test
    void getScopeByIDAndScopeNameWrapsDAOFailure() {
        when(scopeDAO.getScopeByIDAndScopeName(3L, "sms")).thenThrow(new LibraDAOException("error getting scope"));

        assertThatThrownBy(() -> scopeService.getScopeByIDAndScopeName(3L, "sms").join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error getting scope by ID and scope name")
                .hasRootCauseInstanceOf(LibraDAOException.class);
    }

    @Test
    void getScopesByTenantIDReturnsDAOResult() {
        when(scopeDAO.getScopesByTenantID(1L)).thenReturn(List.of(scope));

        assertThat(scopeService.getScopesByTenantID(1L).join()).containsExactly(scope);
    }

    @Test
    void getScopesByTenantIDWrapsDAOFailure() {
        when(scopeDAO.getScopesByTenantID(1L)).thenThrow(new LibraDAOException("error getting scopes"));

        assertThatThrownBy(() -> scopeService.getScopesByTenantID(1L).join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error getting scopes by tenant ID")
                .hasRootCauseInstanceOf(LibraDAOException.class);
    }

    @Test
    void getScopeByIDReturnsDAOResult() {
        when(scopeDAO.getScopeByID(3L)).thenReturn(Optional.of(scope));
        when(scopeDAO.getScopeByID(4L)).thenReturn(Optional.empty());

        assertThat(scopeService.getScopeByID(3L).join()).contains(scope);
        assertThat(scopeService.getScopeByID(4L).join()).isEmpty();
    }

    @Test
    void getScopeByIDWrapsDAOFailure() {
        when(scopeDAO.getScopeByID(3L)).thenThrow(new LibraDAOException("error getting scope"));

        assertThatThrownBy(() -> scopeService.getScopeByID(3L).join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(LibraServiceException.class)
                .hasMessage("error getting scope by ID")
                .hasRootCauseInstanceOf(LibraDAOException.class);
    }

}
