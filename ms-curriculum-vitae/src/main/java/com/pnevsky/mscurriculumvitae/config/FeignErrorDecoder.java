package com.pnevsky.mscurriculumvitae.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FeignErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder fallback = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found");
        }
        return fallback.decode(methodKey, response);
    }
}
