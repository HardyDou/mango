import java.util.zip.ZipFile

def jar = new File(basedir, 'target/baseline-boot-package-1.0.0.jar')
assert jar.isFile(): "Boot JAR was not created: ${jar}"

def expectedEntries = [
        'BOOT-INF/classes/db/baseline/package-alpha/B1__baseline.sql',
        'BOOT-INF/classes/db/baseline/package-beta/B1__baseline.sql',
        'BOOT-INF/classes/META-INF/mango/baseline-manifest.json'
]
new ZipFile(jar).withCloseable { zip ->
    expectedEntries.each { entry ->
        assert zip.getEntry(entry) != null: "Missing generated resource in Boot JAR: ${entry}"
    }
}

assert !new File(basedir, 'src/main/resources/db/baseline').exists()
assert new File(basedir, 'target/generated-resources/META-INF/mango/baseline-manifest.json').isFile()
