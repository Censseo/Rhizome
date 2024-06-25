package rhizome.net.p2p.gossip;

import io.activej.async.callback.Callback;
import io.activej.rpc.client.RpcClientConnectionPool;
import io.activej.rpc.client.sender.DiscoveryService;
import io.activej.rpc.client.sender.RpcSender;
import io.activej.rpc.client.sender.RpcStrategy;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static io.activej.common.Checks.checkArgument;

public class GossipStrategy implements RpcStrategy {
	private final Random random = new Random();
	private final Map<RpcStrategy, Integer> strategyToWeight = new HashMap<>();

	private GossipStrategy() {}

	public static GossipStrategy create() {return new GossipStrategy();}

	public GossipStrategy add(int weight, RpcStrategy strategy) {
		checkArgument(weight >= 0, "weight cannot be negative");
		checkArgument(!strategyToWeight.containsKey(strategy), "withStrategy is already added");

		strategyToWeight.put(strategy, weight);

		return this;
	}

	@Override
	public DiscoveryService getDiscoveryService() {
		return DiscoveryService.combined(strategyToWeight.keySet().stream()
				.map(RpcStrategy::getDiscoveryService)
				.collect(Collectors.toList()));
	}

	@Override
	public RpcSender createSender(RpcClientConnectionPool pool) {
		Map<RpcSender, Integer> senderToWeight = new HashMap<>();
		int totalWeight = 0;
		for (Map.Entry<RpcStrategy, Integer> entry : strategyToWeight.entrySet()) {
			RpcSender sender = entry.getKey().createSender(pool);
			if (sender != null) {
				int weight = entry.getValue();
				senderToWeight.put(sender, weight);
				totalWeight += weight;
			}
		}

		if (totalWeight == 0) {
			return null;
		}

		long randomLong = random.nextLong();
		long seed = randomLong != 0L ? randomLong : 2347230858016798896L;

		return new GossipSender(senderToWeight, seed);
	}

	private static final class GossipSender implements RpcSender {
		private final List<RpcSender> senders;
		private final int[] cumulativeWeights;
		private final int totalWeight;

		private long lastRandomLong;

		GossipSender(Map<RpcSender, Integer> senderToWeight, long seed) {
			assert !senderToWeight.containsKey(null);

			senders = new ArrayList<>(senderToWeight.size());
			cumulativeWeights = new int[senderToWeight.size()];
			int currentCumulativeWeight = 0;
			int currentSender = 0;
			for (Map.Entry<RpcSender, Integer> entry : senderToWeight.entrySet()) {
				currentCumulativeWeight += entry.getValue();
				senders.add(entry.getKey());
				cumulativeWeights[currentSender++] = currentCumulativeWeight;
			}
			totalWeight = currentCumulativeWeight;

			lastRandomLong = seed;
		}

		@Override
		public <I, O> void sendRequest(I request, int timeout, @NotNull Callback<O> cb) {
			lastRandomLong ^= (lastRandomLong << 21);
			lastRandomLong ^= (lastRandomLong >>> 35);
			lastRandomLong ^= (lastRandomLong << 4);
			int currentRandomValue = (int) ((lastRandomLong & Long.MAX_VALUE) % totalWeight);
			int lowerIndex = 0;
			int upperIndex = cumulativeWeights.length;
			while (lowerIndex != upperIndex) {
				int middle = (lowerIndex + upperIndex) / 2;
				if (currentRandomValue >= cumulativeWeights[middle]) {
					lowerIndex = middle + 1;
				} else {
					upperIndex = middle;
				}
			}
			senders.get(lowerIndex).sendRequest(request, timeout, cb);
		}
	}
}
