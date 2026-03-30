#!/bin/bash
# SonarQube 扫描脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SONAR_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$SONAR_DIR")"

# 加载环境变量
if [ -f "$SCRIPT_DIR/.env" ]; then
    export "$(cat "$SCRIPT_DIR/.env" | grep -v '^#' | xargs)"
fi

SONAR_TOKEN="${SONAR_TOKEN:-squ_7cc58ee96cab71462ba194304e441243d37938ef}"

echo "========================================"
echo "Mango SonarQube 代码扫描"
echo "========================================"

# 检查 SonarQube 是否运行
check_sonarqube() {
    echo "检查 SonarQube 服务..."
    if curl -s -f http://localhost:9000/api/system/health > /dev/null 2>&1; then
        echo "✅ SonarQube 运行正常"
        return 0
    else
        echo "❌ SonarQube 未启动"
        echo ""
        echo "请先启动 SonarQube："
        echo "  cd $SCRIPT_DIR"
        echo "  docker-compose up -d"
        echo ""
        echo "然后等待 2-3 分钟后再执行此脚本"
        return 1
    fi
}

# 获取 SonarQube 状态
get_status() {
    curl -s http://localhost:9000/api/system/health | jq -r '.health' 2>/dev/null || echo "unknown"
}

# 运行扫描
run_scan() {
    local module="$1"

    echo ""
    echo "正在扫描 ${module:-mango 全量模块}..."

    cd "$PROJECT_ROOT"

    if [ -n "$module" ]; then
        echo "扫描模块: $module"
        mvn sonar:sonar \
            -Dsonar.projectKey="mango-$module" \
            -Dsonar.projectName="mango-$module" \
            -pl "$module" \
            -Dsonar.skipTests=false
    else
        echo "扫描所有模块..."
        mvn sonar:sonar \
            -Dsonar.projectKey="mango-parent" \
            -Dsonar.projectName="Mango"
    fi

    echo ""
    echo "✅ 扫描完成"
    echo "📊 查看报告: http://localhost:9000"
}

# 主逻辑
case "${1:-scan}" in
    start)
        echo "启动 SonarQube..."
        cd "$SCRIPT_DIR"
        docker-compose up -d
        echo ""
        echo "✅ SonarQube 启动中..."
        echo "   等待 2-3 分钟后访问 http://localhost:9000"
        echo "   默认账号: admin / admin"
        ;;
    stop)
        echo "停止 SonarQube..."
        cd "$SCRIPT_DIR"
        docker-compose down
        echo "✅ SonarQube 已停止"
        ;;
    status)
        if check_sonarqube; then
            STATUS=$(get_status)
            echo "状态: $STATUS"
        fi
        ;;
    scan)
        if check_sonarqube; then
            run_scan "$2"
        fi
        ;;
    *)
        echo "用法: $0 {start|stop|status|scan [module]}"
        echo ""
        echo "  start     启动 SonarQube"
        echo "  stop      停止 SonarQube"
        echo "  status    检查 SonarQube 状态"
        echo "  scan      运行代码扫描"
        echo "  scan xxx  扫描指定模块（如 mango-common）"
        exit 1
        ;;
esac
