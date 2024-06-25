/*
 * RPC Client Connection for the node communication
 * Reimplementation of ActiveJ RPC Client Connection
 */

package rhizome.net.transport.rpc.client;

import io.activej.async.callback.Callback;
import io.activej.async.exception.AsyncCloseException;
import io.activej.async.exception.AsyncTimeoutException;
import io.activej.common.Checks;
import io.activej.common.recycle.Recyclers;
import io.activej.common.time.Stopwatch;
import io.activej.datastream.StreamDataAcceptor;
import io.activej.eventloop.Eventloop;
import io.activej.eventloop.schedule.ScheduledRunnable;
import io.activej.jmx.api.JmxRefreshable;
import io.activej.jmx.api.attribute.JmxAttribute;
import io.activej.jmx.api.attribute.JmxReducers.JmxReducerSum;
import io.activej.jmx.stats.EventStats;
import io.activej.rpc.client.jmx.RpcRequestStats;
import io.activej.rpc.client.sender.RpcSender;
import io.activej.rpc.protocol.RpcControlMessage;
import io.activej.rpc.protocol.RpcException;
import io.activej.rpc.protocol.RpcMandatoryData;
import io.activej.rpc.protocol.RpcOverloadException;
import io.activej.rpc.protocol.RpcRemoteException;
import rhizome.net.protocol.Message;
import rhizome.net.protocol.MessageCode;
import rhizome.net.transport.ChannelOutput;
import rhizome.net.transport.rpc.Listener;
import rhizome.net.transport.rpc.PeerStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.activej.common.Checks.checkState;
import static org.slf4j.LoggerFactory.getLogger;

public final class RpcClientConnection implements ChannelOutput, Listener, RpcSender, JmxRefreshable {
	private static final Logger logger = getLogger(RpcClientConnection.class);
	private static final boolean CHECK = Checks.isEnabled(RpcClientConnection.class);

	private static final RpcException CONNECTION_UNRESPONSIVE = new RpcException("Unresponsive connection");
	private static final RpcOverloadException RPC_OVERLOAD_EXCEPTION = new RpcOverloadException("RPC client is overloaded");

	private StreamDataAcceptor<Message> downstreamDataAcceptor = null;
	private boolean overloaded = false;
	private boolean closed;

	private final Eventloop eventloop;
	private final RpcClient rpcClient;
	private final PeerStream stream;
	private final InetSocketAddress address;
	private final Map<Integer, Callback<?>> activeRequests = new HashMap<>();

	private ArrayList<Message> initialBuffer = new ArrayList<>();

	private int cookie = 0;
	private boolean serverClosing;

	// JMX
	private boolean monitoring;
	private final RpcRequestStats connectionStats;
	private final EventStats totalRequests;
	private final EventStats connectionRequests;

	// keep-alive pings
	private final long keepAliveMillis;
	private boolean pongReceived;

	RpcClientConnection(Eventloop eventloop, RpcClient peerClient, InetSocketAddress address, PeerStream stream,
			long keepAliveMillis) {
		this.eventloop = eventloop;
		this.rpcClient = peerClient;
		this.stream = stream;
		this.address = address;
		this.keepAliveMillis = keepAliveMillis;

		// JMX
		this.monitoring = false;
		this.connectionStats = RpcRequestStats.create(RpcClient.SMOOTHING_WINDOW);
		this.connectionRequests = connectionStats.getTotalRequests();
		this.totalRequests = peerClient.getGeneralRequestsStats().getTotalRequests();
	}

	@Override
	public <I, O> void sendRequest(I request, int timeout, @NotNull Callback<O> cb) {
		if (CHECK) checkState(eventloop.inEventloopThread(), "Not in eventloop thread");

		// jmx
		totalRequests.recordEvent();
		connectionRequests.recordEvent();

		if (!overloaded || request instanceof RpcMandatoryData) {
			cookie++;

			// jmx
			if (monitoring) {
				cb = doJmxMonitoring(request, timeout, cb);
			}

			if (timeout == Integer.MAX_VALUE) {
				activeRequests.put(cookie, cb);
			} else {
				ScheduledCallback<O> scheduledRunnable = new ScheduledCallback<>(eventloop.currentTimeMillis() + timeout, cookie, cb);
				eventloop.scheduleBackground(scheduledRunnable);
				activeRequests.put(cookie, scheduledRunnable);
			}

			// TODO
			// downstreamDataAcceptor.accept(Message.of(cookie, request));
		} else {
			doProcessOverloaded(cb);
		}
	}

	private class ScheduledCallback<O> extends ScheduledRunnable implements Callback<O> {
		final Callback<O> cb;
		final int cookie;

		ScheduledCallback(long timestamp, int cookie, @NotNull Callback<O> cb) {
			super(timestamp);
			this.cookie = cookie;
			this.cb = cb;
		}

		@Override
		public void accept(O result, @Nullable Exception e) {
			if (isActive()) {
				cancel();
				cb.accept(result, e);
			} else {
				Recyclers.recycle(result);
			}
		}

