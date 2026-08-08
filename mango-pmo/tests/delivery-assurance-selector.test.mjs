import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { PassThrough } from "node:stream";
import test from "node:test";
import fs from "node:fs";
import { fileURLToPath } from "node:url";

import {
  applyBatchAnswer,
  applySelectorCommand,
  commandsFromInput,
  createSelectorState,
  renderResult,
  renderSelector,
  runTerminalSelector,
  selectorResult,
} from "../tools/select-delivery-assurance.mjs";

const contract = JSON.parse(
  fs.readFileSync(
    new URL("../contracts/delivery-assurance.json", import.meta.url),
    "utf8",
  ),
);
const blankContextCases = JSON.parse(
  fs.readFileSync(
    new URL(
      "./skills/delivery-selector-blank-context-cases.json",
      import.meta.url,
    ),
    "utf8",
  ),
);
const toolPath = new URL(
  "../tools/select-delivery-assurance.mjs",
  import.meta.url,
);

function recommendation(overrides = {}) {
  return {
    recommendedLevel: "L3",
    recommendedSelections: {
      M01: "REUSE",
      M03: "ENABLE",
      M04: "ENABLE",
      M05: "ENABLE",
      M09: "ENABLE",
    },
    ...overrides,
  };
}

function moveTo(contractState, row) {
  let state = contractState;
  while (state.cursor !== row)
    state = applySelectorCommand(contract, state, "DOWN");
  return state;
}

test("32 个空白上下文场景覆盖五个版本、十六项措施和独立授权", () => {
  assert.equal(blankContextCases.schemaVersion, 1);
  assert.equal(blankContextCases.cases.length, 32);
  const versions = new Set();
  const measures = new Set();
  const ids = new Set();
  for (const item of blankContextCases.cases) {
    assert.match(item.id, /^selector-blank-\d{3}$/u);
    assert.equal(ids.has(item.id), false, item.id);
    ids.add(item.id);
    if (item.expect.version) versions.add(item.expect.version);
    for (const id of item.expect.selected ?? []) measures.add(id);
  }
  assert.deepEqual(
    [...versions].sort(),
    ["一页纸", "四文档", "标准版", "直接做", "详细版"].sort(),
  );
  assert.equal(measures.size, 16);
  assert.ok(
    blankContextCases.cases.some((item) =>
      item.expect.authorization?.includes("M01"),
    ),
  );
  assert.ok(
    blankContextCases.cases.some((item) =>
      item.expect.authorization?.includes("M02"),
    ),
  );
  assert.ok(blankContextCases.cases.some((item) => item.expect.error));
});

for (const item of blankContextCases.cases) {
  test(`${item.id} ${item.prompt}`, () => {
    const initial = createSelectorState(contract, item.recommendation);
    if (item.expect.error) {
      assert.throws(
        () => applyBatchAnswer(contract, initial, item.answer),
        new RegExp(item.expect.error, "u"),
      );
      return;
    }
    const state =
      item.answer === undefined
        ? initial
        : applyBatchAnswer(contract, initial, item.answer);
    const result = selectorResult(contract, state);
    assert.equal(result.documentVersion.label, item.expect.version);
    assert.deepEqual(
      result.selectedMeasures.map((measure) => measure.id),
      item.expect.selected,
    );
    if (item.expect.m01)
      assert.equal(result.machineSelections.M01, item.expect.m01);
    if (item.expect.authorization)
      assert.deepEqual(
        result.separateAuthorizations,
        item.expect.authorization,
      );
  });
}

test("合同提供五个好记的中文文档版本并映射 L0-L5", () => {
  assert.deepEqual(
    contract.selectionUi.documentVersions.map((item) => [
      item.label,
      item.riskLevels,
    ]),
    [
      ["直接做", ["L0", "L1"]],
      ["一页纸", ["L2"]],
      ["标准版", ["L3"]],
      ["详细版", ["L4"]],
      ["四文档", ["L5"]],
    ],
  );
});

test("M01-M16 均有中文名称、具体描述和勾选机器值", () => {
  assert.equal(contract.measures.length, 16);
  assert.deepEqual(
    contract.measures.map((item) => item.id),
    Array.from(
      { length: 16 },
      (_, index) => `M${String(index + 1).padStart(2, "0")}`,
    ),
  );
  for (const measure of contract.measures) {
    assert.ok(measure.name.length >= 4, measure.id);
    assert.ok(measure.selectionDescription.length >= 8, measure.id);
    assert.ok(
      measure.allowedValues.includes(measure.selectedValue),
      measure.id,
    );
    assert.ok(
      measure.allowedValues.includes(measure.unselectedValue),
      measure.id,
    );
  }
});

