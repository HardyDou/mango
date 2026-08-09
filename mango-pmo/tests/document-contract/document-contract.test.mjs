import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

import {
  loadContract,
  repositoryPath,
} from "../../tools/document-contract/contract-loader.mjs";
import { checkDocumentSet } from "../../tools/check-document-set.mjs";
import {
  parseMarkdown,
  tableKey,
} from "../../tools/document-contract/markdown-ast.mjs";
import {
  parseLifecycleArgs,
  sha256,
  validateLifecycle,
} from "../../tools/document-contract/lifecycle.mjs";
import { pinHistoricalPmoVersionDocuments } from "../../tools/pin-historical-pmo-version-documents.mjs";
import { validateDocument } from "../../tools/document-contract/validator.mjs";

const TEST_DIR = path.dirname(fileURLToPath(import.meta.url));
const FIXTURES = path.join(TEST_DIR, "fixtures");

const STAGES = [
  {
    name: "business-requirements",
    contract: "mango-pmo/contracts/business-requirements.json",
    valid: "business-requirements.md",
  },
  {
    name: "system-requirements",
    contract: "mango-pmo/contracts/system-requirements.json",
    valid: "system-requirements.md",
  },
  {
    name: "technical-design",
    contract: "mango-pmo/contracts/technical-design.json",
    valid: "technical-design.md",
  },
  {
    name: "implementation-plan",
    contract: "mango-pmo/contracts/implementation-plan.json",
    valid: "implementation-plan.md",
  },
];

function readFixture(relativePath) {
  return fs.readFileSync(path.join(FIXTURES, relativePath), "utf8");
}

function hydrateLifecycle(levels = {}, pmoVersion = "1.3.13") {
  const brd = readFixture("valid/business-requirements.md")
    .replace("riskLevel: L2", `riskLevel: ${levels.brd ?? "L2"}`)
    .replace("pmoVersion: 1.3.13", `pmoVersion: ${pmoVersion}`);
  const srs = readFixture("valid/system-requirements.md")
    .replace("riskLevel: L2", `riskLevel: ${levels.srs ?? "L2"}`)
    .replace("pmoVersion: 1.3.13", `pmoVersion: ${pmoVersion}`)
    .replace("0".repeat(64), sha256(brd));
  const tdd = readFixture("valid/technical-design.md")
    .replace("riskLevel: L2", `riskLevel: ${levels.tdd ?? "L2"}`)
    .replace("pmoVersion: 1.3.13", `pmoVersion: ${pmoVersion}`)
    .replace("0".repeat(64), sha256(srs));
  const plan = readFixture("valid/implementation-plan.md")
    .replace("riskLevel: L2", `riskLevel: ${levels.plan ?? "L2"}`)
    .replace("pmoVersion: 1.3.13", `pmoVersion: ${pmoVersion}`)
    .replace("0".repeat(64), sha256(tdd));
  return {
    brd: {
      source: brd,
      resolved: path.join(FIXTURES, "valid/business-requirements.md"),
    },
    srs: {
      source: srs,
      resolved: path.join(FIXTURES, "valid/system-requirements.md"),
    },
    tdd: {
      source: tdd,
      resolved: path.join(FIXTURES, "valid/technical-design.md"),
    },
    plan: {
      source: plan,
      resolved: path.join(FIXTURES, "valid/implementation-plan.md"),
    },
  };
}

function writeHydratedDocumentSet(t, { pmoVersion } = {}) {
  const root = fs.mkdtempSync(
    path.join(process.env.TMPDIR || "/tmp", "mango-pmo-document-set-"),
  );
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  fs.cpSync(path.join(FIXTURES, "valid/review"), path.join(root, "review"), {
    recursive: true,
  });
  const documents = hydrateLifecycle({}, pmoVersion);
  for (const [key, document] of Object.entries(documents)) {
    fs.writeFileSync(path.join(root, `${key}.md`), document.source);
  }
  return root;
}

