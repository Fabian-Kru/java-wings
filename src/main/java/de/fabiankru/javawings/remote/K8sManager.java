package de.fabiankru.javawings.remote;


import de.fabiankru.javawings.JavaWings;
import de.fabiankru.javawings.model.PanelServer;
import de.fabiankru.javawings.model.api.InstallationScript;
import de.fabiankru.javawings.model.pod.PodStatus;
import io.kubernetes.client.Attach;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

import static de.fabiankru.javawings.JavaWings.logger;

public class K8sManager {

    public static CoreV1Api api;

    public static void init() throws Exception {

        KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new FileReader("config"));
        ApiClient admin = ClientBuilder.kubeconfig(kubeConfig)
                .build();

        Configuration.setDefaultApiClient(admin);

        admin.setDebugging(false);

        while (true) {
            int code = admin.buildCall("/readyz", "GET",
                            null, null, null, Map.of(), Map.of(), null,
                            new String[]{"BearerToken"}, null)
                    .execute().code();
            if (code == 200) break;
            Thread.sleep(500);
        }

        api = new CoreV1Api();

        JavaWings.executorService.execute(() -> {

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    try {
                        List<Pair<V1Pod, PodMetrics>> cpuMetric = Kubectl.top(V1Pod.class, PodMetrics.class)
                                .apiClient(K8sManager.api.getApiClient())
                                .namespace("java-wings")
                                .metric("cpu")
                                .execute();

                        List<Pair<V1Pod, PodMetrics>> ramMetric = Kubectl.top(V1Pod.class, PodMetrics.class)
                                .apiClient(K8sManager.api.getApiClient())
                                .namespace("java-wings")
                                .metric("memory")
                                .execute();

                        cpuMetric.forEach(t -> {
                            String name = t.getKey().getMetadata().getName();
                            if (!name.startsWith("pserver-")) return;
                            String uid = name.substring(8);

                            double cpu = (t.getRight().getContainers().get(0).getUsage().get("cpu").getNumber().doubleValue() * 100.0);
                            double ram = ramMetric.stream()
                                    .filter(s -> t.getKey().getMetadata().getName().equals(s.getKey().getMetadata().getName()))
                                    .findFirst().get().getRight().getContainers().get(0).getUsage().get("memory").getNumber().doubleValue();
                            PanelServer panelServer = JavaWings.getServerManager().getServer(uid);
                            if (panelServer == null) return;
                            long millis = Instant.now().toEpochMilli() - (t.getKey().getMetadata().getCreationTimestamp().toEpochSecond() * 1000);
                            panelServer.sendStats(ram, cpu, millis);
                        });

                    } catch (Exception e) {
                        // logger.info(e.getMessage());
                        // ingore
                    }


                }
            }, 0, 1000);

        });

    }

    public static String getLogs(String uid) throws Exception {
        String s = null;
        try {
            s = api.readNamespacedPodLog("pserver-" + uid, "java-wings", "mc", false, false, null, null, null, null, null, null);
        } catch (Exception ignored) {
        }
        return s;
    }

    public static PodStatus getPodStatus(String uid) {
        try {
            V1Pod pod = api.readNamespacedPod("pserver-" + uid, "java-wings", null);
            if (pod.getStatus().getPhase().equalsIgnoreCase("pending")) {
                return PodStatus.PENDING;
            } else if (pod.getStatus().getPhase().equalsIgnoreCase("running")) {
                return PodStatus.RUNNING;
            } else if (pod.getStatus().getPhase().equalsIgnoreCase("succeeded")) {
                return PodStatus.SUCCEEDED;
            }
        } catch (Exception ex) {
            return PodStatus.UNKNOWN;
        }
        return PodStatus.UNKNOWN;
    }

    public static void createPod(String uid, Document settings) {

        try {
            // set metadata
            V1Pod pod = new V1Pod();
            V1ObjectMeta metadata = new V1ObjectMeta();
            metadata.setName("pserver-" + uid);
            metadata.putLabelsItem("server", "pserver");
            metadata.putLabelsItem("server-id", uid);
            metadata.setNamespace("java-wings");
            pod.setMetadata(metadata);
            V1Container container = new V1Container();
            container.setName("mc");

            Document container_settings = settings.get("container", Document.class);

            settings.get("environment", Document.class).forEach((key, value) -> {
                container.addEnvItem(new V1EnvVar().name(key).value(value.toString()));
            });


            container.setImage(container_settings.getString("image"));
            container.setImagePullPolicy("Always");
            container.setStdin(true);
            container.setTty(true);

            Document allocations = settings.get("allocations", Document.class);
            Document default_allocation = allocations.get("default", Document.class);
            Document build = settings.get("build", Document.class);
            int container_port = default_allocation.getInteger("port");
            container.addEnvItem(new V1EnvVar().name("LC_ALL").value("C.UTF-8"));
            container.addEnvItem(new V1EnvVar().name("LANG").value("C.UTF-8"));
            container.addEnvItem(new V1EnvVar().name("STARTUP").value(settings.getString("invocation")));
            container.addEnvItem(new V1EnvVar().name("SERVER_IP").value(default_allocation.getString("ip")));
            container.addEnvItem(new V1EnvVar().name("SERVER_PORT").value(container_port + ""));
            container.addEnvItem(new V1EnvVar().name("SERVER_MEMORY")
                    .value(build.getInteger("memory_limit").toString()));

            // for default SERVER_IP and SERVER_PORT (tcp & udp)

            List<V1ContainerPort> ports = new ArrayList<>();

            ports.add(new V1ContainerPort().protocol("TCP")
                    .containerPort(container_port)
                    .hostPort(container_port));


            ports.add(new V1ContainerPort().protocol("UDP")
                    .containerPort(container_port)
                    .hostPort(container_port));


            container.setPorts(ports); //TODO map port
            //container volume
            container.addVolumeMountsItem(new V1VolumeMount().name("server").mountPath("/home/container"));

            V1PodSpec podSpec = new V1PodSpec();
            podSpec.setRestartPolicy("Never");
            podSpec.addContainersItem(container);


            // set pod cpu ressources
            V1ResourceRequirements resourceRequirements = new V1ResourceRequirements();
            resourceRequirements
                    .putRequestsItem("cpu", new Quantity((build.getInteger("cpu_limit") * 10) + "m"));
            // and ram ressources
            resourceRequirements
                    .putRequestsItem("memory", new Quantity(build.getInteger("memory_limit") + "Mi"));
            container.setResources(resourceRequirements);

            // Always create volume for server
            Map<String, String> flexVolumeOptions = new HashMap<>();
            flexVolumeOptions.put("networkPath", "//" + JavaWings.SERVER_IP + "/test/" + uid);
            flexVolumeOptions.put("mountOptions", "dir_mode=0777,file_mode=0777,noperm");
            podSpec.setVolumes(List.of(
                    new V1Volume()
                            .name("server")
                            .flexVolume(new V1FlexVolumeSource().driver("fstab/cifs")
                                    .fsType("cifs")
                                    .options(flexVolumeOptions)
                                    .secretRef(new V1LocalObjectReference().name("cifs-secret")))
            ));

            pod.setSpec(podSpec);
            logger.log(Level.WARNING, "creating pod");
            V1Pod v1pod = api.createNamespacedPod("java-wings", pod, null, null, null, null);
            V1Service v1Service = new V1Service();
            V1ServiceSpec v1ServiceSpec = new V1ServiceSpec();
            v1ServiceSpec.setType("NodePort");
            v1ServiceSpec.setSelector(Map.of("server-id", uid));
            v1ServiceSpec.setPorts(List.of(
                    new V1ServicePort().port(container_port)
                            .name("tcp-" + container_port)
                            .nodePort(container_port)
                            .protocol("TCP"),
                    new V1ServicePort().port(container_port)
                            .name("udp-" + container_port)
                            .nodePort(container_port)
                            .protocol("UDP")
            ));

            v1Service.setMetadata(new V1ObjectMeta().name("pserver-service-" + uid).
                    ownerReferences(List.of(
                            new V1OwnerReference()
                                    .apiVersion("v1")
                                    .kind("Pod")
                                    .name("pserver-" + uid)
                                    .uid(v1pod.getMetadata().getUid())
                    )));
            v1Service.setSpec(v1ServiceSpec);
            api.createNamespacedService("java-wings", v1Service, null, null, null, null);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }

    }

    public static void stopPod(String s) {
        logger.log(Level.WARNING, "deleting pod " + s);
        try {
            api.deleteNamespacedPod("pserver-" + s, "java-wings", null, null, null, null, null, new V1DeleteOptions());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }

    public static void killPod(String s) {
        logger.log(Level.WARNING, "killing pod " + s);
        try {
            api.deleteNamespacedPod("pserver-" + s, "java-wings", null, null, null, null, null, new V1DeleteOptions());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }


    public static boolean exists(String podName) {
        try {
            V1PodList list = api.listNamespacedPod("java-wings", null, null, null, null, null, null, null, null, null, null, null);
            if (list.getItems().stream()
                    .anyMatch(t -> t.getMetadata() != null
                            && t.getMetadata().getName() != null
                            && t.getMetadata().getName().equals(podName))) {
                return true;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        return false;
    }

    public static void runCommandIfServerExits(String server, String command) {
        if (!exists("pserver-" + server)) {
            return;
        }
        JavaWings.executorService.execute(() -> {
            try {
                final Attach.AttachResult result = new Attach()
                        .attach("java-wings", "pserver-" + server, true);
                OutputStream stream = result.getStandardInputStream();
                stream.write(command.getBytes());
                stream.write('\n');
                stream.flush();
                stream.close();
                result.close();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage());
            }
        });
    }

    public static void createInstallPod(String uid, Document settings) throws ApiException {
        // set metadata
        V1Pod pod = new V1Pod();
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName("pserver-" + uid);
        metadata.putLabelsItem("server", "pserver");
        metadata.putLabelsItem("server_state", "installing");
        metadata.setNamespace("java-wings");
        pod.setMetadata(metadata);

        V1Container container = new V1Container();
        container.setName("mc");

        // script data
        InstallationScript script = RemoteAPI.getInstallationScript(uid);

        if (script == null) {
            logger.log(Level.SEVERE, "No installation script found for server " + uid);
            return;
        }
        // needed to resolve server file name in script
        settings.get("environment", Document.class).forEach((key, value) -> {
            container.addEnvItem(new V1EnvVar().name(key).value(value.toString()));
        });

        container.setImage(script.getContainerImage());
        container.setImagePullPolicy("Always");
        container.setStdin(true);
        container.setTty(true);

        // create file from script
        File file = new File(JavaWings.WINGS_BASE_PATH + "/" + uid + "/install.sh");

        try {
            file.delete();
            if (!file.exists() && file.createNewFile()) {
                FileWriter writer = new FileWriter(file);
                writer.write(script.getScript());
                writer.flush();
                writer.close();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while creating install script");
        }


        //container volume
        container.addVolumeMountsItem(new V1VolumeMount().name("server").mountPath("/mnt/server"));

        // set container entrypoint TODO change
        // https://github.com/pterodactyl/wings/blob/f1c5bbd42d423986e7017b4f3c43057a1b7d1717/server/install.go#L116
        container.setCommand(List.of(script.getEntrypoint(), "/mnt/server/install.sh"));


        V1PodSpec podSpec = new V1PodSpec();
        podSpec.setRestartPolicy("Never");
        podSpec.addContainersItem(container);


        // Always create volume for server
        Map<String, String> flexVolumeOptions = new HashMap<>();
        flexVolumeOptions.put("networkPath", "//" + JavaWings.SERVER_IP + "/test/" + uid);
        flexVolumeOptions.put("mountOptions", "dir_mode=0777,file_mode=0777,noperm");
        podSpec.setVolumes(List.of(
                new V1Volume()
                        .name("server")
                        .flexVolume(new V1FlexVolumeSource().driver("fstab/cifs")
                                .fsType("cifs").options(flexVolumeOptions)
                                .secretRef(new V1LocalObjectReference().name("cifs-secret")))
        ));

        pod.setSpec(podSpec);
        logger.log(Level.WARNING, "creating pod for installation");
        api.createNamespacedPod("java-wings",
                pod,
                null,
                null,
                null,
                null
        );

    }
}
