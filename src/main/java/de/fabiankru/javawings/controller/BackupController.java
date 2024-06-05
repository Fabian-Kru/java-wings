package de.fabiankru.javawings.controller;


import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.fabiankru.javawings.JavaWings;
import de.fabiankru.javawings.model.backup.BackupRequest;
import de.fabiankru.javawings.model.PanelServer;
import jakarta.servlet.http.HttpServletResponse;
import net.lingala.zip4j.ZipFile;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.logging.Level;

import static de.fabiankru.javawings.JavaWings.*;

@RestController
public class BackupController {

    @GetMapping(value = "/download/backup")
    public void downloadBackup(@RequestParam(name = "token") String token, HttpServletResponse response) throws IOException {
        DecodedJWT jwt = JWT.require(JavaWings.ALGORITHM)
                .build()
                .verify(token);

        Document d = Document.parse(new String(Base64.getUrlDecoder().decode(jwt.getPayload())));
        String backup_uuid = d.getString("backup_uuid");
        File file = new File(WINGS_BACKUP_PATH + backup_uuid + ".zip");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLengthLong(Files.size(file.toPath()));
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(backup_uuid + ".zip", StandardCharsets.UTF_8)
                .build()
                .toString());

        Files.copy(file.toPath(), response.getOutputStream());
    }

    @PostMapping(value = "/api/servers/{server}/backup/{backup}/restore")
    public HttpStatus restoreBackup(@PathVariable String server, @PathVariable String backup) {
        logger.log(Level.INFO, "restoring backup " + backup + " " + server);
        try {
            ZipFile zipFile = new ZipFile(WINGS_BACKUP_PATH + backup + ".zip");
            zipFile.extractAll(WINGS_BASE_PATH + server);
        } catch (Exception ex) {
            logger.log(Level.SEVERE,"Error while restoring backup " + backup + " @ " + server, ex);
        }
        PanelServer panelServer = JavaWings.getServerManager().getServer(server);
        if (panelServer == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        JavaWings.executorService.execute(panelServer::sendRestoreFinished);
        return HttpStatus.OK;
    }

    @DeleteMapping(value = "/api/servers/{server}/backup/{backup}")
    public HttpStatus deleteBackup(@PathVariable String server, @PathVariable String backup) {
        logger.log(Level.INFO, "deleting backup " + backup + " " + server);
        try {
            //TODO delete Backup from Panel
            Files.delete(Path.of(WINGS_BACKUP_PATH + backup + ".zip"));
        } catch (Exception ex) {
            logger.log(Level.SEVERE,"Error while deleting backup " + backup + " @ " + server, ex);
        }
        return HttpStatus.OK;
    }

    @PostMapping(value = "/api/servers/{server}/backup")
    public HttpStatus backup(@PathVariable String server, @RequestBody String data) throws JSONException {
        JSONObject jsonpObject = new JSONObject(data);
        PanelServer panelServer = JavaWings.getServerManager().getServer(server);

        if (panelServer == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        logger.log(Level.INFO,"Creating backup " + jsonpObject.getString("uuid"));

        String copyFrom = WINGS_BASE_PATH + server;
        String copyTo = WINGS_BACKUP_PATH + jsonpObject.getString("uuid") + ".zip";
        //TODO calc checksum
        try {
            ZipFile zipFile = new ZipFile(copyTo);
            File baseDir = new File(copyFrom);
            for (File file : baseDir.listFiles()) {
                if (file.isDirectory()) {
                    zipFile.addFolder(file);
                } else {
                    zipFile.addFile(file);
                }
            }

            BackupRequest b = new BackupRequest();
            b.setParts(null);
            b.setChecksum("sha1");
            b.setChecksumType("sha1");
            b.setSize(zipFile.getFile().length());
            b.setSuccessful(true);
            panelServer.sendBackupStatusEvent(jsonpObject.getString("uuid"), b);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while creating backup " + jsonpObject.getString("uuid"), ex);
        }

        return HttpStatus.OK;
    }


}
