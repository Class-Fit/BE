package com.example.classfit.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigurationTest {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void loadsKakaoClientRegistrationFromApplicationConfiguration() {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("kakao");

        assertThat(registration).isNotNull();
        assertThat(registration.getProviderDetails().getAuthorizationUri())
                .isEqualTo("https://kauth.kakao.com/oauth/authorize");
        assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
                .isEqualTo("id");
    }
}
