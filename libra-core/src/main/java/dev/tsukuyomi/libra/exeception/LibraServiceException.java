package dev.tsukuyomi.libra.exeception;

public class LibraServiceException extends RuntimeException {

    public LibraServiceException(String message) {
        super(message);
    }

    public LibraServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
