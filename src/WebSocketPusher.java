import plantApplication.PlantData;
import plantApplication.PlantDataRepository;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static util.MyLogger.log;

public class WebSocketPusher {
    private final Socket socket;
    private final OutputStream out;
    private final PlantDataRepository repo = new PlantDataRepository();

    public WebSocketPusher(Socket socket, OutputStream out) {
        this.socket = socket;
        this.out = out;
    }

    public void send() {
        try {
            log("✅ send() 진입");
            if (!socket.isConnected() || socket.isClosed()) {
                log("⛔ 소켓 연결 안됨 또는 닫힘");
                return;
            }

            List<PlantData> dataList = repo.findAllLatest();
            log("📦 데이터 개수: " + dataList.size());

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < dataList.size(); i++) {
                PlantData d = dataList.get(i);
                json.append("{")
                        .append("\"deviceId\":").append(d.getDeviceId()).append(",")
                        .append("\"temp\":").append(d.getTemperature()).append(",")
                        .append("\"hum\":").append(d.getHumidity()).append(",")
                        .append("\"timestamp\":\"").append(d.getCreatedAt()).append("\"")
                        .append("}");
                if (i < dataList.size() - 1) json.append(",");
            }
            json.append("]");

            String message = json.toString();
            log("📤 WebSocket 전송 데이터: " + message);

            sendMessage(json.toString());

        } catch (Exception e) {
            log("WebSocket 푸시 예외 발생: " + e.getMessage());
        }
    }

    private void sendMessage(String message) throws Exception {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        int length = payload.length;

        out.write(0x81); // FIN + Text Frame

        if (length <= 125) {
            out.write(length);
        } else if (length <= 65535) {
            out.write(126);
            out.write((length >> 8) & 0xFF);
            out.write(length & 0xFF);
        } else {
            out.write(127);
            for (int i = 7; i >= 0; i--) {
                out.write((length >> (8 * i)) & 0xFF);
            }
        }

        out.write(payload);
        out.flush();
    }

}
