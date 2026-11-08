package com.scms.backend.scheduling;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.config.SecurityConfiguration;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CoachLeaveImpactController.class)
@Import({SecurityConfiguration.class,SecurityProblemDetailHandler.class})
@TestPropertySource(properties={"scms.cors.allowed-origins=http://localhost:5173","scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"})
class CoachLeaveImpactControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean CoachLeaveImpactService service;
    @MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;
    final UUID manager=UUID.randomUUID(),request=UUID.randomUUID(),session=UUID.randomUUID();
    String base() { return "/manager/coach-leave-requests/"+request; }
    @Test void managerReceivesImpactAndScopedCandidates() throws Exception {
        when(service.detail(manager,request)).thenReturn(new CoachLeaveImpactService.Impact(null,List.of()));
        mvc.perform(get(base()+"/impact").with(jwt().jwt(token->token.subject(manager.toString())).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sessions").isArray());
        mvc.perform(get(base()+"/affected-sessions/PT/"+session+"/available-coaches").with(jwt().jwt(token->token.subject(manager.toString())).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
            .andExpect(status().isOk());
        verify(service).detail(manager,request); verify(service).available(manager,request,"PT",session);
    }
    @Test void otherRolesCannotReadImpactsOrCandidates() throws Exception {
        for(String role:List.of("COACH","MEMBER","RECEPTIONIST")) {
            mvc.perform(get(base()+"/impact").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_"+role))))
                .andExpect(status().isForbidden());
            mvc.perform(get(base()+"/affected-sessions/CLASS/"+session+"/available-coaches").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_"+role))))
                .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }
}
