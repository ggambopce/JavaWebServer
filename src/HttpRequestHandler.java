import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;
import static util.MyLogger.log;

/**
 * 클라이언트가 전달한 HTTP 요청을 처리하는 스레드
 * 동시에 욧청한 수 만큼 별도의 스레드에서 수행된다.
 */
public class HttpRequestHandler implements Runnable{
    private final Socket socket;

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
                plant1(writer);
            } else if (requestString.startsWith("GET /plant/2")) {
                plant2(writer);
            } else if (requestString.startsWith("GET /serach")) {
                search(writer);
            } else if (requestString.startsWith("GET / ")) {
                home(writer);
            } else {
                notFound(writer);
            }

            // 4. HTTP 응답 메세지 생성
            responseToClient(writer);
            log("HTTP 응답 전달 완료");
        }
    }

    private void home(PrintWriter writer) {
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: text/html; charset=UTF-8");
        writer.println();
        writer.println("<h1>나의 반려 식물 목록</h1>");
        writer.println("<ul>");
        writer.println("<li><a href='/plant/1>행복이</a></li>");
        writer.println("<li><a href='/plant/1>사랑이</a></li>");
        writer.println("<li><a href='/search?q=hello'>검색</a></li>");
        writer.println("</ul>");
        writer.flush();
    }
    private void plant1(PrintWriter writer) {
    }
    private void plant2(PrintWriter writer) {

    }
    private void search(PrintWriter writer) {
    }
    private void notFound(PrintWriter writer) {
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
