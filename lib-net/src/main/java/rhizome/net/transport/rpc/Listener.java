package rhizome.net.transport.rpc;

import org.jetbrains.annotations.NotNull;

import io.activej.datastream.StreamDataAcceptor;
import rhizome.net.protocol.Message;

public interface Listener extends StreamDataAcceptor<Message> {
		void onReceiverEndOfStream();
		void onReceiverError(@NotNull Exception e);
		void onSenderError(@NotNull Exception e);
		void onSerializationError(Message message, @NotNull Exception e);
		void onSenderReady(@NotNull StreamDataAcceptor<Message> acceptor);
		void onSenderSuspended();
}
