package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.TtsAudioCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TtsAudioCacheRepository extends JpaRepository<TtsAudioCache, String> {

    List<TtsAudioCache> findAllByCacheKeyIn(Collection<String> cacheKeys);
}
