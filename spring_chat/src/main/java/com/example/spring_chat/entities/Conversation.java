// src/main/java/com/example/chatbackend/model/Conversation.java
package com.example.spring_chat.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "conversations")
public class Conversation {
    @Id
    private String id;
    private String userA;
    private String userB;
    private String between; // "group" or "users"

    // Constructors, getters, and setters
    public Conversation() {}

    public Conversation(String userA, String userB, String between) {
        this.userA = userA;
        this.userB = userB;
        this.between = between;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserA() { return userA; }
    public void setUserA(String userA) { this.userA = userA; }
    public String getUserB() { return userB; }
    public void setUserB(String userB) { this.userB = userB; }
    public String getBetween() { return between; }
    public void setBetween(String between) { this.between = between; }
}