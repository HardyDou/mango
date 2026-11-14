#!/usr/bin/env bash
# Issue #1263 本地真实场景验证：真实后端 + MySQL + Flowable。
# 前置：worktree 内 `node mango-ui/packages/mango-cli/src/index.mjs dev start backend` 已在 18002 就绪。
# 测试数据：role IT_ORSIGN_ROLE + 3 名启用成员，均在脚本外部登记，脚本按 id 前缀清理自己创建的流程实例。
set -euo pipefail
BASE=${BASE:-http://127.0.0.1:18002}
DB_NAME=${MANGO_DB_NAME:-mango_dev_mango_002}
DB="mysql -h127.0.0.1 -uroot $DB_NAME -N -B"
OUT=${1:-local-scenario.json}

login() { curl -s -m 20 -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$1\",\"password\":\"admin123\",\"tenantId\":\"1\"}" | jq -r '.data.accessToken'; }

ADMIN=$(login admin); A=$(login it_orsign_a); B=$(login it_orsign_b); C=$(login it_orsign_c)

DESIGNER='{"id":"root","nodeType":"ROOT","nodeName":"发起","childNode":{"id":"it_role_review","nodeType":"APPROVAL","nodeName":"角色或签复核","properties":{"approvalConfig":{"assigneeType":"SPECIFIED_ROLE","roleIds":["91000"],"approvalMode":"OR_SIGN","emptyAssigneeStrategy":"TO_ADMIN","actions":{"complete":{"enabled":true},"addSign":{"enabled":true}}}}}}'
BODY=$(jq -n --arg d "$DESIGNER" '{domainCode:"COMMON",categoryId:1,definitionName:"IT角色或签验证",definitionKey:"IT_ROLE_OR_SIGN",designerJson:$d,remark:"issue-1263 local scenario"}')
DEF_ID=$($DB -e "select id from workflow_definition where definition_key='IT_ROLE_OR_SIGN' order by id desc limit 1")
if [ -z "$DEF_ID" ]; then
  DEF_ID=$(curl -s -m 30 -X POST "$BASE/workflow/definitions" -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d "$BODY" | jq -r '.data')
fi
jq -n --arg d "$DESIGNER" --arg id "$DEF_ID" '{id:($id|tonumber),domainCode:"COMMON",categoryId:1,definitionName:"IT角色或签验证",definitionKey:"IT_ROLE_OR_SIGN",designerJson:$d,remark:"issue-1263 local scenario"}' > /tmp/e2e1263-update.json
curl -s -m 30 -X PUT "$BASE/workflow/definitions" -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d @/tmp/e2e1263-update.json > /dev/null
DEPLOY=$(curl -s -m 30 -X POST "$BASE/workflow/definitions/deploy?id=$DEF_ID" -H "Authorization: Bearer $ADMIN")
START=$(curl -s -m 30 -X POST "$BASE/workflow/processes/start" -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"definitionKey":"IT_ROLE_OR_SIGN","businessKey":"IT-1263-1","businessType":"IT_TEST","variables":{"mangoInitiator":"admin"}}')
PID=$(echo "$START" | jq -r '.data.processInstanceId')

tasks_before=$($DB -e "select assignee_ from ACT_RU_TASK where proc_inst_id_='$PID' and task_def_key_='it_role_review' order by assignee_")
candidate_groups=$($DB -e "select count(*) from ACT_RU_IDENTITYLINK where proc_inst_id_='$PID' and group_id_ is not null")
todo_a=$(curl -s "$BASE/workflow/tasks/todo?todoType=ASSIGNED&page=1&size=10" -H "Authorization: Bearer $A" | jq -c '{total:.data.total,taskIds:[.data.list[].id],claimStatus:[.data.list[].claimStatus]}')
todo_b=$(curl -s "$BASE/workflow/tasks/todo?todoType=ASSIGNED&page=1&size=10" -H "Authorization: Bearer $B" | jq -c '{total:.data.total,taskIds:[.data.list[].id]}')
claimable_a=$(curl -s "$BASE/workflow/tasks/todo?todoType=CLAIMABLE&page=1&size=10" -H "Authorization: Bearer $A" | jq -c '{total:.data.total}')
summary_a=$(curl -s "$BASE/workflow/tasks/todo/summary" -H "Authorization: Bearer $A" | jq -c '.data')

TASK_A=$($DB -e "select id_ from ACT_RU_TASK where proc_inst_id_='$PID' and assignee_='it_orsign_a'")
DONE=$(curl -s -m 30 -X POST "$BASE/workflow/tasks/complete-result" -H "Authorization: Bearer $A" -H 'Content-Type: application/json' \
  -d "{\"taskId\":\"$TASK_A\",\"comment\":\"IT 或签一人办结\"}")
open_tasks_after=$($DB -e "select count(*) from ACT_RU_TASK where proc_inst_id_='$PID'")
history=$($DB -e "select concat(assignee_,':',ifnull(delete_reason_,'DONE')) from ACT_HI_TASKINST where proc_inst_id_='$PID' and task_def_key_='it_role_review' order by assignee_")
ended=$($DB -e "select end_time_ is not null from ACT_HI_PROCINST where proc_inst_id_='$PID'")

jq -n \
  --arg pid "$PID" --arg defId "$DEF_ID" \
  --arg tasks "$tasks_before" --arg groups "$candidate_groups" \
  --argjson todoA "$todo_a" --argjson todoB "$todo_b" --argjson claimA "$claimable_a" --argjson sumA "$summary_a" \
  --argjson done "$DONE" --arg after "$open_tasks_after" --arg history "$history" --arg ended "$ended" \
  '{issue:"#1263", environment:{backend:"http://127.0.0.1:18002", database:"mango_dev_mango_002", engine:"Flowable", scenario:"角色3人 + OR_SIGN"},
    definitionId:$defId, processInstanceId:$pid,
    assertions:{
      roleMembersExpandedToOwnTasks:($tasks|split("\n")|map(select(length>0))|length==3),
      taskAssignees:($tasks|split("\n")|map(select(length>0))),
      candidateGroupLinks:($groups|tonumber),
      memberTodoAssigned:$todoA, otherMemberTodoAssigned:$todoB, memberClaimable:$claimA, todoSummary:$sumA,
      completeResult:{code:($done|.code), action:($done|.data.actionResult), ended:($done|.data.ended), applyStatus:($done|.data.applyStatus)},
      openTasksAfterComplete:($after|tonumber),
      historicTasks:$history,
      processEnded:($ended|tonumber==1)}}' > "$OUT"
cat "$OUT"
