
package com.collegeproject.chatgptclone.controller;

import com.collegeproject.chatgptclone.model.User;
import com.collegeproject.chatgptclone.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
// import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Removed since flash attributes won't work for static HTML

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // No changes needed for @GetMapping("/login") as it just returns the view name,
    // and the client-side JavaScript will read the error/logout parameters.
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered, // NEW: Handle registered param
            Model model) {
        // These 'model.addAttribute' lines are technically for JSP, but won't harm for
        // HTML.
        // The JavaScript in login.html will now read these from URL params.
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (registered != null) { // NEW: Add success message for registration
            model.addAttribute("successMessage", "Registration successful! Please log in.");
        }
        // IMPORTANT: The return "login" here should correspond to your new static HTML
        // file path.
        // If your static file is at src/main/resources/static/login.html, Spring will
        // find it.
        return "login.html"; // Ensure this returns the HTML file directly
    }

    // No changes needed for @GetMapping("/signup") as it just returns the view
    // name.
    @GetMapping("/signup")
    public String showSignupPage(Model model) {
        model.addAttribute("user", new User()); // Still useful if you map a 'signup.html' directly
        return "signup.html"; // Ensure this returns the HTML file directly
    }

    @PostMapping("/signup")
    public String registerUser(@ModelAttribute("user") User user) { // Removed RedirectAttributes
        User registeredUser = userService.registerNewUser(user.getUsername(), user.getPassword());
        if (registeredUser != null) {
            // Changed from flash attribute to query parameter for static HTML
            return "redirect:/login?registered=true";
        } else {
            // Changed from flash attribute to query parameter for static HTML
            return "redirect:/signup?error=username_exists"; // You can use 'error' or a specific error code
        }
    }

    @GetMapping("/api/auth/status")
    @ResponseBody
    public Map<String, Object> getUserAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {
            response.put("loggedIn", true);
            response.put("username", authentication.getName());
        } else {
            response.put("loggedIn", false);
        }
        return response;
    }
}
