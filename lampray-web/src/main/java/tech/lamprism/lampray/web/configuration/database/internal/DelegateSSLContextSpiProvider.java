package tech.lamprism.lampray.web.configuration.database.internal;

import java.security.Provider;
import java.security.Security;

/**
 * @author RollW
 */
public class DelegateSSLContextSpiProvider extends Provider {
    protected DelegateSSLContextSpiProvider() {
        super("DelegateSSLContextSpi", "1.0", "Delegate SSL Context SPI Provider");
        put("SSLContext.TLS", "tech.lamprism.lampray.web.configuration.database.internal.DelegateSSLContextSpi");
        put("SSLContext.TLSv1.2", "tech.lamprism.lampray.web.configuration.database.internal.DelegateSSLContextSpi");
        put("SSLContext.TLSv1.3", "tech.lamprism.lampray.web.configuration.database.internal.DelegateSSLContextSpi");
        put("SSLContext.TLSv1", "tech.lamprism.lampray.web.configuration.database.internal.DelegateSSLContextSpi");
        put("SSLContext.TLSv1.1", "tech.lamprism.lampray.web.configuration.database.internal.DelegateSSLContextSpi");
    }

    static {
        Security.addProvider(new DelegateSSLContextSpiProvider());
    }
}
