import java.util.zip.ZipFile

def targetDirectory = new File(basedir, 'target')
def jar = new File(targetDirectory, 'resource-artifact-boot-package-1.0.0.jar')
assert jar.isFile(): "Boot JAR was not created: ${jar}"

def hash = '1f01e8b56fb275da665ed10d0d36a63df948d6a7657bb4a13c7b19bd8ef47070'
def expectedEntries = [
        'META-INF/mango/resource-bootstrap-manifest.json',
        'META-INF/mango/files-manifest.json',
        "META-INF/mango/files.bundle/objects/${hash}"
]
new ZipFile(jar).withCloseable { zip ->
    expectedEntries.each { entry ->
        assert zip.getEntry(entry) != null: "Missing generated Resource artifact in Boot JAR: ${entry}"
    }
    def resourceManifest = zip.getInputStream(zip.getEntry(expectedEntries[0])).getText('UTF-8')
    assert resourceManifest.contains('"moduleCode":"package-fixture"')
    assert resourceManifest.contains('"declarationCount":1')
    def filesManifest = zip.getInputStream(zip.getEntry(expectedEntries[1])).getText('UTF-8')
    assert filesManifest.contains(hash)
    assert filesManifest.contains('package-fixture/document.txt')
    assert zip.getInputStream(zip.getEntry(expectedEntries[2])).getText('UTF-8') ==
            'issue-851 packaging fixture\n'
}

assert !new File(basedir, 'src/main/resources/META-INF/mango/resource-bootstrap-manifest.json').exists()
assert new File(basedir,
        'target/classes/META-INF/mango/resource-bootstrap-manifest.json').isFile()
return true
