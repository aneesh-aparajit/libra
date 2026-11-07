package dev.tsukuyomi.libra.dao.base;

import dev.tsukuyomi.libra.exeception.LibraDAOException;

import java.util.List;
import java.util.Objects;

public class BaseDAO {

    protected <T> T filterQueryObjects(List<T> objects) {
        if (Objects.isNull(objects) || objects.isEmpty()) {
            throw new LibraDAOException("objects is null or empty");
        }
        if (objects.size() == 1) {
            return objects.getFirst();
        }
        throw new LibraDAOException("incorrect size of result: expected 1, actual: " + objects.size());
    }

}
