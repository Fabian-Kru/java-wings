package de.fabiankru.javawings.controller;


import de.fabiankru.javawings.JavaWings;
import de.fabiankru.javawings.model.PanelServer;
import de.fabiankru.javawings.model.pod.PodStatus;
import de.fabiankru.javawings.remote.FileSystemManager;
import de.fabiankru.javawings.remote.K8sManager;
import de.fabiankru.javawings.remote.RemoteAPI;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Level;

import static de.fabiankru.javawings.JavaWings.logger;

@RestController
public class SystemController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        return "error get";
    }

    @PostMapping("/error")
    public String handleError2() {
        return "error post";
    }

    @PostMapping("/api/servers")
    public HttpStatus postCreateServer(@RequestBody String body) throws JSONException {
        // {"uuid":"4cba768f-8fe9-4d1e-bef5-d1a3112dec1a","start_on_completion":true}
        // create server and update installtion status
        JSONObject jsonObject = new JSONObject(body);
        //non blocking TODO make thread safe
        JavaWings.executorService.execute(() -> {
            try {
                String server = jsonObject.getString("uuid");
                FileSystemManager.createServerFolder(jsonObject.getString("uuid"));
                System.out.println("installing  " + server);
                PanelServer panelServer = JavaWings.getServerManager().createServer(server, null);
                panelServer.startInstallingPod();
                System.out.println("installing  " + server + " done");
                RemoteAPI.setServerStatusToInstalled(jsonObject.getString("uuid"), true, false);
                Thread.sleep(1000);
                System.out.println("Status:");
                System.out.println(K8sManager.getPodStatus(server));
                while (K8sManager.getPodStatus(server) != PodStatus.SUCCEEDED) {
                    System.out.println("Waiting for Pod to succeed");
                    Thread.sleep(1000);
                }
                K8sManager.killPod(server);
                System.out.println("Pod succeeded");
                // ensure pod is stopped
                Thread.sleep(2000);
                panelServer.setInstalling(false);
                // create manual server -> panel keeps connected
                K8sManager.createPod(server, panelServer.getSettings());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error while creating server", e);
            }
            //TODO create Server
        });

        return HttpStatus.ACCEPTED;
    }

    @GetMapping(value = "/api/logs")
    public String getLogs() {
        throw new UnsupportedOperationException("logs");
    }

    /**
     return [
            'current_state' => Arr::get($model, 'state', 'stopped'),
            'is_suspended' => Arr::get($model, 'is_suspended', false),
            'resources' => [
                'memory_bytes' => Arr::get($model, 'utilization.memory_bytes', 0),
                'cpu_absolute' => Arr::get($model, 'utilization.cpu_absolute', 0),
                'disk_bytes' => Arr::get($model, 'utilization.disk_bytes', 0),
                'network_rx_bytes' => Arr::get($model, 'utilization.network.rx_bytes', 0),
                'network_tx_bytes' => Arr::get($model, 'utilization.network.tx_bytes', 0),
                'uptime' => Arr::get($model, 'utilization.uptime', 0),
            ],
        ];

     */
    @GetMapping(value = "/api/servers/{server}")
    public String getDetails(@PathVariable String server) {
        if(JavaWings.getServerManager().getServer(server) == null) {
            JavaWings.getServerManager().createServer(server, null);
        }
        return JavaWings.getServerManager().getServer(server).getAPIReponse().toDocument().toJson();
    }


    @PostMapping(value = "/api/server/{server}/power")
    public String power(@PathVariable String server) {
        throw new UnsupportedOperationException("power");
    }

    @PostMapping(value = "/api/servers/{server}/commands")
    public HttpStatus commands(@PathVariable String server, @RequestBody String data) throws Exception {
        JSONObject jsonObject = new JSONObject(data);
        Document document = Document.parse(data);
        document.getList("commands", String.class).forEach( o -> {
            K8sManager.runCommandIfServerExits(server, o);
        });

        return HttpStatus.OK;
    }

    @DeleteMapping(value = "/api/servers/{server}")
    public HttpStatus deleteServer(@PathVariable String server) {
        K8sManager.stopPod(server);
        FileSystemManager.deleteServerFolder(server);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/api/server/{server}/reinstall")
    public String reinstall(@PathVariable String server) {
        throw new UnsupportedOperationException("reinstall");
    }


    @PostMapping(value = "/api/servers/{server}/ws/deny")
    public String wsDeny(@PathVariable String server) {
        throw new UnsupportedOperationException("wsDeny");
    }


}
