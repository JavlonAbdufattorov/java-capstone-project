package com.project.back_end.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptsPasswordFromJsonButOmitsItFromResponses() throws Exception {
        Admin admin = mapper.readValue(
                "{\"id\":1,\"username\":\"admin\",\"password\":\"test-secret\"}",
                Admin.class);

        assertThat(admin.getPassword()).isEqualTo("test-secret");
        var response = mapper.readTree(mapper.writeValueAsString(admin));
        assertThat(response.has("password")).isFalse();
        assertThat(response.get("id").asLong()).isEqualTo(1L);
        assertThat(response.get("username").asText()).isEqualTo("admin");
    }

    @Test
    void requiresUsernameAndPasswordButAllowsAnUnassignedId() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            Admin admin = new Admin();

            assertThat(validator.validate(admin))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("username", "password");

            admin.setUsername("admin");
            admin.setPassword("test-secret");
            assertThat(validator.validate(admin)).isEmpty();
            assertThat(admin.getId()).isNull();
        }
    }
}
