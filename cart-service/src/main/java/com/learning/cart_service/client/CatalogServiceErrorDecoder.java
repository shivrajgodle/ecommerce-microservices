package com.learning.cart_service.client;


import com.learning.cart_service.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * By default, Feign throws a generic FeignException for any non-2xx
 * response — which tells you a status code, but not WHAT KIND of
 * failure this is in terms your own code can reason about. This decoder
 * translates specific statuses into specific, meaningful exception
 * types — most importantly, distinguishing "the product genuinely
 * doesn't exist" (404 — a normal, expected business outcome) from every
 * other kind of failure (which stays a generic exception, letting the
 * circuit breaker treat it as an infrastructure problem). This
 * distinction is exactly what Step 5's ignore-exceptions config relies on.
 */
public class CatalogServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();


    @Override
    public Exception decode(String methodKey, Response response) {
        if(response.status() == 404){
            return new ResourceNotFoundException("Product not found in catalog");
        }
        return defaultDecoder.decode(methodKey,response);
    }
}
