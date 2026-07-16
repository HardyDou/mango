package io.mango.system.api.spi;

import io.mango.system.api.command.RecordLoginLogCommand;

public interface LoginLogRecorder {
    boolean record(RecordLoginLogCommand command);
}
