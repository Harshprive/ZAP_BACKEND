package com.ZAP_Backend.ZapServices.Service;

import com.ZAP_Backend.ZapServices.DataTransferObject.GeocodingResponse;
import com.ZAP_Backend.ZapServices.DataTransferObject.LocationIQResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeocodingService {
    private final RestTemplate restTemplate;
    private final String apiKey;

    public GeocodingService(RestTemplate restTemplate, @Value("${locationiq.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }
    public GeocodingResponse geocodeAddress(String address) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://us1.locationiq.com/v1/search.php")
                .queryParam("key", apiKey)
                .queryParam("q", address)
                .queryParam("format", "json")
                .build()
                .toUriString();

        LocationIQResponse[] response = restTemplate.getForObject(url, LocationIQResponse[].class);

        if (response == null || response.length == 0) {
            throw new RuntimeException("No location found");
        }

        LocationIQResponse location = response[0];
        GeocodingResponse result = new GeocodingResponse();
       
        result.setAddress(location.getDisplayName());
        result.setLatitude(location.getLat());
        result.setLongitude(location.getLon());
        return result;
    }

}
