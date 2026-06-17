package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.User;
import com.example.AIResumeAnalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    @Override
    public String userSignin(String username, String password){
        User user = userRepository.findByUsername(username);

        if (user==null){
            String status = "invalid";
            return status;
        }

        if (!user.getPassword().equals(password)){
            String status = "invalid";
            return status;
        }

        String status = "valid";
        return status;

    }

    @Override
    public String userSignup(User user){
        User existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser!=null){
            return "exists";
        }

        userRepository.save(user);
        return "created";
    }

}
