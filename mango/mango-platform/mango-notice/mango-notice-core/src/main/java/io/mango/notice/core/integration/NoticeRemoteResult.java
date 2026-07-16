package io.mango.notice.core.integration;

import io.mango.common.result.R;

import java.util.function.Function;

/** Transport-neutral snapshot of a remote Mango API result. */
public final class NoticeRemoteResult<T> {

    private final boolean success;
    private final T data;
    private final String message;

    private NoticeRemoteResult(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> NoticeRemoteResult<T> from(R<T> result) {
        if (result == null) {
            return new NoticeRemoteResult<>(false, null, null);
        }
        return new NoticeRemoteResult<>(result.isSuccess(), result.getData(), result.getMsg());
    }

    public <R> NoticeRemoteResult<R> map(Function<? super T, ? extends R> mapper) {
        R mappedData = null;
        if (data != null) {
            mappedData = mapper.apply(data);
        }
        return new NoticeRemoteResult<>(success, mappedData, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMsg() {
        return message;
    }
}
