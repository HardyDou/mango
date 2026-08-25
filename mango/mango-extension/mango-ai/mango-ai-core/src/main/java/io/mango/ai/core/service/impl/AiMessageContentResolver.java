package io.mango.ai.core.service.impl;

import io.mango.ai.api.command.AiMessageContentPartCommand;
import io.mango.ai.api.enums.AiCode;
import io.mango.ai.api.enums.AiMessageContentType;
import io.mango.ai.api.enums.AiModality;
import io.mango.ai.api.vo.AiMessageContentPartVO;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.ai.core.service.AiAssistantMediaInput;
import io.mango.ai.core.service.AiUserMessageInput;
import io.mango.common.result.Require;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 校验正式文件引用并把持久化内容块转换为 Spring AI 多模态消息。 */
@Service
public class AiMessageContentResolver {

    private static final int MAX_FILE_COUNT = 6;
    private static final long MAX_FILE_BYTES = 20L * 1024L * 1024L;
    private static final long MAX_TOTAL_FILE_BYTES = 40L * 1024L * 1024L;
    private static final long MAX_CONTEXT_FILE_BYTES = 80L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_AUDIO_TYPES = Set.of("audio/mpeg", "audio/mp3", "audio/wav");

    private final ObjectProvider<IFileContentProvider> fileContentProvider;

    public AiMessageContentResolver(ObjectProvider<IFileContentProvider> fileContentProvider) {
        this.fileContentProvider = fileContentProvider;
    }

    /** 校验请求内容块并生成唯一的持久化表示。 */
    public List<AiMessageContentPartVO> normalize(List<AiMessageContentPartCommand> commands) {
        List<AiMessageContentPartCommand> validCommands = Require.nonNull(
                commands, AiCode.CHAT_REQUEST_INVALID, "消息内容不能为空");
        Require.isTrue(!validCommands.isEmpty() && validCommands.size() <= MAX_FILE_COUNT + 1,
                AiCode.CHAT_REQUEST_INVALID, "每条消息最多包含一段文本和6个文件");
        List<AiMessageContentPartVO> parts = new ArrayList<>(validCommands.size());
        int textCount = 0;
        int fileCount = 0;
        long totalFileBytes = 0L;
        for (AiMessageContentPartCommand command : validCommands) {
            Require.notNull(command, AiCode.CHAT_REQUEST_INVALID, "消息内容块不能为空");
            Require.notNull(command.getType(), AiCode.CHAT_REQUEST_INVALID, "内容块类型不能为空");
            if (command.getType() == AiMessageContentType.TEXT) {
                textCount++;
                String text = StringUtils.hasText(command.getText()) ? command.getText().trim() : null;
                Require.notBlank(text, AiCode.CHAT_REQUEST_INVALID, "文本内容不能为空");
                AiMessageContentPartVO part = new AiMessageContentPartVO();
                part.setType(AiMessageContentType.TEXT);
                part.setText(text);
                parts.add(part);
                continue;
            }
            Require.isTrue(isFileInput(command.getType()), AiCode.CHAT_REQUEST_INVALID,
                    "用户消息不允许使用该内容块类型");
            fileCount++;
            Require.notNull(command.getFileId(), AiCode.CHAT_REQUEST_INVALID, "文件内容块缺少文件标识");
            FileDownloadVO file = download(command.getFileId());
            try {
                validateFile(command.getType(), file);
                totalFileBytes += file.contentLength();
                Require.isTrue(file.contentLength() <= MAX_FILE_BYTES, AiCode.CHAT_REQUEST_INVALID,
                        "单个附件不能超过20MB");
                Require.isTrue(totalFileBytes <= MAX_TOTAL_FILE_BYTES, AiCode.CHAT_REQUEST_INVALID,
                        "单条消息附件总大小不能超过40MB");
                parts.add(filePart(command.getType(), command.getFileId(), file));
            } finally {
                close(file.inputStream());
            }
        }
        Require.isTrue(textCount <= 1 && fileCount <= MAX_FILE_COUNT,
                AiCode.CHAT_REQUEST_INVALID, "每条消息最多包含一段文本和6个文件");
        return List.copyOf(parts);
    }

