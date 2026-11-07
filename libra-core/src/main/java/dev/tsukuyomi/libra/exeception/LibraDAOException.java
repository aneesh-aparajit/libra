package dev.tsukuyomi.libra.exeception;

public class LibraDAOException extends RuntimeException {

    public LibraDAOException(String msg) {
        super(msg);
    }

    public LibraDAOException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
