package dev.tsukuyomi.libra.controller;

import dev.tsukuyomi.libra.dto.request.CreateTenantRequest;
import dev.tsukuyomi.libra.dto.response.TenantResponse;
import dev.tsukuyomi.libra.exception.ApiException;
import dev.tsukuyomi.libra.service.tenant.ITenantService;
import dev.tsukuyomi.libra.transformer.tenant.TenantRequestTransformer;
import dev.tsukuyomi.libra.transformer.tenant.TenantResponseTransformer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final ITenantService tenantService;
    private final TenantRequestTransformer requestTransformer;
    private final TenantResponseTransformer responseTransformer;

    @PostMapping
    public CompletableFuture<ResponseEntity<TenantResponse>> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.createTenant(requestTransformer.transform(request))
                .thenApply(tenant -> ResponseEntity
                        .created(URI.create("/v1/tenants/" + tenant.getId()))
                        .body(responseTransformer.transform(tenant)));
    }

    @GetMapping("/{id}")
    public CompletableFuture<TenantResponse> getTenantByID(@PathVariable Long id) {
        return tenantService.getTenantByID(id)
                .thenApply(tenant -> tenant
                        .map(responseTransformer::transform)
                        .orElseThrow(() -> ApiException.notFound("tenant " + id + " not found")));
    }

    @GetMapping
    public CompletableFuture<TenantResponse> getTenantByCode(@RequestParam @NotBlank String code) {
        return tenantService.getTenantByCode(code)
                .thenApply(tenant -> tenant
                        .map(responseTransformer::transform)
                        .orElseThrow(() -> ApiException.notFound("tenant with code '" + code + "' not found")));
    }

}
