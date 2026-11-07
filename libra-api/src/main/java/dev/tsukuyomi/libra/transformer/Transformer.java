package dev.tsukuyomi.libra.transformer;

public interface Transformer<T, U> {

    U transform(T t);

}