test("界面只用勾选表达采用状态，不显示启用或不启用", () => {
  const screen = renderSelector(
    contract,
    createSelectorState(contract, recommendation()),
    { clear: false },
  );
  assert.match(
    screen,
    /\[x\] M03 业务需求：说明背景、角色诉求、用户故事和业务验收/u,
  );
  assert.match(screen, /\[ \] M16 人工验收：确认自动化无法覆盖的实际结果/u);
  assert.doesNotMatch(screen, /启用|不启用/u);
  assert.match(screen, /一页纸：一份精简文档，最多一张 A4/u);
});

test("M01 在中文界面显示本次具体策略", () => {
  const screen = renderSelector(
    contract,
    createSelectorState(contract, recommendation()),
    { clear: false },
  );
  assert.match(
    screen,
    /M01 任务工作区隔离：创建或复用当前任务工作区（本次：复用当前任务工作区）/u,
  );
});

test("上下键循环移动，空格单选文档版本", () => {
  let state = createSelectorState(contract, recommendation());
  state = applySelectorCommand(contract, state, "UP");
  assert.equal(state.cursor, 1);
  state = applySelectorCommand(contract, state, "TOGGLE");
  assert.equal(selectorResult(contract, state).documentVersion.label, "一页纸");
  state = { ...state, cursor: 0 };
  state = applySelectorCommand(contract, state, "UP");
  assert.equal(state.cursor, 20);
});

test("空格可以一次界面内勾选或取消任一措施", () => {
  const m10Row = contract.selectionUi.documentVersions.length + 9;
  let state = moveTo(createSelectorState(contract, recommendation()), m10Row);
  state = applySelectorCommand(contract, state, "TOGGLE");
  assert.equal(selectorResult(contract, state).machineSelections.M10, "ENABLE");
  state = applySelectorCommand(contract, state, "TOGGLE");
  assert.equal(
    selectorResult(contract, state).machineSelections.M10,
    "DISABLE",
  );
});

test("R 一次恢复推荐的文档版本和全部措施", () => {
  let state = applyBatchAnswer(
    contract,
    createSelectorState(contract, recommendation()),
    "4;2,10,16",
  );
  state = { ...state, confirmed: false };
  state = applySelectorCommand(contract, state, "RESTORE");
  const result = selectorResult(contract, state);
  assert.equal(result.documentVersion.label, "标准版");
  assert.deepEqual(
    result.selectedMeasures.map((item) => item.id),
    ["M01", "M03", "M04", "M05", "M09"],
  );
  assert.equal(result.machineSelections.M01, "REUSE");
});

test("Enter 一次确认全部，Esc 一次取消", () => {
  const initial = createSelectorState(contract, recommendation());
  assert.equal(
    applySelectorCommand(contract, initial, "CONFIRM").confirmed,
    true,
  );
  assert.equal(
    applySelectorCommand(contract, initial, "CANCEL").cancelled,
    true,
  );
});

test("键盘字节解析覆盖上下键、空格、回车、恢复和取消", () => {
  assert.deepEqual(commandsFromInput("\u001b[A\u001b[B R\r\u001b"), [
    "UP",
    "DOWN",
    "TOGGLE",
    "RESTORE",
    "CONFIRM",
    "CANCEL",
  ]);
});

test("非交互模式一次输入文档版本和全部措施编号", () => {
  const state = applyBatchAnswer(
    contract,
    createSelectorState(contract, recommendation()),
    "4;1,3,4,9,12",
  );
  const result = selectorResult(contract, state);
  assert.equal(result.documentVersion.label, "详细版");
  assert.deepEqual(
    result.selectedMeasures.map((item) => item.id),
    ["M01", "M03", "M04", "M09", "M12"],
  );
  assert.equal(result.machineSelections.M01, "REUSE");
  assert.equal(result.machineSelections.M02, "DO_NOT_REBUILD");
});

test("非交互模式支持只选文档版本而不采用任何措施", () => {
  const result = selectorResult(
    contract,
    applyBatchAnswer(
      contract,
      createSelectorState(contract, recommendation()),
      "1;",
    ),
  );
  assert.equal(result.documentVersion.label, "直接做");
  assert.deepEqual(result.selectedMeasures, []);
  assert.equal(Object.keys(result.machineSelections).length, 16);
});

test("固定 L5 事实不能在界面或批量输入中降级", () => {
  const state = createSelectorState(
    contract,
    recommendation({ recommendedLevel: "L5", fixedLevel: "L5" }),
  );
  const onDirect = { ...state, cursor: 0 };
  assert.equal(
    applySelectorCommand(contract, onDirect, "TOGGLE").selectedVersionId,
    "FOUR_DOCUMENTS",
  );
  assert.throws(
    () => applyBatchAnswer(contract, state, "3;1,3,4"),
    /不可降级/u,
  );
});

