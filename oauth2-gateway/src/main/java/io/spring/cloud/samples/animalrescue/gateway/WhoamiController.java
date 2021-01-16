package io.spring.cloud.samples.animalrescue.gateway;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoamiController {

    @RequestMapping("/whoami")
    public String greeting(@AuthenticationPrincipal OidcUser oidcUser) {
        return oidcUser.getEmail();
    }
}
