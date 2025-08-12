package tech.lamprism.lampray.system.database.internal.mysql;

import com.mysql.cj.Session;
import com.mysql.cj.conf.PropertyDefinitions;
import com.mysql.cj.conf.PropertyKey;
import com.mysql.cj.conf.PropertySet;
import com.mysql.cj.exceptions.ExceptionFactory;
import com.mysql.cj.exceptions.SSLParamsException;
import com.mysql.cj.log.Log;
import com.mysql.cj.protocol.ServerSession;
import com.mysql.cj.protocol.SocketConnection;
import com.mysql.cj.protocol.StandardSocketFactory;
import com.mysql.cj.util.StringUtils;
import tech.lamprism.lampray.system.database.internal.HostAndPort;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author RollW
 */
public class SSLContextSocketFactory extends StandardSocketFactory {
    private static final Map<HostAndPort, SSLContext> SSL_CONTEXTS = new ConcurrentHashMap<>();

    public static void registerSSLContext(HostAndPort hostAndPort, SSLContext sslContext) {
        if (hostAndPort == null || sslContext == null) {
            throw new IllegalArgumentException("Host and port or SSL context cannot be null.");
        }
        SSL_CONTEXTS.put(hostAndPort, sslContext);
    }

    public static void unregisterSSLContext(HostAndPort hostAndPort) {
        if (hostAndPort == null) {
            throw new IllegalArgumentException("Host and port cannot be null.");
        }
        SSL_CONTEXTS.remove(hostAndPort);
    }


    @Override
    public <T extends Closeable> T connect(String host, int portNumber, PropertySet props, int loginTimeout) throws IOException {
        return super.connect(host, portNumber, props, loginTimeout);
    }

    @Override
    public <T extends Closeable> T performTlsHandshake(SocketConnection socketConnection, ServerSession serverSession) throws IOException {
        return performTlsHandshake(socketConnection, serverSession, null);
    }

    @Override
    public <T extends Closeable> T performTlsHandshake(SocketConnection socketConnection, ServerSession serverSession, Log log) throws IOException {
        HostAndPort hostAndPort = new HostAndPort(socketConnection.getHost(), socketConnection.getPort());
        SSLContext sslContext = SSL_CONTEXTS.get(hostAndPort);
        if (sslContext == null) {
            return super.performTlsHandshake(socketConnection, serverSession, log);
        }

        PropertySet pset = socketConnection.getPropertySet();
        PropertyDefinitions.SslMode sslMode = pset.<PropertyDefinitions.SslMode>getEnumProperty(PropertyKey.sslMode).getValue();

        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(rawSocket, socketConnection.getHost(), socketConnection.getPort(), true);

//        String[] allowedProtocols = getAllowedProtocols(pset, sslSocket.getSupportedProtocols());
//        sslSocket.setEnabledProtocols(allowedProtocols);
//
//        String[] allowedCiphers = getAllowedCiphers(pset, Arrays.asList(sslSocket.getEnabledCipherSuites()));
//        if (allowedCiphers != null) {
//            sslSocket.setEnabledCipherSuites(allowedCiphers);
//        }

        sslSocket.startHandshake();

        // Verify server identity post TLS handshake.
        if (sslMode == PropertyDefinitions.SslMode.VERIFY_IDENTITY) {
            String hostname = socketConnection.getHost();
            if (!StringUtils.isNullOrEmpty(hostname)) {

                SSLSession session = sslSocket.getSession();
                Certificate[] peerCerts = session.getPeerCertificates();

                X509Certificate peerCert;
                if (peerCerts[0] instanceof X509Certificate) {
                    peerCert = (X509Certificate) peerCerts[0];
                } else {
                    throw ExceptionFactory.createException(SSLParamsException.class,
                            "Server identity verification failed. Could not read Server's X.509 Certificate.");

                }
            }
        }

        // TODO
        return (T) sslSocket;
    }

    @Override
    public void beforeHandshake() throws IOException {
        super.beforeHandshake();
    }

    @Override
    public void afterHandshake() throws IOException {
        super.afterHandshake();
    }

    @Override
    public boolean isLocallyConnected(Session sess) {
        return super.isLocallyConnected(sess);
    }

    @Override
    public boolean isLocallyConnected(Session sess, String processHost) {
        return super.isLocallyConnected(sess, processHost);
    }
}
