package de.fabiankru.javawings.remote;

import de.fabiankru.javawings.JavaWings;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

import static de.fabiankru.javawings.JavaWings.logger;

public class FileSystemManager {

    public static void createServerFolder(String serverId) {
        try {
            SshClient client = SshClient.setUpDefaultClient();
            client.start();
            try (ClientSession session = client.connect("root", JavaWings.SERVER_IP, 2234).verify(1000).getSession()) {
                session.setKeyIdentityProvider(new FileKeyPairProvider(Path.of("./java_wings")));
                session.auth().verify(1000);
                // User-specific factory
                try (SftpClient sftp = DefaultSftpClientFactory.INSTANCE.createSftpClient(session)) {
                    sftp.mkdir(serverId);
                    sftp.setStat(serverId, new SftpClient.Attributes().group("1000").owner("1000"));
                    sftp.close();
                    client.stop();
                }
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while creating server folder", ex);
        }

    }

    public static void deleteFiles(SftpClient sftp, String fullPath) throws IOException {
        if(fullPath.equals(".") || fullPath.equals(".."))return;
        if(sftp.stat(fullPath).isDirectory()) {
            for (SftpClient.DirEntry dirEntry : sftp.readDir(fullPath)) {
                if(dirEntry.getFilename().equals(".") || dirEntry.getFilename().equals(".."))continue;
                deleteFiles(sftp, fullPath + "/" + dirEntry.getFilename());
            }
            sftp.rmdir(fullPath);
        } else {
            sftp.remove(fullPath);
        }
    }

    public static void deleteServerFolder(String server) {
        try {
            SshClient client = SshClient.setUpDefaultClient();
            client.start();
            try (ClientSession session = client.connect("root", JavaWings.SERVER_IP, 2234).verify(1000).getSession()) {
                session.setKeyIdentityProvider(new FileKeyPairProvider(Path.of("./java_wings")));
                session.auth().verify(1000);
                // User-specific factory
                try (SftpClient sftp = DefaultSftpClientFactory.INSTANCE.createSftpClient(session)) {
                    FileSystemManager.deleteFiles(sftp, server);
                    sftp.close();
                    client.stop();
                }
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while deleting server folder", ex);
        }
    }
}
