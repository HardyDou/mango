import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import test from "node:test";
import { fileURLToPath } from "node:url";

const testDir = path.dirname(fileURLToPath(import.meta.url));
const checker = path.resolve(
  testDir,
  "../tools/check-worktree-delivery-integrity.mjs",
);

function run(command, args, cwd, expectedStatus = 0) {
  const result = spawnSync(command, args, {
    cwd,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
  assert.equal(
    result.status,
    expectedStatus,
    `${command} ${args.join(" ")}\nstdout:\n${result.stdout}\nstderr:\n${result.stderr}`,
  );
  return result;
}

function git(cwd, ...args) {
  return run("git", args, cwd);
}

function check(cwd, args, expectedStatus) {
  const result = run(
    "node",
    [checker, "--root", cwd, "--json", ...args],
    cwd,
    expectedStatus,
  );
  return JSON.parse(result.stdout);
}

test("worktree 交付门禁覆盖跨任务、部分提交、推送和清理场景", () => {
  const fixtureRoot = fs.mkdtempSync(
    path.join(os.tmpdir(), "mango-worktree-integrity-"),
  );
  const repository = path.join(fixtureRoot, "repository");
  const taskWorktree = path.join(fixtureRoot, "task-a");
  const remote = path.join(fixtureRoot, "remote.git");

  try {
    fs.mkdirSync(repository);
    git(repository, "init", "-b", "main");
    git(repository, "config", "user.name", "Mango Test");
    git(repository, "config", "user.email", "mango-test@example.com");
    fs.writeFileSync(path.join(repository, "tracked.txt"), "base\n");
    git(repository, "add", "tracked.txt");
    git(repository, "commit", "-m", "base");
    git(repository, "worktree", "add", "-b", "task/a", taskWorktree, "main");

    fs.appendFileSync(path.join(taskWorktree, "tracked.txt"), "task change\n");

    const abandonedCurrent = check(
      taskWorktree,
      ["--mode", "start", "--reuse-current-task", "false"],
      1,
    );
    assert.match(abandonedCurrent.errors.join("\n"), /禁止在这里开始另一任务/);

    const sameTaskReuse = check(
      taskWorktree,
      [
        "--mode",
        "start",
        "--reuse-current-task",
        "true",
        "--expected-branch",
        "task/a",
      ],
      0,
    );
    assert.equal(sameTaskReuse.ok, true);

    const missingTaskEvidence = check(
      taskWorktree,
      ["--mode", "start", "--reuse-current-task", "true"],
      1,
    );
    assert.match(
      missingTaskEvidence.errors.join("\n"),
      /existing task or PR evidence|既有任务或 PR 证据/,
    );

    const unacknowledgedSibling = check(
      repository,
      ["--mode", "start", "--reuse-current-task", "false"],
      1,
    );
    assert.match(
      unacknowledgedSibling.errors.join("\n"),
      /未确认保留的脏 worktree/,
    );

    const acknowledgedSibling = check(
      repository,
      [
        "--mode",
        "start",
        "--reuse-current-task",
        "false",
        "--allow-dirty-worktree",
        taskWorktree,
      ],
      0,
    );
    assert.match(acknowledgedSibling.warnings.join("\n"), /明确确认并行保留/);

    fs.writeFileSync(
      path.join(repository, "main-local.txt"),
      "main local change\n",
    );
    const dirtyMain = check(
      repository,
      [
        "--mode",
        "start",
        "--reuse-current-task",
        "false",
        "--allow-dirty-worktree",
        taskWorktree,
      ],
      1,
    );
    assert.match(dirtyMain.errors.join("\n"), /主工作区存在本地变更/);
    fs.rmSync(path.join(repository, "main-local.txt"));

    git(taskWorktree, "add", "tracked.txt");
    fs.writeFileSync(path.join(taskWorktree, "forgotten.txt"), "forgotten\n");
    const partialCommit = check(
      taskWorktree,
      ["--mode", "commit", "--expected-branch", "task/a"],
      1,
    );
    assert.match(partialCommit.errors.join("\n"), /未跟踪文件/);

    git(taskWorktree, "add", "forgotten.txt");
    const completeCommitCandidate = check(
      taskWorktree,
      ["--mode", "commit", "--expected-branch", "task/a"],
      0,
    );
    assert.deepEqual(completeCommitCandidate.current.stagedFiles.sort(), [
      "forgotten.txt",
      "tracked.txt",
    ]);
    git(taskWorktree, "commit", "-m", "complete task");

    const localDelivery = check(
      taskWorktree,
      [
        "--mode",
        "deliver",
        "--expected-branch",
        "task/a",
        "--require-upstream",
        "false",
      ],
      0,
    );
    assert.equal(localDelivery.current.changes.untracked, 0);

    const missingPushEvidence = check(
      taskWorktree,
      [
        "--mode",
        "deliver",
        "--expected-branch",
        "task/a",
        "--require-upstream",
        "true",
      ],
      1,
    );
    assert.match(missingPushEvidence.errors.join("\n"), /没有 upstream/);

    run("git", ["init", "--bare", remote], fixtureRoot);
    git(repository, "remote", "add", "origin", remote);
    git(repository, "push", "-u", "origin", "main");
    git(taskWorktree, "push", "-u", "origin", "task/a");
    const pushedDelivery = check(
      taskWorktree,
      [
        "--mode",
        "deliver",
        "--expected-branch",
        "task/a",
        "--require-upstream",
        "true",
      ],
      0,
    );
    assert.equal(pushedDelivery.current.upstream.ahead, 0);
    assert.equal(pushedDelivery.current.upstream.behind, 0);

    const prematureCleanup = check(
      taskWorktree,
      ["--mode", "cleanup", "--expected-branch", "task/a"],
      1,
    );
    assert.match(prematureCleanup.errors.join("\n"), /尚未合入 main/);

    git(repository, "merge", "--no-ff", "task/a", "-m", "merge task");
    const mergedCleanup = check(
      taskWorktree,
      ["--mode", "cleanup", "--expected-branch", "task/a"],
      0,
    );
    assert.equal(mergedCleanup.ok, true);

    fs.writeFileSync(path.join(taskWorktree, "late-change.txt"), "late\n");
    const mergedDirtyReuse = check(
      repository,
      [
        "--mode",
        "start",
        "--reuse-current-task",
        "false",
        "--allow-dirty-worktree",
        taskWorktree,
      ],
      1,
    );
    assert.match(
      mergedDirtyReuse.errors.join("\n"),
      /已合并 worktree 仍有未提交内容/,
    );
  } finally {
    fs.rmSync(fixtureRoot, { recursive: true, force: true });
  }
});
