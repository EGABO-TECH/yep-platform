package yep.web;

import com.sun.net.httpserver.HttpExchange;
import yep.model.User;
import yep.util.YepException;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Wraps one HTTP request: path parameters, query string, JSON body and the logged-in user. */
public class Request {
    private final HttpExchange exchange;
    private final Map<String, String> pathParams;
    private final Map<String, String> query;
    private Map<String, Object> body;
    private final User user;
    private final String token;

    public Request(HttpExchange exchange, Map<String, String> pathParams, User user, String token) {
        this.exchange = exchange;
        this.pathParams = pathParams;
        this.query = parseQuery(exchange.getRequestURI().getRawQuery());
        this.user = user;
        this.token = token;
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> body() {
        if (body == null) {
            try {
                String text = new String(exchange.getRequestBody().readNBytes(1_000_000), StandardCharsets.UTF_8);
                Object parsed = Json.parse(text);
                body = parsed instanceof Map ? (Map<String, Object>) parsed : new HashMap<>();
            } catch (IOException | RuntimeException e) {
                throw new YepException("The request could not be read");
            }
        }
        return body;
    }

    public String str(String key) {
        Object v = body().get(key);
        return v == null ? null : String.valueOf(v);
    }

    public int intVal(String key, int fallback) {
        Object v = body().get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return v == null || String.valueOf(v).isBlank() ? fallback : Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public double doubleVal(String key) {
        Object v = body().get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (RuntimeException e) {
            throw new YepException("Enter a number for " + key);
        }
    }

    public boolean bool(String key) {
        return Boolean.TRUE.equals(body().get(key)) || "true".equals(String.valueOf(body().get(key)));
    }

    public List<Integer> intList(String key) {
        Object v = body().get(key);
        List<Integer> out = new ArrayList<>();
        if (v instanceof List<?> list) {
            for (Object o : list) {
                out.add(o == null ? null : ((Number) o).intValue());
            }
        }
        return out;
    }

    public List<String> strList(String key) {
        Object v = body().get(key);
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> list) {
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
        }
        return out;
    }

    public int pathInt(String name) {
        try {
            return Integer.parseInt(pathParams.get(name));
        } catch (NumberFormatException e) {
            throw YepException.notFound("Item");
        }
    }

    public String query(String name) {
        return query.get(name);
    }

    public User user() {
        if (user == null) {
            throw new YepException(401, "Your session has ended. Please log in again.");
        }
        return user;
    }

    public User userOrNull() {
        return user;
    }

    public String token() {
        return token;
    }

    public HttpExchange exchange() {
        return exchange;
    }
}
