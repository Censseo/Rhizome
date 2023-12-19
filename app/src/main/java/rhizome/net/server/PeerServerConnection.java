package rhizome.net.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.activej.common.exception.MalformedDataException;
import io.activej.datastream.StreamDataAcceptor;
import io.activej.jmx.api.JmxRefreshable;
import io.activej.jmx.api.attribute.JmxAttribute;
import io.activej.jmx.stats.EventStats;
import io.activej.jmx.stats.ExceptionStats;
import io.activej.jmx.stats.ValueStats;
import io.activej.promise.Promise;
import io.activej.rpc.protocol.RpcRemoteException;
import io.activej.rpc.server.RpcRequestHandler;
import lombok.extern.slf4j.Slf4j;
import rhizome.net.protocol.PeerStream;
import rhizome.net.protocol.Listener;
import rhizome.net.protocol.Message;
import rhizome.net.protocol.MessageCodes;

@Slf4j
public class PeerServerConnection implements Listener, JmxRefreshable{

    private StreamDataAcceptor<Message> downstreamDataAcceptor;
	private final PeerServer peerServer;
	private PeerStream stream;
	private final Map<Class<?>, RpcRequestHandler<?,?>> handlers;
	private int activeRequests = 1;

    // jmx
	private final InetAddress remoteAddress;
	private final ExceptionStats lastRequestHandlingException = ExceptionStats.create();
	private final ValueStats requestHandlingTime = ValueStats.create(PeerServer.SMOOTHING_WINDOW).withUnit("milliseconds");
	private final EventStats successfulRequests = EventStats.create(PeerServer.SMOOTHING_WINDOW);
	private final EventStats failedRequests = EventStats.create(PeerServer.SMOOTHING_WINDOW);
	private boolean monitoring = false;

    PeerServerConnection(PeerServer peerServer, 
            InetAddress remoteAddress,
            Map<Class<?>, RpcRequestHandler<?,?>> handlers, 
            PeerStream stream) {
        this.peerServer = peerServer;
        this.handlers = handlers;
        this.stream = stream;

        // jmx
        this.remoteAddress = remoteAddress;
    }

    /**
	 * Serves the request by finding the appropriate handler and running it.
	 * @param request
	 * @return
	 * @throws IOException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings("unchecked")
	private Promise<Object> serve(Object request) {
		RpcRequestHandler<Object, Object> requestHandler = (RpcRequestHandler<Object, Object>) handlers.get(request.getClass());
		if (requestHandler == null) {
			return Promise.ofException(new MalformedDataException("Failed to process request " + request));
		}
		return requestHandler.run(request).promise();
	}

    @Override
    public void accept(Message message) {
	    activeRequests++;

		var startTime = monitoring ? System.currentTimeMillis() : 0;

        var messageCode = message.getMessageType();
        Object messageData = message.getData();
        serve(messageData)
            .run((result, e) -> {
                if (startTime != 0) {
                    int value = (int) (System.currentTimeMillis() - startTime);
                    requestHandlingTime.recordValue(value);
                    peerServer.getRequestHandlingTime().recordValue(value);
                }
                if (e == null) {
                    downstreamDataAcceptor.accept(Message.of(messageCode, result));

                    successfulRequests.recordEvent();
                    peerServer.getSuccessfulRequests().recordEvent();
                } else {
                    log.warn("Exception while processing request ID {}", messageCode, e);
                    var errorMessage = Message.of(messageCode, new RpcRemoteException(e));
                    sendError(errorMessage, messageData, e);
                }
                if (--activeRequests == 0) {
                    doClose();
                    stream.sendEndOfStream();
                }
            });

    }

	@Override
	public void onReceiverEndOfStream() {
		activeRequests--;
		if (activeRequests == 0) {
			doClose();
			stream.sendEndOfStream();
		}
	}

	@Override
	public void onReceiverError(@NotNull Exception e) {
		log.error("Receiver error {}", remoteAddress, e);
		peerServer.getLastProtocolError().recordException(e, remoteAddress);
		doClose();
		stream.close();
	}

	@Override
	public void onSenderError(@NotNull Exception e) {
		log.error("Sender error: {}", remoteAddress, e);
		peerServer.getLastProtocolError().recordException(e, remoteAddress);
		doClose();
		stream.close();
	}

	@Override
	public void onSerializationError(Message message, @NotNull Exception e) {
		log.error("Serialization error: {} for data {}", remoteAddress, message.getData(), e);
		var errorMessage = Message.of(message.getMessageType(), new RpcRemoteException(e));
		sendError(errorMessage, message.getData(), e);
	}

	@Override
	public void onSenderReady(@NotNull StreamDataAcceptor<Message> acceptor) {
		this.downstreamDataAcceptor = acceptor;
		stream.receiverResume();
	}

	@Override
	public void onSenderSuspended() {
		stream.receiverSuspend();
	}
    
    private void sendError(Message errorMessage, Object messageData, @Nullable Exception e) {
		downstreamDataAcceptor.accept(errorMessage);
		lastRequestHandlingException.recordException(e, messageData);
		peerServer.getLastRequestHandlingException().recordException(e, messageData);
		failedRequests.recordEvent();
		peerServer.getFailedRequests().recordEvent();
	}

    private void doClose() {
		peerServer.remove(this);
		downstreamDataAcceptor = i -> {};
	}

    public void shutdown() {
		if (downstreamDataAcceptor != null) {
			downstreamDataAcceptor.accept(Message.of(MessageCodes.CLOSE, null));
		}
	}

    // jmx
	public void startMonitoring() {
		monitoring = true;
	}

	public void stopMonitoring() {
		monitoring = false;
	}

	@JmxAttribute
	public EventStats getSuccessfulRequests() {
		return successfulRequests;
	}

	@JmxAttribute
	public EventStats getFailedRequests() {
		return failedRequests;
	}

	@JmxAttribute
	public ValueStats getRequestHandlingTime() {
		return requestHandlingTime;
	}

	@JmxAttribute
	public ExceptionStats getLastRequestHandlingException() {
		return lastRequestHandlingException;
	}

	@JmxAttribute
	public String getRemoteAddress() {
		return remoteAddress.toString();
	}

    @Override
	public void refresh(long timestamp) {
		successfulRequests.refresh(timestamp);
		failedRequests.refresh(timestamp);
		requestHandlingTime.refresh(timestamp);
	}

	@Override
	public String toString() {
		return "RpcServerConnection{" +
				"address=" + remoteAddress +
				", active=" + activeRequests +
				", successes=" + successfulRequests.getTotalCount() +
				", failures=" + failedRequests.getTotalCount() +
				'}';
	}
}
