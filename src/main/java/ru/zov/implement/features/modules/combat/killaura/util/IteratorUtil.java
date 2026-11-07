package ru.zov.implement.features.modules.combat.killaura.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IteratorUtil {
    public static <T> List<T> toList(Iterator<? extends T> it) {
        if (it == null) {
            throw new NullPointerException("Iterator must not be null");
        }
        ArrayList<T> arrayList = new ArrayList<>(256);
        while (it.hasNext()) {
            arrayList.add(it.next());
        }
        return arrayList;
    }

    @SuppressWarnings("unchecked")
    public static <T> Stream<T> stream(Iterable<? extends T> source) {
        if (source == null) {
            throw new NullPointerException("Iterable must not be null");
        }
        return StreamSupport.stream(((Iterable<T>) source).spliterator(), false);
    }
}
