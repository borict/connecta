package connecta.eureka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

class EurekaServerApplicationTest {

    @Test
    void enablesEurekaServer() {
        assertThat(EurekaServerApplication.class.getAnnotation(SpringBootApplication.class)).isNotNull();
        assertThat(EurekaServerApplication.class.getAnnotation(EnableEurekaServer.class)).isNotNull();
    }
}
