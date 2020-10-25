package datadog.lib.datadog.trace.common.writer.ddagent.unixdomainsockets;

import jnr.unixsocket.UnixSocket;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

final class TunnelingUnixSocket extends UnixSocket {
    private final File path;
    private InetSocketAddress inetSocketAddress;

    TunnelingUnixSocket(File path, UnixSocketChannel channel) {
        super(channel);
        this.path = path;
    }

    TunnelingUnixSocket(File path, UnixSocketChannel channel, InetSocketAddress address) {
        this(path, channel);
        this.inetSocketAddress = address;
    }

    public void connect(SocketAddress endpoint) throws IOException {
        this.inetSocketAddress = (InetSocketAddress)endpoint;
        super.connect(new UnixSocketAddress(this.path), 0);
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        this.inetSocketAddress = (InetSocketAddress)endpoint;
        super.connect(new UnixSocketAddress(this.path), timeout);
    }

    public InetAddress getInetAddress() {
        return this.inetSocketAddress.getAddress();
    }
}
