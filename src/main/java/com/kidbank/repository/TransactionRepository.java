package com.kidbank.repository;

import com.kidbank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Transaction> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.createdAt >= :since")
    BigDecimal sumByUserIdAndTypeAndCreatedAtAfter(
            @Param("userId") Long userId,
            @Param("type") Transaction.Type type,
            @Param("since") LocalDateTime since);
}
