import java.util.zip.ZipFile

def jar = new File(basedir, 'target/baseline-boot-package-missing-entry-1.0.0.jar')
assert jar.isFile(): "Boot JAR was not created: ${jar}"

new ZipFile(jar).withCloseable { zip ->
    assert zip.getEntry('BOOT-INF/classes/db/baseline/package-alpha/B1__baseline.sql') != null:
            'Positive control baseline SQL entry is missing'
    assert zip.getEntry('META-INF/mango/baseline-manifest.json') != null:
            'Missing generated resource in Boot JAR: META-INF/mango/baseline-manifest.json'
}
return true
