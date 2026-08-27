import java.util.zip.ZipFile

def targetDirectory = new File(basedir, 'target')
def jar = new File(targetDirectory, 'resource-artifact-bootstrap-consumer-1.0.0.jar')
assert jar.isFile(): "Boot JAR was not created: ${jar}"

def hash = '925c60a89e25caf06a643971c66267d428aa1b16cd5ec13189971bfb8f0a1b8c'
def expectedEntries = [
        'META-INF/mango/resource-bootstrap-manifest.json',
        'META-INF/mango/files-manifest.json',
        "META-INF/mango/files.bundle/objects/${hash}"
]
new ZipFile(jar).withCloseable { zip ->
    expectedEntries.each { entry ->
        assert zip.getEntry(entry) != null: "Missing generated Resource artifact in Boot JAR: ${entry}"
    }
}

def verificationLog = new File(targetDirectory, 'bootstrap-consumer.log')
assert verificationLog.isFile(): "Packaged Bootstrap verification log was not created"
assert verificationLog.getText('UTF-8').contains('FILE_ASSET_BOOTSTRAP_VERIFIED'):
        "Packaged Bootstrap did not publish the FILE_ASSET object"
return true
