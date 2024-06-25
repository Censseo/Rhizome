package rhizome.config;

import java.util.concurrent.Executor;

import io.activej.eventloop.Eventloop;
import io.activej.http.AsyncHttpServer;
import io.activej.http.AsyncServlet;
import io.activej.http.HttpResponse;
import io.activej.http.RoutingServlet;
import io.activej.http.StaticServlet;
import io.activej.inject.annotation.Eager;
import io.activej.inject.annotation.Provides;
import io.activej.inject.module.AbstractModule;
import lombok.extern.slf4j.Slf4j;

import static io.activej.http.HttpMethod.*;

@Slf4j
public class Api extends AbstractModule {

    private static final String RESOURCE_DIR = "static/query";
    private static final int PORT = 8080;

    public static Api create() {
        return new Api();
    }

    @Provides
    AsyncServlet servlet(Executor executor) {
        log.info("HTTP Server is now available at http://localhost:" + PORT);
        return RoutingServlet.create()
            .map(POST, "/hello", request -> request.loadBody()
                .map($ -> {
                    String name = request.getPostParameters().get("name");
                    return HttpResponse.ok200()
                        .withHtml("<h1><center>Hello from POST, " + name + "!</center></h1>");
                }))
            .map(GET, "/hello", request -> {
                String name = request.getQueryParameter("name");
                return HttpResponse.ok200()
                    .withHtml("<h1><center>Hello from GET, " + name + "!</center></h1>");
            })
            .map("/*", StaticServlet.ofClassPath(executor, RESOURCE_DIR)
                .withIndexHtml());
    }

    @Provides
    @Eager
    AsyncHttpServer server(Eventloop eventloop, AsyncServlet servlet) {
        return AsyncHttpServer.create(eventloop, servlet).withListenPort(PORT);
    }
    
}
