package com.ZAP_Backend.ZapServices.Service;

import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate distance between two points using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Differences in coordinates
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // Calculate distance
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Check if a provider is within the specified radius of a user
     * @param userLat User's latitude
     * @param userLon User's longitude
     * @param providerLat Provider's latitude
     * @param providerLon Provider's longitude
     * @param radiusKm Maximum distance in kilometers
     * @return true if provider is within radius, false otherwise
     */
    public boolean isProviderWithinRadius(double userLat, double userLon, 
                                        double providerLat, double providerLon, 
                                        double radiusKm) {
        double distance = calculateDistance(userLat, userLon, providerLat, providerLon);
        return distance <= radiusKm;
    }
} 