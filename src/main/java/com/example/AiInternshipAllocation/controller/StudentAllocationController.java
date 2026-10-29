package com.example.AiInternshipAllocation.controller;

import com.example.AiInternshipAllocation.entity.InternshipAllocation;
import com.example.AiInternshipAllocation.entity.User;
import com.example.AiInternshipAllocation.repository.InternshipAllocationRepository;
import com.example.AiInternshipAllocation.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/airesumeanalyzer")
public class StudentAllocationController {

    private final InternshipAllocationRepository allocationRepository;
    private final AuthService authService;

    @GetMapping("/myallocations")
    public String myAllocations(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return "redirect:/airesumeanalyzer";
        }
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            User user = authService.findUserbyUsername(username);
            role = user != null && user.getRole() != null ? user.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
        }
        User user = authService.findUserbyUsername(username);
        List<InternshipAllocation> allocations = allocationRepository.findByStudent(user);
        model.addAttribute("username", username);
        model.addAttribute("userRole", role);
        model.addAttribute("allocations", allocations);
        return "student/myallocations";
    }
}
