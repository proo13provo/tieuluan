package com.example.short_video.dto.request;

import com.example.short_video.Enum.Gender;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(max = 500, message = "Tiểu sử (bio) không được vượt quá 500 ký tự")
    private String bio;

    private Gender gender;
}
