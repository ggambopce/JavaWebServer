import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static util.MyLogger.log;

public class HttpServer {

    // 고정 크기 스레드풀 사용 20개 동시요청 처리
    private final ExecutorService es = Executors.newFixedThreadPool(20);
    private final int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {

        // 서버 소켓 생성, 지정된 포트에서 클라이언트 연결 대기
        ServerSocket serverSocket = new ServerSocket(port);
        log("서버 시작 port: " + port);

        // 클라이언트 연결요청 계속 대기
        while (true) {
            Socket clientSocket = serverSocket.accept(); // 연결 대기 블로킹, 클라이언트가 연결되면 Socket객체 반환

            // 클라이언트 요청을 처리할 핸들러 객체 생성
            HttpRequestHandler handler = new HttpRequestHandler(clientSocket);
            /**
             * Runnable 구현체
             * 새 스레드를 만드는게 아니라 기존에 만들어둔 스레드를 재사용
             * 스레드가 모두 사용중이라면 대기큐에 쌓이고 순차적으로 실행됨
             */
            es.submit(handler); // 반환값이 있어서 실행결과 확인 가능
        }
    }
}
