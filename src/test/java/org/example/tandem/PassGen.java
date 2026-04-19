package org.example.tandem;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PassGen {
    @Test
    void generate() {
        System.out.println(new BCryptPasswordEncoder().encode("admin1234"));
    }
}