    /** 使用已归一化内容块构造模型消息；模型模态不满足时明确拒绝。 */
    public UserMessage toUserMessage(List<AiMessageContentPartVO> parts, AiModelResolution resolution) {
        return toUserMessage(parts, resolution, null);
    }

    /** 使用显式模型文本构造消息，供结构化服务把固定 Schema 约束放入每一轮请求。 */
    public UserMessage toUserMessage(AiUserMessageInput input) {
        Require.notNull(input, AiCode.CHAT_REQUEST_INVALID, "模型消息输入不能为空");
        return toUserMessage(input.contentParts(), input.resolution(), input.modelText());
    }

    private UserMessage toUserMessage(
            List<AiMessageContentPartVO> parts,
            AiModelResolution resolution,
            String modelTextOverride) {
        Require.notNull(parts, AiCode.CHAT_CONTEXT_UNAVAILABLE, "消息内容缺失");
        StringBuilder text = new StringBuilder();
        appendText(text, modelTextOverride);
        List<Media> media = new ArrayList<>();
        long totalFileBytes = 0L;
        for (AiMessageContentPartVO part : parts) {
            if (part.getType() == AiMessageContentType.TEXT) {
                if (!StringUtils.hasText(modelTextOverride)) {
                    appendText(text, part.getText());
                }
                continue;
            }
            Require.isTrue(isFileInput(part.getType()), AiCode.CHAT_CONTEXT_UNAVAILABLE,
                    "历史用户消息包含非法内容块");
            FileDownloadVO file = download(part.getFileId());
            validateStoredFile(part, file);
            byte[] bytes = read(file);
            totalFileBytes += bytes.length;
            Require.isTrue(totalFileBytes <= MAX_TOTAL_FILE_BYTES, AiCode.CHAT_REQUEST_INVALID,
                    "会话附件总大小不能超过40MB");
            if (isTextFile(file.contentType())) {
                requireModality(resolution, AiModality.TEXT, file.fileName());
                appendText(text, "附件 " + file.fileName() + " 的文本内容：\n" + new String(bytes, StandardCharsets.UTF_8));
            } else {
                requireModality(resolution, modality(part.getType()), file.fileName());
                media.add(media(file, bytes));
            }
        }
        Require.isTrue(text.length() > 0 || !media.isEmpty(), AiCode.CHAT_REQUEST_INVALID, "消息内容不能为空");
        String modelText = text.length() == 0 ? "请处理我上传的附件。" : text.toString();
        return UserMessage.builder().text(modelText).media(media).build();
    }

    /** 限制本轮实际发送给模型的历史与当前附件总量，避免多轮附件绕过单条消息限制。 */
    public void validateContextFileBudget(Iterable<List<AiMessageContentPartVO>> messages) {
        long totalBytes = 0L;
        for (List<AiMessageContentPartVO> parts : messages) {
            for (AiMessageContentPartVO part : parts) {
                if (!isFileInput(part.getType())) {
                    continue;
                }
                Long fileSize = Require.nonNull(part.getFileSize(), AiCode.CHAT_CONTEXT_UNAVAILABLE,
                        "会话附件元数据无效");
                Require.isTrue(fileSize > 0 && fileSize <= MAX_FILE_BYTES,
                        AiCode.CHAT_CONTEXT_UNAVAILABLE, "会话附件元数据无效");
                Require.isTrue(totalBytes <= MAX_CONTEXT_FILE_BYTES - fileSize,
                        AiCode.CHAT_REQUEST_INVALID, "当前会话发送给模型的附件总大小不能超过80MB，请新建会话");
                totalBytes += fileSize;
            }
        }
    }

