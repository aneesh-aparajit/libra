package dev.tsukuyomi.libra.controller;

import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.service.scope.IScopeService;
import dev.tsukuyomi.libra.transformer.scope.ScopeRequestTransformer;
import dev.tsukuyomi.libra.transformer.scope.ScopeResponseTransformer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;
import java.util.Optional;

import static dev.tsukuyomi.libra.controller.Futures.daoFailure;
import static dev.tsukuyomi.libra.controller.Futures.done;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(ScopeController.class)
@Import({ScopeRequestTransformer.class, ScopeResponseTransformer.class})
class ScopeControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IScopeService scopeService;

    private static Scope scope(long id, String name) {
        return Scope.builder().id(id).tenantID(1L).ScopeName(name).createdAt(100L).updatedAt(200L).build();
    }

    private MvcTestResult postScope(String body) {
        return mvc.post().uri("/v1/scopes").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    void createScopeReturns201WithLocationAndBody() {
        when(scopeService.createScope(any())).thenReturn(done(scope(3L, "payment.credit")));

        MvcTestResult result = postScope("""
                {"tenantID": 1, "scopeName": " payment.credit "}""");

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).headers().hasValue("Location", "/v1/scopes/3");
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.tenantID").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.scopeName").isEqualTo("payment.credit");

        ArgumentCaptor<Scope> captor = ArgumentCaptor.forClass(Scope.class);
        verify(scopeService).createScope(captor.capture());
        assertThat(captor.getValue().getTenantID()).isEqualTo(1L);
        assertThat(captor.getValue().getScopeName()).isEqualTo("payment.credit");
    }

    @Test
    void createScopeRejectsMissingTenantID() {
        assertThat(postScope("""
                {"scopeName": "payment.credit"}""")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(scopeService);
    }

    @Test
    void createScopeRejectsNonPositiveTenantID() {
        assertThat(postScope("""
                {"tenantID": 0, "scopeName": "payment.credit"}""")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(scopeService);
    }

    @Test
    void createScopeRejectsBlankName() {
        assertThat(postScope("""
                {"tenantID": 1, "scopeName": ""}""")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(scopeService);
    }

    @Test
    void createScopeReturns409OnDuplicateName() {
        when(scopeService.createScope(any())).thenReturn(daoFailure(new DuplicateKeyException("unq__tenant_scope_name")));

        assertThat(postScope("""
                {"tenantID": 1, "scopeName": "payment.credit"}""")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void createScopeReturns400OnUnknownTenant() {
        when(scopeService.createScope(any())).thenReturn(daoFailure(new DataIntegrityViolationException("fk__scopes_tenants_id")));

        assertThat(postScope("""
                {"tenantID": 99, "scopeName": "payment.credit"}""")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getScopeByIDReturns200() {
        when(scopeService.getScopeByID(3L)).thenReturn(done(Optional.of(scope(3L, "payment.credit"))));

        MvcTestResult result = mvc.get().uri("/v1/scopes/3").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.scopeName").isEqualTo("payment.credit");
    }

    @Test
    void getScopeByIDReturns404WhenMissing() {
        when(scopeService.getScopeByID(9L)).thenReturn(done(Optional.empty()));

        MvcTestResult result = mvc.get().uri("/v1/scopes/9").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("scope 9 not found");
    }

    @Test
    void getScopesByTenantIDReturnsList() {
        when(scopeService.getScopesByTenantID(1L))
                .thenReturn(done(List.of(scope(3L, "payment.credit"), scope(4L, "payment.debit"))));

        MvcTestResult result = mvc.get().uri("/v1/scopes").param("tenantID", "1").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$[1].scopeName").isEqualTo("payment.debit");
    }

    @Test
    void getScopesByTenantIDReturnsEmptyList() {
        when(scopeService.getScopesByTenantID(1L)).thenReturn(done(List.of()));

        MvcTestResult result = mvc.get().uri("/v1/scopes").param("tenantID", "1").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().isEqualTo("[]");
    }

    @Test
    void getScopesRequiresTenantID() {
        assertThat(mvc.get().uri("/v1/scopes").exchange()).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(scopeService);
    }

    @Test
    void getScopesReturns500OnServiceFailure() {
        when(scopeService.getScopesByTenantID(1L)).thenReturn(daoFailure(new RuntimeException("db down")));

        assertThat(mvc.get().uri("/v1/scopes").param("tenantID", "1").exchange())
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
