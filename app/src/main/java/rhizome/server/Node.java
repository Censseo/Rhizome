package rhizome.server;

import java.util.concurrent.Executor;

import io.activej.eventloop.Eventloop;
import io.activej.inject.Injector;
import io.activej.inject.annotation.Provides;
import io.activej.inject.module.Module;
import io.activej.inject.module.ModuleBuilder;
import io.activej.launcher.Launcher;
import io.activej.service.ServiceGraphModule;
import rhizome.config.NetworkModule;

import static java.util.concurrent.Executors.newCachedThreadPool;

public final class Node extends Launcher {

    @Provides
    Eventloop eventloop() {
        return Eventloop.create();
    }

    @Provides
	Executor executor() {
		return newCachedThreadPool();
	}

    @Override
    protected Module getModule() {
		return ModuleBuilder.create()
				.install(ServiceGraphModule.create())
                // .install(Api.create())
                .install(NetworkModule.create())
				.build();
	}

    @Override
    protected void run() throws Exception {
        awaitShutdown();
    }

    public static void main(String[] args) throws Exception {
        Injector.useSpecializer();
        new Node().launch(args);
    }
}