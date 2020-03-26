package com.cephx.def.repository;

import com.cephx.def.model.WpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WpTokenRepository extends JpaRepository<WpToken, Integer> {
    boolean existsWpTokenByToken(final String token);

    void deleteByToken(final String token);
}
