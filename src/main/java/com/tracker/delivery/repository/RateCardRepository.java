package com.tracker.delivery.repository;

import com.tracker.delivery.entity.RateCard;
import com.tracker.delivery.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface RateCardRepository extends JpaRepository<RateCard, Long> {
    Optional<RateCard> findByFromZoneAndToZoneAndOrderType(Zone fromZone, Zone toZone, String orderType);
    List<RateCard> findByOrderType(String orderType);
}
