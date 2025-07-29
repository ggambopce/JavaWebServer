import com.mysql.cj.log.Log;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.in;
import static util.MyLogger.log;

public class WebSocketHandler {

    public static void handle(Socket socket,String requestLine, String headers) {
        try {
            if (!performHandshake(socket, headers)) {
                log("❌ WebSocket 핸드셰이크 실패");
                socket.close();
                return;
            }

            log("✅ WebSocket 연결 및 핸드셰이크 완료");


            // WebSocket 서비스 루프: 스레드 유지
            startWebSocketPushLoop(socket);

        } catch (Exception e) {
            log("WebSocket 처리 중 예외 발생: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private static boolean performHandshake(Socket socket, String headers) throws Exception {
        String webSocketKey = null;

        for (String line : headers.split("\\r?\\n")) {
            if (line.toLowerCase().startsWith("sec-websocket-key:")) {
                webSocketKey = line.split(":", 2)[1].trim();
                break;
            }
        }

        if (webSocketKey == null) {
            log("❌ Sec-WebSocket-Key 없음");
            return false;
        }

        String acceptKey = generateAcceptKey(webSocketKey);
        String response = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

        OutputStream out = socket.getOutputStream();
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
        return true;
    }



    private static void startWebSocketPushLoop(Socket socket) {
        log("loop 진입");
        try {
            OutputStream out = socket.getOutputStream();
            WebSocketPusher pusher = new WebSocketPusher(socket, out);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            scheduler.scheduleAtFixedRate(() -> {
                log("🕒 스케줄러 진입");

                if (socket.isClosed()) {
                    log("⚠️ 소켓이 닫혀 있음 → 스케줄러 종료");
                    scheduler.shutdown();
                    return;
                }

                try {
                    // 1. 센서 데이터 전송
                    log("📡 센서 데이터 전송 시작");
                    pusher.send();

                    // 2. ping 프레임 전송
                    log("📶 ping 전송");
                    out.write(0x89); // opcode 0x9 (ping), FIN=1
                    out.write(0x00); // payload length = 0
                    out.flush();

                } catch (Exception e) {
                    log("❌ WebSocket 전송 오류: " + e.getMessage());
                    try {
                        socket.close();
                    } catch (IOException ignored) {}
                    scheduler.shutdown();
                }
            }, 0, 5, TimeUnit.SECONDS);

        } catch (IOException e) {
            log("❌ WebSocket 전송 루프 초기화 실패: " + e.getMessage());
        }
    }

    private static String generateAcceptKey(String key) throws Exception {
        String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(magic.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

}
