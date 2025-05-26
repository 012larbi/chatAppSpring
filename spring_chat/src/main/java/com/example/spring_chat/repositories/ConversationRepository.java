package com.example.spring_chat.repositories;



import com.example.spring_chat.entities.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    @Query("{ '$or': [ " +
       "{ 'userA': ?0, 'userB': ?1 }, " +
       "{ 'userA': ?1, 'userB': ?0 } " +
       "] }")
Conversation findByParticipants(String userA, String userB);

   // List<Conversation> findByUserAOrUserB(String userA, String userB);
        Conversation findByUserAAndUserB(String userA, String userB);

    List<Conversation> findByBetween(String between);
}
