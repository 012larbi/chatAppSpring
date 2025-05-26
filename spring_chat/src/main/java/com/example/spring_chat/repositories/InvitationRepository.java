package com.example.spring_chat.repositories;


import com.example.spring_chat.entities.Invitation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends MongoRepository<Invitation, String> {
    List<Invitation> findByInvitedIdAndStatus(String invitedId, String status);
    List<Invitation> findByInviterId(String inviterId);
         Optional<Invitation> findByInviterIdAndInvitedId(String inviterId, String invitedId);
          boolean existsByInviterIdAndInvitedId(String currentUserId, String invitedId);
          boolean existsByInviterIdAndInvitedIdAndStatus(String inviterId, String invitedId, String status);

}