# Excel Import API Baseline

- Commit: `8cf15d95caee37d08026ccccbe0aa6a809e6a1fe`
- Date: 2026-07-11
- Environment: macOS, Java 21, Maven 3.9.13, Spring MockMvc, H2
- Proof path: real multipart HTTP request -> argument resolver/import orchestration -> default POI adapter -> Jakarta/business validation -> transactional service -> failure file store.

Reproduce:

```bash
mvn -f mango/pom.xml \
  -pl mango-infra/mango-infra-excel-starter,mango-infra/mango-infra-persistence/mango-infra-persistence-web-starter \
  -am \
  -Dtest=PoiExcelMvcIntegrationTest,BaseCrudControllerTest,ExcelImportTransactionIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Business assertions: multipart import uses the default adapter; Jakarta and multiple business errors are aggregated; PARTIAL_SUCCESS commits only valid rows; ALL_SUCCESS rolls back the whole H2 transaction after a runtime failure; failed workbook storage returns a Mango File ID.
