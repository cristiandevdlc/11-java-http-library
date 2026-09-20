import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LibraryServer {
    static final List<Book> books = new ArrayList<>(List.of(
        new Book(1, "Clean Code", true), new Book(2, "Designing Data-Intensive Applications", true)));
    record Book(int id, String title, boolean available) {}

    static void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8090), 0);
        server.createContext("/books", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(exchange.getRequestMethod()) && "/books".equals(path)) {
                String json = books.stream().map(b -> String.format("{\"id\":%d,\"title\":\"%s\",\"available\":%s}", b.id(), b.title(), b.available())).toList().toString();
                send(exchange, 200, json);
            } else if ("POST".equals(exchange.getRequestMethod()) && path.matches("/books/\\d+/borrow")) {
                int id = Integer.parseInt(path.split("/")[2]);
                for (int i = 0; i < books.size(); i++) if (books.get(i).id() == id) {
                    Book book = books.get(i); if (!book.available()) { send(exchange, 409, "{\"error\":\"Libro no disponible\"}"); return; }
                    books.set(i, new Book(book.id(), book.title(), false)); send(exchange, 200, "{\"status\":\"prestado\"}"); return;
                }
                send(exchange, 404, "{\"error\":\"Libro no encontrado\"}");
            } else send(exchange, 404, "{\"error\":\"Ruta no encontrada\"}");
        });
        server.start(); System.out.println("Library Server: http://localhost:8090/books");
    }
}
