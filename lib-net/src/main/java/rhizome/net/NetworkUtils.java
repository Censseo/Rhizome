package rhizome.net;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import io.activej.eventloop.Eventloop;
import io.activej.http.AsyncHttpClient;
import io.activej.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class NetworkUtils {

    private NetworkUtils() {}

    public static String computeAddress(String ip, int port, boolean firewall) {
        if (firewall) {
            return "http://undiscoverable";
        }

        var eventloop = Eventloop.create();
        var httpClient = AsyncHttpClient.create(eventloop);
        String address = "";
        
        if (ip.isEmpty()) {
            boolean found = false;
            List<String> lookupServices = Arrays.asList(
                "http://checkip.amazonaws.com", 
                "http://icanhazip.com", 
                "http://ifconfig.co", 
                "http://wtfismyip.com/text", 
                "http://ifconfig.io"
            );

            for (String lookupService : lookupServices) {
                try {
                    String rawUrl = eventloop.submit(() ->
				    httpClient.request(HttpRequest.get(lookupService))
						.then(response -> response.loadBody())
						.map(body -> body.getString(UTF_8))).get();
                    ip = rawUrl.trim();
                    if (isValidIPv4(ip)) {
                        address = "http://" + ip + ":" + port;
                        found = true;
                        break;
                    }
                } catch (ExecutionException | InterruptedException e) {
                    log.error(null, e);
                }
            }

            if (!found) {
                log.error("Could not determine current IP address");
            }
        } else {
            address = ip + ":" + port;
        }
        return address;
    }

    public static boolean isValidIPv4(String ip) {
        // String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        var ipPattern = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        return ip.matches(ipPattern);
    }

    /**
     * Get the IP addresses for a given domain name
     * @param domainName
     * @return
     * @throws UnknownHostException
     */
    public static List<String> getIPAddresses(String domainName) {
        List<String> ipAddresses = new ArrayList<>();

        try {
            InetAddress[] inetAddresses = InetAddress.getAllByName(domainName);
            
            for (InetAddress inetAddress : inetAddresses) {
                ipAddresses.add(inetAddress.getHostAddress());
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return ipAddresses;
    }
}
