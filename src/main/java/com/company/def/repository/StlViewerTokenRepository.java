package com.cephx.def.repository;

import com.cephx.def.model.StlViewerToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface StlViewerTokenRepository extends JpaRepository<StlViewerToken, Integer> {
    boolean existsByToken(final String token);

    StlViewerToken findFirstByToken(final String token);

    List<StlViewerToken> findAllByExpirationDateTimeBefore(final Date currentTimestamp);
}
