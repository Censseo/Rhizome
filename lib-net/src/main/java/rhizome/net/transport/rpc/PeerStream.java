/*
 * RPC Stream for the node communication
 * Reimplementation of ActiveJ RPC Stream
 */

package rhizome.net.transport.rpc;

import java.time.Duration;

import org.jetbrains.annotations.NotNull;

import io.activej.async.exception.AsyncCloseException;
import io.activej.common.MemSize;
import io.activej.csp.consumer.ChannelConsumers;
import io.activej.csp.supplier.ChannelSuppliers;
import io.activej.datastream.consumer.AbstractStreamConsumer;
import io.activej.datastream.csp.ChannelDeserializer;
import io.activej.datastream.csp.ChannelSerializer;
import io.activej.datastream.supplier.AbstractStreamSupplier;
import io.activej.net.socket.tcp.ITcpSocket;
import io.activej.serializer.BinarySerializer;
import rhizome.net.protocol.Message;

public class PeerStream {

    private final ChannelDeserializer<Message> deserializer;
	private final ChannelSerializer<Message> serializer;
    private Listener listener;
	private final boolean server;
	private final ITcpSocket socket;

    private final AbstractStreamConsumer<Message> internalConsumer = new AbstractStreamConsumer<>() {};

	private final AbstractStreamSupplier<Message> internalSupplier = new AbstractStreamSupplier<>() {
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

    public PeerStream(ITcpSocket socket,
			BinarySerializer<Message> messageSerializer,
			MemSize initialBufferSize,
			Duration autoFlushInterval,
            boolean server) {
		this.server = server;
		this.socket = socket;

		serializer = ChannelSerializer.builder(messageSerializer)
				.withInitialBufferSize(initialBufferSize)
				.withAutoFlushInterval(autoFlushInterval)
				.withSerializationErrorHandler((message, e) -> listener.onSerializationError(message, e))
				.build();
		deserializer = ChannelDeserializer.builder(messageSerializer).build();

        ChannelSuppliers.ofSocket(socket).bindTo(deserializer.getInput());
        serializer.getOutput().set(ChannelConsumers.ofSocket(socket));

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
