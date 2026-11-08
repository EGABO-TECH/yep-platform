package yep.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import yep.model.User;
import yep.service.YepServices;
import yep.util.YepException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

/**
 * The web server. Uses the HTTP server built into the JDK, so no frameworks are needed.
 * /api/... returns JSON; everything else is a static file from the web folder.
 */
public class YepServer {
    public static final String COOKIE = "yep_session";

    private final YepServices services;
    private final Router router = new Router();
    private final Path webRoot;

    public YepServer(YepServices services, Path webRoot) {
        this.services = services;
        this.webRoot = webRoot.toAbsolutePath().normalize();
        Views views = new Views(services);
        List<ApiModule> modules = List.of(
                new AuthApi(services, views),
                new ParticipantApi(services, views),
                new StaffApi(services, views),
                new OrgApi(services, views),
                new AdminApi(services, views));
        modules.forEach(m -> m.register(router));
    }

    public HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/healthz", ex -> send(ex, 200, "text/plain; charset=utf-8", "ok".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.start();
        return server;
    }

    // ------------------------------------------------------------ API
    private void handleApi(HttpExchange ex) throws IOException {
        int status = 200;
        Object result;
        try {
            Router.Match match = router.match(ex.getRequestMethod(), ex.getRequestURI().getPath());
            if (match == null) {
                throw new YepException(404, "Not found");
            }
            String token = readCookie(ex);
            User user = services.auth.currentUser(token);
            result = match.handler().handle(new Request(ex, match.params(), user, token));
        } catch (YepException e) {
            status = e.getStatus();
            result = Json.obj("error", e.getMessage());
        } catch (ClassCastException e) {
            status = 403;
            result = Json.obj("error", "Access denied");
        } catch (Exception e) {
            e.printStackTrace();
            status = 500;
            result = Json.obj("error", "Something went wrong on the server. Please try again.");
        }

        if (result instanceof RawResponse raw) {
            if (raw.downloadName() != null) {
                ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + raw.downloadName() + "\"");
            }
            send(ex, status, raw.contentType(), raw.body().getBytes(StandardCharsets.UTF_8));
        } else {
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            send(ex, status, "application/json; charset=utf-8", Json.write(result).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Cookie attributes for the session cookie. Adds Secure when the request reached us over HTTPS
     * through a proxy (Render and similar hosts terminate TLS and set X-Forwarded-Proto).
     */
    static String cookieAttributes(HttpExchange ex, int maxAge) {
        String proto = ex.getRequestHeaders().getFirst("X-Forwarded-Proto");
        String secure = proto != null && proto.equalsIgnoreCase("https") ? "; Secure" : "";
        return "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + maxAge + secure;
    }

    private static String readCookie(HttpExchange ex) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) {
            return null;
        }
        for (String header : headers) {
            for (String part : header.split(";")) {
                String p = part.trim();
                if (p.startsWith(COOKIE + "=")) {
                    return p.substring(COOKIE.length() + 1);
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------ static files
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8", "css", "text/css; charset=utf-8", "js", "text/javascript; charset=utf-8",
            "jpg", "image/jpeg", "png", "image/png", "svg", "image/svg+xml", "woff2", "font/woff2",
            "json", "application/json", "ico", "image/x-icon", "webmanifest", "application/manifest+json");

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }
        Path file = webRoot.resolve(path.substring(1)).normalize();
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            if (path.substring(path.lastIndexOf('/') + 1).contains(".")) {
                send(ex, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
                return;
            }
            file = webRoot.resolve("index.html");
        }
        String name = file.getFileName().toString();
        String ext = name.substring(name.lastIndexOf('.') + 1);
        boolean longCache = ext.equals("woff2") || ext.equals("jpg") || ext.equals("png");
        ex.getResponseHeaders().add("Cache-Control", longCache ? "public, max-age=604800" : "no-cache");
        send(ex, 200, TYPES.getOrDefault(ext, "application/octet-stream"), Files.readAllBytes(file));
    }

    /** Sends a response, gzip-compressed for text when the browser supports it (NFR-02 low data use). */
    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("Referrer-Policy", "same-origin");
        String accept = ex.getRequestHeaders().getFirst("Accept-Encoding");
        boolean text = type.startsWith("text/") || type.startsWith("application/json") || type.contains("svg");
        if (text && accept != null && accept.contains("gzip") && body.length > 1024) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(buffer)) {
                gz.write(body);
            }
            body = buffer.toByteArray();
            ex.getResponseHeaders().set("Content-Encoding", "gzip");
        }
        if ("HEAD".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(status, -1);
            ex.close();
            return;
        }
        ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