for (const stage of STAGES) {
  test(`${stage.name} 正例通过合同检查`, () => {
    const contract = loadContract(stage.contract);
    const result = validateDocument(
      readFixture(`valid/${stage.valid}`),
      contract,
      {
        documentPath: path.join(FIXTURES, "valid", stage.valid),
      },
    );
    assert.deepEqual(result.findings, []);
  });

  test(`${stage.name} 模板与合同章节和表格一致`, () => {
    const contract = loadContract(stage.contract);
    const ast = parseMarkdown(
      fs.readFileSync(repositoryPath(contract.template), "utf8"),
    );
    assert.deepEqual(
      ast.sections.map((section) => section.logicalTitle),
      contract.sections.map((section) => section.title),
    );
    for (const sectionSpec of contract.sections) {
      const section = ast.sections.find(
        (candidate) => candidate.logicalTitle === sectionSpec.title,
      );
      assert.ok(section, `missing section ${sectionSpec.title}`);
      const actual = new Set(
        section.tables.map((table) => tableKey(table.headers)),
      );
      for (const tableSpec of sectionSpec.tables)
        assert.ok(
          actual.has(tableKey(tableSpec.headers)),
          `missing table in ${sectionSpec.title}`,
        );
    }
  });
}

test("文档 pmoVersion 必须与版本化合同一致", () => {
  const contract = loadContract(
    "mango-pmo/contracts/business-requirements.json",
  );
  const source = readFixture("valid/business-requirements.md").replace(
    "pmoVersion: 1.3.13",
    "pmoVersion: 9.9.9",
  );
  const result = validateDocument(source, contract);
  assert.ok(
    result.findings.some(
      (finding) =>
        finding.ruleId === "BRD-META-001" &&
        finding.message.includes("pmoVersion 必须为 1.3.13"),
    ),
  );
  const historical = validateDocument(
    readFixture("valid/business-requirements.md").replace(
      "pmoVersion: 1.3.13",
      "pmoVersion: 1.3.6",
    ),
    contract,
  );
  assert.ok(
    historical.findings.some(
      (finding) =>
        finding.ruleId === "BRD-META-001" &&
        finding.message.includes("pmoVersion 必须为 1.3.13"),
    ),
  );
});

test("NEXT 的本地审批证据必须存在且禁止路径穿越", () => {
  const contract = loadContract(
    "mango-pmo/contracts/business-requirements.json",
  );
  const documentPath = path.join(FIXTURES, "valid/business-requirements.md");
  const source = readFixture("valid/business-requirements.md");
  const missing = validateDocument(
    source.replace("review/BRD-ANN-001.md", "review/NOT-FOUND.md"),
    contract,
    { documentPath },
  );
  assert.ok(
    missing.findings.some(
      (finding) =>
        finding.ruleId === "BRD-META-001" &&
        finding.message.includes("本地审批证据不存在"),
    ),
  );
  const traversal = validateDocument(
    source.replace("review/BRD-ANN-001.md", "../review/BRD-ANN-001.md"),
    contract,
    { documentPath },
  );
  assert.ok(
    traversal.findings.some(
      (finding) =>
        finding.ruleId === "BRD-META-001" &&
        finding.message.includes("路径穿越"),
    ),
  );
});

test("SRS、TDD 和 Plan 允许显式声明无前置文档，但两个上游字段必须同时为 NONE", () => {
  for (const stage of STAGES.slice(1)) {
    const contract = loadContract(stage.contract);
    const source = readFixture(`valid/${stage.valid}`);
    const independent = source
      .replace(/^upstreamDocumentId: .*$/mu, "upstreamDocumentId: NONE")
      .replace(/^upstreamDocumentHash: .*$/mu, "upstreamDocumentHash: NONE");
    assert.deepEqual(
      validateDocument(independent, contract).findings,
      [],
      stage.name,
    );

    for (const halfNone of [
      source.replace(/^upstreamDocumentId: .*$/mu, "upstreamDocumentId: NONE"),
      source.replace(
        /^upstreamDocumentHash: .*$/mu,
        "upstreamDocumentHash: NONE",
      ),
    ]) {
      assert.ok(
        validateDocument(halfNone, contract).findings.some(
          (finding) =>
            finding.ruleId === contract.metadata.ruleId &&
            finding.message.includes("必须同时为 NONE"),
        ),
        stage.name,
      );
    }
  }
});

