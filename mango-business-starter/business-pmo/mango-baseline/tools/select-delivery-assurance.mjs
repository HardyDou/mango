#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const toolDir = path.dirname(fileURLToPath(import.meta.url));
const defaultContractPath = path.resolve(
  toolDir,
  "../contracts/delivery-assurance.json",
);
const POSITIVE_VALUES = new Set([
  "CREATE",
  "REUSE",
  "MAIN_EXCEPTION",
  "REBUILD",
  "ENABLE",
]);
const VALUE_DESCRIPTIONS = new Map([
  ["CREATE", "创建任务工作区"],
  ["REUSE", "复用当前任务工作区"],
  ["MAIN_EXCEPTION", "使用已确认的主工作区例外"],
]);

function cloneSelections(value = {}) {
  return Object.fromEntries(Object.entries(value));
}

function versionForLevel(contract, level) {
  return contract.selectionUi.documentVersions.find((item) =>
    item.riskLevels.includes(level),
  );
}

function measureMap(contract) {
  return new Map(contract.measures.map((measure) => [measure.id, measure]));
}

function validateRecommendation(contract, recommendation) {
  const version = versionForLevel(contract, recommendation.recommendedLevel);
  if (!version)
    throw new Error(`未知文档等级：${recommendation.recommendedLevel}`);
  if (
    recommendation.fixedLevel &&
    !versionForLevel(contract, recommendation.fixedLevel)
  ) {
    throw new Error(`未知固定文档等级：${recommendation.fixedLevel}`);
  }
  const catalog = measureMap(contract);
  for (const [id, value] of Object.entries(
    recommendation.recommendedSelections,
  )) {
    const measure = catalog.get(id);
    if (!measure) throw new Error(`未知交付措施：${id}`);
    if (!measure.allowedValues.includes(value))
      throw new Error(`${id} 不支持机器值：${value}`);
  }
}

export function createSelectorState(contract, recommendation = {}) {
  const normalized = {
    recommendedLevel:
      recommendation.recommendedLevel ?? recommendation.finalLevel ?? "L2",
    fixedLevel: recommendation.fixedLevel ?? null,
    recommendedSelections: cloneSelections(
      recommendation.recommendedSelections ??
        recommendation.assuranceSelections ??
        {},
    ),
  };
  validateRecommendation(contract, normalized);
  const version = versionForLevel(
    contract,
    normalized.fixedLevel ?? normalized.recommendedLevel,
  );
  return {
    cursor: contract.selectionUi.documentVersions.indexOf(version),
    selectedVersionId: version.id,
    selectedValues: cloneSelections(normalized.recommendedSelections),
    recommendation: normalized,
    confirmed: false,
    cancelled: false,
  };
}

function rowCount(contract) {
  return (
    contract.selectionUi.documentVersions.length + contract.measures.length
  );
}

function selectedVersion(contract, state) {
  return contract.selectionUi.documentVersions.find(
    (item) => item.id === state.selectedVersionId,
  );
}

function isSelected(state, measure) {
  return POSITIVE_VALUES.has(state.selectedValues[measure.id]);
}

function selectedValue(state, measure) {
  const recommendedValue =
    state.recommendation.recommendedSelections[measure.id];
  return POSITIVE_VALUES.has(recommendedValue)
    ? recommendedValue
    : measure.selectedValue;
}

export function applySelectorCommand(contract, state, command) {
  if (state.confirmed || state.cancelled) return state;
  const next = {
    ...state,
    selectedValues: cloneSelections(state.selectedValues),
  };
  if (command === "UP") {
    next.cursor = (next.cursor - 1 + rowCount(contract)) % rowCount(contract);
    return next;
  }
  if (command === "DOWN") {
    next.cursor = (next.cursor + 1) % rowCount(contract);
    return next;
  }
  if (command === "RESTORE")
    return createSelectorState(contract, state.recommendation);
  if (command === "CANCEL") return { ...next, cancelled: true };
  if (command === "CONFIRM") return { ...next, confirmed: true };
  if (command !== "TOGGLE") return next;

  const versionCount = contract.selectionUi.documentVersions.length;
  if (next.cursor < versionCount) {
    const version = contract.selectionUi.documentVersions[next.cursor];
    const fixedVersion = state.recommendation.fixedLevel
      ? versionForLevel(contract, state.recommendation.fixedLevel)
      : null;
    if (!fixedVersion || fixedVersion.id === version.id)
      next.selectedVersionId = version.id;
    return next;
  }

  const measure = contract.measures[next.cursor - versionCount];
  next.selectedValues[measure.id] = isSelected(next, measure)
    ? measure.unselectedValue
    : selectedValue(next, measure);
  return next;
}

