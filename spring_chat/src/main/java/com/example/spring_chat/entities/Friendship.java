package com.example.spring_chat.entities;



import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "friendships")
public class Friendship {
    @Id
    private String id;
    private String userId;
    private List<String> friends;

    // Constructors, getters, and setters
    public Friendship() {}

    public Friendship(String userId, List<String> friends) {
        this.userId = userId;
        this.friends = friends;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<String> getFriends() { return friends; }
    public void setFriends(List<String> friends) { this.friends = friends; }
}
