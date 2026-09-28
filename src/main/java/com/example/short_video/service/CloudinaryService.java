package com.example.short_video.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.short_video.dto.response.UploadResult;
import com.example.short_video.service.imp.CloudinaryServiceImp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class CloudinaryService implements CloudinaryServiceImp {
    private final Cloudinary cloudinary;


    @Override
    public UploadResult uploadImg(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "folder", "short-video/images",
                        "quality", "auto",
                        "fetch_format", "auto"
                )
        );
        return UploadResult.builder()
                .publicId((String) result.get("public_id"))
                .url((String) result.get("secure_url"))
                .format((String) result.get("format"))
                .bytes(((Number) result.get("bytes")).longValue())
                .width(((Number) result.get("width")).intValue())
                .height(((Number) result.get("height")).intValue())
                .resourceType((String) result.get("resource_type"))
                .build();
    }

    @Override
    public UploadResult uploadVideo(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(), ObjectUtils.asMap(
                        "resource_type", "video",
                        "folder", "short-video/videos",
                        "quality", "auto",
                        "fetch_format", "auto",
                        "overwrite", false,
                        "unique_filename", true,
                        "use_filename", false
                )
        );
        return UploadResult.builder()
                .publicId((String) uploadResult.get("public_id"))
                .url((String) uploadResult.get("url"))
                .format((String) uploadResult.get("format"))
                .bytes(((Number) uploadResult.get("bytes")).longValue())
                .width(((Number) uploadResult.get("width")).intValue())
                .height(((Number) uploadResult.get("height")).intValue())
                .duration(uploadResult.get("duration") != null
                        ?((Number) uploadResult.get("duration")).doubleValue():null)
                .resourceType((String) uploadResult.get("resource_type"))
                .build();
    }

    @Override
    public void deleteVideo(String publicId) {

    }
}
