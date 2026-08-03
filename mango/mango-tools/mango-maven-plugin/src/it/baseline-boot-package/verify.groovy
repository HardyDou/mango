import java.util.zip.ZipFile

def targetDirectory = new File(basedir, 'target')
def jar = new File(targetDirectory, 'baseline-boot-package-1.0.0.jar')
assert jar.isFile(): "Boot JAR was not created: ${jar}"

def expectedEntries = [
        'BOOT-INF/classes/db/baseline/package-alpha/B1__baseline.sql',
        'BOOT-INF/classes/db/baseline/package-beta/B1__baseline.sql',
        'META-INF/mango/baseline-manifest.json'
]
new ZipFile(jar).withCloseable { zip ->
    expectedEntries.each { entry ->
        assert zip.getEntry(entry) != null: "Missing generated resource in Boot JAR: ${entry}"
    }
    def alphaBaseline = zip.getInputStream(zip.getEntry(expectedEntries[0])).getText('UTF-8')
    assert alphaBaseline.contains('DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'):
            'Boot JAR baseline did not use the Mango CLI standard collation'
    def manifest = zip.getInputStream(zip.getEntry(expectedEntries[2])).getText('UTF-8')
    assert manifest.contains('"targetCharacterSet" : "utf8mb4"'):
            'Boot JAR manifest did not record the target character set'
    assert manifest.contains('"targetCollation" : "utf8mb4_unicode_ci"'):
            'Boot JAR manifest did not record the target collation'
}

assert !new File(basedir, 'src/main/resources/db/baseline').exists()
assert new File(basedir, 'target/generated-resources/META-INF/mango/baseline-manifest.json').isFile()

new File(targetDirectory, 'verified-boot-entries.txt').text =
        expectedEntries.join(System.lineSeparator()) + System.lineSeparator()
new File(targetDirectory, 'invoker-post-build-hook.success').text =
        "verified=${jar.name}${System.lineSeparator()}"
return true
