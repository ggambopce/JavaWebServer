import java.io.IOException;

public class ServerMain {

    public static void main(String[] args) throws IOException {
        HttpServer server = new HttpServer();
        server.start();

    }
}
