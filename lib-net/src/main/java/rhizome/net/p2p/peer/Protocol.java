package rhizome.net.p2p.peer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.activej.rpc.server.RpcRequestHandler;


public class Protocol {

	private Map<Class<?>, RpcRequestHandler<?, ?>> handlers = new LinkedHashMap<>();
    private List<Class<?>> messageTypes;

}
