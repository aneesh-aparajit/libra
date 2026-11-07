package dev.tsukuyomi.libra.controller;

import dev.tsukuyomi.libra.dto.request.CreateScopeRequest;
import dev.tsukuyomi.libra.dto.response.ScopeResponse;
import dev.tsukuyomi.libra.exception.ApiException;
import dev.tsukuyomi.libra.service.scope.IScopeService;
import dev.tsukuyomi.libra.transformer.scope.ScopeRequestTransformer;
import dev.tsukuyomi.libra.transformer.scope.ScopeResponseTransformer;
import jakarta.validation.Valid;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/scopes")
@RequiredArgsConstructor
public class ScopeController {

    private final IScopeService scopeService;
    private final ScopeRequestTransformer requestTransformer;
    private final ScopeResponseTransformer responseTransformer;

    @PostMapping
    public CompletableFuture<ResponseEntity<ScopeResponse>> createScope(@Valid @RequestBody CreateScopeRequest request) {
        return scopeService.createScope(requestTransformer.transform(request))
                .thenApply(scope -> ResponseEntity
                        .created(URI.create("/v1/scopes/" + scope.getId()))
                        .body(responseTransformer.transform(scope)));
    }

    @GetMapping("/{id}")
    public CompletableFuture<ScopeResponse> getScopeByID(@PathVariable Long id) {
        return scopeService.getScopeByID(id)
                .thenApply(scope -> scope
                        .map(responseTransformer::transform)
                        .orElseThrow(() -> ApiException.notFound("scope " + id + " not found")));
    }

    @GetMapping
    public CompletableFuture<List<ScopeResponse>> getScopesByTenantID(@RequestParam Long tenantID) {
        return scopeService.getScopesByTenantID(tenantID)
                .thenApply(scopes -> scopes.stream().map(responseTransformer::transform).toList());
    }

}
