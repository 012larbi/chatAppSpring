package com.example.spring_chat.repositories;



import com.example.spring_chat.entities.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByConversationId(String conversationId);
    List<Message> findByUserId(String userId);
        List<Message> findByConversationIdOrderByDateDesc(String conversationId);
}