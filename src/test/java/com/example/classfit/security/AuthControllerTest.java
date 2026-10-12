package com.example.classfit.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    @Test
    void exposesSessionCsrfTokenForLogout() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController()).build();
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "session-token");

        mvc.perform(get("/api/auth/csrf").requestAttr("_csrf", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("session-token"));
    }
}
