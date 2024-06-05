package de.fabiankru.javawings.remote;

import de.fabiankru.javawings.model.PanelServer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.Document;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.fabiankru.javawings.JavaWings.logger;


@Getter
@Setter
@NoArgsConstructor
public class ServerManager {

    private List<PanelServer> servers = new ArrayList<>();


    public PanelServer getServer(String uuid) {
        if(uuid == null || uuid.isEmpty()) {
            return null;
        }
        return servers.stream().filter(t -> t.getUuid().equals(uuid)).findFirst().orElse(null);
    }


    public PanelServer getServer(WebSocketSession session) {
        for(PanelServer server : servers) {
            if(server.getSessions().stream().anyMatch(t -> t.getSession().equals(session))) {
                return server;
            }
        }
        return null;
    }

    public PanelServer createServer(String uuid, WebSocketSession session) {

        if(servers.stream().anyMatch(t -> t.getUuid().equals(uuid))) {
            logger.info("Already exists!");
            return null;
        }
        PanelServer panelServer = new PanelServer(uuid, Optional.ofNullable(session), Optional.empty());
        servers.add(panelServer);
        logger.info("Added Server " + uuid);

        return panelServer;
    }


    public PanelServer createServerFromAuthDataOrUpdateServer(Document d, WebSocketSession session) {
        if(servers.stream().anyMatch(t -> t.getUuid().equals(d.getString("server_uuid")))) {
            PanelServer s = getServer(d.getString("server_uuid"));
            s.setAuthData(d, session);
            return s;
        }
        PanelServer s = createServer(d.getString("server_uuid"), session);

        s.setAuthData(d, session);
        return s;
    }

    public void removeServer(PanelServer panelServer) {
        servers.remove(panelServer);
    }
}
