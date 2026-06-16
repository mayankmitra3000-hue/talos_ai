package com.example.AIResumeAnalyzer.controller;

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
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

    @GetMapping ("/signin")
    public String signin() {
        return "signin";
    }

    @PostMapping("/signin")
    public String signinLogic(@RequestParam String username, @RequestParam String password, Model model){

            String status = authService.userSignin(username,password);
            if (status.equals("valid")) {
                model.addAttribute("valid", "Login successful...");
                return "home";
            }

            model.addAttribute("invalid","Invalid username/password");
            return "signin";
    }

}
