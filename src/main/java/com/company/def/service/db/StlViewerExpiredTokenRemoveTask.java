package com.company.def.service.db;

import com.company.def.model.StlViewerToken;
import com.company.def.repository.StlViewerTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class StlViewerExpiredTokenRemoveTask {

    @Autowired
    private StlViewerTokenRepository repository;

    @Scheduled(fixedDelay = 1000 * 60 * 60 * 24)
    public void handleEvent() {
        final List<StlViewerToken> expiredTokens = repository.findAllByExpirationDateTimeBefore(new Date());
        repository.delete(expiredTokens);
    }
}
