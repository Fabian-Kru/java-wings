package de.fabiankru.javawings.model;


import de.fabiankru.javawings.JavaWings;
import de.fabiankru.javawings.model.api.*;
import de.fabiankru.javawings.model.backup.BackupRequest;
import de.fabiankru.javawings.model.jwt.JWTPermissions;
import de.fabiankru.javawings.model.pod.PodStatus;
import de.fabiankru.javawings.remote.K8sManager;
import de.fabiankru.javawings.remote.RemoteAPI;
import io.kubernetes.client.Attach;
import io.kubernetes.client.openapi.ApiException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;

import static de.fabiankru.javawings.JavaWings.logger;

@Getter
@Setter
public class PanelServer {

    @Setter(AccessLevel.NONE)
    private Document authData;
    private String uuid;
    private boolean online = false;
    private double cpu = 0;
    private double ram = 0;

    // Configuration
    private Configuration configuration = new Configuration();
    private ResourceUsage resourceUsage = new ResourceUsage();
    private List<PanelServerSession> sessions = new ArrayList<>();

    //Settings from panel
    private Document settings;

    private boolean installing = false;

    public PanelServer(String uuid, Optional<WebSocketSession> session, Optional<Document> authData) {

        if(session.isPresent() && authData.isPresent()) {
            List<String> permissions = authData.get().getList("permissions", String.class);
            List<JWTPermissions> jwtPermissions = new ArrayList<>();
            permissions.forEach(s -> jwtPermissions.add(JWTPermissions.getByPermission(s)));
            this.sessions.add(new PanelServerSession(jwtPermissions, session.get()));
        }

        this.uuid = uuid;

        DefaultMapping defaultMapping = new DefaultMapping();
        defaultMapping.setIp("127.0.0.1");
        defaultMapping.setPort(25565);

        resourceUsage.setDiskBytes(0L);
        resourceUsage.setState("running");

        Allocations allocations = new Allocations();
        allocations.setMappings(Map.of("25565", List.of(25565)));
        allocations.setDefaultMapping(defaultMapping);
        ConfigurationMeta configurationMeta = new ConfigurationMeta();
        configurationMeta.setDescription("Test");
        configurationMeta.setName("Test");

        configuration.setMeta(configurationMeta);
        configuration.setAllocations(allocations);
        configuration.setBuild(new Limits());
        configuration.setMounts(List.of(new Mount().toDocument()));
        configuration.setEgg(new EggConfiguration());

        settings = Objects.requireNonNull(RemoteAPI.getServerDetails(uuid))
                .get("settings", Document.class);

    }