function exactMeasureDescription(state, measure) {
  const exact = state.selectedValues[measure.id];
  const detail =
    measure.id === "M01" && POSITIVE_VALUES.has(exact)
      ? VALUE_DESCRIPTIONS.get(exact)
      : "";
  return `${measure.id} ${measure.name}：${measure.selectionDescription}${detail ? `（本次：${detail}）` : ""}`;
}

export function renderSelector(contract, state, { clear = true } = {}) {
  const lines = ["文档版本（请选择一项）"];
  const versions = contract.selectionUi.documentVersions;
  const fixedVersion = state.recommendation.fixedLevel
    ? versionForLevel(contract, state.recommendation.fixedLevel)
    : null;
  versions.forEach((version, index) => {
    const cursor = state.cursor === index ? ">" : " ";
    const radio = state.selectedVersionId === version.id ? "(●)" : "( )";
    const unavailable =
      fixedVersion && fixedVersion.id !== version.id
        ? "（当前任务不可降级）"
        : "";
    lines.push(
      `${cursor} ${radio} ${version.label}：${version.description}${unavailable}`,
    );
  });
  lines.push("交付措施（空格键勾选）");
  contract.measures.forEach((measure, index) => {
    const row = versions.length + index;
    const cursor = state.cursor === row ? ">" : " ";
    const checkbox = isSelected(state, measure) ? "[x]" : "[ ]";
    lines.push(
      `${cursor} ${checkbox} ${exactMeasureDescription(state, measure)}`,
    );
  });
  lines.push("↑/↓ 移动  空格键勾选  回车键确认全部  R 恢复推荐  Esc 取消");
  return `${clear ? "\u001b[2J\u001b[H" : ""}${lines.join("\n")}\n`;
}

export function selectorResult(contract, state) {
  const version = selectedVersion(contract, state);
  const selectedMeasures = contract.measures
    .filter((measure) => isSelected(state, measure))
    .map((measure) => ({
      id: measure.id,
      name: measure.name,
      description:
        measure.id === "M01"
          ? exactMeasureDescription(state, measure).replace(
              /^M01\s+[^：]+：/u,
              "",
            )
          : measure.selectionDescription,
      value: state.selectedValues[measure.id],
    }));
  const machineSelections = Object.fromEntries(
    contract.measures.map((measure) => [
      measure.id,
      isSelected(state, measure)
        ? state.selectedValues[measure.id]
        : measure.unselectedValue,
    ]),
  );
  return {
    documentVersion: {
      id: version.id,
      label: version.label,
      deliveryMode: version.deliveryMode,
      riskLevels: version.riskLevels,
      description: version.description,
    },
    selectedMeasures,
    machineSelections,
    separateAuthorizations: selectedMeasures
      .filter(
        (measure) => measure.id === "M02" || measure.value === "MAIN_EXCEPTION",
      )
      .map((measure) => measure.id),
  };
}

export function renderResult(result) {
  const lines = [
    "已确认",
    `文档版本：${result.documentVersion.label}`,
    "采用措施：",
  ];
  if (result.selectedMeasures.length === 0) lines.push("- 无");
  for (const measure of result.selectedMeasures) {
    lines.push(`- ${measure.id} ${measure.name}：${measure.description}`);
  }
  if (result.separateAuthorizations.includes("M02")) {
    lines.push(
      "注意：数据库重建只确认纳入范围，执行破坏性操作前仍需单独授权。",
    );
  }
  if (result.separateAuthorizations.includes("M01")) {
    lines.push("注意：主工作区例外必须保留人工确认记录。");
  }
  return `${lines.join("\n")}\n`;
}

export function commandsFromInput(chunk) {
  const value = Buffer.isBuffer(chunk) ? chunk.toString("utf8") : String(chunk);
  const commands = [];
  for (let index = 0; index < value.length;) {
    if (value.startsWith("\u001b[A", index)) {
      commands.push("UP");
      index += 3;
    } else if (value.startsWith("\u001b[B", index)) {
      commands.push("DOWN");
      index += 3;
    } else {
      const char = value[index];
      if (char === " ") commands.push("TOGGLE");
      else if (char === "\r" || char === "\n") commands.push("CONFIRM");
      else if (char === "r" || char === "R") commands.push("RESTORE");
      else if (char === "\u001b" || char === "\u0003") commands.push("CANCEL");
      index += 1;
    }
  }
  return commands;
}

