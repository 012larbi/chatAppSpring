// src/main/java/com/example/chatbackend/repository/SessionRepository.java
package com.example.spring_chat.repositories;

import com.example.spring_chat.entities.Session;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SessionRepository extends MongoRepository<Session, String> {
    Session findByUserId(String userId);
    Session findBySid(String sid);
    void deleteBySid(String sid);
}