test("只启用 SRS 时可直接追踪人工确认基线，不反向要求 BRD", () => {
  const contract = loadContract("mango-pmo/contracts/system-requirements.json");
  const independent = readFixture("valid/system-requirements.md")
    .replace(
      "riskAssessmentEvidence: BRD-ANN-001 risk assessment",
      "riskAssessmentEvidence: human-confirmed assurance baseline task-123",
    )
    .replace(/^upstreamDocumentId: .*$/mu, "upstreamDocumentId: NONE")
    .replace(/^upstreamDocumentHash: .*$/mu, "upstreamDocumentHash: NONE")
    .replace("| SC-001 | BS-001, BG-001, BF-001 |", "| SC-001 | NONE |")
    .replace("| SA-001 | BA-001 |", "| SA-001 | NONE |")
    .replace(
      "| FR-001 | BG-001, BF-001, BR-001, BAC-001 |",
      "| FR-001 | NONE |",
    )
    .replace("| UC-001 | BF-001 |", "| UC-001 | NONE |")
    .replace("| DR-001 | BO-001, BR-001, FR-001 |", "| DR-001 | FR-001 |")
    .replace("| IR-001 | BF-001, BR-001, FR-001 |", "| IR-001 | FR-001 |")
    .replace(
      "| NFR-001 | BG-001, BS-001, BR-001, BAC-001 |",
      "| NFR-001 | NONE |",
    )
    .replace("| SAC-001 | BAC-001 |", "| SAC-001 | NONE |")
    .replace(
      "| BP-001, BG-001, BS-001, BS-002, BA-001, BO-001, BF-001, BR-001, BAC-001 |",
      "| NONE |",
    );
  assert.deepEqual(validateDocument(independent, contract).findings, []);
});

test("每个规范章节都包含完整章节级契约", () => {
  const ruleSources = [
    ...STAGES.map((stage) => loadContract(stage.contract).ruleSource),
    "mango-pmo/rules/product/05-document-lifecycle.md",
  ];
  const labels = [
    "**目的**",
    "**正向要求**",
    "**禁止项**",
    "**正例**",
    "**反例**",
    "**机器判定**",
  ];
  for (const ruleSource of ruleSources) {
    const ast = parseMarkdown(
      fs.readFileSync(repositoryPath(ruleSource), "utf8"),
    );
    for (const section of ast.sections) {
      const text = section.nodes
        .map((node) => node.value ?? node.title ?? "")
        .join("\n");
      for (const label of labels)
        assert.ok(
          text.includes(label),
          `${ruleSource} / ${section.title} 缺少 ${label}`,
        );
    }
  }
});

test("规则和模板没有 trailing blank line", () => {
  const files = [
    ...STAGES.flatMap((stage) => {
      const contract = loadContract(stage.contract);
      return [contract.ruleSource, contract.template];
    }),
    "mango-pmo/rules/product/05-document-lifecycle.md",
  ];
  for (const file of files) {
    const content = fs.readFileSync(repositoryPath(file), "utf8");
    assert.ok(content.endsWith("\n"), `${file} 必须以换行结束`);
    assert.ok(!content.endsWith("\n\n"), `${file} 不能以空白行结束`);
  }
});

