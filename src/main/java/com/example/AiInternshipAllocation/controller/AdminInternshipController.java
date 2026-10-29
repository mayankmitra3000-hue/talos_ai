package com.example.AiInternshipAllocation.controller;

import com.example.AiInternshipAllocation.entity.Internship;
import com.example.AiInternshipAllocation.entity.InternshipAllocation;
import com.example.AiInternshipAllocation.entity.Resume;
import com.example.AiInternshipAllocation.entity.User;
import com.example.AiInternshipAllocation.repository.InternshipAllocationRepository;
import com.example.AiInternshipAllocation.repository.InternshipRepository;
import com.example.AiInternshipAllocation.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/airesumeanalyzer")
public class AdminInternshipController {

    private final InternshipRepository internshipRepository;
    private final InternshipAllocationRepository allocationRepository;
    private final AuthService authService;
    private final ResumeService resumeService;
    private final BestResumeService bestResumeService;
    private final GroqService groqService;

    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            String username = (String) session.getAttribute("loggedInUser");
            if (username != null) {
                User user = authService.findUserbyUsername(username);
                if (user != null && user.getRole() != null) {
                    role = user.getRole();
                    session.setAttribute("userRole", role);
                }
            }
        }
        return "ADMIN".equals(role);
    }

    @GetMapping("/admin/internships")
    public String manageInternships(HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/airesumeanalyzer";
        }
        if (!isAdmin(session)) {
            return "redirect:/airesumeanalyzer/home";
        }
        String username = (String) session.getAttribute("loggedInUser");
        model.addAttribute("username", username);
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("internships", internshipRepository.findAllByOrderByIdDesc());
        return "admin/internships";
    }

    @PostMapping("/admin/internships")
    public String addInternship(@RequestParam String title,
                                @RequestParam String description,
                                @RequestParam(required = false) String requiredSkills,
                                @RequestParam(required = false) String experience,
                                @RequestParam(required = false) String location,
                                @RequestParam Integer seats,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/airesumeanalyzer";
        }
        if (!isAdmin(session)) {
            return "redirect:/airesumeanalyzer/home";
        }
        String username = (String) session.getAttribute("loggedInUser");
        Internship internship = new Internship();
        internship.setTitle(title);
        internship.setDescription(description);
        internship.setRequiredSkills(requiredSkills);
        internship.setExperience(experience);
        internship.setLocation(location);
        internship.setSeats(seats);
        internship.setAllocatedCount(0);
        internship.setPostedBy(username);
        internshipRepository.save(internship);
        redirectAttributes.addFlashAttribute("success", "Internship posted successfully");
        return "redirect:/airesumeanalyzer/admin/internships";
    }

    @GetMapping("/admin/bestresumes")
    public String bestResumesForInternships(HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/airesumeanalyzer";
        }
        if (!isAdmin(session)) {
            return "redirect:/airesumeanalyzer/home";
        }
        String username = (String) session.getAttribute("loggedInUser");
        model.addAttribute("username", username);
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("internships", internshipRepository.findAllByOrderByIdDesc());
        return "admin/bestresumes";
    }

    @PostMapping("/admin/bestresumes")
    public String findBestResumesForInternship(@RequestParam Long internshipId,
                                                HttpSession session,
                                                Model model) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/airesumeanalyzer";
        }
        if (!isAdmin(session)) {
            return "redirect:/airesumeanalyzer/home";
        }
        String username = (String) session.getAttribute("loggedInUser");
        model.addAttribute("username", username);
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("internships", internshipRepository.findAllByOrderByIdDesc());

        Optional<Internship> internshipOpt = internshipRepository.findById(internshipId);
        if (internshipOpt.isEmpty()) {
            model.addAttribute("error", "Internship not found");
            return "admin/bestresumes";
        }
        Internship internship = internshipOpt.get();

        String jobDescription = buildJobDescription(internship);
        try {
            // For admin, search across all resumes in the system? We need to search using RAG across all users' collections
            // But current implementation searches by username's collection. We'll search each student's collection.
            List<User> allUsers = authService.findAllUsers();
            List<Map<String, Object>> allMatches = new ArrayList<>();

            for (User user : allUsers) {
                if (user.getRole() != null && user.getRole().equals("ADMIN")) {
                    continue;
                }
                try {
                    List<Map<String, Object>> matches = bestResumeService.findBestResumes(user.getUsername(), jobDescription);
                    for (Map<String, Object> match : matches) {
                        match.put("studentUsername", user.getUsername());
                        match.put("studentName", user.getFullName());
                        allMatches.add(match);
                    }
                } catch (Exception ignored) {
                }
            }

            allMatches.sort((a, b) -> {
                Double sa = ((Number) a.get("score")).doubleValue();
                Double sb = ((Number) b.get("score")).doubleValue();
                return Double.compare(sb, sa);
            });

            List<Map<String, Object>> topMatches = allMatches.stream().limit(20).collect(Collectors.toList());
            String aiComparison = groqService.compareResumesWithJob(jobDescription, topMatches);

            model.addAttribute("selectedInternship", internship);
            model.addAttribute("matches", topMatches);
            model.addAttribute("aiComparison", aiComparison);
            model.addAttribute("jobDescription", jobDescription);
        } catch (Exception e) {
            model.addAttribute("error", "Error finding best resumes: " + e.getMessage());
        }
        return "admin/bestresumes";
    }

    @GetMapping("/admin/allocations")
    public String allocationsPage(HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/airesumeanalyzer";
        }
        if (!isAdmin(session)) {
            return "redirect:/airesumeanalyzer/home";
        }
        String username = (String) session.getAttribute("loggedInUser");
        model.addAttribute("username", username);
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("internships", internshipRepository.findAllByOrderByIdDesc());
        model.addAttribute("allocations", allocationRepository.findAll());
        return "admin/allocations";
    }

    @PostMapping("/admin/allocate")
    public String allocateStudents(@RequestParam Long internshipId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/airesumeanalyzer";
        }
        if (!isAdmin(session)) {
            return "redirect:/airesumeanalyzer/home";
        }

        Optional<Internship> internshipOpt = internshipRepository.findById(internshipId);
        if (internshipOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Internship not found");
            return "redirect:/airesumeanalyzer/admin/allocations";
        }
        Internship internship = internshipOpt.get();

        if (internship.getSeats() == null) {
            internship.setSeats(0);
        }
        if (internship.getAllocatedCount() == null) {
            internship.setAllocatedCount(0);
        }

        if (internship.getAllocatedCount() >= internship.getSeats()) {
            redirectAttributes.addFlashAttribute("error", "No seats available for this internship");
            return "redirect:/airesumeanalyzer/admin/allocations";
        }

        String jobDescription = buildJobDescription(internship);
        try {
            List<User> allUsers = authService.findAllUsers();
            List<Map<String, Object>> allMatches = new ArrayList<>();

            for (User user : allUsers) {
                if (user.getRole() != null && user.getRole().equals("ADMIN")) {
                    continue;
                }
                /*
                 * A student can hold only one internship at a time.
                 */
                if (allocationRepository.existsByStudent(user)) {
                    continue;
                }
                try {
                    List<Map<String, Object>> matches = bestResumeService.findBestResumes(user.getUsername(), jobDescription);
                    for (Map<String, Object> match : matches) {
                        match.put("student", user);
                        match.put("studentUsername", user.getUsername());
                        allMatches.add(match);
                    }
                } catch (Exception ignored) {
                }
            }

            allMatches.sort((a, b) -> {
                Double sa = ((Number) a.get("score")).doubleValue();
                Double sb = ((Number) b.get("score")).doubleValue();
                return Double.compare(sb, sa);
            });

            int seatsLeft = internship.getSeats() - internship.getAllocatedCount();
            int allocated = 0;
            for (Map<String, Object> match : allMatches) {
                if (allocated >= seatsLeft) {
                    break;
                }
                User student = (User) match.get("student");
                if (student == null) {
                    continue;
                }
                /*
                 * Re-check in case the student was allocated earlier
                 * in this same run.
                 */
                if (allocationRepository.existsByStudent(student)) {
                    continue;
                }
                InternshipAllocation allocation = new InternshipAllocation();
                allocation.setStudent(student);
                allocation.setInternship(internship);
                Object resumeObj = match.get("resume");
                if (resumeObj instanceof Resume) {
                    allocation.setAllocatedResumeId(String.valueOf(((Resume) resumeObj).getResumeId()));
                }
                allocation.setMatchScore(((Number) match.get("score")).doubleValue());
                allocationRepository.save(allocation);
                allocated++;
            }

            internship.setAllocatedCount(internship.getAllocatedCount() + allocated);
            internshipRepository.save(internship);

            redirectAttributes.addFlashAttribute("success", allocated + " student(s) allocated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error during allocation: " + e.getMessage());
        }

        return "redirect:/airesumeanalyzer/admin/allocations";
    }

    @PostMapping("/admin/allocations/{id}/delete")
    @Transactional
    public String removeAllocation(@PathVariable Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/airesumeanalyzer";
        }
        if (!isAdmin(session)) {
            return "redirect:/airesumeanalyzer/home";
        }

        Optional<InternshipAllocation> allocationOpt =
                allocationRepository.findById(id);

        if (allocationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Allocation not found"
            );
            return "redirect:/airesumeanalyzer/admin/allocations";
        }

        InternshipAllocation allocation = allocationOpt.get();
        Internship internship = allocation.getInternship();

        allocationRepository.delete(allocation);

        if (internship != null) {
            int allocatedCount = internship.getAllocatedCount() == null
                    ? 0
                    : internship.getAllocatedCount();

            internship.setAllocatedCount(Math.max(0, allocatedCount - 1));
            internshipRepository.save(internship);
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Student removed from the internship successfully"
        );

        return "redirect:/airesumeanalyzer/admin/allocations";
    }

    private String buildJobDescription(Internship internship) {
        StringBuilder sb = new StringBuilder();
        if (internship.getTitle() != null) {
            sb.append("Job Title:\n").append(internship.getTitle()).append("\n\n");
        }
        if (internship.getExperience() != null) {
            sb.append("Required Experience:\n").append(internship.getExperience()).append("\n\n");
        }
        if (internship.getRequiredSkills() != null) {
            sb.append("Required Skills:\n").append(internship.getRequiredSkills()).append("\n\n");
        }
        if (internship.getDescription() != null) {
            sb.append("Description:\n").append(internship.getDescription()).append("\n\n");
        }
        if (internship.getLocation() != null) {
            sb.append("Location:\n").append(internship.getLocation()).append("\n\n");
        }
        return sb.toString();
    }
}
