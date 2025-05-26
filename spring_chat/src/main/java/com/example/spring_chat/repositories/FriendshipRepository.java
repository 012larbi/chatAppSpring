package com.example.spring_chat.repositories;



import com.example.spring_chat.entities.Friendship;


import org.springframework.data.mongodb.repository.MongoRepository;

public interface FriendshipRepository extends MongoRepository<Friendship, String> {
    Friendship findByUserId(String userId);
}
