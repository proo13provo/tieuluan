package com.example.short_video.service.imp;

import com.example.short_video.dto.response.UploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CloudinaryServiceImp {
    UploadResult uploadImg(MultipartFile file) throws IOException;

    UploadResult  uploadVideo(MultipartFile file) throws IOException;

    void deleteVideo(String publicId);
}
