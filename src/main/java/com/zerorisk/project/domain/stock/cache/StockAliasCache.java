package com.zerorisk.project.domain.stock.cache;

import com.zerorisk.project.domain.stock.entity.StockAlias;
import com.zerorisk.project.domain.stock.repository.StockAliasRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StockAliasCache {

    private final StockAliasRepository stockAliasRepository;

    private volatile Map<String, String> aliasToCode = Map.of();

    @Transactional(readOnly = true)
    public void reload() {
        Map<String, String> next = new HashMap<>();
        for (StockAlias alias : stockAliasRepository.findAllWithActiveStock()) {
            next.put(alias.getAlias(), alias.getStock().getCode());
        }
        this.aliasToCode = Map.copyOf(next);
    }

    public Optional<String> resolve(String alias) {
        return Optional.ofNullable(aliasToCode.get(alias));
    }

    public int size() {
        return aliasToCode.size();
    }
}