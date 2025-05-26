package com.example.spring_chat.entities;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sessions")
public class Session {
    @Id
    private String id;
    private String userId;
    private String sid;
    private String status; // "connected" or "disconnected"

    // Constructors, getters, and setters
    public Session() {}

    public Session(String userId, String sid, String status) {
        this.userId = userId;
        this.sid = sid;
        this.status = status;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSid() { return sid; }
    public void setSid(String sid) { this.sid = sid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

  
}