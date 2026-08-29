def jar = new java.util.jar.JarFile(new File(basedir, 'target/resource-baseline-package-1.0.0.jar'))
assert jar.getEntry('BOOT-INF/classes/db/baseline/resource/B2__baseline.sql') != null
assert jar.getEntry('BOOT-INF/classes/db/baseline/kv/B1__baseline.sql') != null
assert jar.getEntry('BOOT-INF/classes/db/baseline/guarantee/B1__baseline.sql') != null
assert jar.getEntry('META-INF/mango/baseline-manifest.json') != null
jar.close()

def utf8Hex = { String value ->
    value.getBytes('UTF-8').encodeHex().toString().toUpperCase(Locale.ROOT)
}
def portableBizKey = utf8Hex('guarantee.product.standard')
def environmentBizKey = utf8Hex('guarantee.endpoint.bank')
def runtimeOnlyMarker = utf8Hex('runtime-only')

def guaranteeBaseline = new File(basedir, 'target/generated-resources/db/baseline/guarantee/B1__baseline.sql').text
assert guaranteeBaseline.contains("CONVERT(X'${portableBizKey}' USING utf8mb4)")
assert !guaranteeBaseline.contains(environmentBizKey)
assert !guaranteeBaseline.contains(runtimeOnlyMarker)

def resourceBaseline = new File(basedir, 'target/generated-resources/db/baseline/resource/B2__baseline.sql').text
assert resourceBaseline.contains("CONVERT(X'${portableBizKey}' USING utf8mb4)")
assert !resourceBaseline.contains(environmentBizKey)
assert !resourceBaseline.contains('INSERT IGNORE INTO `resource_module_receipt`')
assert !resourceBaseline.contains('INSERT IGNORE INTO `resource_sync_log`')
assert !resourceBaseline.contains('INSERT IGNORE INTO `resource_change_log`')
assert !resourceBaseline.contains('mango_bootstrap_execution')

new File(basedir, 'target/invoker-post-build-hook.success').text = 'ok\n'
return true
