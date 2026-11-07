package com.ginomarin.persistencia.service;

import com.ginomarin.persistencia.dto.RegistroRequestDTO;
import com.ginomarin.persistencia.dto.TokenServicioDTO;
import com.ginomarin.persistencia.dto.UsuarioDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Registra y lista usuarios usando la Admin REST API de Keycloak.
 * La API no guarda la contraseña, solo se la pasa a Keycloak al crear el usuario.
 */
@Service
public class UsuarioService {

    private final RestClient restClient = RestClient.create();
    private final String tokenUri;
    private final String adminUri;
    private final String adminClientId;
    private final String adminClientSecret;

    public UsuarioService(@Value("${keycloak.token-uri}") String tokenUri,
                          @Value("${keycloak.admin-uri}") String adminUri,
                          @Value("${keycloak.admin-client-id}") String adminClientId,
                          @Value("${keycloak.admin-client-secret}") String adminClientSecret) {
        this.tokenUri = tokenUri;
        this.adminUri = adminUri;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
    }

    public void registrar(RegistroRequestDTO request) {
        if (vacio(request.username()) || vacio(request.email()) || vacio(request.nombre())
                || vacio(request.apellido()) || vacio(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos los campos son obligatorios");
        }

        String token = obtenerToken();

        Map<String, Object> usuario = Map.of(
                "username", request.username(),
                "email", request.email(),
                "firstName", request.nombre(),
                "lastName", request.apellido(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of("type", "password", "value", request.password(), "temporary", false))
        );

        URI ubicacion;
        try {
            ubicacion = restClient.post()
                    .uri(adminUri + "/users")
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(usuario)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre de usuario o correo");
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo registrar el usuario, revise los datos");
        }

        // Keycloak devuelve el id del usuario nuevo al final del header Location
        String path = ubicacion.getPath();
        String usuarioId = path.substring(path.lastIndexOf('/') + 1);

        // Le asignamos el rol USER
        Map<String, Object> rolUser = restClient.get()
                .uri(adminUri + "/roles/USER")
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        restClient.post()
                .uri(adminUri + "/users/" + usuarioId + "/role-mappings/realm")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(rolUser))
                .retrieve()
                .toBodilessEntity();
    }

    public List<UsuarioDTO> listar() {
        String token = obtenerToken();
        return restClient.get()
                .uri(adminUri + "/users")
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    // Token del cliente de servicio, con los permisos de realm-management
    private String obtenerToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);

        TokenServicioDTO respuesta = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenServicioDTO.class);
        return respuesta.accessToken();
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
