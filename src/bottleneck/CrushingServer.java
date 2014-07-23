package bottleneck;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrushingServer<E>{

    final int serveTime;
    Lock lock = new ReentrantLock();
    public CrushingServer(int serveTime) {
        this.serveTime = serveTime;
    }

    Executor e = Executors.newSingleThreadExecutor();

    public void price(final E req) throws InterruptedException {
        if (lock.tryLock())  {
            lock.unlock();
        } else {
            System.err.println("Server crushed.");
            System.exit(1);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        e.execute(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    Thread.sleep(serveTime);
                } catch (InterruptedException e1) { e1.printStackTrace(); }
                finally {
                    lock.unlock();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e1) { throw e1; }

    }
}
