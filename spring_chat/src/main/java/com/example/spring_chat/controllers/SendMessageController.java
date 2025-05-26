package com.example.spring_chat.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_chat.entities.*;
import com.example.spring_chat.services.MyWebSocketHandler;
import com.example.spring_chat.repositories.*;


import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;






@RestController
public class SendMessageController {
     
    @Autowired
    private final MyWebSocketHandler webSocketHandler;
     @Autowired
    private SessionRepository sessionRepository;
      @Autowired
    private ConversationRepository conversationRepository;
      @Autowired
    private GroupRepository groupRepository;
      @Autowired
    private MessageRepository messageRepository;
     @Autowired
    private UserRepository userRepository;
    @Autowired
     private FriendshipRepository friendshipRepository;
     @Autowired
    private InvitationRepository invitationRepository;


    ObjectMapper objectMapper = new ObjectMapper();

// If you're using ObjectNode

    public SendMessageController(MyWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }
    @PostMapping("/chat")
    public ResponseEntity<?> sendMessage(
            @RequestBody Map<String, String> requestBody,
            @CookieValue(value = "sid", required = false) String sessionId) throws JsonProcessingException {
        
        // Validate session
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        Session session = sessionRepository.findBySid(sessionId);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid session"));
        }

        String currentUserId = session.getUserId();
        String targetId = requestBody.get("id");
        String content = requestBody.get("content");

        if (targetId == null || content == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Missing id or content"));
        }

        // Find or create conversation
        Conversation conversation = conversationRepository.findByParticipants(currentUserId, targetId);
        
        if (conversation == null) {
            // Create new conversation
            conversation = new Conversation();
            conversation.setUserA(currentUserId);
            conversation.setUserB(targetId);
            
            // Check if target is a group or user
            boolean isGroup = groupRepository.existsById(targetId);
            if (isGroup) {
               conversation.setUserA(targetId);
            }
            conversation.setBetween(isGroup ? "group" : "users");
            
            conversation = conversationRepository.save(conversation);
        }

        // Create and save message
        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setContent(content);
        message.setUserId(currentUserId);
        message.setDate(new Date());
        message = messageRepository.save(message);

        // Prepare response payload
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("type", "message");
        messagePayload.put("senderId", currentUserId);
        messagePayload.put("content", content);
        String jsonString = objectMapper.writeValueAsString(messagePayload);
        //System.out.println(jsonString);
        if ("group".equals(conversation.getBetween())) {
            // Send to all group members except sender
            groupRepository.findById(targetId).ifPresent(group -> {
                group.getMembers().stream()
                    .filter(memberId -> !memberId.equals(currentUserId))
                    .forEach(memberId -> {
                        // prepare json for a message
                       webSocketHandler.sendMessageTo(memberId, jsonString);
                    });
            });
        } else {
            // Send to individual user
            webSocketHandler.sendMessageTo(targetId, jsonString);

        }

        return ResponseEntity.ok(Map.of(
            "message", "Message sent"
        ));
    }
    

