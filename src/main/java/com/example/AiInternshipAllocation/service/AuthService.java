package com.example.AiInternshipAllocation.service;

import com.example.AiInternshipAllocation.entity.User;

public interface AuthService {
    String userSignin(String username, String password);
    String userSignup(User user);
    User findUserbyUsername(String username);
    void updateProfile(User user);
    java.util.List<User> findAllUsers();
}
