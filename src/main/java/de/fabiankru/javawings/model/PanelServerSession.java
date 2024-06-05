package de.fabiankru.javawings.model;

import de.fabiankru.javawings.JavaWings;
import de.fabiankru.javawings.model.jwt.JWTPermissions;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class PanelServerSession {

    private List<JWTPermissions> jwtPermissions = new ArrayList<>();

    private WebSocketSession session;

    public static PanelServerSession getByWebSocketSession(WebSocketSession session) {
        for(PanelServer server: JavaWings.getServerManager().getServers()) {
            if(server.getSessions().stream().anyMatch(t -> t.getSession().getId().equals(session.getId()))) {
                return server.getSessions()
                        .stream()
                        .filter(t -> t.getSession().getId().equals(session.getId()))
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

}
