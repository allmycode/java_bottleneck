package com.db.fxplusoptions.service.fxoapi;

import java.util.HashSet;
import java.util.Set;

public class SlidingCounter<T> {
    final long windowSize;
    final int windowCount;
    final Window<T>[] windows;

    public SlidingCounter(long windowSize, int windowCount) {
        this.windowSize = windowSize;
        this.windowCount = windowCount;
        this.windows = new Window[windowCount];
        for (int i = 0; i < windows.length; i++)
            windows[i] = new Window<T>(0);
    }

    private Window getWindow(long time) {
        long real = (time /windowSize);
        int num = (int)(real % windowCount);
        Window<T> w = windows[num];
        if (w.timestamp < time - windowSize*windowCount) {
            w = windows[num] = new Window<T>(time);
        }
        return w;
    }

    public int inc(T t) {
        final long time = System.currentTimeMillis();
        getWindow(time).counter.inc(t);

        return get(t);
    }

    public int inc(T t, int incVal) {
        final long time = System.currentTimeMillis();
        getWindow(time).counter.inc(t, incVal);

        return get(t);
    }

    public int get(T t) {
        final long time = System.currentTimeMillis();
        int sum = 0;
        for (int i = 0; i < windowCount; i++) {
            if (windows[i].timestamp > time - windowSize*windowCount)
            sum += windows[i].counter.get(t);
        }
        return sum;
    }

    public Set<T> keySet() {
        Set<T> set = new HashSet<T>();
        for (int i = 0; i < windowCount; i++) {
            set.addAll(windows[i].counter.keySet());
        }
        return set;
    }

    private static class Window<T> {
        final long timestamp;
        final Counter<T> counter;

        private Window(long timestamp) {
            this.timestamp = timestamp;
            counter = new Counter<T>();
        }
    }
}