test("反例变异必须命中声明的 ruleId", () => {
  const contractByBase = new Map(
    STAGES.map((stage) => [stage.valid, loadContract(stage.contract)]),
  );
  const specs = fs
    .readdirSync(path.join(FIXTURES, "invalid"))
    .filter(
      (name) => name.endsWith(".json") && name !== "l2-blank-context.json",
    );
  for (const name of specs) {
    const specPath = path.join(FIXTURES, "invalid", name);
    const spec = JSON.parse(fs.readFileSync(specPath, "utf8"));
    const basePath = path.resolve(path.dirname(specPath), spec.base);
    const base = fs.readFileSync(basePath, "utf8");
    assert.ok(base.includes(spec.replace), `${name} 的 replace 片段不存在`);
    const invalid = base.replace(spec.replace, spec.with);
    const contract = contractByBase.get(path.basename(basePath));
    const result = validateDocument(invalid, contract);
    assert.ok(
      result.findings.some((finding) => finding.ruleId === spec.expectedRuleId),
      `${name} 未命中 ${spec.expectedRuleId}`,
    );
  }
});

test("完整 L2 生命周期通过摘要、审批和双向追踪检查", () => {
  const result = validateLifecycle(hydrateLifecycle(), { riskLevel: "L2" });
  assert.deepEqual(result.findings, []);
});

test("需求影响可以在技术方案阶段升级并由实施计划继承最终等级", () => {
  const result = validateLifecycle(
    hydrateLifecycle({ brd: "L1", srs: "L1", tdd: "L2", plan: "L2" }),
    { riskLevel: "L2" },
  );
  assert.deepEqual(result.findings, []);
});

test("下游阶段禁止把已识别风险降级", () => {
  const result = validateLifecycle(
    hydrateLifecycle({ brd: "L2", srs: "L1", tdd: "L2", plan: "L2" }),
    { riskLevel: "L2" },
  );
  assert.ok(
    result.findings.some(
      (finding) =>
        finding.ruleId === "LIFE-RISK-001" &&
        finding.message.includes("禁止下游降级"),
    ),
  );
});

test("入口风险必须匹配当前阶段，TDD 和 Plan 必须保持同一最终等级", () => {
  const entryMismatch = validateLifecycle(
    hydrateLifecycle({ brd: "L1", srs: "L1", tdd: "L2", plan: "L2" }),
    { riskLevel: "L1" },
  );
  assert.ok(
    entryMismatch.findings.some(
      (finding) =>
        finding.ruleId === "LIFE-RISK-001" &&
        finding.message.includes("与当前入口 L1 不一致"),
    ),
  );

  const finalMismatch = validateLifecycle(
    hydrateLifecycle({ brd: "L1", srs: "L1", tdd: "L2", plan: "L3" }),
    { riskLevel: "L3" },
  );
  assert.ok(
    finalMismatch.findings.some(
      (finding) =>
        finding.ruleId === "LIFE-RISK-001" &&
        finding.message.includes("必须使用同一最终风险等级"),
    ),
  );
});

test("业务文档集合自动发现并检查四阶段文档", (t) => {
  const root = writeHydratedDocumentSet(t);
  const result = checkDocumentSet(root);
  assert.equal(result.documents.length, 4);
  assert.deepEqual(result.findings, []);
});

test("业务文档集合允许合同声明的历史 PMO 版本", (t) => {
  const root = writeHydratedDocumentSet(t, { pmoVersion: "1.3.6" });
  const beforePinning = checkDocumentSet(root);
  assert.ok(
    beforePinning.findings.some((finding) => finding.ruleId === "LIFE-HASH-020"),
  );
  const pinned = pinHistoricalPmoVersionDocuments(root);
  assert.equal(pinned.added.length, 4);
  const result = checkDocumentSet(root);
  assert.deepEqual(result.findings, []);
  assert.equal(result.historicalPmoVersionDocuments.length, 4);
  assert.ok(
    result.historicalPmoVersionDocuments.every(
      (document) => document.pmoVersion === "1.3.6",
    ),
  );
  const brdPath = path.join(root, "brd.md");
  fs.writeFileSync(brdPath, `${fs.readFileSync(brdPath, "utf8").trimEnd()}\n\n未经基线批准的变更\n`);
  const changed = checkDocumentSet(root);
  assert.ok(
    changed.findings.some(
      (finding) =>
        finding.ruleId === "LIFE-HASH-020" && finding.file === brdPath,
    ),
  );
});

