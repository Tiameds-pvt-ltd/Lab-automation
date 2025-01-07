package tiameds.com.tiameds;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("prod")  // Ensure 'prod' profile is active
class TiamedsApplicationTests {

    @Test
    void contextLoads() {
    }
}
