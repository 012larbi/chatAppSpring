package com.example.spring_chat.repositories;



import com.example.spring_chat.entities.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface UserRepository extends MongoRepository<User, String> {
    User findByUsername(String username);
    User findByEmail(String email);  // New method
    @Query("{ '_id': { '$nin': ?0 } }")
List<User> findByIdNotIn(List<String> ids);

    
      List<User> findByIdIn(List<String> friendIds);

      Optional<User> findById(String id);
}
