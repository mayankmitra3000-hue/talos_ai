package com.example.AIResumeAnalyzer.controller;

import com.example.AIResumeAnalyzer.entity.User;
import com.example.AIResumeAnalyzer.entity.Resume;
import com.example.AIResumeAnalyzer.repository.ResumeRepository;
import com.example.AIResumeAnalyzer.service.AuthService;
import com.example.AIResumeAnalyzer.service.GroqService;
import com.example.AIResumeAnalyzer.service.ResumeService;
import com.example.AIResumeAnalyzer.service.ResumeVectorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/airesumeanalyzer")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeVectorService resumeVectorService;
    private final AuthService authService;
    private final GroqService groqService;
//    private final HttpSession session; -----> Field injection not required when method parameter injection is available

    @GetMapping
    public String firstPage(){
        return "firstpage";
    }

    @GetMapping("/analyzeresume")
    public String analyzeresume(HttpSession session, Model model){
        String username = (String)session.getAttribute("loggedInUser");
        if(username==null){
            return "redirect:/airesumeanalyzer";
        }

        model.addAttribute("username",username);
        return "analyzeresumepage";

    }

    @PostMapping("/analyzeresume")
    public String analyzeResume(@RequestParam("resume")MultipartFile file, HttpSession session, Model model){

        if(session.getAttribute("loggedInUser")==null){
            return "redirect:/airesumeanalyzer";
        }

        try{

            if (file.isEmpty()) {
                model.addAttribute("error", "Please select a PDF file.");
                return "home";
            }

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
    public String signinLogic(@RequestParam String username, @RequestParam String password, HttpSession session, Model model){

            String status = authService.userSignin(username,password);
            if (status.equals("valid")) {
                session.setAttribute("loggedInUser",username);
                model.addAttribute("valid", "Login successful...");
                return "redirect:/airesumeanalyzer/home";
            }

            model.addAttribute("invalid","Invalid username/password");
            return "firstpage";
    }

    @GetMapping("/home")
    public String home(HttpSession session, HttpServletResponse response,Model model) {

        String username = (String) session.getAttribute("loggedInUser");

        if (username== null) {
            return "redirect:/airesumeanalyzer";
        }
        model.addAttribute("username",username);
        return "home";
    }

    @GetMapping("/uploadresume")
    public String resumeUploadPage(HttpSession session, Model model) {

        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "redirect:/airesumeanalyzer";
        }

        model.addAttribute("username", username);

        return "resumeuploadpage";
    }

    @PostMapping("/uploadresume")
    public String uploadResume(@RequestParam("resume") MultipartFile file,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "redirect:/airesumeanalyzer";
        }

        // File selected?
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Please select a PDF file."
            );
            return "redirect:/airesumeanalyzer/uploadresume";
        }

        // Validate PDF
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null ||
                !originalFileName.toLowerCase().endsWith(".pdf")) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Only PDF files are allowed."
            );

            return "redirect:/airesumeanalyzer/uploadresume";
        }

        try {

            String resumeText;

            try (PDDocument document = Loader.loadPDF(file.getBytes())) {

                PDFTextStripper stripper = new PDFTextStripper();
                resumeText = stripper.getText(document);
            }

            // Empty PDF?
            if (resumeText == null || resumeText.isBlank()) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "No readable text was found in the PDF."
                );

                return "redirect:/airesumeanalyzer/uploadresume";
            }

            // Save into MySQL
            Resume savedResume =
                    resumeService.saveResume(
                            username,
                            file,
                            resumeText
                    );

            // Store embeddings into Qdrant
            resumeVectorService.indexResume(savedResume);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Resume uploaded successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Failed to upload resume: " + e.getMessage()
            );
        }

        return "redirect:/airesumeanalyzer/uploadresume";
    }

    @GetMapping("/myresumes")
    public String myResumes(HttpSession session, Model model){
        String username = (String)session.getAttribute("loggedInUser");

        if(username==null){
            return "redirect:/airesumeanalyzer";
        }

        User user = authService.findUserbyUsername(username);
        List<Resume> resumes = resumeService.findByUser(user);

        if(resumes.isEmpty()){
            model.addAttribute("nouploads","No Resume Uploaded Yet");
        }

        model.addAttribute("username",username);
        model.addAttribute("resumes",resumes);

        return "viewmyresumes";
    }

    @GetMapping("/aboutus")
    public String aboutus(HttpSession session, Model model){
        String username = (String)session.getAttribute("loggedInUser");
        if(username==null){
            return "redirect:/airesumeanalyzer";
        }

        model.addAttribute("username",username);
        return "aboutuspage";

    }

    @GetMapping("/bestresumefinder")
    public String bestResumePage(HttpSession session, Model model){
        String username = (String)session.getAttribute("loggedInUser");
        if(username==null){
            return "redirect:/airesumeanalyzer";
        }

        model.addAttribute("username",username);
        return "jdpage";

    }

    @PostMapping("/bestresumefinder")
    public String bestResumePage(
            @RequestParam String jobTitle,
            @RequestParam String experience,
            @RequestParam String requiredSkills,
            @RequestParam(required = false) String preferredSkills,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String qualifications,
            HttpSession session,
            Model model){
        String username = (String)session.getAttribute("loggedInUser");
        if (username==null){
            return "redirect:/airesumeanalyzer";
        }

        String jobDescription = """
            Job Title: %s

            Experience: %s

            Required Skills:
            %s

            Preferred Skills:
            %s

            Education:
            %s

            Qualifications:
            %s
            """.formatted(
                jobTitle,
                experience,
                requiredSkills,
                preferredSkills,
                education,
                qualifications
        );

        return "bestresumeresult";
    }

    @GetMapping("/ats-score-generator")
    public String atsgenerator(HttpSession session, Model model){
        String username = (String)session.getAttribute("loggedInUser");
        if(username==null){
            return "redirect:/airesumeanalyzer";
        }

        model.addAttribute("username",username);
        return "atsscorepage";
    }

    @PostMapping("/ats-score-generator/results")
    public String atsgenerator(@RequestParam("resume")MultipartFile file, HttpSession session, Model model){

        if(session.getAttribute("loggedInUser")==null){
            return "redirect:/airesumeanalyzer";
        }

        try{

            if (file.isEmpty()) {
                model.addAttribute("error", "Please select a PDF file.");
                return "home";
            }

            PDDocument document = Loader.loadPDF(file.getBytes());

            PDFTextStripper textStripper = new PDFTextStripper();
            String resumeText = textStripper.getText(document);

            document.close();

            String analysis = groqService.atsScoreGeneration(resumeText);

            model.addAttribute("analysis",analysis);
            System.out.println(resumeText);
        }
        catch (IOException e){
            e.printStackTrace();
            model.addAttribute("error","Failed to read PDF File");
        }
        return "atsresult";
    }

    @PostMapping("/deleteresume/{id}")
    public String deleteResume(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes){
        String username = (String) session.getAttribute("loggedInUser");
        resumeService.deleteResume(id,username);

        redirectAttributes.addFlashAttribute("success","Resume deleted successfully...");
        return "redirect:/airesumeanalyzer/myresumes";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/airesumeanalyzer";
    }
}
