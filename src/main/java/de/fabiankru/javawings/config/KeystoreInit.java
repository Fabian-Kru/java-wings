package de.fabiankru.javawings.config;

import de.fabiankru.javawings.JavaWings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class KeystoreInit {

    @Autowired
    public KeystoreInit(Environment environment) {}


    @Bean
    @Primary
    public ServerProperties serverProperties() {
        final ServerProperties serverProperties = new ServerProperties();
        final Ssl ssl = new Ssl();
        final String keystorePassword = getKeystorePassword();
        ssl.setKeyPassword(keystorePassword);
        System.setProperty("server.ssl.key-store", JavaWings.KEY_STORE);
        System.setProperty("server.ssl.keyStoreType", "PKCS12");
        System.setProperty("server.ssl.keyAlias", "tomcat");
        serverProperties.setSsl(ssl);

        System.setProperty("server.ssl.key-store-password", "");

        return serverProperties;
    }

    private String getKeystorePassword() {
        return null;
    }

}