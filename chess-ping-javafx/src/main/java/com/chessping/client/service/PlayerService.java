package com.chessping.client.service;

import com.chessping.client.model.Player;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class PlayerService {

    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public PlayerService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public List<Player> findAll() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/players"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erreur HTTP " + response.statusCode());
        }

        return mapper.readValue(response.body(), new TypeReference<List<Player>>() {});
    }

    public Player create(String name) throws IOException, InterruptedException {
        Player p = new Player();
        p.setName(name);

        String json = mapper.writeValueAsString(p);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/players"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Erreur HTTP " + response.statusCode());
        }

        return mapper.readValue(response.body(), Player.class);
    }
}
