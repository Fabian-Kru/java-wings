package de.fabiankru.javawings.controller;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.fabiankru.javawings.JavaWings;
import de.fabiankru.javawings.model.PanelServer;
import de.fabiankru.javawings.model.jwt.JWTPermissions;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Base64;

import static de.fabiankru.javawings.JavaWings.logger;

public class WebSocketHandler extends TextWebSocketHandler {



	// TODO:
	// https://github.com/pterodactyl/panel/blob/a9bdf7a1ef27a65f07ebbf71d8ea20285cdaf30f/resources/scripts/components/server/events.ts#L19
	@Override
	public void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) {
		//System.out.println(session.getUri());
		if(!message.getPayload().startsWith("{"))return;
		Document document = Document.parse(message.getPayload());
		logger.info(session.getId() + " Payload: " + document.toJson());
		String event = document.getString("event");

		if(event.equals("send stats")) {
			return;
		}

		if(event.equals("auth")) {
			String token = document.getList("args", String.class).get(0);
			try {
				JWTVerifier verifier = JavaWings.getJwtVerifier();
				DecodedJWT jwt = verifier.verify(token);
				Document d = Document.parse(new String(Base64.getUrlDecoder().decode(jwt.getPayload())));
				PanelServer s = JavaWings.getServerManager().createServerFromAuthDataOrUpdateServer(d, session);

				if(s != null) {
					if(s.hasPermissions(JWTPermissions.PermissionConnect, session)) {
						s.connect(session);
						s.sendMessage(new TextMessage(new Document("event", "auth success").toJson(), true), session);
					} else {
						s.sendMessage(new TextMessage(new Document("event", "auth error").toJson(), true), session);
						session.close();
					}
				}
			} catch (Exception e) {
				logger.info("Auth error");
			}
			return;
		}

		if(event.equals("send logs")) {
			PanelServer server = JavaWings.getServerManager().getServer(session);
			if(server == null)return;
			server.sendOldLogs(session);
			return;
		}

		if(event.equals("send command")) {
			String command = document.getList("args", String.class).get(0);
			if(command == null || command.isEmpty())return;
			PanelServer server = JavaWings.getServerManager().getServer(session);
			if(server == null)return;
			server.sendCommand(command, session);
			return;
		}

		if(event.equals("set state")) {
			PanelServer server = JavaWings.getServerManager().getServer(session);
			if(server == null) {
				return;
			}
			switch (document.getList("args", String.class).get(0)) {

				case "start": {
					server.createPod(session);
					return;
				}

				case "stop": {
					server.destoryPod(session);
					return;
				}

				case "restart": {
					server.restartPod(session);
					return;
				}

				default: {
					System.err.println(document.getList("args", String.class).get(0));
                }

			}
		}
		//https://github.com/pterodactyl/panel/blob/develop/resources/scripts/components/server/WebsocketHandler.tsx#L85
	}

	@Override
	public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) throws Exception {

		if(JavaWings.getServerManager().getServer(session) != null) {
			JavaWings.getServerManager().getServer(session).close();
		}

		super.afterConnectionClosed(session, status);
	}
}