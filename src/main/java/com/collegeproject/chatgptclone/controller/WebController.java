
package com.collegeproject.chatgptclone.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model; // Import Model
import java.security.Principal; // Import Principal

@Controller
public class WebController {

    /**
     * Maps the root URL ("/") to the "chat" JSP view.
     * Passes the authenticated username to the view if available.
     * @param principal The authenticated user's principal (provided by Spring Security).
     * @param model Model to pass attributes to the JSP.
     * @return The name of the JSP view ("chat" which resolves to chat.jsp)
     */
    @GetMapping("/")
    public String showChatPage(Principal principal, Model model) {
        if (principal != null) {
            // If a user is logged in, add their username to the model
            model.addAttribute("username", principal.getName());
        }
        return "chat"; // This will resolve to /WEB-INF/jsp/chat.jsp
    }
}
