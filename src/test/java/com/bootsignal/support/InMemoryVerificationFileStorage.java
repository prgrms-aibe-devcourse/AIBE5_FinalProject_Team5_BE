package com.bootsignal.support;

import com.bootsignal.domain.verification.storage.VerificationFileStorage;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.multipart.MultipartFile;

/** 통합 테스트용 인메모리 파일 저장소 — S3 연결 없이 업로드/다운로드를 시뮬레이션합니다. **/
public class InMemoryVerificationFileStorage implements VerificationFileStorage {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public String upload(MultipartFile file, String keyPrefix) {
        try {
            String key = keyPrefix + "/" + UUID.randomUUID();
            store.put(key, file.getBytes());
            return key;
        } catch (IOException e) {
            throw new RuntimeException("테스트 파일 저장 실패", e);
        }
    }

    @Override
    public byte[] download(String s3Key) {
        byte[] data = store.get(s3Key);
        if (data == null) {
            throw new RuntimeException("테스트 파일 없음: " + s3Key);
        }
        return data;
    }
}
