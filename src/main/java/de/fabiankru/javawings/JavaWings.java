package de.fabiankru.javawings;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import de.fabiankru.javawings.remote.K8sManager;
import de.fabiankru.javawings.remote.ServerManager;
import de.fabiankru.javawings.ssh.SshManager;
import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@SpringBootApplication
public class JavaWings implements WebMvcConfigurer {

    public static String SERVER_IP;
    public static String WINGS_SECRET_ID;
    public static String WINGS_BASE_PATH;
    public static String WINGS_BACKUP_PATH;
    public static String WINGS_SECRET;
    public static String KEY_STORE;
    public static Algorithm ALGORITHM;
    @Getter public static JWTVerifier jwtVerifier;
    public static final Executor executorService = Executors.newCachedThreadPool();

    @Getter
    private static ServerManager serverManager;

    public static final Logger logger = Logger.getLogger("JavaWings");

    public JavaWings() {
        System.out.println("JavaWings started");
        ALGORITHM = Algorithm.HMAC256(JavaWings.WINGS_SECRET);
        jwtVerifier = JWT.require(JavaWings.ALGORITHM).build();
    }

    private static void readConfig() {
        try {
            Yaml yaml = new Yaml();

            // custom config
            File jw_file = new File("/etc/pterodactyl/javawings.yml");

            // if custom config file does not exist, use default config file
            if(!jw_file.exists()) {
                jw_file = new File("/etc/pterodactyl/config.yml");
            }

            InputStream inputStream = new FileInputStream(jw_file);

            Map<String, Object> config_map = yaml.load(inputStream);

            WINGS_SECRET = (String) config_map.get("token");
            WINGS_SECRET_ID = (String) config_map.get("token_id");

            if (config_map.get("api") != null && config_map.get("api") instanceof LinkedHashMap<?,?> api) {
                SERVER_IP = (String) api.get("host");

                if (api.get("ssl") != null && api.get("ssl") instanceof LinkedHashMap<?,?> ssl) {
                    if(ssl.get("enabled").toString().equalsIgnoreCase("true")) {
                        KEY_STORE = (String) ssl.get("p12");
                    }
                }
            }



            if (config_map.get("system") != null && config_map.get("system") instanceof LinkedHashMap<?,?> system) {
                WINGS_BASE_PATH = (String) system.get("data");
                WINGS_BACKUP_PATH = (String) system.get("backup");
            }

        } catch (Exception e) {
            logger.severe("Error while reading config file");
        }
    }


    public static void main(String[] args) throws Exception {
        readConfig();

        new File(WINGS_BACKUP_PATH).mkdir();

        serverManager = new ServerManager();
        /*
          SFTP Server
         */
        Thread t = new SshManager();
        t.start();

        Thread thread = new Thread(() -> {
            try {
                K8sManager.init();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        SpringApplication.run(JavaWings.class, args);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedMethods("*");
    }

}