@PostMapping("/acceptInvitation")
public ResponseEntity<?> acceptInvitation(
        @RequestBody Map<String, String> requestBody,
        @CookieValue("sid") String sessionId) throws JsonProcessingException {
    
    // Authentication check
    Session session = sessionRepository.findBySid(sessionId);
    if (session == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid session"));
    }

    String currentUserId = session.getUserId();
    String inviterId = requestBody.get("id");
    
    if (inviterId == null) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Missing inviterId"));
    }

    // 1. First check if invitation exists
    Optional<Invitation> invitationOpt = invitationRepository.findByInviterIdAndInvitedId(inviterId, currentUserId);
    if (invitationOpt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "No pending invitation found"));
    }

    Invitation invitation = invitationOpt.get();
    
    // 2. Only proceed if invitation is pending
    if (!"pending".equals(invitation.getStatus())) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Invitation already processed"));
    }

    // 3. Update friendship
    Friendship friendship = friendshipRepository.findByUserId(currentUserId);
    if (friendship == null) {
        friendship = new Friendship();
        friendship.setUserId(currentUserId);
        friendship.setFriends(new ArrayList<>());
    }
    
    // Prevent duplicate friendships
    if (!friendship.getFriends().contains(inviterId)) {
        friendship.getFriends().add(inviterId);
        friendshipRepository.save(friendship);
    }

        // 3. Update friendship
    Friendship friendship2 = friendshipRepository.findByUserId(inviterId);
    if (friendship2 == null) {
        friendship2 = new Friendship();
        friendship2.setUserId(inviterId);
        friendship2.setFriends(new ArrayList<>());
    }
    
    // Prevent duplicate friendships
    if (!friendship2.getFriends().contains(currentUserId)) {
        friendship2.getFriends().add(currentUserId);
        friendshipRepository.save(friendship2);
    }



    // 4. Update invitation status
    invitation.setStatus("accepted");
    invitationRepository.save(invitation);

    // 5. Send WebSocket notification if user is online
    User currentUser = userRepository.findById(currentUserId).orElse(null);
    if (currentUser != null) {
        Map<String, Object> messagePayload = Map.of(
            "type", "notification",
            "notifType", "decision",
            "username", currentUser.getUsername(),
            "status", "accepted"
        );
        String jsonString = objectMapper.writeValueAsString(messagePayload);
        webSocketHandler.sendMessageTo(inviterId, jsonString);
    }

    return ResponseEntity.ok(Map.of(
        "message", "Invitation accepted"
    ));
}



    @PostMapping("/rejectInvitation")
    public ResponseEntity<?> rejectInvitation(
            @RequestBody Map<String, String> requestBody,
            @CookieValue(value = "sid", required = false) String sessionId) throws JsonProcessingException {
        
        // Authentication check
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        Session session = sessionRepository.findBySid(sessionId);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid session"));
        }

        String currentUserId = session.getUserId();
        String inviterId = requestBody.get("inviterId");
        
        if (inviterId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Missing inviterId"));
        }

        // 1. Send WebSocket notification if inviter is online
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        String username = currentUser != null ? currentUser.getUsername() : "Unknown";
        
        Map<String, Object> dt = new HashMap<>();
        dt.put("type", "notification");
        dt.put( "notifType", "decision");
        dt.put("username", username);
        dt.put( "status", "rejected");

        // 2. Send WebSocket notification if user is online
        String jsonString= objectMapper.writeValueAsString(dt);
       webSocketHandler.sendMessageTo(inviterId,jsonString );

        // 2. Update invitation status
        invitationRepository.findByInviterIdAndInvitedId(inviterId, currentUserId)
            .ifPresent(invitation -> {
                invitation.setStatus("rejected");
                invitationRepository.save(invitation);
            });

        return ResponseEntity.ok(Map.of(
            "message", "Invitation rejected"
        ));
    }





 

    @PostMapping("/invite")
    public ResponseEntity<?> handleInvitation(
            @RequestBody Map<String, String> requestBody,
            @CookieValue(value = "sid", required = false) String sessionId) throws JsonProcessingException {
        System.out.println("sis from invite"+sessionId +" id : " +requestBody.get("id"));
        // Authentication check
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        Session session = sessionRepository.findBySid(sessionId);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid session"));
        }

        String currentUserId = session.getUserId();
        String action = requestBody.get("action");
        String invitedId = requestBody.get("id");
        
        if (action == null || invitedId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Missing action or invitedId"));
        }

        if ("add".equals(action)) {
            return handleAddInvitation(currentUserId, invitedId);
        } else if ("cancel".equals(action)) {
            return handleCancelInvitation(currentUserId, invitedId);
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid action"));
        }
    }

    private ResponseEntity<?> handleAddInvitation(String currentUserId, String invitedId) throws JsonProcessingException {
        // Check if invitation already exists
        System.out.println("if exist :"+invitationRepository.existsByInviterIdAndInvitedId(currentUserId, invitedId));
        if (invitationRepository.existsByInviterIdAndInvitedId(currentUserId, invitedId)) {
            
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Invitation already exists"));
        }
              System.out.println("invitation not exist");
        // Create new invitation
        Invitation invitation = new Invitation();
        invitation.setInviterId(currentUserId);
        invitation.setInvitedId(invitedId);
        invitation.setStatus("pending");
        invitation.setCreatedAt(new Date());
        invitationRepository.save(invitation);

        // Send WebSocket notification if invited user is online
        User inviter = userRepository.findById(currentUserId).orElse(null);
        if (inviter != null) {
      
               Map<String, Object> dt =  Map.of(
                    "type", "notification",
                    "notifType", "invitation",
                    "username", inviter.getUsername(),
                    "id", currentUserId
                );
                String jsonMsg=objectMapper.writeValueAsString(dt);
                webSocketHandler.sendMessageTo(invitedId, jsonMsg);
          
        }

        return ResponseEntity.ok(Map.of(
            "message", "Invitation sent"
        ));
    }

    private ResponseEntity<?> handleCancelInvitation(String currentUserId, String invitedId) {
        // Find and delete invitation
        System.out.println("dddd1");
        Optional<Invitation> invitation = invitationRepository.findByInviterIdAndInvitedId(currentUserId, invitedId);

        if (invitation.isPresent()) {
            invitationRepository.delete(invitation.get());
            System.out.println("Invitation canceled");
            return ResponseEntity.ok(Map.of(
                "message", "Invitation canceled"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Invitation not found"));
        }
    }

     @SuppressWarnings("unchecked")
    @PostMapping("/createGroup")
    public ResponseEntity<?> createGroup(
            @RequestBody Map<String, Object> requestBody,
            @CookieValue(value = "sid", required = false) String sessionId) {
        
        // Authentication check
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        Session session = sessionRepository.findBySid(sessionId);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid session"));
        }

        String currentUserId = session.getUserId();
        String groupName = (String) requestBody.get("name");
        List<String> members = (List<String>) requestBody.get("members");

        // Validate inputs
        if (groupName == null || groupName.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Group name cannot be empty"));
        }

        if (members == null || members.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Members list cannot be empty"));
        }

        // Ensure the admin is included in members
        if (!members.contains(currentUserId)) {
            members.add(currentUserId);
        }

        // Create and save group
        Group group = new Group();
        group.setName(groupName);
        group.setMembers(members);
        group.setAdmin(currentUserId);
        group.setCreatedAt(new Date());
        
        group = groupRepository.save(group);

        return ResponseEntity.ok(Map.of(
            "message", "Group created successfully"
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getNonFriendUsers(
            @CookieValue(value = "sid", required = false) String sessionId) {
         System.out.println("get users");
        // Authentication check
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        Session session = sessionRepository.findBySid(sessionId);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid session"));
        }

        String currentUserId = session.getUserId();

        // 1. Get current user's friends
         Friendship friendship = friendshipRepository.findByUserId(currentUserId);
        List<String> friendIds = friendship != null ? friendship.getFriends() : Collections.emptyList();

        // 2. Get all users who aren't friends
        List<User> nonFriends = userRepository.findByIdNotIn(friendIds);

        List<Map<String, Object>> response=new ArrayList<Map<String, Object>>();
        nonFriends.forEach(user -> {
             
                    if (!user.getId().equals(currentUserId)) {
                boolean invitationSent = invitationRepository.existsByInviterIdAndInvitedIdAndStatus(
                    currentUserId, 
                    user.getId(), 
                    "pending"
                );

                        response.add(Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "invitationSent", invitationSent
                )); 
                    }
               
        });
            

        return ResponseEntity.ok(response);
    }



@GetMapping("/contacts")
    public ResponseEntity<?> getContacts(
            @CookieValue(value = "sid", required = false) String sessionId) {

             System.out.println("get contacts");

        // Authentication check
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        Session session = sessionRepository.findBySid(sessionId);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid session"));
        }
        String currentUserId = session.getUserId();
        List<Map<String, Object>> response=new ArrayList<Map<String, Object>>();

         
          Friendship friendship = friendshipRepository.findByUserId(currentUserId);

         if (friendship!=null) {
              List<String> friendIds = friendship.getFriends();
          
        // Step 2: Get users from those friendIds
        List<User> users = userRepository.findByIdIn(friendIds);



        List<Map<String, Object>> userContacts = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getUsername());
            map.put("type", "user");
            return map;
        }).toList();
                    response.addAll(userContacts);

         }
      

        // Step 3: Get groups where current user is a member
        List<Group> groups = groupRepository.findByMembersContaining(currentUserId);
        if (groups!=null) {
              List<Map<String, Object>> groupContacts = groups.stream().map(group -> {

            Map<String, Object> map = new HashMap<>();
            map.put("id", group.getId());
            map.put("name", group.getName());
            map.put("type", "group");
            return map;

        }).toList();
                response.addAll(groupContacts);

        }
      

        // Step 4: Merge and return
    





                return ResponseEntity.ok(response);
           
            }



      @PostMapping("/discussion")
    public ResponseEntity<?> getDiscussion(
            @RequestBody Map<String, String> requestBody,
            @CookieValue(value = "sid", required = false) String sessionId) {
        
        // Authentication check
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        Session session = sessionRepository.findBySid(sessionId);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid session"));
        }

        String currentUserId = session.getUserId();
        String otherUserId = requestBody.get("id");
        
        if (otherUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Missing user ID"));
        }

        // 1. Find conversation
        Conversation conversation = conversationRepository.findByUserAAndUserB(
            currentUserId, 
            otherUserId
        );
        
        if (conversation == null) {
            conversation = conversationRepository.findByUserAAndUserB(
                otherUserId, 
                currentUserId
            );
        }

        if (conversation == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // 2. Get messages sorted by date (newest first)
        List<Message> messages = messageRepository.findByConversationIdOrderByDateDesc(
            conversation.getId()
        );

        // 3. Transform messages
        List<Map<String, String>> response = messages.stream()
            .map(message -> {
                Map<String, String> msg = new HashMap<>();
                msg.put("content", message.getContent());
                msg.put("user", 
                    message.getUserId().equals(currentUserId) ? "me" : "other"
                );
                return msg;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }


   @GetMapping("/disconnect")
    public ResponseEntity<Void> disconnectUser(
            HttpServletRequest request, 
            HttpServletResponse response) throws IOException {
        
        // Get the "sid" cookie
        String sid = getCookieValue(request, "sid");
        
        if (sid != null) {
         






            // Delete the session from database
            
            // Expire the cookie client-side
            ResponseCookie cookie = ResponseCookie.from("sid", "")
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(0)  // Immediately expire
                    .sameSite("Lax")
                    .build();
            
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

             String currentUserId=sessionRepository.findBySid(sid).getUserId();
                         sessionRepository.deleteBySid(sid);

        Friendship friendship = friendshipRepository.findByUserId(currentUserId);
        if (friendship!=null) {
                List<String> friendIds = friendship.getFriends();
               Map<String, Object> dt =  Map.of(
                    "type", "userstatus",
                    "status", "disconnected",
                    "userId", currentUserId
                );

                 friendIds.forEach(id->{
                 String jsonMsg;
                 try {
                    jsonMsg = objectMapper.writeValueAsString(dt);  
                    webSocketHandler.sendMessageTo(id, jsonMsg);
                 } catch (JsonProcessingException e) {
                 
                    e.printStackTrace();
                 }
                 
                 });
        }
    
           



        }
         
        // Redirect to home page
        response.sendRedirect("/");
        return null;
    }
        private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    @GetMapping("/inactive")
    public ResponseEntity<?> getMethodName(
            HttpServletRequest request, 
            HttpServletResponse response) {

         System.out.println("/inactve");


                 String sid = getCookieValue(request, "sid");
               Session session= sessionRepository.findBySid(sid);
             
         if (session!=null) {
               session.setStatus("disconnected");
            sessionRepository.save(session);
         }
      
 


        String currentUserId=session.getUserId();
               Friendship friendship = friendshipRepository.findByUserId(currentUserId);
               System.out.println(friendship);
               if (friendship!=null) {

                System.out.println("user status being sent");
                 List<String> friendIds = friendship.getFriends();
               Map<String, Object> dt =  Map.of(
                    "type", "userstatus",
                    "status", "disconnected",
                    "userId", currentUserId
                );

                 friendIds.forEach(id->{
                 String jsonMsg;
                 try {
                    jsonMsg = objectMapper.writeValueAsString(dt);  
                    webSocketHandler.sendMessageTo(id, jsonMsg);
                 } catch (JsonProcessingException e) {
                 System.out.println("error in inactive");
                   // e.printStackTrace();
                 }
                 
                 });
               }
              
         System.out.println("/inactve");
        return ResponseEntity.ok("");
    }
    
 

}
    
    


