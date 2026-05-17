package com.example.expensechallenge.infrastructure.persistence;

import com.example.expensechallenge.domain.PurchaseTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface PurchaseTransactionRepository extends CrudRepository<PurchaseTransaction, UUID> {

    @Query("""
        SELECT *
          FROM purchase_transactions
         ORDER BY transaction_date DESC, created_at DESC
         LIMIT :limit OFFSET :offset
        """)
    List<PurchaseTransaction> findPage(@Param("limit") int limit, @Param("offset") int offset);
}
