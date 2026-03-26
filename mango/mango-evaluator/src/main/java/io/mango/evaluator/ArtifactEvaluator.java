package io.mango.evaluator;

/**
 * 评估器接口
 *
 * @author Mango
 */
public interface ArtifactEvaluator {

    /**
     * 评估指定路径的产物
     *
     * @param path 产物路径
     * @return 评估报告
     */
    EvaluationReport evaluate(String path);

    /**
     * 获取评估器名称
     */
    String getName();

    /**
     * 获取支持的产物类型
     */
    String getSupportedArtifactType();
}
