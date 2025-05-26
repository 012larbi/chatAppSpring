package com.example.spring_chat.entities;


import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "invitations")
public class Invitation {
    @Id
    private String id;
    private String inviterId;
    private String invitedId;
    private Date createdAt; 
    private String status; // "pending", "accepted", "rejected"

    // Constructors, getters, and setters
    public Invitation() {}

    public Invitation(String inviterId, String invitedId, String status) {
        this.inviterId = inviterId;
        this.invitedId = invitedId;
        this.status = status;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInviterId() { return inviterId; }
    public void setInviterId(String inviterId) { this.inviterId = inviterId; }
    public String getInvitedId() { return invitedId; }
    public void setInvitedId(String invitedId) { this.invitedId = invitedId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; } 
    public Date getCreatedAt() { return createdAt;}
    public void setCreatedAt(Date createdAt) {this.createdAt = createdAt;}
}
