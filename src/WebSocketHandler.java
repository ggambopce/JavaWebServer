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

            // ✅ 타임아웃 해제: 읽기 무제한 대기
            socket.setSoTimeout(0);

            // WebSocket 서비스 루프: 스레드 유지
            startWebSocketLoop(socket);

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



    private static void startWebSocketLoop(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            WebSocketPusher pusher = new WebSocketPusher(socket, out);

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

            // 1. 수신 스레드: 클라이언트로부터 메시지 수신 및 ping/pong 처리
            scheduler.submit(() -> {
                try {
                    while (!socket.isClosed()) {
                        int b1 = in.read();
                        if (b1 == -1) break;

                        int b2 = in.read();
                        if (b2 == -1) break;

                        int payloadLen = b2 & 0x7F;
                        if (payloadLen == 126) {
                            payloadLen = (in.read() << 8) | in.read();
                        } else if (payloadLen == 127) {
                            payloadLen = 0;
                            for (int i = 0; i < 8; i++) {
                                payloadLen = (payloadLen << 8) | in.read();
                            }
                        }

                        byte[] payload = new byte[payloadLen];
                        in.read(payload);

                        int opcode = b1 & 0x0F;

                        switch (opcode) {
                            case 0x1: // 텍스트 메시지
                                String msg = new String(payload, StandardCharsets.UTF_8);
                                log("💬 클라이언트 메시지: " + msg);
                                break;

                            case 0x8: // close
                                log("❗ 클라이언트 연결 종료 요청");
                                socket.close();
                                return;

                            case 0x9: // ping
                                log("📡 ping 수신 → pong 응답");
                                sendPong(out, payload);
                                break;

                            case 0xA: // pong
                                log("📶 pong 수신");
                                break;

                            default:
                                log("⚠️ 알 수 없는 opcode: " + opcode);
                        }
                    }
                } catch (IOException e) {
                    log("🔻 수신 스레드 오류: " + e.getMessage());
                }
            });

            // 2. 주기적 전송 + ping 프레임 전송
            scheduler.scheduleAtFixedRate(() -> {
                if (socket.isClosed()) {
                    scheduler.shutdown();
                    return;
                }

                try {
                    log("📡 센서 데이터 전송 시작");
                    pusher.send();

                    log("🔁 ping 프레임 전송");
                    sendPing(out);

                } catch (Exception e) {
                    log("WebSocket 전송 오류: " + e.getMessage());
                    try { socket.close(); } catch (IOException ignored) {}
                    scheduler.shutdown();
                }
            }, 0, 5, TimeUnit.SECONDS);

        } catch (IOException e) {
            log("WebSocket 루프 초기화 실패: " + e.getMessage());
        }
    }

    private static void sendPing(OutputStream out) throws IOException {
        out.write(0x89); // FIN + opcode 0x9 (ping)
        out.write(0x00); // payload length = 0
        out.flush();
    }

    private static void sendPong(OutputStream out, byte[] payload) throws IOException {
        out.write(0x8A); // FIN + opcode 0xA (pong)
        out.write(payload.length);
        out.write(payload);
        out.flush();
    }

    // JSON을 WebSocket 프레임으로 인코딩 (텍스트 메시지)
    private static byte[] encodeFrame(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        int length = data.length;
        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        frame.write(0x81); // FIN + 텍스트 프레임
        if (length <= 125) {
            frame.write(length);
        } else if (length <= 65535) {
            frame.write(126);
            frame.write((length >> 8) & 0xFF);
            frame.write(length & 0xFF);
        } else {
            frame.write(127);
            for (int i = 7; i >= 0; i--) {
                frame.write((length >> (8 * i)) & 0xFF);
            }
        }
        frame.write(data, 0, length);
        return frame.toByteArray();
    }

    private static String generateAcceptKey(String key) throws Exception {
        String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(magic.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

}
