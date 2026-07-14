package com.tracker.delivery.repository;

import com.tracker.delivery.entity.ZonePincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ZonePincodeRepository extends JpaRepository<ZonePincode, Long> {
    Optional<ZonePincode> findByPincode(String pincode);
    boolean existsByPincode(String pincode);
}
