package rhizome.config;

import io.activej.config.Config;
import io.activej.eventloop.Eventloop;
import io.activej.inject.annotation.Eager;
import io.activej.inject.annotation.Provides;
import io.activej.inject.module.AbstractModule;
import io.activej.promise.Promise;
import io.activej.rpc.client.RpcClient;
import io.activej.rpc.client.sender.RpcStrategy;
import io.activej.serializer.SerializerBuilder;
import rhizome.net.protocol.Message;
import rhizome.net.transport.TransportChannel;
import rhizome.net.transport.rpc.server.RpcServer;

import java.time.Duration;
import static io.activej.config.converter.ConfigConverters.ofInteger;

public final class RpcModule extends AbstractModule {
	private RpcModule() {
	}

	public static RpcModule create() {
		return new RpcModule();
	}

	@Provides RpcClient rpcClient(Eventloop eventloop, RpcStrategy strategy) {
		return RpcClient.create(eventloop)
				.withConnectTimeout(Duration.ofSeconds(1))
				.withSerializerBuilder(SerializerBuilder.create())
				.withMessageTypes(Integer.class)
				.withStrategy(strategy);
	}

	@Provides
	@Eager
	RpcServer rpcServer(Eventloop eventloop, Config config) {
		return RpcServer.create(eventloop)
				.withMessageTypes()
				.withHandler(Message.class, message -> {
					return Promise.of(message);
				})
				.withListenPort(config.get(ofInteger(), "rpc.port"));
	}

	@Provides TransportChannel transportChannel(Config config) {
		return null;
	}
}