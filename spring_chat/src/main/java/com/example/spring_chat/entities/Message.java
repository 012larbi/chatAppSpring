package com.example.spring_chat.entities;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    private String conversationId;
    private String content;
    private Date date;
    private String userId;

    // Constructors, getters, and setters
    public Message() {
        this.date = new Date();
    }

    public Message(String conversationId, String content, String userId) {
        this.conversationId = conversationId;
        this.content = content;
        this.userId = userId;
        this.date = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}