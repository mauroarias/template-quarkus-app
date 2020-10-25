package datadog.lib.datadog.trace.common.writer.ddagent.unixdomainsockets;

import jnr.unixsocket.UnixSocketChannel;

import javax.net.SocketFactory;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class UnixDomainSocketFactory extends SocketFactory {
    private final File path;

    public UnixDomainSocketFactory(File path) {
        this.path = path;
    }

    public Socket createSocket() throws IOException {
        UnixSocketChannel channel = UnixSocketChannel.open();
        return new TunnelingUnixSocket(this.path, channel);
    }

    public Socket createSocket(String host, int port) throws IOException {
        Socket result = this.createSocket();
        result.connect(new InetSocketAddress(host, port));
        return result;
    }

    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return this.createSocket(host, port);
    }

    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket result = this.createSocket();
        result.connect(new InetSocketAddress(host, port));
        return result;
    }

    public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException {
        return this.createSocket(host, port);
    }
}
