package com.example.short_video.service.zego;

import com.example.short_video.config.ZegoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZegoTokenService {

    private final ZegoProperties zegoProperties;

    /**
     * Sinh ZEGOCLOUD Token04 cho User và Room
     *
     * @param userId          ID người dùng
     * @param roomId          ID phòng live
     * @param effectiveTimeInSeconds Thời gian hiệu lực (mặc định 3600 * 24 = 24h)
     * @return Chuỗi Token04 hoàn chỉnh bắt đầu bằng "04..."
     */
    public String generateToken04(String userId, String roomId, long effectiveTimeInSeconds) {
        long appId = zegoProperties.getAppId();
        String serverSecret = zegoProperties.getServerSecret();

        if (serverSecret == null || serverSecret.isEmpty()) {
            serverSecret = zegoProperties.getAppSign();
        }

        try {
            long createTime = System.currentTimeMillis() / 1000;
            long expireTime = createTime + effectiveTimeInSeconds;
            int nonce = new SecureRandom().nextInt();

            // Payload JSON chứa quyền truy cập phòng (room_id, stream quyền)
            String payload = String.format("{\"room_id\":\"%s\",\"privilege\":{\"1\":1,\"2\":1},\"stream_id_list\":null}", roomId != null ? roomId : "");

            // 1. Pack Content
            byte[] userIdBytes = userId.getBytes(StandardCharsets.UTF_8);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

            ByteBuffer contentBuffer = ByteBuffer.allocate(8 + 8 + 4 + 2 + userIdBytes.length + 8 + 2 + payloadBytes.length);
            contentBuffer.order(ByteOrder.BIG_ENDIAN);
            contentBuffer.putLong(expireTime);
            contentBuffer.putLong(createTime);
            contentBuffer.putInt(nonce);
            contentBuffer.putShort((short) userIdBytes.length);
            contentBuffer.put(userIdBytes);
            contentBuffer.putLong(appId);
            contentBuffer.putShort((short) payloadBytes.length);
            contentBuffer.put(payloadBytes);
            byte[] content = contentBuffer.array();

            // 2. Chuẩn bị key AES 32 bytes (hoặc 16 bytes)
            byte[] keyBytes = serverSecret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length > 32) {
                byte[] trimmedKey = new byte[32];
                System.arraycopy(keyBytes, 0, trimmedKey, 0, 32);
                keyBytes = trimmedKey;
            } else if (keyBytes.length < 32 && keyBytes.length != 16 && keyBytes.length != 24) {
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
                keyBytes = paddedKey;
            }

            // 3. Tạo IV ngẫu nhiên 16 bytes
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);

            // 4. Mã hóa AES/CBC/PKCS5Padding
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(content);

            // 5. Đóng gói binary: expireTime (8 bytes) + IV length (2 bytes) + IV + Encrypted length (2 bytes) + Encrypted
            ByteBuffer resultBuffer = ByteBuffer.allocate(8 + 2 + iv.length + 2 + encrypted.length);
            resultBuffer.order(ByteOrder.BIG_ENDIAN);
            resultBuffer.putLong(expireTime);
            resultBuffer.putShort((short) iv.length);
            resultBuffer.put(iv);
            resultBuffer.putShort((short) encrypted.length);
            resultBuffer.put(encrypted);

            return "04" + Base64.getEncoder().encodeToString(resultBuffer.array());
        } catch (Exception e) {
            log.warn("Could not generate ZEGOCLOUD Token04: {}, fallback to basic token", e.getMessage());
            return "04" + Base64.getEncoder().encodeToString((appId + ":" + userId + ":" + roomId).getBytes(StandardCharsets.UTF_8));
        }
    }
}
