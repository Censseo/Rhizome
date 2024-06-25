package rhizome;

import org.junit.jupiter.api.Test;

import io.activej.csp.ChannelConsumer;
import io.activej.csp.ChannelSupplier;
import io.activej.csp.binary.BinaryChannelSupplier;
import io.activej.csp.binary.ByteBufsDecoder;
import io.activej.datastream.StreamConsumerToList;
import io.activej.datastream.StreamSupplier;
import io.activej.datastream.csp.ChannelDeserializer;
import io.activej.datastream.csp.ChannelSerializer;
import io.activej.datastream.processor.StreamFilter;
import io.activej.eventloop.Eventloop;
import io.activej.eventloop.net.ServerSocketSettings;
import io.activej.net.SimpleServer;
import io.activej.net.socket.tcp.AsyncTcpSocket;
import io.activej.net.socket.tcp.AsyncTcpSocketNio;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;

import static io.activej.bytebuf.ByteBufStrings.wrapAscii;
import static io.activej.promise.Promises.loop;
import static io.activej.promise.Promises.repeat;
import static java.nio.charset.StandardCharsets.UTF_8;

import static io.activej.common.exception.FatalErrorHandler.rethrow;
import static io.activej.serializer.BinarySerializers.INT_SERIALIZER;
import static io.activej.serializer.BinarySerializers.UTF8_SERIALIZER;

class NetworkTest {
	private static final InetSocketAddress ADDRESS = new InetSocketAddress("localhost", 9022);
	private static final int ITERATIONS = 3;
	private static final String REQUEST_MSG = "PING";
	private static final String RESPONSE_MSG = "PONG";
    public static final int PORT = 9922;


	private static final ByteBufsDecoder<String> DECODER = ByteBufsDecoder.ofFixedSize(4)
			.andThen(buf -> buf.asString(UTF_8));
    @Test
    void SocketConnectionTest() throws IOException {
	    Eventloop eventloop = Eventloop.create().withCurrentThread();

		SimpleServer server = SimpleServer.create(
				socket -> {
					BinaryChannelSupplier bufsSupplier = BinaryChannelSupplier.of(ChannelSupplier.ofSocket(socket));
					repeat(() ->
							bufsSupplier.decode(DECODER)
									.whenResult(x -> System.out.println(x))
									.then(() -> socket.write(wrapAscii(RESPONSE_MSG)))
									.map($ -> true))
							.whenComplete(socket::close);
				})
				.withListenAddress(ADDRESS)
				.withAcceptOnce();

		server.listen();

		AsyncTcpSocketNio.connect(ADDRESS)
				.whenResult(socket -> {
					BinaryChannelSupplier bufsSupplier = BinaryChannelSupplier.of(ChannelSupplier.ofSocket(socket));
					loop(0,
							i -> i < ITERATIONS,
							i -> socket.write(wrapAscii(REQUEST_MSG))
									.then(() -> bufsSupplier.decode(DECODER)
											.whenResult(x -> System.out.println(x))
											.map($2 -> i + 1)))
							.whenComplete(socket::close);
				})
				.whenException(e -> { throw new RuntimeException(e); });

		eventloop.run();

        assertTrue(true);

    }


    // @Test
    private void tcpClientExemple() throws IOException{
        Eventloop eventloop = Eventloop.create().withEventloopFatalErrorHandler(rethrow());

        eventloop.listen(new InetSocketAddress("localhost", PORT), ServerSocketSettings.create(100), channel -> {
        AsyncTcpSocket socket;

        try {
            socket = AsyncTcpSocketNio.wrapChannel(eventloop, channel, null);
            System.out.println("Client connected: " + channel.getRemoteAddress());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ChannelSupplier.ofSocket(socket)
                .transformWith(ChannelDeserializer.create(INT_SERIALIZER))
                .transformWith(StreamFilter.mapper(x -> x + " times 10 = " + x * 10))
                .transformWith(ChannelSerializer.create(UTF8_SERIALIZER))
                .streamTo(ChannelConsumer.ofSocket(socket));
		});

		System.out.println("Connect to the server by running datastream.TcpClientExample");

		eventloop. run();

        Eventloop eventloop2 = Eventloop.create().withEventloopFatalErrorHandler(rethrow());
		eventloop2.connect(new InetSocketAddress("localhost", PORT), (socketChannel, e) -> {
			if (e == null) {
				AsyncTcpSocket socket;
				try {
					socket = AsyncTcpSocketNio.wrapChannel(eventloop, socketChannel, null);
				} catch (IOException ioEx) {
					throw new RuntimeException(ioEx);
				}

				StreamSupplier.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
						.transformWith(ChannelSerializer.create(INT_SERIALIZER))
						.streamTo(ChannelConsumer.ofSocket(socket));

				StreamConsumerToList<String> consumer = StreamConsumerToList.create();

				ChannelSupplier.ofSocket(socket)
						.transformWith(ChannelDeserializer.create(UTF8_SERIALIZER))
						.streamTo(consumer);

				consumer.getResult()
						.whenResult(list -> list.forEach(System.out::println));

			} else {
				System.out.printf("Could not connect to server, make sure it is started: %s%n", e);
			}
		});

		eventloop2.run();
        assertTrue(true);
    }
}
