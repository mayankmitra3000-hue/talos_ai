package com.example.AIResumeAnalyzer.controller;

import com.example.AIResumeAnalyzer.entity.User;
import com.example.AIResumeAnalyzer.service.AuthService;
import com.example.AIResumeAnalyzer.service.GroqService;
import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/airesumeanalyzer")
public class ResumeController {

    private final AuthService authService;
    private final GroqService groqService;

    @GetMapping
    public String firstPage(){
        return "firstpage";
    }

    @PostMapping("/upload")
    public String uploadResume(@RequestParam("resume")MultipartFile file, Model model){
        try{
            PDDocument document = Loader.loadPDF(file.getBytes());

            PDFTextStripper textStripper = new PDFTextStripper();
            String resumeText = textStripper.getText(document);

            document.close();

            String analysis = groqService.analyzeResume(resumeText);

            model.addAttribute("analysis",analysis);
            System.out.println(resumeText);
        }
        catch (IOException e){
            e.printStackTrace();
            model.addAttribute("error","Failed to read PDF File");
        }
        return "results";
    }

    @PostMapping("/signup")
    public String signupLogic
            (@RequestParam String username,
             @RequestParam String fullname,
             @RequestParam String password,
             @RequestParam Long contact,
             @RequestParam String email,
             Model model){

        User user = new User();
        user.setUsername(username);
        user.setFullName(fullname);
        user.setPassword(password);
        user.setContact(contact);
        user.setEmail(email);

        String status = authService.userSignup(user);

        if (status.equals("created")) {
            model.addAttribute(
                    "success",
                    "Account created successfully. Please sign in."
            );
            return "firstpage";
        }

        model.addAttribute(
                "error",
                "Username already exists."
        );
        return "firstpage";
    }

    @PostMapping("/signin")
    public String signinLogic(@RequestParam String username, @RequestParam String password, Model model){

            String status = authService.userSignin(username,password);
            if (status.equals("valid")) {
                model.addAttribute("valid", "Login successful...");
                return "home";
            }

            model.addAttribute("invalid","Invalid username/password");
            return "firstpage";
    }

}
