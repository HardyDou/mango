package io.mango.plugin.check;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantSetterAnalyzerTest {

    @TempDir
    Path directory;

    @Test
    void flagsTenantSetterOnNonProtocolReceiver() throws IOException {
        Path source = source("demo/Plain.java", """
                package demo;

                public class Plain {
                    void run() {
                        Plain other = new Plain();
                        other.setTenantId("1");
                    }

                    void setTenantId(String tenantId) {
                    }
                }
                """);

        TenantSetterAnalyzer.AnalysisResult result = TenantSetterAnalyzer.analyze(List.of(source));

        assertEquals(Map.of(source, 6), result.unsafeLines());
        assertTrue(result.unresolvedFiles().isEmpty());
    }

    @Test
    void ignoresTenantSetterOnApiCommandReceiver() throws IOException {
        Path source = source("demo/Service.java", """
                package demo;

                import io.mango.demo.api.command.DemoCommand;

                public class Service {
                    void run(DemoCommand command) {
                        command.setTenantId(1L);
                    }
                }
                """);

        TenantSetterAnalyzer.AnalysisResult result = TenantSetterAnalyzer.analyze(List.of(source));

        assertTrue(result.unsafeLines().isEmpty());
        assertTrue(result.unresolvedFiles().isEmpty());
    }

    @Test
    void ignoresTenantSetterOnThisFieldOfProtocolType() throws IOException {
        Path source = source("demo/Holder.java", """
                package demo;

                import io.mango.demo.api.vo.DemoVO;

                public class Holder {
                    private DemoVO vo;

                    void run() {
                        this.vo.setTenantId(1L);
                    }
                }
                """);

        TenantSetterAnalyzer.AnalysisResult result = TenantSetterAnalyzer.analyze(List.of(source));

        assertTrue(result.unsafeLines().isEmpty());
    }

    @Test
    void ignoresTenantSetterWhenApiPackageSuppliesProtocolType() throws IOException {
        Path source = source("io/mango/demo/api/Service.java", """
                package io.mango.demo.api;

                public class Service {
                    void run(DemoVO vo) {
                        vo.setTenantId(1L);
                    }
                }
                """);

        TenantSetterAnalyzer.AnalysisResult result = TenantSetterAnalyzer.analyze(List.of(source));

        assertTrue(result.unsafeLines().isEmpty());
    }

    @Test
    void reportsNothingWhenNoTenantSetterCall() throws IOException {
        Path source = source("demo/Clean.java", """
                package demo;

                public class Clean {
                    void run() {
                        String tenantId = "1";
                    }
                }
                """);

        TenantSetterAnalyzer.AnalysisResult result = TenantSetterAnalyzer.analyze(List.of(source));

        assertTrue(result.unsafeLines().isEmpty());
        assertTrue(result.unresolvedFiles().isEmpty());
    }

    @Test
    void reportsUnparsableSourcesAsUnresolved() throws IOException {
        Path source = source("demo/Broken.java", """
                package demo;

                public class Broken {
                    void run( {
                }
                """);

        TenantSetterAnalyzer.AnalysisResult result = TenantSetterAnalyzer.analyze(List.of(source));

        assertTrue(result.unsafeLines().isEmpty());
        assertEquals(List.of(source), List.copyOf(result.unresolvedFiles()));
    }

    private Path source(String relativePath, String content) throws IOException {
        Path file = directory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file.toAbsolutePath().normalize();
    }
}
