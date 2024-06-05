package de.fabiankru.javawings.controller;


import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.fabiankru.javawings.JavaWings;
import de.fabiankru.javawings.model.FileData;
import de.fabiankru.javawings.remote.FileSystemManager;
import de.fabiankru.javawings.ssh.SshManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.sftp.client.SftpClient;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.fabiankru.javawings.JavaWings.WINGS_BASE_PATH;
import static de.fabiankru.javawings.JavaWings.logger;

@RestController
public class FileExplorerController {


    @GetMapping(value = "/api/servers/{server}/files/list-directory")
    public String listDirectory(@PathVariable String server, @RequestParam(name = "directory") String directory) {
        try {
            System.out.println("test");
            List<Document> fileDataList = new ArrayList<>();
            SftpClient sftp = SshManager.getSftpClientSessionRoot(server);
            if (sftp == null) {
                return "HttpStatus.INTERNAL_SERVER_ERROR";
            }
            System.out.println("listDirectory " + directory);
            //                   sftp.readDir(WINGS_BASE_PATH + server + "/" + directory).forEach(dir -> {
            sftp.readDir(directory).forEach(dir -> {
                if (dir == null) return;
                if (dir.getFilename().equals(".") || dir.getFilename().equals("..")) return;
                FileData fileData = new FileData();
                fileData.setName(dir.getFilename());
                fileData.setCreated(new Date(dir.getAttributes().getCreateTime() == null ? 0 : dir.getAttributes().getCreateTime().toMillis()).toString());
                fileData.setModified(new Date(dir.getAttributes().getModifyTime() == null ? 0 : dir.getAttributes().getModifyTime().toMillis()).toString());
                String mimeType = URLConnection.guessContentTypeFromName(dir.getFilename());
                fileData.setMode("writeable");
                fileData.setModeBits("0755");
                fileData.setSize(dir.getAttributes().getSize());
                fileData.setDirectory(dir.getAttributes().isDirectory());
                fileData.setFile(!dir.getAttributes().isDirectory());
                fileData.setSymlink(dir.getAttributes().isSymbolicLink());
                fileData.setMime(mimeType == null ? "text/plain" : mimeType);
                fileDataList.add(fileData.convertToDocument());
            });

            JSONArray jsonArray = new JSONArray();
            fileDataList.forEach(fd -> {
                try {
                    jsonArray.put(new JSONObject(fd.toJson()));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });
            return jsonArray.toString();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while listDirectory", ex);
        }
        return new JSONArray().toString();
    }

    @GetMapping(value = "/api/servers/{server}/files/contents")
    public String getFileContents(@PathVariable String server, @RequestParam(name = "file") String file) {
        try {
            SftpClient sftp = SshManager.getSftpClientSessionRoot(server);

            if (sftp == null) {
                return "HttpStatus.INTERNAL_SERVER_ERROR";
            }
            InputStream s = sftp.read(file);
            return new BufferedReader(
                    new InputStreamReader(s, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while getFileContents", ex);
        }
        return file + " error";
    }


    @PostMapping(value = "/api/servers/{server}/files/write")
    public HttpStatus saveFile(@PathVariable String server, @RequestParam(name = "file") String file, @RequestBody String data
    ) {
        try {
            SftpClient sftp = SshManager.getSftpClientSessionRoot(server);

            if (sftp == null) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
            OutputStream s = sftp.write(file);

            for (String line : data.split("\n")) {
                s.write(line.getBytes());
                s.write('\n');
            }
            s.flush();
            s.close();
            return HttpStatus.OK;

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while saveFile", ex);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @PostMapping(value = "/api/servers/{server}/files/delete")
    public HttpStatus deleteFile(@PathVariable String server, @RequestBody String file) {
        //JSONObject jsonObject = new JSONObject(file);
        Document jsonObject = Document.parse(file);
        // {"root":"\/","files":["backup"]}]

        try {
            SftpClient sftp = SshManager.getSftpClientSessionRoot(server);

            if (sftp == null) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
            String root = jsonObject.getString("root");

            if (root.equals("/")) {
                root = "";
            }
            System.out.println("----------------------------------");
            for (Object s : jsonObject.getList("files", String.class)) {
                //  String f = WINGS_BASE_PATH + server + root + "/" + s.toString();
                String f = root + "/" + s.toString();
                System.out.println("delete " + f);
                FileSystemManager.deleteFiles(sftp, f);
            }
            return HttpStatus.OK;

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while deleteFile", ex);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }


    @PutMapping(value = "/api/servers/{server}/files/rename")
    public HttpStatus renameFile(@PathVariable String server, @RequestBody String data) {
        try {

            Document j = Document.parse(data);
            List<Document> jsonArray = j.getList("files", Document.class);

            SftpClient sftp = SshManager.getSftpClientSessionRoot(server);

            if (sftp == null) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }

            jsonArray.forEach(o -> {
                try {
                    JSONObject jsonObject = new JSONObject(o.toJson());
                    String root = j.getString("root");
                    String name = root + "/" + jsonObject.getString("from");
                    String to = root + "/" + jsonObject.getString("to");
                    sftp.rename(name, to);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            sftp.close();
            return HttpStatus.OK;

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while renameFile", ex);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }


    @PostMapping(value = "/api/servers/{server}/files/create-directory")
    public HttpStatus createDirectory(@PathVariable String server, @RequestBody String data) {
        try {

            JSONObject jsonObject = new JSONObject(data);
            String name = jsonObject.getString("path") + "/" + jsonObject.get("name");
            SftpClient sftp = SshManager.getSftpClientSessionRoot(server);

            if (sftp == null) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }

            sftp.mkdir(name);
            sftp.close();
            return HttpStatus.OK;

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while createDirectory", ex);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @PostMapping(value = "/api/servers/{server}/files/copy")
    public HttpStatus copyFile(@PathVariable String server, @RequestBody String data) {
        try {
            SftpClient sftp = SshManager.getSftpClientSessionRoot(server);

            if (sftp == null) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }

            JSONObject jsonObject = new JSONObject(data);
            String location = jsonObject.getString("location");

            // read old file
            InputStream inputStream = sftp.read(location);
            // write all bytes to new file named old_file  + _copy
            OutputStream s = sftp.write(location + "_copy");
            s.write(inputStream.readAllBytes());
            s.flush();
            sftp.close();
            return HttpStatus.OK;

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while copyFile", ex);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @PostMapping(value = "/upload/file")
    public HttpStatus uploadFile(@RequestParam("token") String token, @RequestParam("directory") String directory, HttpServletRequest request) throws ServletException, IOException {
        DecodedJWT jwt = JWT.require(JavaWings.ALGORITHM)
                .build()
                .verify(token);
        Document d = Document.parse(new String(Base64.getUrlDecoder().decode(jwt.getPayload())));
        String base_dir = d.getString("server_uuid") + directory;
        if (base_dir.endsWith("/")) {
            base_dir = base_dir.substring(0, base_dir.length() - 1);
        }
        SftpClient sftp = SshManager.getSftpClientSessionRoot(d.getString("server_uuid"));

        if (sftp == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        for (Part part : request.getParts()) {
            byte[] bytes = part.getInputStream().readAllBytes();
            OutputStream s = sftp.write(base_dir + "/" + part.getSubmittedFileName());
            s.write(bytes);
            s.flush();
            s.close();
        }
        sftp.close();
        return HttpStatus.OK;
    }

    @GetMapping(value = "/download/file")
    public void downloadFile(@RequestParam(name = "token") String token, HttpServletResponse response) throws IOException {
        DecodedJWT jwt = JWT.require(JavaWings.ALGORITHM)
                .build()
                .verify(token);
        Document d = Document.parse(new String(Base64.getUrlDecoder().decode(jwt.getPayload())));
        String file_path = d.getString("server_uuid") + "/" + d.getString("file_path");
        File file = new File(WINGS_BASE_PATH + file_path);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLengthLong(Files.size(file.toPath()));
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(file.getName(), StandardCharsets.UTF_8)
                .build()
                .toString());

        Files.copy(file.toPath(), response.getOutputStream());
    }

    @PostMapping(value = "/api/servers/{server}/files/chmod")
    public HttpStatus chmod(@PathVariable String server, @RequestBody String data) {
        try {
            SshClient client = SshClient.setUpDefaultClient();
            client.start();

            JSONObject j = new JSONObject(data);
            JSONObject jsonObject = j.getJSONArray("files").getJSONObject(0);
            String root = j.getString("root");
            String name = root + jsonObject.getString("file");
            String mode = jsonObject.getString("mode");
            SftpClient sftp = SshManager.getSftpClientSessionRoot(server);

            if (sftp == null) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }

            sftp.setStat(name,
                    new SftpClient.Attributes()
                            .owner("1000").group("1000")
                            .perms(Short.parseShort(jsonObject.getString("mode"))));
            sftp.close();
            return HttpStatus.OK;

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while chmod", ex);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @PostMapping(value = "/api/servers/{server}/files/compress")
    public HttpStatus compressFile(@PathVariable String server, @RequestBody String data) {
        try {
            SshClient client = SshClient.setUpDefaultClient();
            client.start();
            Document j = Document.parse(data);
            List<String> jsonArray = j.getList("files", String.class);

            SftpClient sftp = SshManager.getSftpClientSessionRoot(server);

            if (sftp == null) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
            try {
                String zipName = j.getString("root") + "/" + System.currentTimeMillis() + ".zip";
                String tmpFile = System.currentTimeMillis() + ".zip";
                ZipFile zipFile = new ZipFile(tmpFile);
                jsonArray.forEach(o -> {
                    String f = o;
                    String root = j.getString("root");
                    String name = root + "/" + f;
                    System.out.printf("compressing %s %n", name);
                    try {
                        ZipParameters zp = new ZipParameters();
                        zp.setFileNameInZip(f);
                        zipFile.addStream(sftp.read(name), zp);
                        OutputStream s = sftp.write(zipName);
                        s.write(new FileInputStream(tmpFile).readAllBytes());
                        s.flush();
                        s.close();
                        zipFile.getFile().delete();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                });
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "error while compressFile", ex);
            }
            sftp.close();
            return HttpStatus.OK;

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while compressFile", ex);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @PostMapping(value = "/api/servers/{server}/files/decompress")
    public HttpStatus decompressFile(@PathVariable String server, @RequestBody String data) {
        //TODO secure
        try {
            // payload={"root":"\/","file":"1701113893292.zip"}]
            JSONObject j = new JSONObject(data);
            String root = j.getString("root");

            if (root.equals("/")) {
                root = "";
            }

            String file = j.getString("file");
            ZipFile zipFile = new ZipFile(WINGS_BASE_PATH + server + root + "/" + file);
            zipFile.extractAll(WINGS_BASE_PATH + server + root);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error while decompressFile", ex);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }


}
