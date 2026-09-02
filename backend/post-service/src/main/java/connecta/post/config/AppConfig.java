package connecta.post.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        StorageProperties.class,
        AzureStorageProperties.class,
        ServiceBusProperties.class
})
public class AppConfig {
}
