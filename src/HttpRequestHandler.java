import plantApplication.Plant;
import plantApplication.PlantData;
import plantApplication.PlantDataRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static util.MyLogger.log;

/**
 * 클라이언트가 전달한 HTTP 요청을 처리하는 스레드
 * 동시에 욧청한 수 만큼 별도의 스레드에서 수행된다.
 */
public class HttpRequestHandler implements Runnable{
    private final Socket socket;
    private final PlantDataRepository repository = new PlantDataRepository();

    public HttpRequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            process();
        } catch (Exception e) {
            log(e);
        }
    }

    private void process() throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), UTF_8));

        PrintWriter writer = new PrintWriter(
                socket.getOutputStream(), false, UTF_8);

        String requestLine = reader.readLine();
        if (requestLine == null) return;

        String headers = requestToString(reader);
        log("\uD83D\uDCE5 [요청 전체 수신됨]");
        log("\uD83D\uDD0E HTTP 요청 정보 출력:\n" + requestLine + "\n" + headers);

        if (requestLine.startsWith("GET /ws") && headers.toLowerCase().contains("upgrade: websocket")) {
            log("\uD83D\uDD0C WebSocket 연결 요청 수신: " + requestLine);
            new Thread(() -> WebSocketHandler.handle(socket, requestLine, headers)).start();
            return;
        }

        if (requestLine.contains("/favicon.ico")) {
            log("\uD83C\uDF1F favicon.ico 요청 감지됨");
            writer.println("HTTP/1.1 204 No Content");
            writer.println("Content-Type: image/x-icon");
            writer.println("Connection: close");
            writer.println();
            writer.flush();
            return;
        }

        log("HTTP 요청 정보 출력");
        System.out.println(requestLine);

        log("Http 응답 생성중...");
        if (requestLine.startsWith("GET /plant/1")) {
            respondPlantData(writer, 1, "행복이");
        } else if (requestLine.startsWith("GET /plant/2")) {
            respondPlantData(writer, 2, "사랑이");
        } else if (requestLine.startsWith("GET /plants")) {
            plantList(writer);
        } else if (requestLine.startsWith("GET /")) {
            home(writer);
        } else {
            notFound(writer);
        }

        log("HTTP 응답 전달 완료");
    }
    private void respondPlantData(PrintWriter writer, int plantId, String name) {

        PlantData data = repository.findLatestByPlantId(plantId);

        StringBuilder html = new StringBuilder("<h1>" + name + "</h1>");
        if (data != null) {
            html.append("<p>온도: ").append(data.getTemperature()).append("℃</p>")
                    .append("<p>습도: ").append(data.getHumidity()).append("%</p>")
                    .append("<p>시각: ").append(data.getCreatedAt()).append("</p>");
        } else {
            html.append("<p>측정 데이터가 없습니다.</p>");
        }

        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: text/html; charset=UTF-8");
        writer.println();
        writer.println(html);
        writer.flush();
    }

    private void home(PrintWriter writer) {
        // 원칙적으로 Content-Length 계산해야 한다.
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: text/html; charset=UTF-8");
        writer.println();
        writer.println("<h1>나의 반려 식물 목록</h1>");
        writer.println("<ul>");
        writer.println("<li><a href='/plant/1'>행복이</a></li>");
        writer.println("<li><a href='/plant/2'>사랑이</a></li>");
        writer.println("<li><a href='/plants'>리스트</a></li>");
        writer.println("</ul>");
        writer.flush();
    }

    private void notFound(PrintWriter writer) {
        writer.println("HTTP/1.1 404 Not Found");
        writer.println("Content-Type: text/html; charset=UTF-8");
        writer.println();
        writer.println("<h1>404 페이지를 찾을 수 없습니다.</h1>");
        writer.flush();
    }

    private void plantList(PrintWriter writer) {
        List<PlantData> dataList = repository.findAllLatest(); // 전체 장치 데이터
        StringBuilder html = new StringBuilder();
        html.append("<h1>전체 식물 실시간 센서 데이터</h1>");
        html.append("<table border='1' id='data-table'>");
        html.append("<tr><th>장치 ID</th><th>온도(℃)</th><th>습도(%)</th><th>측정시각</th></tr>");

        for (PlantData data : dataList) {
            html.append("<tr>")
                    .append("<td>").append(data.getDeviceId()).append("</td>")
                    .append("<td>").append(data.getTemperature()).append("</td>")
                    .append("<td>").append(data.getHumidity()).append("</td>")
                    .append("<td>").append(data.getCreatedAt()).append("</td>")
                    .append("</tr>");
        }

        html.append("</table>");

        // WebSocket JS 코드 삽입
        html.append("<script>");
        html.append("const socket = new WebSocket('ws://localhost:12345/ws');");

        html.append("socket.onopen = () => {");
        html.append("  console.log('WebSocket 연결됨');");
        html.append("};");

        html.append("socket.onmessage = (event) => {");
        html.append("  try {");
        html.append("    const data = JSON.parse(event.data);");
        html.append("    const table = document.getElementById('data-table');");
        html.append("    table.innerHTML = '<tr><th>장치 ID</th><th>온도(℃)</th><th>습도(%)</th><th>측정시각</th></tr>';");

        html.append("    data.forEach(row => {");
        html.append("      const tr = document.createElement('tr');");
        html.append("      tr.innerHTML = `<td>${row.deviceId}</td><td>${row.temp}</td><td>${row.hum}</td><td>${row.timestamp}</td>`;");
        html.append("      table.appendChild(tr);");
        html.append("    });");
        html.append("  } catch(e) {");
        html.append("    console.warn('JSON 아님:', event.data);");
        html.append("  }");
        html.append("};");

        html.append("socket.onerror = (e) => console.error('WebSocket error:', e);");
        html.append("socket.onclose = () => console.log('WebSocket closed');");

        html.append("</script>");



        html.append("</body>");
        html.append("</html>");

        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: text/html; charset=UTF-8");
        writer.println();
        writer.println(html);
        writer.flush();
    }

    /* HTTP 요청 양식 예시
    GET /index.html HTTP/1.1
    Host: localhost:8080
    User-Agent: curl/7.68.0
    Accept: *
    <빈 줄>
     */
    private static String requestToString(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null){
            if (line.isEmpty()) {
                break;
            }
            sb.append(line).append("\n");           // 헤더의 끝
        }
        return sb.toString();
    }

    /* HTTP 응답 양식 예시
    HTTP/1.1 200 OK\r\n
    Content-Type: text/html
    Content-Length: <바이트 수>\r\n
    \r\n
    <본문>
    */
    private void responseToClient(PrintWriter writer) {
        // 웹 브라우저에 전달하는 내용
        String body = "<h1>Hello HttpServer</h1>";
        int length = body.getBytes(UTF_8).length;   // 브라우저가 본문이 어디까지인지 확인하기 위함

        // Http 공식 양식
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n");           // 응답 상태 라인
        sb.append("Content-Type: text/html\r\n");   // 본문 형식
        sb.append("Content-Length: ").append(length).append("\r\n");    // 본문 길이
        sb.append("\r\n");                          //header, body 구분 라인
        sb.append(body);

        log("HTTP 응답 정보 출력");
        System.out.println(sb);

        writer.println(sb);
        writer.flush();                 // 버퍼에 저장된 데이터를 강제로 출력스트림으로 밀어냄
    }

    private static void sleep(int millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
}
