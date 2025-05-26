package com.example.spring_chat.repositories;



import com.example.spring_chat.entities.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface GroupRepository extends MongoRepository<Group, String> {
    List<Group> findByMembersContaining(String userId);
    List<Group> findByAdmin(String admin);
}