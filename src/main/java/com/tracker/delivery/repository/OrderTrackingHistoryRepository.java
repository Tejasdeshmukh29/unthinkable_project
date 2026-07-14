package com.tracker.delivery.repository;

import com.tracker.delivery.entity.OrderTrackingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderTrackingHistoryRepository extends JpaRepository<OrderTrackingHistory, Long> {
    List<OrderTrackingHistory> findByOrderIdOrderByTimestampAsc(Long orderId);
}
