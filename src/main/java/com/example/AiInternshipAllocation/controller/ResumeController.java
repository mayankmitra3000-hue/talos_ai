package com.example.AiInternshipAllocation.controller;

import com.example.AiInternshipAllocation.entity.User;
import com.example.AiInternshipAllocation.entity.Resume;
import com.example.AiInternshipAllocation.service.*;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/airesumeanalyzer")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeVectorServiceImpl resumeVectorService;
    private final AuthService authService;
    private final GroqService groqService;
    private final BestResumeService bestResumeService;
//    private final HttpSession session; -----> Field injection not required when method parameter injection is available

    @GetMapping({"","/"})
    public String firstPage(){
        return "firstpage";
    }

    @GetMapping("/analyzeresume")
    public String analyzeresume(HttpSession session, Model model){
        String username = (String)session.getAttribute("loggedInUser");
        if(username==null){
            return "redirect:/airesumeanalyzer";
        }
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            User u = authService.findUserbyUsername(username);
            role = u != null && u.getRole() != null ? u.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
        }

        model.addAttribute("username",username);
        model.addAttribute("userRole", role);
        return "analyzeresumepage";

    }

    @PostMapping("/analyzeresume")
    public String analyzeResume(@RequestParam("resume")MultipartFile file, HttpSession session, Model model){

        if(session.getAttribute("loggedInUser")==null){
            return "redirect:/airesumeanalyzer";
        }
        String username = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            User u = authService.findUserbyUsername(username);
            role = u != null && u.getRole() != null ? u.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
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
             @RequestParam(required = false) String role,
             Model model){

        User user = new User();
        user.setUsername(username);
        user.setFullName(fullname);
        user.setPassword(password);
        user.setContact(contact);
        user.setEmail(email);
        if (role != null && !role.isBlank()) {
            user.setRole(role);
        } else {
            user.setRole("STUDENT");
        }

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
                User user = authService.findUserbyUsername(username);
                session.setAttribute("loggedInUser",username);
                if (user != null && user.getRole() != null) {
                    session.setAttribute("userRole", user.getRole());
                } else {
                    session.setAttribute("userRole", "STUDENT");
                }
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
        if (session.getAttribute("userRole") != null) {
            model.addAttribute("userRole", session.getAttribute("userRole"));
        } else {
            User user = authService.findUserbyUsername(username);
            String role = user != null && user.getRole() != null ? user.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
            model.addAttribute("userRole", role);
        }
        return "home";
    }

    @GetMapping("/uploadresume")
    public String resumeUploadPage(HttpSession session, Model model) {

        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "redirect:/airesumeanalyzer";
        }

        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            User u = authService.findUserbyUsername(username);
            role = u != null && u.getRole() != null ? u.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
        }

        model.addAttribute("username", username);
        model.addAttribute("userRole", role);

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

        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            User u = authService.findUserbyUsername(username);
            role = u != null && u.getRole() != null ? u.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
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

        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            User u = authService.findUserbyUsername(username);
            role = u != null && u.getRole() != null ? u.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
        }

        User user = authService.findUserbyUsername(username);
        List<Resume> resumes = resumeService.findByUser(user);

        if(resumes.isEmpty()){
            model.addAttribute("nouploads","No Resume Uploaded Yet");
        }

        model.addAttribute("username",username);
        model.addAttribute("userRole", role);
        model.addAttribute("resumes",resumes);

        return "viewmyresumes";
    }

    @GetMapping("/resume/view/{id}")
    public ResponseEntity<byte[]> viewResume(
            @PathVariable("id") Long resumeId,
            HttpSession session
    ) {

        String username =
                (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return ResponseEntity
                    .status(401)
                    .build();
        }

        Resume resume =
                resumeService.getResumeByIdAndUsername(
                        resumeId,
                        username
                );

        byte[] resumeFile = resume.getResumeFile();

        if (resumeFile == null || resumeFile.length == 0) {
            return ResponseEntity
                    .notFound()
                    .build();
        }

        String fileName = resume.getFileName();

        if (fileName == null || fileName.isBlank()) {
            fileName = "resume.pdf";
        }

        String encodedFileName = java.net.URLEncoder
                .encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + encodedFileName
                )
                .contentLength(resumeFile.length)
                .body(resumeFile);
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
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            User u = authService.findUserbyUsername(username);
            role = u != null && u.getRole() != null ? u.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
        }

        model.addAttribute("username",username);
        model.addAttribute("userRole", role);
        return "jdpage";

    }

    @PostMapping("/bestresumefinder")
    public String findBestResume(
            HttpSession session,
            @RequestParam String jobTitle,

            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String experience,

            @RequestParam String requiredSkills,

            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String preferredSkills,

            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String education,

            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String qualifications,

            Model model
    ) {
        String username = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (role == null && username != null) {
            User u = authService.findUserbyUsername(username);
            role = u != null && u.getRole() != null ? u.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
        }

        if (username == null) {
            return "redirect:/airesumeanalyzer";
        }

        /*
         * Server-side validation
         */
        if (jobTitle == null || jobTitle.isBlank()) {

            model.addAttribute(
                    "error",
                    "Job title is required."
            );

            preserveJobFormData(
                    model,
                    jobTitle,
                    experience,
                    requiredSkills,
                    preferredSkills,
                    education,
                    qualifications
            );

            return "jdpage";
        }

        if (requiredSkills == null
                || requiredSkills.isBlank()) {

            model.addAttribute(
                    "error",
                    "Required skills are required."
            );

            preserveJobFormData(
                    model,
                    jobTitle,
                    experience,
                    requiredSkills,
                    preferredSkills,
                    education,
                    qualifications
            );

            return "jdpage";
        }

        /*
         * Combine all form fields into one query.
         * Voyage AI will convert this text into
         * a query embedding.
         */
        String jobDescription = """
            Job Title:
            %s

            Required Experience:
            %s

            Required Skills:
            %s

            Preferred Skills:
            %s

            Education:
            %s

            Additional Qualifications:
            %s
            """.formatted(
                cleanValue(jobTitle),
                cleanValue(experience),
                cleanValue(requiredSkills),
                cleanValue(preferredSkills),
                cleanValue(education),
                cleanValue(qualifications)
        );

        try {

            /*
             * Voyage AI:
             * Converts jobDescription into query embedding.
             *
             * Qdrant:
             * Searches matching resume chunks.
             */
            List<Map<String, Object>> matches =
                    bestResumeService.findBestResumes(
                            username,
                            jobDescription
                    );

            if (matches.isEmpty()) {

                model.addAttribute(
                        "error",
                        "No matching indexed resumes were found."
                );

                preserveJobFormData(
                        model,
                        jobTitle,
                        experience,
                        requiredSkills,
                        preferredSkills,
                        education,
                        qualifications
                );

                return "jdpage";
            }

            /*
             * Send only the top five retrieved resumes
             * to Groq for final recruiter-style analysis.
             */
            List<Map<String, Object>> topMatches =
                    matches.stream()
                            .limit(5)
                            .toList();

            String aiComparison =
                    groqService.compareResumesWithJob(
                            jobDescription,
                            topMatches
                    );

            /*
             * Data for result page.
             */
            model.addAttribute("username", username);
            String userRoleAttr = (String) session.getAttribute("userRole");
            if (userRoleAttr == null) {
                User u2 = authService.findUserbyUsername(username);
                userRoleAttr = u2 != null && u2.getRole() != null ? u2.getRole() : "STUDENT";
                session.setAttribute("userRole", userRoleAttr);
            }
            model.addAttribute("userRole", userRoleAttr);
            model.addAttribute("matches", topMatches);
            model.addAttribute("aiComparison", aiComparison);

            model.addAttribute(
                    "jobDescription",
                    jobDescription
            );

            model.addAttribute(
                    "jobTitle",
                    cleanValue(jobTitle)
            );

            model.addAttribute(
                    "experience",
                    cleanValue(experience)
            );

            model.addAttribute(
                    "requiredSkills",
                    cleanValue(requiredSkills)
            );

            model.addAttribute(
                    "preferredSkills",
                    cleanValue(preferredSkills)
            );

            model.addAttribute(
                    "education",
                    cleanValue(education)
            );

            model.addAttribute(
                    "qualifications",
                    cleanValue(qualifications)
            );

            return "bestresumeresult";

        } catch (Exception exception) {

            exception.printStackTrace();

            model.addAttribute(
                    "error",
                    "Unable to find the best resume: "
                            + exception.getMessage()
            );

            preserveJobFormData(
                    model,
                    jobTitle,
                    experience,
                    requiredSkills,
                    preferredSkills,
                    education,
                    qualifications
            );

            return "bestresumeresult";
        }
    }

    private String cleanValue(String value) {

        if (value == null || value.isBlank()) {
            return "Not specified";
        }

        return value.trim();
    }

    private void preserveJobFormData(
            Model model,
            String jobTitle,
            String experience,
            String requiredSkills,
            String preferredSkills,
            String education,
            String qualifications
    ) {

        model.addAttribute(
                "jobTitle",
                jobTitle
        );

        model.addAttribute(
                "experience",
                experience
        );

        model.addAttribute(
                "requiredSkills",
                requiredSkills
        );

        model.addAttribute(
                "preferredSkills",
                preferredSkills
        );

        model.addAttribute(
                "education",
                education
        );

        model.addAttribute(
                "qualifications",
                qualifications
        );
    }

    @GetMapping("/ats-score-generator")
    public String atsgenerator(HttpSession session, Model model){
        String username = (String)session.getAttribute("loggedInUser");
        if(username==null){
            return "redirect:/airesumeanalyzer";
        }
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            User u = authService.findUserbyUsername(username);
            role = u != null && u.getRole() != null ? u.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
        }

        model.addAttribute("username",username);
        model.addAttribute("userRole", role);
        return "atsscorepage";
    }

    @PostMapping("/ats-score-generator/results")
    public String atsgenerator(@RequestParam("resume")MultipartFile file, HttpSession session, Model model){

        if(session.getAttribute("loggedInUser")==null){
            return "redirect:/airesumeanalyzer";
        }
        String username = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            User u = authService.findUserbyUsername(username);
            role = u != null && u.getRole() != null ? u.getRole() : "STUDENT";
            session.setAttribute("userRole", role);
        }
        if ("ADMIN".equals(role)) {
            return "redirect:/airesumeanalyzer/home";
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

    @PostMapping("/setdefaultresume/{id}")
    public String setDefaultResume(@PathVariable Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes){
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "redirect:/airesumeanalyzer";
        }

        try {
            resumeService.setDefaultResume(id, username);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Default resume updated successfully..."
            );
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/airesumeanalyzer/myresumes";
    }

    @GetMapping("/myprofile")
    public String myProfile(HttpSession session, Model model){

        String username = (String) session.getAttribute("loggedInUser");
        User user = authService.findUserbyUsername(username);

        model.addAttribute("user",user);
        model.addAttribute("username", username);

        return "myprofile";
    }

    @PostMapping("/myprofile")
    public String updateProfile(@ModelAttribute("user") User formUser,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model){
        String loggedInUsername =
                (String) session.getAttribute("loggedInUser");

        if (loggedInUsername == null) {
            return "redirect:/airesumeanalyzer";
        }

        User existingUser =
                authService.findUserbyUsername(loggedInUsername);

        if (existingUser == null) {
            session.invalidate();
            return "redirect:/airesumeanalyzer";
        }

        // Update only fields that the user is allowed to change
        existingUser.setFullName(formUser.getFullName());
        existingUser.setContact(formUser.getContact());
        existingUser.setEmail(formUser.getEmail());

        authService.updateProfile(existingUser);

        redirectAttributes.addFlashAttribute(
                "success",
                "Profile updated successfully."
        );

        return "redirect:/airesumeanalyzer/myprofile";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/airesumeanalyzer";
    }
}
