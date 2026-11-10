package yep;

import yep.data.DataStore;
import yep.data.Seeder;
import yep.service.YepServices;
import yep.web.YepServer;

import com.sun.net.httpserver.HttpServer;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", args.length > 0 ? args[0] : "8080"));
        Path web = Path.of(System.getenv().getOrDefault("YEP_WEB_DIR", "web"));
        if (!Files.isDirectory(web)) {
            System.err.println("Cannot find the 'web' folder. Run the program from the project folder.");
            System.exit(1);
        }

        System.out.println("Loading YEP demo data...");
        DataStore store = new DataStore();
        YepServices services = new YepServices(store);
        new Seeder(services).seed();

        HttpServer server = new YepServer(services, web).start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(2)));

        String url = System.getenv().getOrDefault("RENDER_EXTERNAL_URL", "http://localhost:" + port);
        System.out.println();
        System.out.println("  YEP is running at  " + url);
        System.out.println("  Demo password for every account: " + Seeder.DEMO_PASSWORD);
        System.out.println("  Press Ctrl+C to stop.");
    }
}