    /** 将模型返回的二进制媒体保存到 Mango 文件中心并生成持久化内容块。 */
    public AiMessageContentPartVO saveAssistantMedia(AiAssistantMediaInput input) {
        Require.notNull(input, AiCode.CHAT_MODEL_UNAVAILABLE, "AI 模型媒体输入不能为空");
        Media media = input.media();
        String requestId = input.requestId();
        int index = input.index();
        Require.notNull(media, AiCode.CHAT_MODEL_UNAVAILABLE, "AI 模型返回了空媒体");
        byte[] content = Require.nonNull(media.getDataAsByteArray(), AiCode.CHAT_MODEL_UNAVAILABLE,
                "AI 模型返回的媒体为空或超过20MB");
        Require.isTrue(content.length > 0 && content.length <= MAX_FILE_BYTES,
                AiCode.CHAT_MODEL_UNAVAILABLE, "AI 模型返回的媒体为空或超过20MB");
        String contentType = media.getMimeType().toString();
        AiMessageContentType type = outputType(contentType);
        String fileName = StringUtils.hasText(media.getName())
                ? media.getName() : "ai-output-" + requestId + '-' + index + extension(contentType);
        SaveFileCommand command = new SaveFileCommand();
        command.setInputStream(new java.io.ByteArrayInputStream(content));
        command.setFileName(fileName);
        command.setFileSize((long) content.length);
        command.setContentType(contentType);
        command.setPurpose("ai-output");
        command.setAccessLevel("PRIVATE");
        command.setBizType("AI_CHAT");
        command.setBizId(requestId);
        io.mango.file.api.vo.FileRecordVO saved = Require.nonNull(requireFileProvider().save(command),
                AiCode.CHAT_MODEL_UNAVAILABLE, "AI 输出媒体保存失败");
        AiMessageContentPartVO part = new AiMessageContentPartVO();
        part.setType(type);
        part.setFileId(saved.getId());
        part.setFileName(saved.getFileName());
        part.setContentType(saved.getContentType());
        part.setFileSize(saved.getFileSize());
        return part;
    }

    private FileDownloadVO download(Long fileId) {
        return Require.nonNull(requireFileProvider().downloadForService(fileId), AiCode.CHAT_REQUEST_INVALID,
                "附件不存在或无权访问");
    }

    private IFileContentProvider requireFileProvider() {
        return Require.nonNull(fileContentProvider.getIfAvailable(),
                AiCode.CHAT_REQUEST_INVALID, "当前部署未启用 Mango 文件能力");
    }

    private byte[] read(FileDownloadVO file) {
        Require.isTrue(file.contentLength() > 0 && file.contentLength() <= MAX_FILE_BYTES,
                AiCode.CHAT_REQUEST_INVALID, "附件大小无效或超过20MB");
        try (InputStream input = file.inputStream()) {
            byte[] bytes = input.readNBytes(Math.toIntExact(MAX_FILE_BYTES + 1));
            Require.isTrue(bytes.length <= MAX_FILE_BYTES, AiCode.CHAT_REQUEST_INVALID, "附件实际大小超过20MB");
            return bytes;
        } catch (IOException exception) {
            return Require.fail(AiCode.CHAT_REQUEST_INVALID, "读取附件失败", exception);
        }
    }

    private void validateFile(AiMessageContentType type, FileDownloadVO file) {
        Require.notNull(file.inputStream(), AiCode.CHAT_REQUEST_INVALID, "附件内容不可用");
        Require.isTrue(StringUtils.hasText(file.fileName()), AiCode.CHAT_REQUEST_INVALID, "附件名称缺失");
        Require.isTrue(StringUtils.hasText(file.contentType()), AiCode.CHAT_REQUEST_INVALID, "附件类型缺失");
        Require.isTrue(file.contentLength() > 0, AiCode.CHAT_REQUEST_INVALID, "附件内容为空");
        String contentType = baseContentType(file.contentType());
        if (type == AiMessageContentType.IMAGE) {
            Require.isTrue(contentType.startsWith("image/"), AiCode.CHAT_REQUEST_INVALID, "图片附件类型不匹配");
        } else if (type == AiMessageContentType.AUDIO) {
            Require.isTrue(SUPPORTED_AUDIO_TYPES.contains(contentType), AiCode.CHAT_REQUEST_INVALID,
                    "音频输入当前仅支持 MP3 和 WAV");
        } else if (type == AiMessageContentType.VIDEO) {
            Require.isTrue(contentType.startsWith("video/"), AiCode.CHAT_REQUEST_INVALID, "视频附件类型不匹配");
        } else {
            Require.isTrue(isTextFile(contentType) || "application/pdf".equals(contentType),
                    AiCode.CHAT_REQUEST_INVALID, "普通文件输入当前支持 PDF、TXT、CSV、JSON、XML 和 Markdown");
        }
    }

