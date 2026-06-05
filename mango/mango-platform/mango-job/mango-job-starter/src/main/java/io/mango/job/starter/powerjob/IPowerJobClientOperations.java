package io.mango.job.starter.powerjob;

import tech.powerjob.common.request.http.RunJobRequest;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.PowerResultDTO;
import tech.powerjob.common.response.ResultDTO;

/**
 * PowerJob SDK 操作适配接口。
 */
public interface IPowerJobClientOperations {

    ResultDTO<Long> saveJob(SaveJobInfoRequest request);

    ResultDTO<Void> enableJob(Long jobId);

    ResultDTO<Void> disableJob(Long jobId);

    ResultDTO<Void> deleteJob(Long jobId);

    PowerResultDTO<Long> runJob(RunJobRequest request);

    ResultDTO<?> fetchAllJob();
}
