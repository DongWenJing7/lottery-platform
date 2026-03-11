package com.lottery.controller;

import com.lottery.common.Result;
import com.lottery.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${lottery.upload.path:./uploads}")
    private String uploadPath;

    @PostMapping("/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择文件");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        // 只允许图片格式
        String lower = ext.toLowerCase();
        if (!lower.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            throw new BusinessException("仅支持 jpg/png/gif/webp 格式");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        File dir = new File(uploadPath).getAbsoluteFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }

        Map<String, String> result = new HashMap<>();
        result.put("url", "/uploads/" + fileName);
        return Result.success(result);
    }
}
