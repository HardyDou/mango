package io.mango.file.core.service.remote;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Resolves every address currently associated with a remote host. */
@FunctionalInterface
public interface RemoteHostResolver {

    /**
     * Resolves all addresses for a normalized ASCII host name.
     *
     * @param host normalized host
     * @return every resolved address
     * @throws UnknownHostException when the host cannot be resolved
     */
    InetAddress[] resolve(String host) throws UnknownHostException;
}
