package com.ginomarin.persistencia.dto;

public record RegistroRequestDTO(
        String username,
        String email,
        String nombre,
        String apellido,
        String password
) {
}
