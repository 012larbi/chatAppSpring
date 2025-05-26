package com.example.spring_chat.services;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("some client is con");
          String userId = (String) session.getAttributes().get("userId");
        
        if (userId != null) {
            sessions.put(userId, session);
        } else {
            // This shouldn't happen if interceptor is working correctly
            System.out.println("it get closed");
            session.close();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("some client is diconnected");
      String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
        }   
     }


       public WebSocketSession getSessionByUserId(String id)  {       
        return sessions.get(id);
    }

    public boolean sendMessageTo(String id,String msg) {
        WebSocketSession wb = getSessionByUserId(id);
        if(wb==null||!wb.isOpen())
         return false;
         try {
            wb.sendMessage(new TextMessage(msg));
            return true;

        } catch (IOException e) {
           System.out.println("error while sending message : websocketHandler");
         return false;
        }
    }
}