test("业务文档集合拒绝合同未声明的历史 PMO 版本", (t) => {
  const root = writeHydratedDocumentSet(t, { pmoVersion: "1.3.5" });
  const pinned = pinHistoricalPmoVersionDocuments(root);
  assert.equal(pinned.added.length, 0);
  const result = checkDocumentSet(root);
  assert.ok(
    result.findings.some(
      (finding) =>
        finding.ruleId === "BRD-META-001" &&
        finding.message.includes("pmoVersion 必须为 1.3.13"),
    ),
  );
});

test("业务文档集合阻断缺少类型、未知类型和失效摘要", (t) => {
  const root = writeHydratedDocumentSet(t);
  fs.writeFileSync(
    path.join(root, "missing-type.md"),
    "# 绕过合同的业务需求说明书\n",
  );
  fs.writeFileSync(
    path.join(root, "unknown-type.md"),
    "---\ndocumentType: combined-prd\n---\n\n# 混合 PRD\n",
  );
  const brdPath = path.join(root, "brd.md");
  fs.writeFileSync(
    brdPath,
    `${fs.readFileSync(brdPath, "utf8").trimEnd()}\n\n未经下游同步的变更\n`,
  );

  const result = checkDocumentSet(root);
  assert.ok(
    result.findings.some(
      (item) =>
        item.ruleId === "LIFE-ORDER-010" &&
        item.file.endsWith("missing-type.md"),
    ),
  );
  assert.ok(
    result.findings.some(
      (item) =>
        item.ruleId === "LIFE-ORDER-010" &&
        item.message.includes("combined-prd"),
    ),
  );
  assert.ok(result.findings.some((item) => item.ruleId === "LIFE-HASH-020"));
});

test("业务文档集合允许独立 SRS、TDD 和 Plan 跳过上游查找", (t) => {
  const root = fs.mkdtempSync(
    path.join(process.env.TMPDIR || "/tmp", "mango-pmo-independent-docs-"),
  );
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  fs.cpSync(path.join(FIXTURES, "valid/review"), path.join(root, "review"), {
    recursive: true,
  });
  for (const stage of STAGES.slice(1)) {
    const independent = readFixture(`valid/${stage.valid}`)
      .replace(/^upstreamDocumentId: .*$/mu, "upstreamDocumentId: NONE")
      .replace(/^upstreamDocumentHash: .*$/mu, "upstreamDocumentHash: NONE");
    fs.writeFileSync(path.join(root, stage.valid), independent);
  }

  const result = checkDocumentSet(root);
  assert.equal(result.documents.length, 3);
  assert.deepEqual(result.findings, []);
});

test("业务文档集合仅允许哈希锁定的历史生命周期文档", (t) => {
  const root = fs.mkdtempSync(
    path.join(process.env.TMPDIR || "/tmp", "mango-pmo-legacy-docs-"),
  );
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const legacyPath = path.join(root, "legacy-plan.md");
  const legacySource = "# 历史实施计划\n\n存量内容。\n";
  fs.writeFileSync(legacyPath, legacySource);
  fs.writeFileSync(
    path.join(root, ".mango-pmo-legacy-documents.json"),
    `${JSON.stringify(
      {
        schemaVersion: 1,
        documents: [
          {
            path: "legacy-plan.md",
            sha256: sha256(legacySource),
            reason: "PMO 合同启用前形成，单独迁移",
          },
        ],
      },
      null,
      2,
    )}\n`,
  );

  const pinned = checkDocumentSet(root);
  assert.deepEqual(pinned.findings, []);
  assert.equal(pinned.legacyDocuments.length, 1);

  fs.writeFileSync(legacyPath, `${legacySource}\n未经基线批准的变化\n`);
  const changed = checkDocumentSet(root);
  assert.ok(
    changed.findings.some(
      (item) => item.ruleId === "LIFE-HASH-020" && item.file === legacyPath,
    ),
  );
});

