package io.mango.message.core.service;

import io.mango.message.api.MessageApi;
import io.mango.message.api.po.SysMessagePo;
import io.mango.message.api.vo.SysMessageVO;
import io.mango.message.core.channel.MessageChannel;
import io.mango.message.core.mapper.SysMessageMapper;

import java.util.List;

public interface IMessageService extends MessageApi {

    void registerChannel(MessageChannel channel);
}
