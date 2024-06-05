package de.fabiankru.javawings.remote;

import de.fabiankru.javawings.JavaWings;
import de.fabiankru.javawings.model.api.InstallationScript;
import de.fabiankru.javawings.model.backup.BackupRequest;
import de.fabiankru.javawings.model.PanelServer;
import de.fabiankru.javawings.model.sftp.SftpAnswer;
import org.bson.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

//TODO improve error handling
public class RemoteAPI {

    public static void setServerStatusToInstalled(PanelServer server, boolean installed, boolean reinstalled) {
        setServerStatusToInstalled(server.getUuid(), installed, reinstalled);
    }

     public static void setServerStatusToInstalled(String server, boolean installed, boolean reinstalled) {
        HttpClient client = HttpClient.newHttpClient();

        // json formatted data
        String json = "{\"successful\": " + installed + ",\"reinstall\": " + reinstalled + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .setHeader("User-Agent", "Pterodactyl Wings/v%s (id:%s)".formatted("1", JavaWings.WINGS_SECRET_ID))
                .headers(
                        "Accept", "application/json",
                        "content-type", "application/json",
                        "Authorization", "Bearer %s.%s".formatted(JavaWings.WINGS_SECRET_ID, JavaWings.WINGS_SECRET)
                )
                .uri(URI.create("https://%s/api/remote/servers/%s/install".formatted("panel.fabiankru.de", server)))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .join();
    }

    public static void setBackupState(String backupuuid, BackupRequest backup) {
        HttpClient client = HttpClient.newHttpClient();
        // json formatted data
        String json = BackupRequest.convertToDocument(backup).toJson();
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .setHeader("User-Agent", "Pterodactyl Wings/v%s (id:%s)".formatted("1", JavaWings.WINGS_SECRET_ID))
                .headers(
                        "Accept", "application/json",
                        "content-type", "application/json",
                        "Authorization", "Bearer %s.%s".formatted(JavaWings.WINGS_SECRET_ID, JavaWings.WINGS_SECRET)
                )
                .uri(URI.create("https://%s/api/remote/backups/%s".formatted("panel.fabiankru.de", backupuuid)))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println)
                .join();
    }

    public static Document getServerDetails(String serverId) {
        HttpClient client = HttpClient.newHttpClient();

        // json formatted data

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .setHeader("User-Agent", "Pterodactyl Wings/v%s (id:%s)".formatted("1", JavaWings.WINGS_SECRET_ID))
                .headers(
                        "Accept", "application/json",
                        "content-type", "application/json",
                        "Authorization", "Bearer %s.%s".formatted(JavaWings.WINGS_SECRET_ID, JavaWings.WINGS_SECRET)
                )
                .uri(URI.create("https://%s/api/remote/servers/%s".formatted("panel.fabiankru.de", serverId)))
                .build();


        try {
            String s = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return Document.parse(s);
        } catch (Exception e) {
            JavaWings.logger.severe("Error while parsing server details");
        }
        return null;

    }

    public static InstallationScript getInstallationScript(String serverId) {
        HttpClient client = HttpClient.newHttpClient();

        // json formatted data

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .setHeader("User-Agent", "Pterodactyl Wings/v%s (id:%s)".formatted("1", JavaWings.WINGS_SECRET_ID))
                .headers(
                        "Accept", "application/json",
                        "content-type", "application/json",
                        "Authorization", "Bearer %s.%s".formatted(JavaWings.WINGS_SECRET_ID, JavaWings.WINGS_SECRET)
                )
                .uri(URI.create("https://%s/api/remote/servers/%s/install".formatted("panel.fabiankru.de", serverId)))
                .build();


        try {
            String s = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            Document document = Document.parse(s);
            return new InstallationScript(
                    document.getString("container_image"),
                    document.getString("entrypoint"),
                    document.getString("script")
                            .replaceAll("\\r\\n", "\n")
                            .replaceAll("\\r", "\n")
            );

        } catch (Exception e) {
            JavaWings.logger.severe("Error while parsing server details");
        }
        return null;

    }

    public static SftpAnswer isSftpLoginAllowed(String username, String password) {
        HttpClient client = HttpClient.newHttpClient();

        // json formatted data
        String json = ("{\"type\":\"password\",\"username\":\"%s\",\"password\":" +
                "\"%s\",\"ip\":\"%s\",\"session_id\":\"%s\",\"client_version\":\"%s\",\"server\":\"%s\"}")
                .formatted(username, password, "141.30.222.122", "1", "2", "baee2b9b-8fda-423b-8517-4f2d0cc2844e");
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .setHeader("User-Agent", "Pterodactyl Wings/v%s (id:%s)".formatted("1", JavaWings.WINGS_SECRET_ID))
                .headers(
                        "Accept", "application/json",
                        "content-type", "application/json",
                        "Authorization", "Bearer %s.%s".formatted(JavaWings.WINGS_SECRET_ID, JavaWings.WINGS_SECRET)
                )
                .uri(URI.create("https://%s/api/remote/sftp/auth".formatted("panel.fabiankru.de")))
                .build();
        try {
            HttpResponse<String> s = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new SftpAnswer(s.statusCode() == 200, s.body());
        } catch (Exception e) {
            JavaWings.logger.severe("Error while parsing server details");
        }
        return new SftpAnswer(false, null);
    }


}
