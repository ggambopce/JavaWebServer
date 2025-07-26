import java.io.IOException;

public class ServerMain {

    private static final int PORT = 12345;

    public static void main(String[] args) throws IOException {
        HttpServer server = new HttpServer(PORT);
        server.start();

    }
}
