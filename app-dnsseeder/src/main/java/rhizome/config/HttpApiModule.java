package rhizome.config;

import java.util.concurrent.Executor;

import io.activej.eventloop.Eventloop;
import io.activej.http.AsyncServlet;
import io.activej.http.HttpResponse;
import io.activej.http.HttpServer;
import io.activej.http.RoutingServlet;
import io.activej.http.StaticServlet;
import io.activej.http.loader.IStaticLoader;
import io.activej.inject.annotation.Eager;
import io.activej.inject.annotation.Provides;
import io.activej.inject.module.AbstractModule;
import io.activej.reactor.Reactor;
import lombok.extern.slf4j.Slf4j;

import static io.activej.http.HttpMethod.*;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

@Slf4j
public class HttpApiModule extends AbstractModule {

    private static final String RESOURCE_DIR = "static/query";
    private static final int PORT = 8080;

    public static HttpApiModule create() {
        return new HttpApiModule();
    }

	@Provides
	Executor executor() {
		return newSingleThreadExecutor();
	}

	//[START REGION_1]
	@Provides
	IStaticLoader staticLoader(Reactor reactor, Executor executor) {
		return IStaticLoader.ofClassPath(reactor, executor, RESOURCE_DIR);
	}

	@Provides
	AsyncServlet servlet(Reactor reactor, IStaticLoader staticLoader) {
		return RoutingServlet.builder(reactor)
			.with(POST, "/hello", request -> request.loadBody()
				.then($ -> {
					String name = request.getPostParameters().get("name");
					return HttpResponse.ok200()
						.withHtml("<h1><center>Hello from POST, " + name + "!</center></h1>")
						.toPromise();
				}))
			.with(GET, "/hello", request -> {
				String name = request.getQueryParameter("name");
				return HttpResponse.ok200()
					.withHtml("<h1><center>Hello from GET, " + name + "!</center></h1>")
					.toPromise();
			})
			.with("/*", StaticServlet.builder(reactor, staticLoader)
				.withIndexHtml()
				.build())
			.build();
	}

    @Provides
    @Eager
    HttpServer server(Eventloop eventloop, AsyncServlet servlet) {
        return HttpServer.builder(eventloop, servlet).withListenPort(PORT).build();
    }
    
}
