package com.learning.order_service.exception;

public class CatalogServiceUnavailableException extends RuntimeException{

    public CatalogServiceUnavailableException(String message){
        super(message);
    }
}
