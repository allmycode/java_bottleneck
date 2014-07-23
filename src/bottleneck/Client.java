package bottleneck;

import com.db.fxplusoptions.service.fxoapi.BottleServer;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {

    public final String name;
    private final BottleServer<String, Request> s;
    private final int persec;
    private final int large;
    final Random r = new Random();

    private AtomicInteger proceed = new AtomicInteger(0);
    private AtomicInteger rejected = new AtomicInteger(0);
    private AtomicInteger sent = new AtomicInteger(0);

    public Client(String name, BottleServer<String, Request> s1, final int persec, final int timeout2) {
        this.name = name;
        this.s = s1;
        this.persec = persec;
        large = timeout2;

    }

    public void start() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        fire();
                        Thread.sleep(Main.TIME_SCALE);
                        if (sent.get() % 50 == 0) {
                            Thread.sleep(large);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void fire() {
        new Thread() {
            @Override
            public void run() {
                final int weight = persec;
                sent.incrementAndGet();
                boolean res = s.enque(new Request(name, weight));
                if (res)
                    proceed.incrementAndGet();
                else
                    rejected.incrementAndGet();
            }
        }.start();
    }

    @Override
    public String toString() {
        return name;
    }

    public void printStats() {
        System.out.println(name + "\t\t" + sent.get() + "\t"  + proceed.get() + "\t" + rejected.get() + "\t" + happiness());
    }

    private double happiness() {
        return sent.get() == 0 ? 1 : (double)proceed.get()/(proceed.get() + rejected.get());
    }
}
