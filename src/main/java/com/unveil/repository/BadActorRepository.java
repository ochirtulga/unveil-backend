package com.unveil.repository;

import com.unveil.entity.BadActor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BadActorRepository  extends JpaRepository<BadActor, Long> {

    /**
     * Search BadActors by query string across multiple fields
     * This searches name, email, phone, and company fields
     */
    @Query("SELECT s FROM BadActor s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "s.phone LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(s.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.actions) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<BadActor> searchBadActors(@Param("query") String query);

    /**
     * Search with pagination for better performance
     */
    @Query("SELECT s FROM BadActor s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "s.phone LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(s.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.actions) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<BadActor> searchBadActors(@Param("query") String query, Pageable pageable);

    /**
     * Find by exact email match
     */
    Page<BadActor> findByEmailIgnoreCase(String email, Pageable pageable);

    /**
     * Find by exact phone match
     */
    Page<BadActor> findByPhone(String phone, Pageable pageable);

    /**
     * Find by name containing (case insensitive)
     */
    Page<BadActor> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find by company containing (case insensitive)
     */
    Page<BadActor> findByCompanyContainingIgnoreCase(String company, Pageable pageable);

    /**
     * Find by scam type
     */
    Page<BadActor> findByActionsContainingIgnoreCase(String actions, Pageable pageable);

    /**
     * Get all distinct scam types for filtering
     */
    @Query("SELECT DISTINCT s.actions FROM BadActor s WHERE s.actions IS NOT NULL ORDER BY s.actions")
    List<String> findAllActions();
}
