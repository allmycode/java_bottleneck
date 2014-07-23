package bottleneck;

import com.db.fxplusoptions.service.fxoapi.BottleServer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static int TIME_SCALE = 1000;
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final CrushingServer<Request> cs = new CrushingServer<Request>(1 * TIME_SCALE);
        final BottleServer<String, Request> qs = new BottleServer<String, Request>(
                500,
                500*TIME_SCALE,
                2*TIME_SCALE) {
            @Override
            public String getIssuer(Request request) {
                return request.client;
            }

            @Override
            public int getWeight(Request request) {
                return request.weight;
            }

            @Override
            public void process(Request request) throws InterruptedException {
                cs.price(request);
            }
        };
        qs.start();

        final Client[] clients = new Client[] {
            new Client("frequent1  ", qs,  1, 0),
            new Client("frequent30 ", qs,  30, 50*TIME_SCALE),
        };

        for (int i = 0; i < clients.length; i++)
            clients[i].start();

        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            @Override
            public void run() {
                qs.stop();
                System.exit(1);
            }
        }, 60, TimeUnit.SECONDS);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                    System.out.println("----------------");
                    qs.log();

                    for (int i = 0; i < clients.length; i++) clients[i].printStats();
                    }
                },
                1,
                1,
                TimeUnit.SECONDS);



    }
}
