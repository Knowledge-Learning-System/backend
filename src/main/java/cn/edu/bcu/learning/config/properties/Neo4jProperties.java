package cn.edu.bcu.learning.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.neo4j")
public class Neo4jProperties {
    private String uri;
    private Authentication authentication = new Authentication();

    @Data
    public static class Authentication {
        private String username;
        private String password;
    }
}