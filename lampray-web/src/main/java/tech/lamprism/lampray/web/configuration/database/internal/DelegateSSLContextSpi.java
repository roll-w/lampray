package tech.lamprism.lampray.web.configuration.database.internal;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.SecureRandom;

/**
 * @author RollW
 */
public class DelegateSSLContextSpi extends SSLContextSpi {
    private SSLContext delegate;

    @Override
    protected void engineInit(KeyManager[] keyManagers,
                              TrustManager[] trustManagers,
                              SecureRandom secureRandom) throws KeyManagementException {

    }

    @Override
    protected SSLSocketFactory engineGetSocketFactory() {
        return delegate.getSocketFactory();
    }

    @Override
    protected SSLServerSocketFactory engineGetServerSocketFactory() {
        return delegate.getServerSocketFactory();
    }

    @Override
    protected SSLEngine engineCreateSSLEngine() {
        return delegate.createSSLEngine();
    }

    @Override
    protected SSLEngine engineCreateSSLEngine(String s, int i) {
        return delegate.createSSLEngine(s, i);
    }

    @Override
    protected SSLSessionContext engineGetServerSessionContext() {
        return delegate.getServerSessionContext();
    }

    @Override
    protected SSLSessionContext engineGetClientSessionContext() {
        return delegate.getClientSessionContext();
    }
}
