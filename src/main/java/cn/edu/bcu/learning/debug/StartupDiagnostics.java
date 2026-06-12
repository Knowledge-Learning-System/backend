package cn.edu.bcu.learning.debug;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
public class StartupDiagnostics implements CommandLineRunner {

    private final Environment environment;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        String profile = String.join(",", environment.getActiveProfiles());
        String jdbcUrl = environment.getProperty("spring.datasource.url", "missing");
        String jdbcUser = environment.getProperty("spring.datasource.username", "missing");
        String neo4jUri = environment.getProperty("spring.neo4j.uri", "missing");

        // #region agent log
        DebugLog.log(
                "E",
                "StartupDiagnostics.java:run",
                "active config snapshot",
                "{\"profile\":\"" + profile + "\",\"jdbcUrl\":\"" + jdbcUrl.replace("\"", "\\\"") + "\",\"jdbcUser\":\"" + jdbcUser + "\",\"neo4jUri\":\"" + neo4jUri + "\"}");
        // #endregion

        try (Connection conn = dataSource.getConnection()) {
            String catalog = conn.getCatalog();
            String product = conn.getMetaData().getDatabaseProductVersion();
            // #region agent log
            DebugLog.log(
                    "C",
                    "StartupDiagnostics.java:mysql",
                    "mysql connection ok",
                    "{\"catalog\":\"" + (catalog == null ? "" : catalog) + "\",\"version\":\"" + product.replace("\"", "'") + "\"}");
            // #endregion
        } catch (Exception e) {
            String type = e.getClass().getSimpleName();
            String msg = e.getMessage() == null ? "" : e.getMessage().replace("\"", "'");
            // #region agent log
            DebugLog.log(
                    "A,B,C",
                    "StartupDiagnostics.java:mysql",
                    "mysql connection failed",
                    "{\"errorType\":\"" + type + "\",\"errorMsg\":\"" + msg + "\"}");
            // #endregion
        }
    }
}
