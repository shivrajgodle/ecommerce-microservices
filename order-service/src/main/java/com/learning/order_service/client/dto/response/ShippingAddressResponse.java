package com.learning.order_service.client.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShippingAddressResponse {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}