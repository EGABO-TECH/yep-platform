package yep.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Maps "METHOD /api/path/{id}" to a handler. */
public class Router {

    @FunctionalInterface
    public interface Handler {
        Object handle(Request request) throws Exception;
    }

    private record Route(String method, Pattern pattern, List<String> names, Handler handler) {
    }

    public record Match(Handler handler, Map<String, String> params) {
    }

    private final List<Route> routes = new ArrayList<>();

    public Router get(String path, Handler h) { return add("GET", path, h); }
    public Router post(String path, Handler h) { return add("POST", path, h); }
    public Router put(String path, Handler h) { return add("PUT", path, h); }

    private Router add(String method, String path, Handler handler) {
        List<String> names = new ArrayList<>();
        Matcher m = Pattern.compile("\\{(\\w+)}").matcher(path);
        StringBuilder regex = new StringBuilder();
        int last = 0;
        while (m.find()) {
            regex.append(Pattern.quote(path.substring(last, m.start()))).append("([^/]+)");
            names.add(m.group(1));
            last = m.end();
        }
        regex.append(Pattern.quote(path.substring(last)));
        routes.add(new Route(method, Pattern.compile("^" + regex + "$"), names, handler));
        return this;
    }

    public Match match(String method, String path) {
        for (Route r : routes) {
            if (!r.method.equals(method)) {
                continue;
            }
            Matcher m = r.pattern.matcher(path);
            if (m.matches()) {
                Map<String, String> params = new HashMap<>();
                for (int i = 0; i < r.names.size(); i++) {
                    params.put(r.names.get(i), m.group(i + 1));
                }
                return new Match(r.handler, params);
            }
        }
        return null;
    }
}
