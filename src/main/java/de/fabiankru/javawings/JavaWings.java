package de.fabiankru.javawings;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import de.fabiankru.javawings.remote.K8sManager;
import de.fabiankru.javawings.remote.ServerManager;
import de.fabiankru.javawings.ssh.SshManager;
import lombok.Getter;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.FileWriter;
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
            File file = new File("config.json");
            if (!file.exists()) {
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                Document document = new Document();
                writer.write(document.toJson());
                writer.flush();
                writer.close();
            }
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            Document configuration = Document.parse(content);
            WINGS_SECRET_ID = configuration.getString("WINGS_SECRET_ID");
            WINGS_SECRET = configuration.getString("WINGS_SECRET");
            SERVER_IP = configuration.getString("SERVER_IP");
            WINGS_BASE_PATH = configuration.getString("WINGS_BASE_PATH");
            WINGS_BACKUP_PATH = configuration.getString("WINGS_BACKUP_PATH");

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
