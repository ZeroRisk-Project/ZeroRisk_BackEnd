package com.zerorisk.project.benchmark;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ExecutionContext;

// 키셋(Seek) 페이징 Reader - "마지막으로 본 ID보다 큰 것만" 조회. OFFSET이 없어 뒷페이지로
// 갈수록 앞의 행을 다시 읽고 버리는 낭비가 없다. Step 단위로 매번 새로 생성되므로 필드 상태를
// 그대로 유지해도 안전함 (Step 재사용 안 함 - StepScope로 매 Job 실행마다 새 인스턴스).
public class KeysetItemReader implements ItemReader<BenchmarkLog>, ItemStream {

    private static final int CHUNK_SIZE = 1000;

    private final EntityManager entityManager;
    private Long lastId = 0L;
    private List<BenchmarkLog> currentChunk = new ArrayList<>();
    private int currentIndex = 0;

    public KeysetItemReader(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public BenchmarkLog read() {
        if (currentIndex >= currentChunk.size()) {
            // JpaPagingItemReader(offset)는 페이지마다 영속성 컨텍스트를 비우는데, 여기선 그걸
            // 안 하고 있었음 - EntityManager를 안 바꾸고 계속 재사용하다 보니 1차 캐시(영속성
            // 컨텍스트)에 이미 읽은 엔티티가 계속 쌓여서 Old Gen이 더 커지는 원인이 됐다.
            // 다음 청크를 읽기 전에 비워서 offset과 동일한 조건으로 맞춘다.
            entityManager.clear();
            currentChunk = entityManager.createQuery(
                            "SELECT b FROM BenchmarkLog b WHERE b.id > :lastId ORDER BY b.id",
                            BenchmarkLog.class)
                    .setParameter("lastId", lastId)
                    .setMaxResults(CHUNK_SIZE)
                    .getResultList();
            currentIndex = 0;
            if (currentChunk.isEmpty()) {
                return null; // 더 이상 없음, Step 종료
            }
        }
        BenchmarkLog result = currentChunk.get(currentIndex++);
        lastId = result.getId();
        return result;
    }

    // 같은 Reader 빈이 여러 번(반복 측정) 재사용될 수 있으므로, Step 시작 시 상태를 초기화한다.
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        lastId = 0L;
        currentChunk = new ArrayList<>();
        currentIndex = 0;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // 재시작 지원이 필요 없는 일회성 벤치마크라 상태 저장 안 함.
    }

    @Override
    public void close() throws ItemStreamException {
    }
}
