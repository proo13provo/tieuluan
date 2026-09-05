package com.example.short_video.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBioRequest {

    @Size(max = 500, message = "Tiểu sử (bio) không được vượt quá 500 ký tự")
    private String bio;
}
