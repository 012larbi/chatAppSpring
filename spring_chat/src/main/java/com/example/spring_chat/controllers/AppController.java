package com.example.spring_chat.controllers;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.spring_chat.repositories.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.spring_chat.entities.*;
import com.example.spring_chat.services.*;




@Controller
public class AppController {
    



    @Autowired
    private UserRepository userRepository;
      @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
 

   @Autowired
   private MyWebSocketHandler webSocketHandler;



     ObjectMapper objectMapper = new ObjectMapper();
     
   /**
     * @param model
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
  @GetMapping("/")
public Object servePage(Model model, HttpServletRequest request) throws IOException {
    String sid = getCookieValue(request, "sid");
    boolean validSession = (sid != null) && (sessionRepository.findBySid(sid) != null);

    if (!validSession) {
        // 🔹 Serve static login.html
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setCacheControl(CacheControl.noStore().mustRevalidate());
        headers.setPragma("no-cache");
        headers.setExpires(0);

        ClassPathResource resource = new ClassPathResource("static/login.html");

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        byte[] htmlBytes = StreamUtils.copyToByteArray(resource.getInputStream());
        return new ResponseEntity<>(htmlBytes, headers, HttpStatus.OK);
    }

    // 🔹 Valid session: prepare Thymeleaf model
    Session session = sessionRepository.findBySid(sid);
    session.setStatus("connected");
    sessionRepository.save(session);

    String currentUserId = session.getUserId();
    Friendship friendship = friendshipRepository.findByUserId(currentUserId);

    if (friendship != null) {
        List<String> friendIds = friendship.getFriends();
        Map<String, Object> dt = Map.of(
            "type", "userstatus",
            "status", "connected",
            "userId", currentUserId
        );

        friendIds.forEach(id -> {
            try {
                String jsonMsg = objectMapper.writeValueAsString(dt);
                webSocketHandler.sendMessageTo(id, jsonMsg);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }


    List<String> userIDs =new ArrayList<>();
    userIDs.add(currentUserId);
     List<User> users = userRepository.findByIdIn(userIDs);
      String username=users.get(0).getUsername();
    // Add any needed attributes for Thymeleaf
    model.addAttribute("username", username); // or anything else needed

    return "chaty"; // 🔹 This is chat.html rendered with Thymeleaf
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
    

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(
            @RequestBody Map<String, String> requestBody,
            HttpServletResponse response) throws IOException {
        
        String username = requestBody.get("username");
        String email = requestBody.get("email");
        String password = requestBody.get("password");

        Map<String, String> responseBody = new HashMap<>();

        // Validate input
        if (username == null || username.isEmpty()) {
            responseBody.put("message", "Username is required");
            return ResponseEntity.badRequest().body(responseBody);
        }

        if (email == null || email.isEmpty()) {
            responseBody.put("message", "Email is required");
            return ResponseEntity.badRequest().body(responseBody);
        }

        if (password == null || password.isEmpty()) {
            responseBody.put("message", "Password is required");
            return ResponseEntity.badRequest().body(responseBody);
        }

        // Check if username exists
        if (userRepository.findByUsername(username) != null) {
            responseBody.put("message", "Username already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(responseBody);
        }

        // Check if email exists
        if (userRepository.findByEmail(email) != null) {
            responseBody.put("message", "Email already registered");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(responseBody);
        }

        // Create and save new user
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(password); // Note: Should hash password in production
        newUser.setCreatedAt(new Date());
        userRepository.save(newUser);

        // Redirect on success
        response.sendRedirect("/");
        return null;
    }

 



     @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // Validate input
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username and password are required"));
        }

        // Find user by username
        User user = userRepository.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }

        // Generate session ID (Base64 of 30 chars)
        String sessionId = generateSessionId();

        // Create and save session
        Session session = new Session();
        session.setUserId(user.getId());
        session.setSid(sessionId);
        session.setStatus("connected");
        sessionRepository.save(session);

        // Create secure cookie
        ResponseCookie cookie = ResponseCookie.from("sid", sessionId)
                .httpOnly(true)
                .secure(false)  // Enable in production with HTTPS
                .path("/")
                .maxAge(Duration.ofDays(7))  // 7 days expiration
                .sameSite("Lax")  // Helps prevent CSRF
                .build();

        // Return response with cookie
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Login successful"));
    }

    private String generateSessionId() {
        // Generate random bytes and encode as Base64
        byte[] randomBytes = new byte[30];
        new java.util.Random().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }




    
}
