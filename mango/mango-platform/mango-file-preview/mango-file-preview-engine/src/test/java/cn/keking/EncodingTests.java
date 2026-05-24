package cn.keking;

import cn.keking.utils.EncodingDetects;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 编码检测测试类
 *
 * @author asiawu3
 * @create 2023-07-24 16:53
 **/
public class EncodingTests {
    @Test
    void testCharDet() throws URISyntaxException {
        for (int i = 0; i < 29; i++) {
            URL resource = getClass().getClassLoader().getResource(Paths.get("testData", String.valueOf(i)).toString());
            assertNotNull(resource);
            File dir = new File(resource.toURI());
            String dirPath = dir.getPath();
            String textFileName = dir.list()[0];
            String textFilePath = dirPath + "/" + textFileName;
            System.out.printf("%-15s -->\t %-10s\n", textFileName, EncodingDetects.getJavaEncode(textFilePath));
        }
    }
}