function resolveVersionToken(contract, token) {
  const normalized = token.trim();
  const numeric = Number.parseInt(normalized, 10);
  if (
    String(numeric) === normalized &&
    numeric >= 1 &&
    numeric <= contract.selectionUi.documentVersions.length
  ) {
    return contract.selectionUi.documentVersions[numeric - 1];
  }
  return contract.selectionUi.documentVersions.find((version) =>
    [
      version.id,
      version.label,
      version.deliveryMode,
      ...version.riskLevels,
    ].some((value) => value.toLowerCase() === normalized.toLowerCase()),
  );
}

export function applyBatchAnswer(contract, state, answer) {
  const [versionToken, measureToken = ""] = answer.trim().split(/[;；]/u, 2);
  const version = resolveVersionToken(contract, versionToken ?? "");
  if (!version) throw new Error("批量输入中的文档版本无效");
  const fixedVersion = state.recommendation.fixedLevel
    ? versionForLevel(contract, state.recommendation.fixedLevel)
    : null;
  if (fixedVersion && version.id !== fixedVersion.id)
    throw new Error("当前任务的文档版本不可降级");
  const selectedIds = new Set();
  for (const raw of measureToken.split(/[,，\s]+/u).filter(Boolean)) {
    const id = /^\d{1,2}$/u.test(raw)
      ? `M${raw.padStart(2, "0")}`
      : raw.toUpperCase();
    if (!contract.measures.some((measure) => measure.id === id))
      throw new Error(`批量输入中的措施无效：${raw}`);
    selectedIds.add(id);
  }
  const next = { ...state, selectedVersionId: version.id, selectedValues: {} };
  for (const measure of contract.measures) {
    next.selectedValues[measure.id] = selectedIds.has(measure.id)
      ? selectedValue(state, measure)
      : measure.unselectedValue;
  }
  return { ...next, confirmed: true };
}

function renderBatchPrompt(contract, state) {
  const recommended = selectorResult(contract, state);
  const versionNumber =
    contract.selectionUi.documentVersions.findIndex(
      (version) => version.id === recommended.documentVersion.id,
    ) + 1;
  const measures = recommended.selectedMeasures
    .map((measure) => Number.parseInt(measure.id.slice(1), 10))
    .join(",");
  const lines = [
    renderSelector(contract, state, { clear: false }).trimEnd(),
    "",
    "非交互输入格式：文档版本编号;措施编号（逗号分隔）",
    `例如：${versionNumber};${measures}`,
    `直接回车采用当前勾选，或一次输入全部选择：`,
  ];
  return `${lines.join("\n")} `;
}

async function readBatchAnswer(input) {
  return await new Promise((resolvePromise, reject) => {
    let value = "";
    let received = false;
    input.setEncoding?.("utf8");
    input.on("data", (chunk) => {
      received = true;
      value += chunk;
    });
    input.on("end", () =>
      received
        ? resolvePromise(value.split(/\r?\n/u, 1)[0].trim())
        : reject(new Error("非交互模式未收到确认输入")),
    );
    input.on("error", reject);
    input.resume?.();
  });
}

export async function runTerminalSelector(
  contract,
  recommendation,
  options = {},
) {
  const input = options.input ?? process.stdin;
  const output = options.output ?? process.stdout;
  let state = createSelectorState(contract, recommendation);
  const writeResult = (current) => {
    const result = selectorResult(contract, current);
    output.write(
      options.json
        ? `${JSON.stringify(result, null, 2)}\n`
        : renderResult(result),
    );
    return result;
  };

  if (options.answer !== undefined)
    return writeResult(applyBatchAnswer(contract, state, options.answer));
  if (options.acceptRecommended)
    return writeResult({ ...state, confirmed: true });
  if (!input.isTTY || !output.isTTY) {
    output.write(renderBatchPrompt(contract, state));
    const answer = await readBatchAnswer(input);
    state = answer
      ? applyBatchAnswer(contract, state, answer)
      : { ...state, confirmed: true };
    return writeResult(state);
  }

  return await new Promise((resolvePromise, reject) => {
    const cleanup = () => {
      input.off("data", onData);
      if (typeof input.setRawMode === "function") input.setRawMode(false);
      input.pause?.();
      output.write("\u001b[?25h");
    };
    const onData = (chunk) => {
      try {
        for (const command of commandsFromInput(chunk)) {
          state = applySelectorCommand(contract, state, command);
          if (state.cancelled) {
            cleanup();
            output.write("\n已取消，未确认任何选择。\n");
            resolvePromise(null);
            return;
          }
          if (state.confirmed) {
            cleanup();
            output.write("\u001b[2J\u001b[H");
            resolvePromise(writeResult(state));
            return;
          }
        }
        output.write(renderSelector(contract, state));
      } catch (error) {
        cleanup();
        reject(error);
      }
    };
    if (typeof input.setRawMode === "function") input.setRawMode(true);
    input.resume?.();
    input.on("data", onData);
    output.write("\u001b[?25l");
    output.write(renderSelector(contract, state));
  });
}

