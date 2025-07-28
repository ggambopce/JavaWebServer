import plantApplication.PlantData;
import plantApplication.PlantDataRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static util.MyLogger.log;

public class WebSocketHandler {

    public static void handleHandshakeAndData(Socket socket, BufferedReader reader, OutputStream out) throws Exception {
        String line;
        String webSocketKey = null;

        while (!(line = reader.readLine()).isEmpty()) {
            if (line.startsWith("Sec-WebSocket-Key:")) {
                webSocketKey = line.split(":")[1].trim();
            }
        }

        String acceptKey = generateAcceptKey(webSocketKey);
        String response = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();

        handleMessages(socket.getInputStream(), out); // 👈 프레임 통신 시작
    }

    private static String generateAcceptKey(String key) throws Exception {
        String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(magic.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private static void handleMessages(InputStream in, OutputStream out) throws IOException {
        while (true) {
            // WebSocket 메시지 프레임 파싱
            int b1 = in.read();
            int b2 = in.read();

            boolean isFinal = (b1 & 0x80) != 0;
            int opcode = b1 & 0x0F;
            int length = b2 & 0x7F;

            byte[] mask = new byte[4];
            in.read(mask);
            byte[] payload = new byte[length];
            in.read(payload);

            for (int i = 0; i < length; i++) {
                payload[i] ^= mask[i % 4];
            }

            String received = new String(payload, StandardCharsets.UTF_8);
            log("수신 메시지: " + received);

            // 실시간 데이터 응답
            PlantData data = new PlantDataRepository().findLatestByPlantId(1);
            String json = "{\"temp\":" + data.getTemperature() + ",\"hum\":" + data.getHumidity() + "}";

            sendMessage(out, json);
        }
    }

    private static void sendMessage(OutputStream out, String message) throws IOException {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        out.write(0x81); // FIN + Text frame
        out.write(payload.length);
        out.write(payload);
        out.flush();
    }
}