test("结果只向人展示已勾选措施，机器记录仍含 M01-M16 精确值", () => {
  const result = selectorResult(
    contract,
    createSelectorState(contract, recommendation()),
  );
  const human = renderResult(result);
  assert.match(
    human,
    /M01 任务工作区隔离：创建或复用当前任务工作区（本次：复用当前任务工作区）/u,
  );
  assert.match(human, /M09 静态验证/u);
  assert.doesNotMatch(human, /M10 单元测试/u);
  assert.equal(Object.keys(result.machineSelections).length, 16);
  assert.equal(result.machineSelections.M10, "DISABLE");
});

test("数据库重建和主工作区例外保留单独授权边界", () => {
  const rebuild = selectorResult(
    contract,
    applyBatchAnswer(
      contract,
      createSelectorState(
        contract,
        recommendation({
          recommendedSelections: { M01: "MAIN_EXCEPTION", M02: "REBUILD" },
        }),
      ),
      "2;1,2",
    ),
  );
  assert.deepEqual(rebuild.separateAuthorizations, ["M01", "M02"]);
  const human = renderResult(rebuild);
  assert.match(human, /破坏性操作前仍需单独授权/u);
  assert.match(human, /主工作区例外必须保留人工确认记录/u);
});

test("TTY 流使用原始模式处理方向键、空格和一次 Enter", async () => {
  const input = new PassThrough();
  const output = new PassThrough();
  input.isTTY = true;
  output.isTTY = true;
  const rawModes = [];
  input.setRawMode = (value) => rawModes.push(value);
  let text = "";
  output.on("data", (chunk) => {
    text += chunk.toString("utf8");
  });
  const running = runTerminalSelector(
    contract,
    {
      recommendedLevel: "L2",
      recommendedSelections: { M03: "ENABLE" },
    },
    { input, output },
  );
  input.write("\u001b[B \u001b[B\u001b[B\u001b[B \r");
  const result = await running;
  assert.equal(result.documentVersion.label, "标准版");
  assert.deepEqual(
    result.selectedMeasures.map((item) => item.id),
    ["M01", "M03"],
  );
  assert.deepEqual(rawModes, [true, false]);
  assert.match(text, /已确认/u);
});

test("非 TTY 流只读取一行批量输入并返回结果", async () => {
  const input = new PassThrough();
  const output = new PassThrough();
  input.isTTY = false;
  output.isTTY = false;
  let text = "";
  output.on("data", (chunk) => {
    text += chunk.toString("utf8");
  });
  const running = runTerminalSelector(contract, recommendation(), {
    input,
    output,
  });
  input.end("5;1,3,4,5,6,9,14\n");
  const result = await running;
  assert.equal(result.documentVersion.label, "四文档");
  assert.match(text, /非交互输入格式/u);
  assert.match(text, /已确认/u);
});

test(
  "真实伪终端可以接收方向键、空格和 Enter 完成整批确认",
  { skip: process.platform === "win32" },
  () => {
    const python = spawnSync("python3", ["--version"], { encoding: "utf8" });
    if (python.status !== 0) return;
    const driver = String.raw`
import errno, os, pty, select, signal, sys
signal.alarm(8)
pid, fd = pty.fork()
if pid == 0:
    os.execv(sys.argv[1], sys.argv[1:])
chunks = []
marker = '文档版本（请选择一项）'.encode('utf-8')
while marker not in b''.join(chunks):
    ready, _, _ = select.select([fd], [], [], 2)
    if not ready:
        raise RuntimeError('selector prompt timeout')
    chunks.append(os.read(fd, 65536))
os.write(fd, b'\x1b[B \x1b[B\x1b[B\x1b[B \r')
while True:
    try:
        chunk = os.read(fd, 65536)
        if not chunk:
            break
        chunks.append(chunk)
    except OSError as error:
        if error.errno == errno.EIO:
            break
        raise
_, status = os.waitpid(pid, 0)
sys.stdout.buffer.write(b''.join(chunks))
sys.exit(os.waitstatus_to_exitcode(status))
`;
    const run = spawnSync(
      "python3",
      [
        "-c",
        driver,
        process.execPath,
        fileURLToPath(toolPath),
        "--level",
        "L2",
        "--measures",
        "M03=ENABLE",
      ],
      { encoding: "utf8", timeout: 10000 },
    );
    assert.equal(run.status, 0, `${run.stdout}\n${run.stderr}`);
    assert.match(run.stdout, /文档版本：标准版/u);
    assert.doesNotMatch(run.stdout, /文档版本：标准版（L3）/u);
    assert.match(run.stdout, /M01 任务工作区隔离/u);
    assert.match(run.stdout, /M03 业务需求/u);
  },
);
