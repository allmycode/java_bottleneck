package com.db.fxplusoptions.service.fxoapi;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Counter<T> {
    ConcurrentMap<T, AtomicInteger> count = new ConcurrentHashMap<T, AtomicInteger>();

    public int inc(T t) {
        AtomicInteger l = count.get(t);
        if (l == null) {
            AtomicInteger val = new AtomicInteger(0);
            l = count.put(t, val);
            if (l == null) {
                l = val;
            }
        }
        return l.incrementAndGet();
    }

    public int inc(T t, int incVal) {
        AtomicInteger l = count.get(t);
        if (l == null) {
            AtomicInteger val = new AtomicInteger(0);
            l = count.put(t, val);
            if (l == null) {
                l = val;
            }
        }
        return l.addAndGet(incVal);
    }

    public int get(T t) {
        AtomicInteger l = count.get(t);
        if (l == null) {
            return 0;
        }
        return l.get();
    }

    public Set<T> keySet() {
        return count.keySet();
    }
}