    private void validateStoredFile(AiMessageContentPartVO part, FileDownloadVO file) {
        validateFile(part.getType(), file);
        Require.isTrue(java.util.Objects.equals(part.getFileName(), file.fileName())
                        && part.getFileSize() != null && part.getFileSize() == file.contentLength()
                        && StringUtils.hasText(part.getContentType())
                        && baseContentType(part.getContentType()).equals(baseContentType(file.contentType())),
                AiCode.CHAT_CONTEXT_UNAVAILABLE, "附件元数据已变化，请新建会话后重试");
    }

    private AiMessageContentPartVO filePart(
            AiMessageContentType type,
            Long fileId,
            FileDownloadVO file) {
        AiMessageContentPartVO part = new AiMessageContentPartVO();
        part.setType(type);
        part.setFileId(fileId);
        part.setFileName(file.fileName());
        part.setContentType(baseContentType(file.contentType()));
        part.setFileSize(file.contentLength());
        return part;
    }

    private Media media(FileDownloadVO file, byte[] bytes) {
        MimeType mimeType = MimeTypeUtils.parseMimeType(baseContentType(file.contentType()));
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return file.fileName();
            }
        };
        return Media.builder().mimeType(mimeType).data(resource).name(file.fileName()).build();
    }

    private void requireModality(AiModelResolution resolution, AiModality modality, String fileName) {
        Require.isTrue(resolution.getInputModalities().contains(modality), AiCode.MODEL_INVALID,
                "当前模型不支持附件输入：" + fileName);
    }

    private AiModality modality(AiMessageContentType type) {
        return switch (type) {
            case IMAGE -> AiModality.IMAGE;
            case AUDIO -> AiModality.AUDIO;
            case VIDEO -> AiModality.VIDEO;
            case FILE -> AiModality.FILE;
            default -> AiModality.TEXT;
        };
    }

    private boolean isFileInput(AiMessageContentType type) {
        return type == AiMessageContentType.IMAGE
                || type == AiMessageContentType.AUDIO
                || type == AiMessageContentType.VIDEO
                || type == AiMessageContentType.FILE;
    }

    private boolean isTextFile(String contentType) {
        String value = baseContentType(contentType);
        return value.startsWith("text/")
                || "application/json".equals(value)
                || "application/xml".equals(value)
                || "application/csv".equals(value);
    }

    private AiMessageContentType outputType(String contentType) {
        String value = baseContentType(contentType);
        if (value.startsWith("image/")) {
            return AiMessageContentType.IMAGE;
        }
        if (value.startsWith("audio/")) {
            return AiMessageContentType.AUDIO;
        }
        if (value.startsWith("video/")) {
            return AiMessageContentType.VIDEO;
        }
        return Require.fail(AiCode.CHAT_MODEL_UNAVAILABLE, "AI 模型返回了不支持的媒体类型");
    }

    private String extension(String contentType) {
        String value = baseContentType(contentType);
        if ("image/png".equals(value)) {
            return ".png";
        }
        if ("image/jpeg".equals(value)) {
            return ".jpg";
        }
        if ("audio/mpeg".equals(value) || "audio/mp3".equals(value)) {
            return ".mp3";
        }
        if ("audio/wav".equals(value)) {
            return ".wav";
        }
        if ("video/mp4".equals(value)) {
            return ".mp4";
        }
        return ".bin";
    }

    private void close(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.close();
        } catch (IOException exception) {
            Require.fail(AiCode.CHAT_CONTEXT_UNAVAILABLE, "关闭附件输入流失败", exception);
        }
    }

    private String baseContentType(String value) {
        int separator = value.indexOf(';');
        return (separator < 0 ? value : value.substring(0, separator)).trim().toLowerCase();
    }

    private void appendText(StringBuilder target, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!target.isEmpty()) {
            target.append("\n\n");
        }
        target.append(value.trim());
    }
}
