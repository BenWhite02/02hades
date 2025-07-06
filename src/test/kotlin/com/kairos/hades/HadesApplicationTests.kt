// =============================================================================
// 🔥 HADES BACKEND - APPLICATION TEST
// =============================================================================
// File: src/test/kotlin/com/kairos/hades/HadesApplicationTests.kt
// Purpose: Simple test to verify Spring Boot application starts

package com.kairos.hades

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class HadesApplicationTests {

    @Test
    fun contextLoads() {
        // This test ensures that the Spring context loads successfully
        println("🔥 Hades application context loaded successfully!")
    }
}
