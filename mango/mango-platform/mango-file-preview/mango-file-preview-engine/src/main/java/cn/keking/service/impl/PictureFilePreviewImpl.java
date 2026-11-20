package cn.keking.service.impl;

import cn.keking.model.FileAttribute;
import cn.keking.service.FileHandlerService;
import cn.keking.utils.KkFileUtils;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kl on 2018/1/17.
 * Content :图片文件处理
 */
@Service
public class PictureFilePreviewImpl extends CommonPreviewImpl {

    private final FileHandlerService fileHandlerService;

    public PictureFilePreviewImpl(FileHandlerService fileHandlerService, OtherFilePreviewImpl otherFilePreview) {
        super(fileHandlerService, otherFilePreview);
        this.fileHandlerService = fileHandlerService;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        url= KkFileUtils.htmlEscape(url);
        String suffix = fileAttribute.getSuffix();
        List<String> imgUrls = new ArrayList<>();
        imgUrls.add(url);
        String compressFileKey = fileAttribute.getCompressFileKey();
        List<String> zipImgUrls = fileHandlerService.getImgCache(compressFileKey);
        if (!CollectionUtils.isEmpty(zipImgUrls)) {
            imgUrls.addAll(zipImgUrls);
            model.addAttribute("imgUrls", imgUrls);
        }else {
            // 不是http开头，浏览器不能直接访问，需下载到本地
            super.filePreviewHandle(url, model, fileAttribute);
            if ( url.toLowerCase().startsWith("file") || url.toLowerCase().startsWith("ftp")) {
                model.addAttribute("imgUrls", fileAttribute.getName());
            }else if (url.toLowerCase().startsWith("http")) {
                model.addAttribute("imgUrls", url);
            }else {
                // 取源票据通道（fileId）下票据本身在浏览器里没有 handler，改用落盘文件的统一前缀地址。
                model.addAttribute("imgUrls", publicUrl(model, url));
            }

        }
        if(suffix.equalsIgnoreCase("heic")||suffix.equalsIgnoreCase("heif")){
            return HEIC_FILE_PREVIEW_PAGE;
        }else {
            return PICTURE_FILE_PREVIEW_PAGE;
        }
    }

    /**
     * 非 http/ftp 的取源地址（fileId 通道下即票据）在浏览器里没有对应 handler，页面里必须改用落盘文件地址。
     *
     * @param model 已由 {@link CommonPreviewImpl} 写入 {@code currentUrl}（引擎 file 目录下的相对路径）
     * @param url   原始取源地址
     * @return {@code /preview-file/} 前缀下的同源地址，取不到落盘文件时保留原地址
     */
    private static String publicUrl(Model model, String url) {
        Object currentUrl = model.asMap().get("currentUrl");
        if (currentUrl instanceof String path && !path.isBlank()) {
            return FileHandlerService.toPublicPath(path);
        }
        return url;
    }
}
