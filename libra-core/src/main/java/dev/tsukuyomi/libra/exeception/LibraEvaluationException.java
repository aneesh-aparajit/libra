package dev.tsukuyomi.libra.exeception;

public class LibraEvaluationException extends RuntimeException {

    public LibraEvaluationException(String message) {
        super(message);
    }

    public LibraEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

}
