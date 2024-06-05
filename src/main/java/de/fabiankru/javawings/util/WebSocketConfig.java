package de.fabiankru.javawings.util;

import de.fabiankru.javawings.controller.WebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(myHandler(), "/api/servers/{server}/ws").setAllowedOrigins("*");
  }

  @Bean
  public org.springframework.web.socket.WebSocketHandler myHandler() {
    return new WebSocketHandler();
  }

}