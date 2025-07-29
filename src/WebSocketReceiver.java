import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static util.MyLogger.log;

public class WebSocketReceiver implements Runnable {
    private final Socket socket;

    public WebSocketReceiver(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream()) {
            while (!socket.isClosed()) {
                int firstByte = in.read(); // FIN + Opcode
                if (firstByte == -1) break;

                int secondByte = in.read(); // MASK + payload length
                if (secondByte == -1) break;

                boolean masked = (secondByte & 0x80) != 0;
                int payloadLen = secondByte & 0x7F;

                if (payloadLen == 126) {
                    payloadLen = (in.read() << 8) | in.read();
                } else if (payloadLen == 127) {
                    for (int i = 0; i < 8; i++) in.read(); // 64bit는 생략
                    payloadLen = 0; // 실제 구현 시 처리 필요
                }

                byte[] mask = new byte[4];
                if (masked) in.read(mask);

                byte[] payload = new byte[payloadLen];
                int read = in.read(payload);
                if (read != payloadLen) break;

                // 언마스킹
                for (int i = 0; i < payloadLen; i++) {
                    payload[i] ^= mask[i % 4];
                }

                String message = new String(payload, StandardCharsets.UTF_8);
                log("💬 클라이언트 메시지 수신: " + message);

                // TODO: 메시지에 따라 처리 분기
            }
        } catch (Exception e) {
            log("📴 WebSocket 수신 중 예외 발생: " + e.getMessage());
        }
    }
}
