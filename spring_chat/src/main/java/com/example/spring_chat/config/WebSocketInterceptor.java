package com.example.spring_chat.config;




import com.example.spring_chat.entities.Session;
import com.example.spring_chat.repositories.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Map;

@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

    @Autowired
    private SessionRepository sessionRepository;

  public WebSocketInterceptor(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        Cookie[] cookies = httpRequest.getCookies();
        if (cookies == null) return false;

        // Assume the session ID is stored in a cookie named "SESSION"
        String sessionId = Arrays.stream(cookies)
            .filter(c -> "sid".equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);

        if (sessionId == null) return false;
          System.out.println("from interceptor : "+sessionId);
        // Retrieve session from DB
        Session session = sessionRepository.findBySid(sessionId);
        if (session == null) return false;

        // Store user ID in session attributes for later use
        attributes.put("userId", session.getUserId());
          System.out.println("from interceptor 2 : "+sessionId);

        return true; // Continue with handshake
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                              WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after handshake
    }
}