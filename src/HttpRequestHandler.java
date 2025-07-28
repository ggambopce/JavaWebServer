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

    private void process() throws IOException {
        try(socket;
            // 1.소켓에서 들어오는 데이터를 문자열로 읽기위해 사용하는 스트림 구성
            BufferedReader reader = new BufferedReader( // 버퍼링 처리후 readLine()으로 라인단위로 읽어옴
                    new InputStreamReader(              // 입력스트림을 문자스트림으로 변환
                            socket.getInputStream(),    // 보내진 바이트데이터를 읽는 입력스트림
                            UTF_8
                    ));
            // 2.소캣 출력 스트림을 문자 기반으로 감싸서 출력하는 기능을 제공하는 객체 생성
            PrintWriter writer = new PrintWriter(
                    socket.getOutputStream(),       // Socket 객체에서 출력스트림을 가져옴
                    false,                          // autoFlush 여부, flush() 직접호출해서 모아서 전송
                    UTF_8                           // 텍스트를 인코딩해서 바이트 스트림에 전달
            )) {

            // 3. HTTP 요청 읽어서 String으로 반환
            String requestString = requestToString(reader);

            // favicon.ico 파비콘 요청 무시
            if (requestString.contains("/favicon.ico")) {
                log("favicon 요청");
                return;
            }

            // HTTP 요청 정보 확인
            log("HTTP 요청 정보 출력");
            System.out.println(requestString);

            log("Http 응답 생성중...");
            if (requestString.startsWith("GET /plant/1")) {
                respondPlantData(writer, 1, "행복이");
            } else if (requestString.startsWith("GET /plant/2")) {
                respondPlantData(writer, 2, "사랑이");
            } else if (requestString.startsWith("GET /plants")) {
                plantList(writer, requestString);
            } else if (requestString.startsWith("GET / ")) {
                home(writer);
            } else {
                notFound(writer);
            }

            log("HTTP 응답 전달 완료");
        }
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

    private void plantList(PrintWriter writer,String requestString ) {
        List<PlantData> dataList = repository.findAllLatest(); // 전체 장치 데이터
        StringBuilder html = new StringBuilder();
        html.append("<h1>전체 식물 실시간 센서 데이터</h1>");
        html.append("<table border='1'>");
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
