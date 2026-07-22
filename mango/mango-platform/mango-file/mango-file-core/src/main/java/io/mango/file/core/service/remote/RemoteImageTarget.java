package io.mango.file.core.service.remote;

import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;

/** A normalized remote target paired with the exact addresses approved for connection. */
public record RemoteImageTarget(URI uri, InetAddress[] addresses) {

    public RemoteImageTarget {
        addresses = Arrays.copyOf(addresses, addresses.length);
    }

    @Override
    public InetAddress[] addresses() {
        return Arrays.copyOf(addresses, addresses.length);
    }
}
