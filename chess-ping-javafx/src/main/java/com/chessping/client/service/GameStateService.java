package com.chessping.client.service;

import com.chessping.client.model.GamePieceStateDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class GameStateService {

    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public GameStateService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public List<GamePieceStateDTO> getByGame(long gameId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/game-states/game/" + gameId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erreur HTTP " + response.statusCode());
        }

        return mapper.readValue(response.body(), new TypeReference<List<GamePieceStateDTO>>() {});
    }

    public void updatePieceHealth(int stateId, int newHealth) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(Map.of("newHealth", newHealth));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/game-states/" + stateId + "/health"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new IOException("Erreur HTTP " + response.statusCode());
        }
    }

    public void capturePiece(int stateId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/game-states/" + stateId + "/capture"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new IOException("Erreur HTTP " + response.statusCode());
        }
    }
}
