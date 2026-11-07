package com.ginomarin.persistencia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Usuario como lo devuelve Keycloak, solo con los campos que necesitamos
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioDTO(
        String id,
        String username,
        String email,
        String firstName,
        String lastName
) {
}
