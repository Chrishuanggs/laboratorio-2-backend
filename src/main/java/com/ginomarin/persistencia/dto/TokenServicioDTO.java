package com.ginomarin.persistencia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Token del cliente de servicio (client_credentials)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenServicioDTO(
        @JsonProperty("access_token") String accessToken
) {
}
