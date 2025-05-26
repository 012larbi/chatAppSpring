package com.example.spring_chat.entities;



import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "groups")
public class Group {
    @Id
    private String id;
    private String name;
    private List<String> members;
    private String admin;
    private Date createdAt;
 


        

    // Constructors, getters, and setters
    public Group() {}

    public Group(String name, List<String> members, String admin) {
        this.name = name;
        this.members = members;
        this.admin = admin;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
    public String getAdmin() { return admin; }
    public void setAdmin(String admin) { this.admin = admin; } 
    public Date getCreatedAt() {return createdAt;}
    public void setCreatedAt(Date createdAt) {this.createdAt = createdAt; }
}
