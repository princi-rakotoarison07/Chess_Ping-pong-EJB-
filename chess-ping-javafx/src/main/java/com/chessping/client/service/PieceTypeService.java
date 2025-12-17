package com.chessping.client.service;

import com.chessping.client.chess.model.PieceTypeDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.List;

public class PieceTypeService {

    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public PieceTypeService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public List<PieceTypeDTO> findAll() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/piece-types"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erreur HTTP " + response.statusCode());
        }

        return mapper.readValue(response.body(), new TypeReference<List<PieceTypeDTO>>() {});
    }

    public PieceTypeDTO updateMaxHealth(int id, int newMaxHealth) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(Map.of("newMaxHealth", newMaxHealth));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/piece-types/" + id + "/max-health"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erreur HTTP " + response.statusCode());
        }

        return mapper.readValue(response.body(), PieceTypeDTO.class);
    }
}