		@Override
		public void run() {
			Callback<?> expiredCb = activeRequests.remove(cookie);
			if (expiredCb != null) {
				assert expiredCb == this;
				// jmx
				connectionStats.getExpiredRequests().recordEvent();
				rpcClient.getGeneralRequestsStats().getExpiredRequests().recordEvent();

				cb.accept(null, new AsyncTimeoutException("RPC request has timed out"));
			}

			if (serverClosing && activeRequests.size() == 0) {
				RpcClientConnection.this.shutdown();
			}
		}
	}

	@Override
	public <I, O> void sendRequest(I request, @NotNull Callback<O> cb) {
		if (CHECK) checkState(eventloop.inEventloopThread(), "Not in eventloop thread");

		// jmx
		totalRequests.recordEvent();
		connectionRequests.recordEvent();

		if (!overloaded || request instanceof RpcMandatoryData) {
			cookie++;

			// jmx
			if (monitoring) {
				cb = doJmxMonitoring(request, Integer.MAX_VALUE, cb);
			}

			activeRequests.put(cookie, cb);

			// TODO
			// downstreamDataAcceptor.accept(Message.of(cookie, request));
		} else {
			doProcessOverloaded(cb);
		}
	}

	private <I, O> Callback<O> doJmxMonitoring(I request, int timeout, @NotNull Callback<O> cb) {
		RpcRequestStats requestStatsPerClass = rpcClient.ensureRequestStatsPerClass(request.getClass());
		requestStatsPerClass.getTotalRequests().recordEvent();
		return new JmxConnectionMonitoringResultCallback<>(requestStatsPerClass, cb, timeout);
	}

	private <O> void doProcessOverloaded(@NotNull Callback<O> cb) {
		// jmx
		rpcClient.getGeneralRequestsStats().getRejectedRequests().recordEvent();
		connectionStats.getRejectedRequests().recordEvent();
		if (logger.isTraceEnabled()) logger.trace("RPC client uplink is overloaded");

		cb.accept(null, RPC_OVERLOAD_EXCEPTION);
	}

	@Override
	public void accept(Message message) {
		if (message.data().getClass() == RpcRemoteException.class) {
			processErrorMessage(message);
		} else if (message.data().getClass() == RpcControlMessage.class) {
			processControlMessage((RpcControlMessage) message.data());
		} else {

			//TODO
			// @SuppressWarnings("unchecked")
			// Callback<Object> cb = (Callback<Object>) activeRequests.remove(message.getCookie());
			// if (cb == null) return;

			// cb.accept(message.getData(), null);
			if (serverClosing && activeRequests.size() == 0) {
				shutdown();
			}
		}
	}

	private void processErrorMessage(Message message) {
		RpcRemoteException remoteException = (RpcRemoteException) message.data();
		// jmx
		connectionStats.getFailedRequests().recordEvent();
		rpcClient.getGeneralRequestsStats().getFailedRequests().recordEvent();
		connectionStats.getServerExceptions().recordException(remoteException, null);
		rpcClient.getGeneralRequestsStats().getServerExceptions().recordException(remoteException, null);

		// TODO
		// Callback<?> cb = activeRequests.remove(message.getCookie());
		// if (cb != null) {
		// 	cb.accept(null, remoteException);
		// }
	}

	private void processControlMessage(RpcControlMessage controlMessage) {
		if (controlMessage == RpcControlMessage.CLOSE) {
			rpcClient.removeConnection(address);
			serverClosing = true;
			if (activeRequests.size() == 0) {
				shutdown();
			}
		} else if (controlMessage == RpcControlMessage.PONG) {
			pongReceived = true;
		} else {
			throw new RuntimeException("Received unknown RpcControlMessage");
		}
	}

	public void ping() {
		if (isClosed()) return;
		if (keepAliveMillis == 0) return;
		pongReceived = false;
		downstreamDataAcceptor.accept(Message.of(MessageCode.SYNC, RpcControlMessage.PING));
		eventloop.delayBackground(keepAliveMillis, () -> {
			if (isClosed()) return;
			if (!pongReceived) {
				onReceiverError(CONNECTION_UNRESPONSIVE);
			} else {
				ping();
			}
		});
	}

	@Override
	public void onReceiverEndOfStream() {
		if (isClosed()) return;
		logger.info("Receiver EOS: {}", address);
		doClose();
	}

	@Override
	public void onReceiverError(@NotNull Exception e) {
		if (isClosed()) return;
		logger.error("Receiver error: {}", address, e);
		rpcClient.getLastProtocolError().recordException(e, address);
		doClose();
	}

	@Override
	public void onSenderError(@NotNull Exception e) {
		if (isClosed()) return;
		logger.error("Sender error: {}", address, e);
		rpcClient.getLastProtocolError().recordException(e, address);
		doClose();
	}

