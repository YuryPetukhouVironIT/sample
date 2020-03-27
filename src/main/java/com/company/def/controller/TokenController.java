package com.company.def.controller;

import com.company.def.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/token")
public class TokenController {

    @Autowired
    private TokenService tokenService;

    @GetMapping("/createToken")
    @ResponseBody
    public String createToken() {
        return tokenService.createWpToken();
    }

    @RequestMapping("/confirmRegistration")
    public void confirmRegistration(final HttpServletRequest request,
                                    final HttpServletResponse response) throws Exception {
        response.sendRedirect(tokenService.registerDoctor(request.getRequestURL().toString() + "?" + request.getQueryString()));
    }

    @GetMapping("/createStlViewerToken")
    @ResponseBody
    public String createStlViewerToken() {
        return tokenService.createViewerToken();
    }

}
