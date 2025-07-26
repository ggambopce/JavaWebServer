import java.io.IOException;
import java.net.ServerSocket;

public class HttpServer {
    // <h1>Hello WebServer!!</h2>
    private final int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

    }
}