	@Override
	public void onSerializationError(Message message, @NotNull Exception e) {
		if (isClosed()) return;
		logger.error("Serialization error: {} for data {}", address, message.data(), e);
		rpcClient.getLastProtocolError().recordException(e, address);

		// TODO
		// activeRequests.remove(message.getCookie()).accept(null, e);
	}

	@Override
	public void onSenderReady(@NotNull StreamDataAcceptor<Message> acceptor) {
		if (isClosed()) return;
		downstreamDataAcceptor = acceptor;
		overloaded = false;
		if (initialBuffer != null) {
			for (Message message : initialBuffer) {
				acceptor.accept(message);
			}
			initialBuffer = null;
			ping();
		}
	}

	@Override
	public void onSenderSuspended() {
		overloaded = true;
	}

	private void doClose() {
		if (isClosed()) return;
		stream.close();
		downstreamDataAcceptor = null;
		closed = true;
		rpcClient.removeConnection(address);

		while (!activeRequests.isEmpty()) {
			for (Integer cookie : new HashSet<>(activeRequests.keySet())) {
				Callback<?> cb = activeRequests.remove(cookie);
				if (cb != null) {
					cb.accept(null, new AsyncCloseException("Connection closed"));
				}
			}
		}
	}

	public boolean isClosed() {
		return closed;
	}

	public void shutdown() {
		if (isClosed()) return;
		stream.sendEndOfStream();
	}

	public void forceShutdown() {
		doClose();
	}

	// JMX

	public void startMonitoring() {
		monitoring = true;
	}

	public void stopMonitoring() {
		monitoring = false;
	}

	@JmxAttribute(name = "")
	public RpcRequestStats getRequestStats() {
		return connectionStats;
	}

	@JmxAttribute(reducer = JmxReducerSum.class)
	public int getActiveRequests() {
		return activeRequests.size();
	}

	@Override
	public void refresh(long timestamp) {
		connectionStats.refresh(timestamp);
	}

	private final class JmxConnectionMonitoringResultCallback<T> implements Callback<T> {
		private final Stopwatch stopwatch;
		private final Callback<T> callback;
		private final RpcRequestStats requestStatsPerClass;
		private final long dueTimestamp;

		public JmxConnectionMonitoringResultCallback(RpcRequestStats requestStatsPerClass, Callback<T> cb,
				long timeout) {
			this.stopwatch = Stopwatch.createStarted();
			this.callback = cb;
			this.requestStatsPerClass = requestStatsPerClass;
			this.dueTimestamp = eventloop.currentTimeMillis() + timeout;
		}

		@Override
		public void accept(T result, @Nullable Exception e) {
			if (e == null) {
				onResult(result);
			} else {
				onException(e);
			}
		}

		private void onResult(T result) {
			int responseTime = timeElapsed();
			connectionStats.getResponseTime().recordValue(responseTime);
			requestStatsPerClass.getResponseTime().recordValue(responseTime);
			rpcClient.getGeneralRequestsStats().getResponseTime().recordValue(responseTime);
			recordOverdue();
			callback.accept(result, null);
		}

		private void onException(@NotNull Exception e) {
			if (e instanceof RpcRemoteException) {
				int responseTime = timeElapsed();
				connectionStats.getFailedRequests().recordEvent();
				connectionStats.getResponseTime().recordValue(responseTime);
				connectionStats.getServerExceptions().recordException(e, null);
				requestStatsPerClass.getFailedRequests().recordEvent();
				requestStatsPerClass.getResponseTime().recordValue(responseTime);
				rpcClient.getGeneralRequestsStats().getResponseTime().recordValue(responseTime);
				requestStatsPerClass.getServerExceptions().recordException(e, null);
				recordOverdue();
			} else if (e instanceof AsyncTimeoutException) {
				connectionStats.getExpiredRequests().recordEvent();
				requestStatsPerClass.getExpiredRequests().recordEvent();
			} else if (e instanceof RpcOverloadException) {
				connectionStats.getRejectedRequests().recordEvent();
				requestStatsPerClass.getRejectedRequests().recordEvent();
			}
			callback.accept(null, e);
		}

		private int timeElapsed() {
			return (int) stopwatch.elapsed(TimeUnit.MILLISECONDS);
		}

		private void recordOverdue() {
			int overdue = (int) (System.currentTimeMillis() - dueTimestamp);
			if (overdue > 0) {
				connectionStats.getOverdues().recordValue(overdue);
				requestStatsPerClass.getOverdues().recordValue(overdue);
				rpcClient.getGeneralRequestsStats().getOverdues().recordValue(overdue);
			}
		}
	}

	@Override
	public String toString() {
		int active = activeRequests.size();
		long failed = connectionStats.getFailedRequests().getTotalCount();

		return "peerClientConnection{" +
				"address=" + address +
				", active=" + active +
				", successes=" + (connectionStats.getTotalRequests().getTotalCount() - failed - active) +
				", failures=" + failed +
				'}';
	}
}
