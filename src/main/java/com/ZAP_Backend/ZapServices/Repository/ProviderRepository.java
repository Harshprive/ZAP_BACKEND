package com.ZAP_Backend.ZapServices.Repository;

import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<ServiceProvider, Long> {
    @Query("SELECT p FROM ServiceProvider p LEFT JOIN FETCH p.service WHERE p.id = :id")
    Optional<ServiceProvider> findByIdWithService(@Param("id") Long id);

    @Query("SELECT p FROM ServiceProvider p WHERE p.service.id = :serviceId")
    List<ServiceProvider> findByServiceId(@Param("serviceId") Long serviceId);
    @Query(value = """
        SELECT *, (
            6371 * acos(
                cos(radians(:lat)) *
                cos(radians(latitude)) *
                cos(radians(longitude) - radians(:lng)) +
                sin(radians(:lat)) *
                sin(radians(latitude))
            )
        ) AS distance
        FROM service_provider
        WHERE available = true
        HAVING distance < :radius
        ORDER BY distance ASC
        """, nativeQuery = true)
    List<ServiceProvider> findNearbyAvailableDrivers(
        @Param("lat") Double lat,
        @Param("lng") Double lng,
        @Param("radius") Double radiusInKm
    );
}
