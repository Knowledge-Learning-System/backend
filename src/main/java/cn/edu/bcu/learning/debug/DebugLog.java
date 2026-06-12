package cn.edu.bcu.learning.debug;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class DebugLog {

    private static final Path LOG_PATH = Path.of("e:/work/debug-7894d3.log");
    private static final String SESSION_ID = "7894d3";

    private DebugLog() {
    }

    public static void log(String hypothesisId, String location, String message, String dataJson) {
        // #region agent log
        try {
            long ts = System.currentTimeMillis();
            String line = String.format(
                    "{\"sessionId\":\"%s\",\"hypothesisId\":\"%s\",\"location\":\"%s\",\"message\":\"%s\",\"data\":%s,\"timestamp\":%d}%n",
                    SESSION_ID, hypothesisId, location, escape(message), dataJson == null ? "{}" : dataJson, ts);
            Files.writeString(LOG_PATH, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
        // #endregion
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
