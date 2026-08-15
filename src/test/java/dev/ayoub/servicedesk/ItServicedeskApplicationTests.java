
package dev.ayoub.servicedesk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:servicedesk-test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "jwt.secret=AyoubServiceDeskJwtTestKey2026StrongSecretForTesting"
})
class ItServicedeskApplicationTests {

    @Test
    void contextLoads() {
    }
}