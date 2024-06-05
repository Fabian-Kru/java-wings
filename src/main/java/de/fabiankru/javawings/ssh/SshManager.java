package de.fabiankru.javawings.ssh;

import de.fabiankru.javawings.JavaWings;
import de.fabiankru.javawings.remote.RemoteAPI;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;

import static de.fabiankru.javawings.JavaWings.WINGS_SECRET;
import static de.fabiankru.javawings.JavaWings.logger;

public class SshManager extends Thread {

    @Override
    public void run() {
        logger.info("Starting SSH-Server..");
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(2234);
        sshd.setShellFactory(new ProcessShellFactory("/bin/sh","-i","-l"));
        sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        VirtualFileSystemFactory v = new VirtualFileSystemFactory();
        v.setUserHomeDir("root", Path.of("/home/test"));
        sshd.setFileSystemFactory(v);

        File authorizedKeys = new File("authorized_keys");
        if(!authorizedKeys.exists()) {
            try {
                authorizedKeys.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //ssh-key login
        sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(new File("authorized_keys").toPath()));

        sshd.setPasswordAuthenticator((username, password, session) -> {
            // TODO use cert
            // internal root login to serve user files safely - username: root_serverUuid
            if(username.startsWith("root_") && password.equalsIgnoreCase(WINGS_SECRET)) {
                String uid = username.split("_")[1];

                String path = RemoteAPI.getServerDetails(uid.split("-")[0])
                        .get("settings", Document.class).getString("uuid");

                v.setUserHomeDir(username, Path.of("/home/test/" + path));
                return true;
            }

            // user connecting with sftp client -> username.shortUuid
            if(username.contains(".") && RemoteAPI.isSftpLoginAllowed(username,password, ((InetSocketAddress) session.getClientAddress()).getHostName()).allowed()) {
                String path = RemoteAPI.getServerDetails(username.substring(username.lastIndexOf(".") + 1))
                        .get("settings", Document.class).getString("uuid");
                v.setUserHomeDir(username, Path.of("/home/test/" + path));
                return true;
            }
            return false;
        });

        //store known host keys
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("host.ser").toPath()));
        try {
            sshd.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static SftpClient getSftpClientSessionRoot(String server) {
        try {
            SshClient client = SshClient.setUpDefaultClient();
            client.start();

            ClientSession session = client
                    .connect("root_" + server, JavaWings.SERVER_IP, 2234)
                    .verify(1000)
                    .getSession();

            session.addPasswordIdentity(WINGS_SECRET);
            session.auth().verify(1000);
            // User-specific factory
            return DefaultSftpClientFactory.INSTANCE.createSftpClient(session);
        } catch (Exception e) {
            logger.info("Error while creating SFTP-Client-Session"+ e) ;
            return null;
        }
    }

}
