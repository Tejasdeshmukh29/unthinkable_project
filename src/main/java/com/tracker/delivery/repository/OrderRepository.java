package com.tracker.delivery.repository;

import com.tracker.delivery.entity.Order;
import com.tracker.delivery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Order> findByAgentOrderByCreatedAtDesc(User agent);
    List<Order> findAllByOrderByCreatedAtDesc();

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:zoneId IS NULL OR o.pickupZone.id = :zoneId OR o.dropZone.id = :zoneId) AND " +
           "(:agentId IS NULL OR o.agent.id = :agentId) " +
           "ORDER BY o.createdAt DESC")
    List<Order> findFilteredOrders(
        @Param("status") String status,
        @Param("zoneId") Long zoneId,
        @Param("agentId") Long agentId
    );
}
