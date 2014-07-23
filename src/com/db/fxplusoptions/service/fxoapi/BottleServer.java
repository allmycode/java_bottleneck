package com.db.fxplusoptions.service.fxoapi;

import com.hazelcast.logging.Logger;

import java.util.ArrayList;
import java.util.concurrent.*;

import static java.lang.String.format;

public abstract class BottleServer<I, E> {

    private final long maxQueueSize;
    private final int timeout;
    private final SlidingCounter<I> requests;
    private final PriorityBlockingQueue<QueuedRequest> pqueue = new PriorityBlockingQueue<QueuedRequest>();
    private final ExecutorService executor;
    private volatile boolean acceptRequests;

    public BottleServer(long maxQueueSize, int timeout, int countPeriod) {
        this.maxQueueSize = maxQueueSize;
        this.timeout = timeout;
        requests = new SlidingCounter<I>(countPeriod/10, 10);
        executor = Executors.newSingleThreadExecutor();
    }

    private static class FutureResult {
        final CountDownLatch latch = new CountDownLatch(1);
        volatile boolean result;

        public void set(boolean success) {
            result = success;
            latch.countDown();
        }
        public boolean get(int timeout) {
            try {
                return latch.await(timeout, TimeUnit.MILLISECONDS) && result;
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    private static class QueuedRequest<E> implements Comparable<QueuedRequest>{
        final int priority;
        final E request;
        final FutureResult result;

        private QueuedRequest(int priority, E request, FutureResult result) {
            this.priority = priority;
            this.request = request;
            this.result = result;
        }

        @Override
        public int compareTo(QueuedRequest o) {
            return priority - o.priority;
        }
    }

    public abstract I getIssuer(E request);

    public abstract int getWeight(E request);

    public abstract void process(E request) throws InterruptedException;

    public void start() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                acceptRequests = true;

                while (!Thread.interrupted()) {
                    try {
                        final QueuedRequest<E> queuedRequest = pqueue.take();
                        queuedRequest.result.set(true);
                        process(queuedRequest.request);
                        requests.inc(getIssuer(queuedRequest.request), getWeight(queuedRequest.request));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                acceptRequests = false;
                pqueue.clear();
            }
        });
    }

    public void stop() {
        executor.shutdownNow();
        acceptRequests = false;
    }

    public boolean enque(E request) {
        if (!acceptRequests) return false;
        return enqueInternal(request).get(timeout);
     }

    private synchronized FutureResult enqueInternal(E request) {
        FutureResult result = new FutureResult();
        pqueue.add(new QueuedRequest<E>(requests.get(getIssuer(request)), request, result));
        if (pqueue.size() > maxQueueSize) {
            int newSize = (int)(maxQueueSize * 2)/3;
            ArrayList<QueuedRequest> lst = new ArrayList<QueuedRequest>(newSize);
            pqueue.drainTo(lst, newSize);
            pqueue.clear();
            pqueue.addAll(lst);
        }
        return result;
    }

    public void log() {
        System.out.println(format("queue: %d", pqueue.size()));
        for (I c : requests.keySet()) {
            System.out.print(c + ": " + requests.get(c) + ", ");
        }
        System.out.println();
    }
}
