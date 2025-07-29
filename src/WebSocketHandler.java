import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static util.MyLogger.log;

public class WebSocketHandler {

    public static void handleHandshakeAndData(Socket socket, BufferedReader reader, OutputStream out) {
        try {
            log("🌐 [WebSocketHandler] 요청 수신: " + socket.getRemoteSocketAddress());

            String line;
            String webSocketKey = null;
            StringBuilder headers = new StringBuilder();

            // 1. 요청 헤더 읽기
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                log("👉 읽은 라인: " + line); // 각각의 헤더 라인 출력
                headers.append(line).append("\n");

                // 대소문자 구분 없이 key 추출
                if (line.toLowerCase().startsWith("sec-websocket-key:")) {
                    webSocketKey = line.split(":", 2)[1].trim();
                    log("🔑 WebSocket Key 추출: " + webSocketKey);
                }
            }

            log("📥 전체 요청 헤더:\n" + headers);

            if (webSocketKey == null || webSocketKey.isEmpty()) {
                log("❌ WebSocket Key 누락 또는 비어있음 - 핸드셰이크 실패");
                socket.close();
                return;
            }

            // 2. Accept Key 생성
            String acceptKey = generateAcceptKey(webSocketKey);
            log("🔐 생성된 Sec-WebSocket-Accept: " + acceptKey);

            // 3. 핸드셰이크 응답 전송
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
            log("✅ WebSocket 핸드셰이크 응답 전송 완료");

            // 4. WebSocket 푸시 스케줄러 실행
            log("WebSocketPusher 스케쥴러 시작");
            WebSocketPusher pusher = new WebSocketPusher(socket, out);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                if (!socket.isClosed()) {
                    pusher.send();
                } else {
                    scheduler.shutdown();
                }
            }, 0, 5, TimeUnit.SECONDS);

            // WebSocketReceiver 추가
            log("📡 WebSocketReceiver 수신 스레드 시작");
            new Thread(new WebSocketReceiver(socket)).start();

        } catch (Exception e) {
            log("WebSocket 핸드셰이크 예외 발생: " + e.getMessage());
            try {
                socket.close();
                log("🔌 소켓 닫힘");
            } catch (IOException ignored) {}
        }
    }

    private static String generateAcceptKey(String key) throws Exception {
        String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        log("🧪 Accept Key 생성용 문자열: " + magic);

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(magic.getBytes(StandardCharsets.UTF_8));

        String encoded = Base64.getEncoder().encodeToString(hash);
        log("📦 SHA-1 해시 Base64 인코딩 결과: " + encoded);
        return encoded;
    }
}
