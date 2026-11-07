package dev.tsukuyomi.libra.controller;

import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.service.tenant.ITenantService;
import dev.tsukuyomi.libra.transformer.tenant.TenantRequestTransformer;
import dev.tsukuyomi.libra.transformer.tenant.TenantResponseTransformer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static dev.tsukuyomi.libra.controller.Futures.daoFailure;
import static dev.tsukuyomi.libra.controller.Futures.done;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(TenantController.class)
@Import({TenantRequestTransformer.class, TenantResponseTransformer.class})
class TenantControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ITenantService tenantService;

    private static Tenant tenant(long id) {
        return Tenant.builder().id(id).tenantCode("acme").tenantName("Acme Corp").createdAt(100L).updatedAt(200L).build();
    }

    private MvcTestResult postTenant(String body) {
        return mvc.post().uri("/v1/tenants").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    void createTenantReturns201WithLocationAndBody() {
        when(tenantService.createTenant(any())).thenReturn(done(tenant(7L)));

        MvcTestResult result = postTenant("""
                {"tenantCode": "  acme ", "tenantName": " Acme Corp "}""");

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).headers().hasValue("Location", "/v1/tenants/7");
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(7);
        assertThat(result).bodyJson().extractingPath("$.tenantCode").isEqualTo("acme");
        assertThat(result).bodyJson().extractingPath("$.tenantName").isEqualTo("Acme Corp");
        assertThat(result).bodyJson().extractingPath("$.createdAt").isEqualTo(100);

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantService).createTenant(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getTenantCode()).isEqualTo("acme");
        assertThat(captor.getValue().getTenantName()).isEqualTo("Acme Corp");
    }

    @Test
    void createTenantRejectsBlankCode() {
        assertThat(postTenant("""
                {"tenantCode": " ", "tenantName": "Acme"}""")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(tenantService);
    }

    @Test
    void createTenantRejectsMissingName() {
        assertThat(postTenant("""
                {"tenantCode": "acme"}""")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(tenantService);
    }

    @Test
    void createTenantRejectsTooLongCode() {
        assertThat(postTenant("{\"tenantCode\": \"" + "a".repeat(256) + "\", \"tenantName\": \"Acme\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(tenantService);
    }

    @Test
    void createTenantRejectsMalformedJson() {
        assertThat(postTenant("{\"tenantCode\": ")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(tenantService);
    }

    @Test
    void createTenantReturns409OnDuplicate() {
        when(tenantService.createTenant(any())).thenReturn(daoFailure(new DuplicateKeyException("dup")));

        MvcTestResult result = postTenant("""
                {"tenantCode": "acme", "tenantName": "Acme"}""");

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("resource already exists");
    }

    @Test
    void createTenantReturns500WithoutLeakingInternals() {
        when(tenantService.createTenant(any())).thenReturn(daoFailure(new RuntimeException("connection refused")));

        MvcTestResult result = postTenant("""
                {"tenantCode": "acme", "tenantName": "Acme"}""");

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("internal server error");
    }

    @Test
    void getTenantByIDReturns200() {
        when(tenantService.getTenantByID(7L)).thenReturn(done(Optional.of(tenant(7L))));

        MvcTestResult result = mvc.get().uri("/v1/tenants/7").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.tenantCode").isEqualTo("acme");
    }

    @Test
    void getTenantByIDReturns404WhenMissing() {
        when(tenantService.getTenantByID(9L)).thenReturn(done(Optional.empty()));

        MvcTestResult result = mvc.get().uri("/v1/tenants/9").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("tenant 9 not found");
    }

    @Test
    void getTenantByIDRejectsNonNumericID() {
        assertThat(mvc.get().uri("/v1/tenants/abc").exchange()).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(tenantService);
    }

    @Test
    void getTenantByCodeReturns200() {
        when(tenantService.getTenantByCode("acme")).thenReturn(done(Optional.of(tenant(7L))));

        MvcTestResult result = mvc.get().uri("/v1/tenants").param("code", "acme").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(7);
    }

    @Test
    void getTenantByCodeReturns404WhenMissing() {
        when(tenantService.getTenantByCode("nope")).thenReturn(done(Optional.empty()));

        assertThat(mvc.get().uri("/v1/tenants").param("code", "nope").exchange()).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void getTenantByCodeRequiresCode() {
        assertThat(mvc.get().uri("/v1/tenants").exchange()).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(tenantService);
    }

    @Test
    void getTenantByCodeRejectsBlankCode() {
        assertThat(mvc.get().uri("/v1/tenants").param("code", " ").exchange()).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(tenantService);
    }

    @Test
    void getTenantByIDReturns500OnServiceFailure() {
        when(tenantService.getTenantByID(1L)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("boom")));

        assertThat(mvc.get().uri("/v1/tenants/1").exchange()).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