function parseMeasureValues(value) {
  const result = {};
  for (const entry of String(value || "")
    .split(/[,，;；]/u)
    .map((item) => item.trim())
    .filter(Boolean)) {
    const match = /^(M(?:0[1-9]|1[0-6]))(?:=([A-Z_]+))?$/u.exec(
      entry.toUpperCase(),
    );
    if (!match) throw new Error(`措施参数无效：${entry}`);
    result[match[1]] =
      match[2] ||
      (match[1] === "M01"
        ? "CREATE"
        : match[1] === "M02"
          ? "REBUILD"
          : "ENABLE");
  }
  return result;
}

export function parseSelectorArgs(argv) {
  const options = {
    level: "",
    measures: "",
    recommendation: "",
    answer: undefined,
    fixedLevel: "",
    acceptRecommended: false,
    json: false,
    help: false,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--accept-recommended" || arg === "--采用推荐")
      options.acceptRecommended = true;
    else if (arg === "--json" || arg === "--机器结果") options.json = true;
    else if (arg === "--help" || arg === "--帮助" || arg === "-h")
      options.help = true;
    else if (
      [
        "--level",
        "--measures",
        "--recommendation",
        "--answer",
        "--fixed-level",
        "--等级",
        "--措施",
        "--推荐文件",
        "--选择",
        "--固定等级",
      ].includes(arg)
    ) {
      const next = argv[index + 1];
      if (next === undefined || next.startsWith("--"))
        throw new Error(`缺少参数值：${arg}`);
      const key = {
        "--level": "level",
        "--等级": "level",
        "--measures": "measures",
        "--措施": "measures",
        "--recommendation": "recommendation",
        "--推荐文件": "recommendation",
        "--answer": "answer",
        "--选择": "answer",
        "--fixed-level": "fixedLevel",
        "--固定等级": "fixedLevel",
      }[arg];
      options[key] = next;
      index += 1;
    } else throw new Error(`未知参数：${arg}`);
  }
  return options;
}

function loadRecommendation(options) {
  const fromFile = options.recommendation
    ? JSON.parse(fs.readFileSync(path.resolve(options.recommendation), "utf8"))
    : {};
  const recommendedLevel =
    options.level || fromFile.recommendedLevel || fromFile.finalLevel;
  if (!recommendedLevel) {
    throw new Error(
      "缺少任务事实推荐；请先让交付保障 Skill 明确目标、边界和文档版本",
    );
  }
  return {
    ...fromFile,
    recommendedLevel,
    fixedLevel: options.fixedLevel || fromFile.fixedLevel || null,
    recommendedSelections: options.measures
      ? parseMeasureValues(options.measures)
      : fromFile.recommendedSelections || fromFile.assuranceSelections || {},
  };
}

export async function runSelectorCli(argv = process.argv.slice(2), io = {}) {
  const options = parseSelectorArgs(argv);
  if (options.help) {
    (io.output ?? process.stdout).write(
      [
        "用法：mango pmo 选择 [选项]",
        "  --等级 <L0-L5>              推荐的内部文档等级",
        "  --措施 <机器值清单>          推荐采用的措施",
        "  --固定等级 <L5>             固定不可降级的文档等级",
        "  --推荐文件 <文件>           读取推荐结果",
        "  --选择 <3;1,3,9>            一次提供非交互选择",
        "  --采用推荐                   直接确认推荐项",
        "  --机器结果                   输出机器可读结果",
        "",
      ].join("\n"),
    );
    return 0;
  }
  const contractPath = io.contractPath ?? defaultContractPath;
  const contract = JSON.parse(fs.readFileSync(contractPath, "utf8"));
  const result = await runTerminalSelector(
    contract,
    loadRecommendation(options),
    { ...io, ...options },
  );
  return result ? 0 : 130;
}

const invokedPath = process.argv[1] ? path.resolve(process.argv[1]) : "";
if (invokedPath === fileURLToPath(import.meta.url)) {
  runSelectorCli()
    .then((code) => {
      process.exitCode = code;
    })
    .catch((error) => {
      process.stderr.write(`选择失败：${error.message}\n`);
      process.exitCode = 1;
    });
}
