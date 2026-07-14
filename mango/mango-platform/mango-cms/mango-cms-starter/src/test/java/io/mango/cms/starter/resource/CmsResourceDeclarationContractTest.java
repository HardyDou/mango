package io.mango.cms.starter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CmsResourceDeclarationContractTest {

    private static final Path FORMAL_ROOT = Path.of("src/main/resources/META-INF/mango/resources");
    private static final Path DEMO_ROOT = Path.of("src/main/resources/META-INF/mango/demo");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> DEMO_TYPES = Set.of(
            CmsResourceTypes.SITE,
            CmsResourceTypes.SITE_SETTING,
            CmsResourceTypes.SITE_CATEGORY,
            CmsResourceTypes.CONTENT,
            CmsResourceTypes.CONTENT_PUBLISH,
            CmsResourceTypes.NAVIGATION,
            CmsResourceTypes.BANNER,
            CmsResourceTypes.ADVERTISEMENT,
            CmsResourceTypes.AD_DELIVERY);
    private static final Set<String> FILE_ID_FIELDS = Set.of(
            "logoFileId",
            "coverFileId",
            "attachmentFileId",
            "videoFileId",
            "mediaFileId",
            "materialFileId",
            "imageFileId",
            "imageFileIds");
    private static final Set<String> LOCAL_DATE_TIME_FIELDS = Set.of(
            "publishTime",
            "scheduledPublishTime",
            "offlineTime",
            "startTime",
            "endTime");

    @Test
    @DisplayName("CMS should own flat typed demo declarations and keep formal menu separate")
    void cmsOwnsTypedDemoDeclarationsAndFormalMenu() throws IOException {
        List<DeclaredResource> demo = declarations(DEMO_ROOT, "cms-demo-");

        assertThat(FORMAL_ROOT.resolve("cms-common-menu.json")).isRegularFile();
        assertThat(demo).hasSize(71);
        assertThat(types(demo)).containsExactlyInAnyOrderElementsOf(DEMO_TYPES);
        assertThat(demo).allSatisfy(resource -> {
            assertThat(resource.moduleCode()).isEqualTo("cms");
            assertThat(resource.targetModule()).isEqualTo("cms");
            assertThat(resource.syncMode()).isEqualTo("INIT_ONLY");
        });
        assertThat(demo.stream().map(DeclaredResource::id)).doesNotHaveDuplicates();
        assertThat(demo.stream().map(DeclaredResource::bizKey)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("CMS demo should be self-contained and publicly visible")
    void cmsDemoIsSelfContainedAndPubliclyVisible() throws IOException {
        List<DeclaredResource> demo = declarations(DEMO_ROOT, "cms-demo-");

        demo.forEach(resource -> fileFields(resource.node()).forEach(field -> {
            assertThat(field.path("type").asText()).as(resource.bizKey()).isEqualTo("STRING");
            assertThat(field.path("value").isNull()).as(resource.bizKey()).isTrue();
        }));
        demo.forEach(resource -> localDateTimeFields(resource.node()).forEach(field -> {
            if (!field.path("value").isNull()) {
                assertThat(field.path("value").asText())
                        .as(resource.bizKey())
                        .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
            }
        }));
        demo.stream()
                .filter(resource -> CmsResourceTypes.SITE_CATEGORY.equals(resource.type()))
                .forEach(resource -> assertThat(resource.node().path("fields").path("visibleStatus").path("value")
                        .asText()).isEqualTo("ENABLED"));
    }

    private List<JsonNode> fileFields(JsonNode declaration) {
        return fieldsNamed(declaration, FILE_ID_FIELDS);
    }

    private List<JsonNode> localDateTimeFields(JsonNode declaration) {
        return fieldsNamed(declaration, LOCAL_DATE_TIME_FIELDS);
    }

    private List<JsonNode> fieldsNamed(JsonNode declaration, Set<String> fieldNames) {
        List<JsonNode> result = new ArrayList<>();
        declaration.path("fields").fields().forEachRemaining(entry -> {
            if (fieldNames.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        });
        return result;
    }

    private List<DeclaredResource> declarations(Path root, String filePrefix) throws IOException {
        assertThat(root).isDirectory();
        List<Path> files;
        try (Stream<Path> paths = Files.walk(root)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(filePrefix))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .toList();
        }
        assertThat(files).allSatisfy(path -> assertThat(path.getParent()).isEqualTo(root));

        List<DeclaredResource> result = new ArrayList<>();
        for (Path path : files) {
            JsonNode resource = MAPPER.readTree(path.toFile()).path("mango").path("resource");
            Iterator<Map.Entry<String, JsonNode>> declarations = resource.path("declarations").fields();
            while (declarations.hasNext()) {
                Map.Entry<String, JsonNode> type = declarations.next();
                type.getValue().forEach(node -> result.add(new DeclaredResource(
                        type.getKey(),
                        resource.path("moduleCode").asText(),
                        node.path("id").asText(),
                        node.path("bizKey").asText(),
                        node.path("targetModule").asText(),
                        node.path("syncMode").asText(),
                        node)));
            }
        }
        return result;
    }

    private Set<String> types(List<DeclaredResource> declarations) {
        Set<String> result = new HashSet<>();
        declarations.forEach(resource -> result.add(resource.type()));
        return result;
    }

    private record DeclaredResource(
            String type,
            String moduleCode,
            String id,
            String bizKey,
            String targetModule,
            String syncMode,
            JsonNode node) {
    }
}