    public void sendMessage(TextMessage message, WebSocketSession session)  {
        JavaWings.executorService.execute( () -> {
            synchronized (session) {
                try {
                    if(session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Error while sending message", exception);
                }
            }
        });



    }

    public synchronized void connect(WebSocketSession session) {
       JavaWings.executorService.execute( () -> {
           logger.info("connect called");

           Attach attachment = new Attach();

           // Wait for Pending pods to start.. or even nodes to start and schedule pods
           int sleepTime = 0;
           while (K8sManager.getPodStatus(getUuid()) == PodStatus.PENDING && sleepTime < 100*1000) {
               try {
                   sleepTime += 10*1000;
                   Thread.sleep(10*1000);
               } catch (InterruptedException e) {
                   throw new RuntimeException(e);
               }
           }
           // Attach to the pod and start reading the logs

               try {
                   Attach.AttachResult result = attachment.newConnectionBuilder("java-wings", "pserver-"+getUuid()).setStdout(true).connect();
                   Scanner scanner = new Scanner(result.getStandardOutputStream());

                   if(K8sManager.getPodStatus(getUuid()) == PodStatus.RUNNING) {
                       sendMessage(new TextMessage(new Document("event", "status").append("args",List.of("online")).toJson()), session);
                   }
                   sendOldLogs(session);
                   online = true;
                   boolean isStopping = false;
                   while(scanner.hasNextLine()) {
                       String s = scanner.nextLine();
                       if(s.startsWith(">....")) {
                           continue;
                       }

                       if(s.toLowerCase().contains("stopping server") && !isStopping) {
                           isStopping = true;
                           logger.log(java.util.logging.Level.INFO, "Server is stopping " + getUuid());
                           sendMessage(new TextMessage(new Document("event", "status").append("args", List.of("stopping")).toJson()), session);
                       }

                       sendMessage(
                               new TextMessage(
                                       new Document("event", "console output")
                                               .append("args", List.of(s)).toJson()
                               ),
                               session
                       );
                   }
               } catch (Exception ex) {
                   logger.log(java.util.logging.Level.SEVERE, "Error while connecting to pod", ex);
               }

           sendMessage(new TextMessage(new Document("event", "status").append("args", List.of("offline")).toJson()), session);
           online = false;
           logger.info("CLOSED attachment");
       });
    }

    public void close() {
        logger.info("SERVER STREAM LOST " + getUuid());
    //TODO nicht löschen sinnvoll? JavaWings.getServerManager().removeServer(this);
    }

    public void destoryPod(WebSocketSession session) {
        if(!hasPermissions(JWTPermissions.PermissionSendPowerStop, session)) {
            logger.log(java.util.logging.Level.INFO, "No permission to stop pod");
            return;
        }
        online = false;
        K8sManager.stopPod(getUuid());
    }

    public void sendStats(double ram, double cpu, long millis) {
        try {
            setCpu(cpu);
            setRam(ram);
            //TODO Cluster kann das noch nicht ;(
            for(PanelServerSession panelServerSession : sessions) {
                sendMessage(new TextMessage(new Document("event", "stats")
                        .append("args", List.of(
                                new StatsData(ram, cpu, -1, -1, -1, millis).toDocument().toJson()
                        ))
                        .toJson(), true), panelServerSession.getSession());
            }
           // System.out.println("stats" + ram + " " + cpu);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while sending stats", ex);
        }
    }

    public void setAuthData(Document authData, WebSocketSession session) {
        this.authData = authData;

        if(sessions.stream().noneMatch(t -> t.getSession().getId().equals(session.getId()))) {
            System.out.println("Added new session " + session.getId());
            sessions.add(new PanelServerSession(new ArrayList<>(), session));
        }

        if(authData.containsKey("permissions")) {
            List<String> permissions = authData.getList("permissions", String.class);
            List<JWTPermissions> jwtPermissions = new ArrayList<>();
            permissions.forEach(s -> jwtPermissions.add(JWTPermissions.getByPermission(s)));
            getSessions()
                    .stream()
                    .filter(t -> t.getSession().getId().equals(session.getId())).findFirst()
                    .ifPresent(pss -> {
                        System.out.println("Set permissions" +  jwtPermissions + " " + pss.getSession().getId());
                        pss.setJwtPermissions(jwtPermissions);
                    });

            logger.log(java.util.logging.Level.INFO, "Permissions: " + jwtPermissions);
        }

    }

    public void sendOldLogs(WebSocketSession session) {
        JavaWings.executorService.execute( () -> {
            try {
                String oldLog = K8sManager.getLogs(getUuid());
                if(oldLog != null) {
                    List<String> list = new ArrayList<>(Arrays.stream(oldLog.split("\n")).toList());
                    list.sort(Collections.reverseOrder());
                    list.forEach(s -> {
                        if(!s.startsWith(">...."))
                        sendMessage(new TextMessage(new Document("event", "console output").
                                append("args", List.of(s)).toJson()), session);
                    });
                    if(!list.isEmpty()) {
                        sendMessage(new TextMessage(new Document("event", "status").append("args",List.of("online")).toJson()), session);
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error while sending old logs", ex);
            }
        });
    }

    public void sendCommand(String command, WebSocketSession session) {

        if(!hasPermissions(JWTPermissions.PermissionSendCommand, session)) {
            logger.log(java.util.logging.Level.INFO, "No permission to send command");
            return;
        }

        JavaWings.executorService.execute( () -> {
            try {
                final Attach.AttachResult result = new Attach()
                        .attach("java-wings", "pserver-"+getUuid(), true);
                OutputStream stream = result.getStandardInputStream();
                stream.write(command.getBytes());
                stream.write('\n');
                stream.flush();
                stream.close();
                result.close();
            } catch (Exception ex) {
                sendMessage(new TextMessage(new Document("event", "status")
                        .append("args", List.of("offline")).toJson()), session);
                logger.log(Level.SEVERE, "Error while sending command", ex);
            }
        });

    }

    public void restartPod(WebSocketSession session) {

        if(!hasPermissions(JWTPermissions.PermissionSendPowerRestart, session)) {
            logger.log(java.util.logging.Level.INFO, "No permission to restart pod");
            return;
        }

        JavaWings.executorService.execute( () -> {
            destoryPod(session);
            try {
                Thread.sleep(4000);
                createPod(session);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        });
    }

    public void createPod(WebSocketSession session) {

        if(isInstalling())  {
            logger.log(java.util.logging.Level.INFO, "installing! no start!");
            return;
        }

        if(!hasPermissions(JWTPermissions.PermissionSendPowerStart, session)) {
            logger.log(java.util.logging.Level.INFO, "No permission to start pod");
            return;
        }
        //TODO make thread safe, clients could start multiply pods
        online = true;
        sendMessage(new TextMessage(new Document("event", "status").append("args",List.of("online")).toJson()), session);
        try {
            K8sManager.createPod(getUuid(), settings);
            Thread.sleep(2000);
            connect(session);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while creating pod", ex);
        }
    }

    public void sendBackupStatusEvent(String uuid, BackupRequest b) {

        logger.info("Backup completed");

        for (PanelServerSession panelServerSession : sessions) {
            sendMessage(new TextMessage(
                    new Document("event", "backup completed:" + uuid)
                            .append("args",
                                    new Document("uuid", uuid)
                                            .append("is_successful", true)
                                            .append("checksum", "")
                                            .append("checksum_type", "sha1")
                                            .append("file_size", 1).toJson()
                            ).toJson()

            ),  panelServerSession.getSession());
        }

        JavaWings.executorService.execute( () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            RemoteAPI.setBackupState(uuid, b);
        });

    }

    public void sendRestoreFinished() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
       for (PanelServerSession panelServerSession : sessions) {
           sendMessage(new TextMessage(
                   new Document("event", "backup restore completed")
                           .append("args",List.of("")).toJson()
           ), panelServerSession.getSession());
       }
    }

    public ServerPowerState getPowerState() {
        return online ? ServerPowerState.RUNNING : ServerPowerState.OFFLINE;
    }

    public NetworkStats getNetworkStats() {
        NetworkStats network = new NetworkStats();
        network.setRxBytes(100L);
        network.setTxBytes(2000L);
        return network;
    }

    public EnviormentStats getEnviormentStats() {
        EnviormentStats enviormentStats = new EnviormentStats();
        enviormentStats.setMemoryBytes((long) getRam());
        enviormentStats.setMemoryLimitBytes((long) getRam());
        enviormentStats.setCpuAbsolute((long) getCpu());
        enviormentStats.setNetwork(getNetworkStats());
        enviormentStats.setUptime(0L);
        return enviormentStats;
    }




    public APIResponse getAPIReponse() {
        APIResponse apiResponse = new APIResponse();
        apiResponse.setConfiguration(configuration);
        apiResponse.setSuspended(false);
        apiResponse.setState(isOnline() ? "running" : "offline");

        resourceUsage.setState(isOnline() ? "running" : "offline");
        resourceUsage.setEnviormentStats(getEnviormentStats());

        File file = new File(JavaWings.WINGS_BASE_PATH + uuid + "/");

        resourceUsage.setDiskBytes(getFileSize(file));
        apiResponse.setUtilization(resourceUsage);
        return apiResponse;
    }

    private long getFileSize(File file) {
        if(file.isDirectory()) {
            return Arrays.stream(file.listFiles()).mapToLong(this::getFileSize).sum();
        }
        return file.length();
    }

    public boolean hasPermissions(JWTPermissions permission, WebSocketSession session) {
        PanelServerSession panelServerSession = PanelServerSession.getByWebSocketSession(session);

        if(panelServerSession == null) {
            return false;
        }

        return
                panelServerSession.getJwtPermissions().contains(permission) ||
                panelServerSession.getJwtPermissions().contains(JWTPermissions.ALL);
    }

    public synchronized void  startInstallingPod() throws ApiException {
        if(installing) {
            return;
        }
        installing = true;
        K8sManager.createInstallPod(getUuid(), settings);

    }


}
