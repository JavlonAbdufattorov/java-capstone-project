package com.project.back_end.mvc;

import com.project.back_end.services.Service;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardControllerTest {
    @ParameterizedTest @ValueSource(strings = {"admin", "doctor"})
    void validTokenRendersDashboard(String role) throws Exception {
        Service service = mock(Service.class);
        when(service.validateToken("valid", role)).thenReturn(Map.of());
        MockMvcBuilders.standaloneSetup(new DashboardController(service)).build()
            .perform(get("/" + role + "Dashboard/valid"))
            .andExpect(status().isOk()).andExpect(view().name(role + "/" + role + "Dashboard"));
        verify(service).validateToken("valid", role);
    }
    @ParameterizedTest @ValueSource(strings = {"admin", "doctor"})
    void invalidTokenRedirectsToLogin(String role) throws Exception {
        Service service = mock(Service.class);
        when(service.validateToken("invalid", role)).thenReturn(Map.of("message", "Invalid token"));
        MockMvcBuilders.standaloneSetup(new DashboardController(service)).build()
            .perform(get("/" + role + "Dashboard/invalid"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost:8080"));
    }
}
