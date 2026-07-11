package com.xd.smartworksite.datasource.application;

import com.xd.smartworksite.ai.infra.AiPythonServiceProperties;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DataSourcePasswordCipher {
    private static final String AES_GCM_PREFIX = "AES_GCM:";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final AiPythonServiceProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public DataSourcePasswordCipher(AiPythonServiceProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "password is required");
        }
        try {
            byte[] keyBytes = decodeKey();
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(rawPassword.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return AES_GCM_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "data source password encrypt failed");
        }
    }


    public String decrypt(String passwordCipher) {
        if (passwordCipher == null || passwordCipher.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "data source password is empty");
        }
        if (!passwordCipher.startsWith(AES_GCM_PREFIX)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "data source password must use AES_GCM format");
        }
        try {
            byte[] keyBytes = decodeKey();
            byte[] payload = Base64.getDecoder().decode(passwordCipher.substring(AES_GCM_PREFIX.length()));
            if (payload.length <= GCM_IV_BYTES) {
                throw new IllegalArgumentException("payload too short");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[GCM_IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "data source password decrypt failed");
        }
    }

    private byte[] decodeKey() {
        String keyText = properties.getSecurity() == null ? "" : properties.getSecurity().getDataSourcePasswordKey();
        if (keyText == null || keyText.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "data source password key is not configured");
        }
        byte[] keyBytes = keyText.startsWith("base64:")
                ? Base64.getDecoder().decode(keyText.substring("base64:".length()))
                : keyText.getBytes(StandardCharsets.UTF_8);
        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "data source password key must be 16, 24, or 32 bytes");
        }
        return keyBytes;
    }
}