test("业务文档集合阻断失效或越界的历史文档基线项", (t) => {
  const root = fs.mkdtempSync(
    path.join(process.env.TMPDIR || "/tmp", "mango-pmo-stale-docs-"),
  );
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  fs.writeFileSync(
    path.join(root, ".mango-pmo-legacy-documents.json"),
    `${JSON.stringify(
      {
        schemaVersion: 1,
        documents: [
          { path: "missing-plan.md", sha256: "0".repeat(64), reason: "待迁移" },
          { path: "../outside.md", sha256: "0".repeat(64), reason: "非法路径" },
        ],
      },
      null,
      2,
    )}\n`,
  );

  const result = checkDocumentSet(root);
  assert.ok(
    result.findings.some((item) =>
      item.message.includes("历史文档基线路径非法"),
    ),
  );
  assert.ok(
    result.findings.some((item) =>
      item.message.includes("历史文档基线项已失效"),
    ),
  );
});

test("FULL 产品流程按当前阶段连续 handoff，不要求未来文档提前存在", () => {
  const documents = hydrateLifecycle({
    brd: "L3",
    srs: "L3",
    tdd: "L3",
    plan: "L3",
  });
  const staged = validateLifecycle(
    { brd: documents.brd, srs: documents.srs },
    { riskLevel: "L3", deliveryMode: "FULL", throughStage: "srs" },
  );
  assert.deepEqual(staged.findings, []);

  const missingUpstream = validateLifecycle(
    { srs: documents.srs },
    { riskLevel: "L3", deliveryMode: "FULL", throughStage: "srs" },
  );
  assert.ok(
    missingUpstream.findings.some(
      (finding) => finding.ruleId === "LIFE-ORDER-010",
    ),
  );
});

test("上游内容变化会使下游摘要立即失效", () => {
  const documents = hydrateLifecycle();
  documents.brd.source = `${documents.brd.source.trimEnd()}\n\n变更后的业务事实\n`;
  const result = validateLifecycle(documents, { riskLevel: "L2" });
  assert.ok(
    result.findings.some((finding) => finding.ruleId === "LIFE-HASH-020"),
  );
});

test("FULL 产品流程禁止用空文档集合绕过适用阶段", () => {
  const result = validateLifecycle(
    {},
    { riskLevel: "L3", deliveryMode: "FULL", requiredStages: [] },
  );
  assert.ok(
    result.findings.some(
      (finding) =>
        finding.ruleId === "LIFE-ORDER-010" &&
        finding.message.includes("至少一个适用生命周期阶段"),
    ),
  );
});

test("FULL 技术型产品任务可按模式基线声明仅 TDD 适用", () => {
  const documents = hydrateLifecycle({ tdd: "L3" });
  documents.tdd.source = documents.tdd.source
    .replace(/^upstreamDocumentId: .*$/mu, "upstreamDocumentId: NONE")
    .replace(/^upstreamDocumentHash: .*$/mu, "upstreamDocumentHash: NONE");
  const result = validateLifecycle(
    { tdd: documents.tdd },
    {
      riskLevel: "L3",
      deliveryMode: "FULL",
      requiredStages: ["tdd"],
      applicabilityEvidence: path.join(FIXTURES, "valid/review/BRD-ANN-001.md"),
    },
  );
  assert.deepEqual(result.findings, []);
});

