package com.recruit.controller;

import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 * 处理简历PDF、企业营业执照等文件上传
 */
@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    @Value("${file.access-url:/uploads/}")
    private String accessUrl;

    /**
     * 通用文件上传接口
     * 
     * @param file 上传的文件
     * @param type 文件类型（resume=简历，license=营业执照，avatar=头像）
     * @return 文件访问路径
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "resume") String type) {

        // 1. 验证文件是否为空
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }

        // 2. 验证文件大小（限制10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过10MB");
        }

        // 3. 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error(400, "文件名不能为空");
        }
        
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        
        if ("resume".equals(type) && !"pdf".equals(fileExtension)) {
            return Result.error(400, "简历仅支持PDF格式");
        }
        
        if ("avatar".equals(type) && !"jpg".equals(fileExtension) 
                && !"png".equals(fileExtension) && !"jpeg".equals(fileExtension)) {
            return Result.error(400, "头像仅支持JPG/PNG格式");
        }

        try {
            // 4. 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "." + fileExtension;
            
            // 5. 按日期组织目录（如：uploads/resume/2026-06-03/）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String datePath = sdf.format(new Date());
            String relativePath = type + "/" + datePath + "/";
            String fullPath = uploadPath + relativePath;
            
            // 6. 创建目录
            File destDir = new File(fullPath);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            // 7. 保存文件
            File destFile = new File(fullPath + fileName);
            file.transferTo(destFile);

            // 8. 返回文件访问路径
            String fileUrl = accessUrl + relativePath + fileName;
            
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("fileName", originalFilename);
            result.put("fileSize", String.valueOf(file.getSize()));

            return Result.success("上传成功", result);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     * 
     * @param fileUrl 文件URL
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam String fileUrl) {
        try {
            // 将URL路径转换为本地路径
            String localPath = fileUrl.replace(accessUrl, uploadPath);
            File file = new File(localPath);
            
            if (file.exists() && file.delete()) {
                return Result.success("删除成功");
            } else {
                return Result.error(404, "文件不存在或删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "删除失败：" + e.getMessage());
        }
    }
}
