package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.User;

public interface AuthService {
    String userSignin(String username, String password);
    String userSignup(User user);
    User findUserbyUsername(String username);
}