test("FULL 产品流程缩减阶段但缺少外部适用性证据时失败", () => {
  const documents = hydrateLifecycle({ tdd: "L3" });
  const result = validateLifecycle(
    { tdd: documents.tdd },
    { riskLevel: "L3", deliveryMode: "FULL", requiredStages: ["tdd"] },
  );
  assert.ok(
    result.findings.some(
      (finding) =>
        finding.ruleId === "LIFE-ORDER-010" &&
        finding.message.includes("外部适用性证据文件"),
    ),
  );
});

test("生命周期只对显式引用且实际存在的相邻上游做摘要和追踪检查", () => {
  const documents = hydrateLifecycle();
  documents.srs.source = documents.srs.source
    .replace(/^upstreamDocumentId: .*$/mu, "upstreamDocumentId: NONE")
    .replace(/^upstreamDocumentHash: .*$/mu, "upstreamDocumentHash: NONE");
  const result = validateLifecycle(
    { brd: documents.brd, srs: documents.srs },
    { riskLevel: "L2", requiredStages: ["brd", "srs"] },
  );
  assert.deepEqual(result.findings, []);
});

test("显式选择的生命周期文档缺失时失败", () => {
  const result = validateLifecycle(
    {},
    { riskLevel: "L3", requiredStages: ["tdd"] },
  );
  assert.ok(
    result.findings.some(
      (finding) =>
        finding.ruleId === "LIFE-ORDER-010" &&
        finding.message.includes("technical-design"),
    ),
  );
});

test("CLI 解析交付模式和显式 required stages", () => {
  assert.equal(
    parseLifecycleArgs(["--risk", "L3", "--mode", "FULL"]).deliveryMode,
    "FULL",
  );
  assert.equal(
    parseLifecycleArgs(["--applicability-evidence", "decisions/task.md"])
      .applicabilityEvidence,
    "decisions/task.md",
  );
  assert.deepEqual(
    parseLifecycleArgs(["--risk", "L3", "--required-stages", "brd,tdd"])
      .requiredStages,
    ["brd", "tdd"],
  );
  assert.deepEqual(
    parseLifecycleArgs(["--risk", "L3", "--required-stages", ""])
      .requiredStages,
    [],
  );
});

test("SIMPLE 和 STANDARD 禁止携带四阶段产品文档", () => {
  const documents = hydrateLifecycle();
  for (const deliveryMode of ["SIMPLE", "STANDARD"]) {
    const result = validateLifecycle(
      { brd: documents.brd },
      {
        riskLevel: deliveryMode === "SIMPLE" ? "L1" : "L2",
        deliveryMode,
        requiredStages: [],
      },
    );
    assert.ok(
      result.findings.some(
        (finding) =>
          finding.ruleId === "LIFE-ORDER-010" &&
          finding.message.includes("不使用 BRD/SRS/TDD/Plan"),
      ),
    );
  }
});

test("L0 非行为任务不会被强制套用四阶段文档", () => {
  const result = validateLifecycle({}, { riskLevel: "L0" });
  assert.deepEqual(result.findings, []);
});

test("旧 check-prd 只转发新文档类型并阻断混合 PRD", () => {
  const checker = repositoryPath("mango-pmo/tools/check-prd.mjs");
  const valid = path.join(FIXTURES, "valid/business-requirements.md");
  const accepted = spawnSync(process.execPath, [checker, "--prd", valid], {
    encoding: "utf8",
  });
  assert.equal(accepted.status, 0, accepted.stdout + accepted.stderr);

  const legacy = repositoryPath(
    "mango-pmo/templates/sample-prd-announcement.md",
  );
  const rejected = spawnSync(process.execPath, [checker, "--prd", legacy], {
    encoding: "utf8",
  });
  assert.equal(rejected.status, 1);
  assert.match(rejected.stderr, /LEGACY-PRD-MIGRATION-001/);
});

