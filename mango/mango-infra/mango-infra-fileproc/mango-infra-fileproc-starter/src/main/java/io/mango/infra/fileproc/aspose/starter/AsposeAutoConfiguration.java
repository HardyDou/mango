package io.mango.infra.fileproc.aspose.starter;

import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.DefaultAsposeLicenseApi;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Aspose 工具自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(AsposeProperties.class)
@ConditionalOnProperty(prefix = "mango.fileproc.aspose", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsposeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AsposeLicenseApi asposeLicenseApi(AsposeProperties properties, ResourceLoader resourceLoader) {
        Map<AsposeProduct, byte[]> licenses = new EnumMap<>(AsposeProduct.class);
        licenses.put(AsposeProduct.WORDS, readLicense(properties.getWordsLicenseLocation(),
                properties.getLicenseLocation(), resourceLoader));
        licenses.put(AsposeProduct.CELLS, readLicense(properties.getCellsLicenseLocation(),
                properties.getLicenseLocation(), resourceLoader));
        licenses.put(AsposeProduct.SLIDES, readLicense(properties.getSlidesLicenseLocation(),
                properties.getLicenseLocation(), resourceLoader));
        licenses.put(AsposeProduct.PDF, readLicense(properties.getPdfLicenseLocation(),
                properties.getLicenseLocation(), resourceLoader));
        licenses.put(AsposeProduct.IMAGING, readLicense(properties.getImagingLicenseLocation(),
                properties.getLicenseLocation(), resourceLoader));
        return new DefaultAsposeLicenseApi(licenses);
    }

    private byte[] readLicense(String dedicatedLocation, String fallbackLocation, ResourceLoader resourceLoader) {
        String location = firstText(dedicatedLocation, fallbackLocation);
        if (location == null) {
            return null;
        }
        try {
            if (location.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
                Resource resource = resourceLoader.getResource(location);
                if (!resource.exists()) {
                    return null;
                }
                try (java.io.InputStream inputStream = resource.getInputStream()) {
                    return inputStream.readAllBytes();
                }
            }
            Path path = Path.of(location);
            if (Files.isDirectory(path)) {
                path = path.resolve("license.xml");
            }
            if (!Files.exists(path)) {
                return null;
            }
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new IllegalStateException("读取 Aspose License 失败: " + location, ex);
        }
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
