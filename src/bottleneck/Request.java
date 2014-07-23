package bottleneck;

public class Request {
    public final String client;
    public final int weight;

    public Request(String client, int weight) {
        this.client = client;
        this.weight = weight;
    }
}
