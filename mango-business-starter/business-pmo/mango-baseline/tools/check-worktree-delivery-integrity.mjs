#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";

function parseArgs(argv) {
  const args = {
    mode: "",
    root: process.cwd(),
    base: "main",
    reuseCurrentTask: "",
    expectedBranch: "",
    requireUpstream: false,
    allowedDirtyWorktrees: [],
    json: false,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === "--json") {
      args.json = true;
      continue;
    }
    if (argument === "--allow-dirty-worktree") {
      args.allowedDirtyWorktrees.push(argv[index + 1] ?? "");
      index += 1;
      continue;
    }
    if (!argument.startsWith("--")) continue;
    const key = argument
      .slice(2)
      .replace(/-([a-z])/g, (_, char) => char.toUpperCase());
    const value = argv[index + 1] ?? "";
    index += 1;
    if (key === "requireUpstream") {
      args.requireUpstream = value === "true";
    } else if (key in args) {
      args[key] = value;
    }
  }
  return args;
}

function runGit(cwd, args, options = {}) {
  const result = spawnSync("git", ["-C", cwd, ...args], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
  if (result.status !== 0 && !options.allowFailure) {
    throw new Error(
      (result.stderr || result.stdout || `git ${args.join(" ")} failed`).trim(),
    );
  }
  return {
    status: result.status ?? 1,
    stdout: result.stdout ?? "",
    stderr: result.stderr ?? "",
  };
}

function nulList(text) {
  return text.split("\0").filter(Boolean);
}

function inspectChanges(worktreePath) {
  const staged = nulList(
    runGit(worktreePath, ["diff", "--cached", "--name-only", "-z"]).stdout,
  );
  const unstaged = nulList(
    runGit(worktreePath, ["diff", "--name-only", "-z"]).stdout,
  );
  const untracked = nulList(
    runGit(worktreePath, ["ls-files", "--others", "--exclude-standard", "-z"])
      .stdout,
  );
  const conflicts = nulList(
    runGit(worktreePath, ["diff", "--name-only", "--diff-filter=U", "-z"])
      .stdout,
  );
  return {
    staged,
    unstaged,
    untracked,
    conflicts,
    dirty:
      staged.length + unstaged.length + untracked.length + conflicts.length > 0,
  };
}

function parseWorktrees(root) {
  const output = runGit(root, ["worktree", "list", "--porcelain"]).stdout;
  const records = output.trim().split(/\n\n+/).filter(Boolean);
  return records.map((record, index) => {
    const lines = record.split(/\r?\n/);
    const worktreeLine = lines.find((line) => line.startsWith("worktree "));
    const branchLine = lines.find((line) =>
      line.startsWith("branch refs/heads/"),
    );
    const headLine = lines.find((line) => line.startsWith("HEAD "));
    const worktreePath = fs.realpathSync(
      worktreeLine.slice("worktree ".length),
    );
    return {
      path: worktreePath,
      branch: branchLine
        ? branchLine.slice("branch refs/heads/".length)
        : "(detached)",
      head: headLine?.slice("HEAD ".length) ?? "",
      primary: index === 0,
      changes: inspectChanges(worktreePath),
    };
  });
}

function isMerged(worktreePath, base) {
  const result = runGit(
    worktreePath,
    ["merge-base", "--is-ancestor", "HEAD", base],
    { allowFailure: true },
  );
  return result.status === 0;
}

function isMergedCompletedWorktree(worktree, base) {
  if (!isMerged(worktree.path, base)) return false;
  const baseHead = runGit(worktree.path, ["rev-parse", base]).stdout.trim();
  return worktree.head !== baseHead;
}

function resolveUpstream(worktreePath) {
  const upstream = runGit(
    worktreePath,
    ["rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{upstream}"],
    {
      allowFailure: true,
    },
  );
  if (upstream.status !== 0) return null;
  const divergence = runGit(worktreePath, [
    "rev-list",
    "--left-right",
    "--count",
    "@{upstream}...HEAD",
  ]);
  const [behind, ahead] = divergence.stdout.trim().split(/\s+/).map(Number);
  return { name: upstream.stdout.trim(), behind, ahead };
}

function summarizeChanges(changes) {
  return {
    staged: changes.staged.length,
    unstaged: changes.unstaged.length,
    untracked: changes.untracked.length,
    conflicts: changes.conflicts.length,
  };
}

function samePath(left, right) {
  try {
    return fs.realpathSync(left) === fs.realpathSync(right);
  } catch {
    return path.resolve(left) === path.resolve(right);
  }
}

function inspect(args) {
  if (!["start", "commit", "deliver", "cleanup"].includes(args.mode)) {
    throw new Error("--mode 必须是 start、commit、deliver 或 cleanup");
  }
  const repositoryRoot = fs.realpathSync(
    runGit(path.resolve(args.root), [
      "rev-parse",
      "--show-toplevel",
    ]).stdout.trim(),
  );
  const worktrees = parseWorktrees(repositoryRoot);
  const current = worktrees.find((entry) =>
    samePath(entry.path, repositoryRoot),
  );
  if (!current) throw new Error(`当前 Git worktree 未登记：${repositoryRoot}`);

  const allowedDirtyWorktrees = args.allowedDirtyWorktrees
    .filter(Boolean)
    .map((entry) => {
      try {
        return fs.realpathSync(entry);
      } catch {
        return path.resolve(entry);
      }
    });
  const errors = [];
  const warnings = [];
  const isMain = current.branch === "main" || current.branch === "master";
  const taskWorktree =
    !current.primary && !isMain && current.branch !== "(detached)";

  if (args.mode === "start") {
    if (args.reuseCurrentTask === "true") {
      if (!taskWorktree) {
        errors.push("声明复用当前任务，但当前不是非 main 任务 worktree。");
      }
      if (!args.expectedBranch) {
        errors.push(
          "复用任务 worktree 必须从既有任务或 PR 证据提供 --expected-branch，不能只根据当前分支猜测。",
        );
      } else if (args.expectedBranch !== current.branch) {
        errors.push(
          `任务分支不匹配：期望 ${args.expectedBranch}，当前 ${current.branch}。`,
        );
      }
    } else if (taskWorktree) {
      if (current.changes.dirty) {
        errors.push(
          "当前任务 worktree 仍有未提交内容，禁止在这里开始另一任务或另建 worktree。",
        );
      } else {
        warnings.push(
          "当前是其它任务 worktree；新任务必须回到主工作区并从最新 base 创建。",
        );
      }
    } else if (current.changes.dirty) {
      errors.push(
        "当前主工作区存在本地变更，禁止以此为基准创建任务 worktree。",
      );
    }

    for (const worktree of worktrees) {
      if (worktree.path === current.path || !worktree.changes.dirty) continue;
      const merged = isMergedCompletedWorktree(worktree, args.base);
      const explicitlyAllowed = allowedDirtyWorktrees.includes(worktree.path);
      if (merged) {
        errors.push(
          `已合并 worktree 仍有未提交内容，禁止复用或忽略：${worktree.path} (${worktree.branch})。`,
        );
      } else if (!explicitlyAllowed) {
        errors.push(
          `发现未确认保留的脏 worktree：${worktree.path} (${worktree.branch})。先完成、提交或由用户明确确认并行保留。`,
        );
      } else {
        warnings.push(
          `用户已明确确认并行保留脏 worktree：${worktree.path} (${worktree.branch})。`,
        );
      }
    }
  }

  if (["commit", "deliver", "cleanup"].includes(args.mode)) {
    if (!taskWorktree) {
      errors.push(`${args.mode} 门禁只能在非 main 任务 worktree 执行。`);
    }
    if (!args.expectedBranch) {
      errors.push(`${args.mode} 门禁必须提供任务证据中的 --expected-branch。`);
    } else if (args.expectedBranch !== current.branch) {
      errors.push(
        `任务分支不匹配：期望 ${args.expectedBranch}，当前 ${current.branch}。`,
      );
    }
  }

  if (args.mode === "commit") {
    if (current.changes.conflicts.length > 0)
      errors.push("存在未解决冲突，禁止提交。");
    if (current.changes.staged.length === 0)
      errors.push("没有已暂存的任务变更，不能形成提交。");
    if (current.changes.unstaged.length > 0)
      errors.push("仍有未暂存修改；可能只提交了部分代码。");
    if (current.changes.untracked.length > 0)
      errors.push("仍有未跟踪文件；必须逐项决定提交、忽略或清理。");
  }

  let upstream = null;
  if (args.mode === "deliver" || args.mode === "cleanup") {
    if (current.changes.dirty)
      errors.push(
        "任务 worktree 不干净；提交、PR、合并或清理前不得遗留本地变更。",
      );
    upstream = resolveUpstream(current.path);
    if (args.requireUpstream) {
      if (!upstream) {
        errors.push("任务分支没有 upstream，无法证明本地提交已全部推送。");
      } else {
        if (upstream.ahead > 0)
          errors.push(`本地还有 ${upstream.ahead} 个未推送提交。`);
        if (upstream.behind > 0)
          errors.push(`本地落后 upstream ${upstream.behind} 个提交。`);
      }
    }
  }

  if (args.mode === "cleanup") {
    if (!isMerged(current.path, args.base)) {
      errors.push(
        `当前 HEAD 尚未合入 ${args.base}，禁止按已完成任务清理 worktree。`,
      );
    }
  }

  if (args.mode !== "start") {
    for (const worktree of worktrees) {
      if (worktree.path !== current.path && worktree.changes.dirty) {
        warnings.push(
          `其它 worktree 仍有本地变更：${worktree.path} (${worktree.branch})。`,
        );
      }
    }
  }

  return {
    mode: args.mode,
    repositoryRoot,
    base: args.base,
    current: {
      path: current.path,
      branch: current.branch,
      primary: current.primary,
      head: current.head,
      changes: summarizeChanges(current.changes),
      stagedFiles: current.changes.staged,
      unstagedFiles: current.changes.unstaged,
      untrackedFiles: current.changes.untracked,
      conflictFiles: current.changes.conflicts,
      upstream,
    },
    worktrees: worktrees.map((entry) => ({
      path: entry.path,
      branch: entry.branch,
      head: entry.head,
      primary: entry.primary,
      changes: summarizeChanges(entry.changes),
    })),
    warnings,
    errors,
    ok: errors.length === 0,
  };
}

function printText(result) {
  console.log("Worktree 交付完整性检查");
  console.log(`阶段：${result.mode}`);
  console.log(`当前：${result.current.path}`);
  console.log(`分支：${result.current.branch}`);
  console.log(
    `变更：已暂存 ${result.current.changes.staged}，未暂存 ${result.current.changes.unstaged}，未跟踪 ${result.current.changes.untracked}，冲突 ${result.current.changes.conflicts}`,
  );
  if (result.current.upstream) {
    console.log(
      `远端：${result.current.upstream.name}，领先 ${result.current.upstream.ahead}，落后 ${result.current.upstream.behind}`,
    );
  }
  for (const warning of result.warnings) console.log(`提醒：${warning}`);
  for (const error of result.errors) console.log(`阻断：${error}`);
  console.log(result.ok ? "结果：PASS" : "结果：STOP");
}

try {
  const args = parseArgs(process.argv.slice(2));
  const result = inspect(args);
  if (args.json) console.log(JSON.stringify(result, null, 2));
  else printText(result);
  process.exit(result.ok ? 0 : 1);
} catch (error) {
  console.error(`Worktree 交付完整性检查失败：${error.message}`);
  process.exit(1);
}
