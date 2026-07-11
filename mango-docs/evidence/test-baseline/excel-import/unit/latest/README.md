# Excel Import UNIT Baseline

- Commit: `8cf15d95caee37d08026ccccbe0aa6a809e6a1fe`
- Date: 2026-07-11
- Environment: macOS, Java 21, Maven 3.9.13
- Dataset: tests dynamically create `.xlsx` workbooks covering shuffled Chinese titles, zero-based indexes, dictionary/custom conversion, formulas, dates, amounts, merged cells, empty rows, structural errors, failed-row workbooks, and classpath templates.

Reproduce:

```bash
mvn -f mango/pom.xml \
  -pl mango-infra/mango-infra-excel-starter \
  -am \
  -Dtest=PoiExcelAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test

node mango-pmo/tools/verify-targeted-mutations.mjs \
  --head HEAD \
  --catalog .runtime/pmo/excel-import-mutations.json \
  --report .runtime/pmo/targeted-mutations.json
```

Business assertions: title/idx mapping, Converter-over-dictionary priority, typed dictionary conversion, detailed conversion errors, failed-only workbook content, and byte-identical classpath templates all passed. Four curated mutation seeds were killed.
