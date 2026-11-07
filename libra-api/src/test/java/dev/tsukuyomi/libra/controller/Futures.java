package dev.tsukuyomi.libra.controller;

import dev.tsukuyomi.libra.exeception.LibraDAOException;
import dev.tsukuyomi.libra.exeception.LibraServiceException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Test helpers producing futures shaped like the ones the service layer returns.
 */
final class Futures {

    private Futures() {
    }

    static <T> CompletableFuture<T> done(T value) {
        return CompletableFuture.completedFuture(value);
    }

    /**
     * Mirrors a DAO failure surfacing through a service: the DAO wraps the root cause, supplyAsync wraps
     * that in a CompletionException, and the service's exceptionally() rethrows a LibraServiceException.
     */
    static <T> CompletableFuture<T> daoFailure(Throwable rootCause) {
        LibraServiceException serviceEx = new LibraServiceException("service failure",
                new CompletionException(new LibraDAOException("dao failure", rootCause)));
        return CompletableFuture.failedFuture(new CompletionException(serviceEx));
    }

}
