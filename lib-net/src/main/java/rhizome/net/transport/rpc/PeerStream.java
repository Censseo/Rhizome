/*
 * RPC Stream for the node communication
 * Reimplementation of ActiveJ RPC Stream
 */

package rhizome.net.transport.rpc;

import java.time.Duration;

import org.jetbrains.annotations.NotNull;

import io.activej.async.exception.AsyncCloseException;
import io.activej.common.MemSize;
import io.activej.csp.ChannelConsumer;
import io.activej.csp.ChannelSupplier;
import io.activej.datastream.AbstractStreamConsumer;
import io.activej.datastream.AbstractStreamSupplier;
import io.activej.datastream.csp.ChannelDeserializer;
import io.activej.datastream.csp.ChannelSerializer;
import io.activej.net.socket.tcp.AsyncTcpSocket;
import io.activej.serializer.BinarySerializer;
import rhizome.net.protocol.Message;

public class PeerStream {
    private final ChannelDeserializer<Message> deserializer;
	private final ChannelSerializer<Message> serializer;
    private Listener listener;
	private final boolean server;
	private final AsyncTcpSocket socket;

    private final AbstractStreamConsumer<Message> internalConsumer = new AbstractStreamConsumer<Message>() {};

	private final AbstractStreamSupplier<Message> internalSupplier = new AbstractStreamSupplier<Message>() {
		@Override
		protected void onResumed() {
			deserializer.updateDataAcceptor();
			//noinspection ConstantConditions - dataAcceptorr is not null in onResumed state
			listener.onSenderReady(getDataAcceptor());
		}

		@Override
		protected void onSuspended() {
			if (server) {
				deserializer.updateDataAcceptor();
			}
			listener.onSenderSuspended();
		}

	};

    public PeerStream(AsyncTcpSocket socket,
			BinarySerializer<Message> messageSerializer,
			MemSize initialBufferSize,
			Duration autoFlushInterval,
            boolean server) {
		this.server = server;
		this.socket = socket;

		serializer = ChannelSerializer.create(messageSerializer)
				.withInitialBufferSize(initialBufferSize)
				.withAutoFlushInterval(autoFlushInterval)
				.withSerializationErrorHandler((message, e) -> listener.onSerializationError(message, e));
		deserializer = ChannelDeserializer.create(messageSerializer);

        ChannelSupplier.ofSocket(socket).bindTo(deserializer.getInput());
        serializer.getOutput().set(ChannelConsumer.ofSocket(socket));

		deserializer.streamTo(internalConsumer);
	}

    public void setListener(Listener listener) {
		this.listener = listener;
		deserializer.getEndOfStream()
				.whenResult(listener::onReceiverEndOfStream)
				.whenException(listener::onReceiverError);
		serializer.getAcknowledgement()
				.whenException(listener::onSenderError);
		internalSupplier.streamTo(serializer);
		internalConsumer.resume(this.listener);
	}

	public void receiverSuspend() {
		internalConsumer.suspend();
	}

	public void receiverResume() {
		internalConsumer.resume(listener);
	}

	public void sendEndOfStream() {
		internalSupplier.sendEndOfStream();
	}

	public void close() {
		closeEx(new AsyncCloseException("RPC Channel Closed"));
	}

	public void closeEx(@NotNull Exception e) {
		socket.closeEx(e);
		serializer.closeEx(e);
		deserializer.closeEx(e);
	}
}