test("AI 自批和伪造审批证据不能获得 NEXT", () => {
  const contract = loadContract(
    "mango-pmo/contracts/business-requirements.json",
  );
  const invalid = readFixture("valid/business-requirements.md")
    .replace("approver: 业务负责人", "approver: AI Agent")
    .replace(
      "approvalEvidence: review/BRD-ANN-001",
      "approvalEvidence: invented-by-agent",
    );
  const result = validateDocument(invalid, contract);
  assert.ok(
    result.findings.some((finding) => finding.ruleId === "BRD-META-001"),
  );
  assert.ok(
    result.findings.some((finding) => finding.message.includes("人工审批人")),
  );
});

test("BRD/SRS 阻断路径模板、技术类型、框架词和 Markdown 拆词绕过", () => {
  const brdContract = loadContract(
    "mango-pmo/contracts/business-requirements.json",
  );
  const brd = readFixture("valid/business-requirements.md");
  for (const injected of [
    "/announcements/{announcementId}/approval + ApprovalPayload",
    "NoticeCont**roller** 负责审批",
  ]) {
    const invalid = brd.replace("审核决定依赖线下消息传递", injected);
    assert.notEqual(invalid, brd, "test mutation must change the BRD fixture");
    const result = validateDocument(invalid, brdContract);
    assert.ok(
      result.findings.some((finding) => finding.ruleId === "BRD-BOUNDARY-001"),
      injected,
    );
  }

  const srsContract = loadContract(
    "mango-pmo/contracts/system-requirements.json",
  );
  const srs = readFixture("valid/system-requirements.md");
  const invalidSrs = srs.replace(
    "记录提交、审核和撤回过程并展示当前结果",
    "系统使用 Spring MVC、Redis 和 Kafka",
  );
  assert.notEqual(invalidSrs, srs, "test mutation must change the SRS fixture");
  const srsResult = validateDocument(invalidSrs, srsContract);
  assert.ok(
    srsResult.findings.some((finding) => finding.ruleId === "SRS-BOUNDARY-001"),
  );
});

test("TDD API 表阻断路径变量和持久化模型泄漏", () => {
  const contract = loadContract("mango-pmo/contracts/technical-design.json");
  const source = readFixture("valid/technical-design.md");
  const pathVariable = validateDocument(
    source.replace("POST /notices/submit", "GET /notices/{id}"),
    contract,
  );
  assert.ok(
    pathVariable.findings.some(
      (finding) => finding.ruleId === "TDD-BOUNDARY-001",
    ),
  );
  const entityLeak = validateDocument(
    source.replace("SubmitNoticeCommand and NoticeVO", "NoticeEntity"),
    contract,
  );
  assert.ok(
    entityLeak.findings.some(
      (finding) => finding.ruleId === "TDD-BOUNDARY-001",
    ),
  );
});

test("实施计划阻断未修订 TDD 的新增设计及代码块藏匿", () => {
  const contract = loadContract("mango-pmo/contracts/implementation-plan.json");
  const source = readFixture("valid/implementation-plan.md");
  const redesign = validateDocument(
    source.replace(
      "按设计实现契约、模型、流程、安全、交互和测试",
      "未修改 TDD，增加批量端点并选用 Redis 锁",
    ),
    contract,
  );
  assert.ok(
    redesign.findings.some((finding) => finding.ruleId === "PLAN-BOUNDARY-001"),
  );
  const hidden = validateDocument(
    source.replace(
      "## 9. 阶段判定与审批",
      "```text\n新增接口并选用 Redis\n```\n\n## 9. 阶段判定与审批",
    ),
    contract,
  );
  assert.ok(
    hidden.findings.some((finding) => finding.ruleId === "PLAN-BOUNDARY-001"),
  );
  const neutralDecision = validateDocument(
    source.replace(
      "按设计实现契约、模型、流程、安全、交互和测试",
      "最终决定受 API-001 的既有设计和验证结果约束",
    ),
    contract,
  );
  assert.equal(
    neutralDecision.findings.some(
      (finding) => finding.ruleId === "PLAN-BOUNDARY-001",
    ),
    false,
  );
});
