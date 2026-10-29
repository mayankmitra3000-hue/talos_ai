package com.example.AiInternshipAllocation.service;

import com.example.AiInternshipAllocation.entity.User;
import com.example.AiInternshipAllocation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("STUDENT");
        }

        userRepository.save(user);
        return "created";
    }

    @Override
    public User findUserbyUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public void updateProfile(User user){
        userRepository.save(user);
    }

    @Override
    public java.util.List<User> findAllUsers() {
        return userRepository.findAll();
    }
}
