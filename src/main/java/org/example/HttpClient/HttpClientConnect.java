package org.example.HttpClient;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.BotInfo.InfoBot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class HttpClientConnect {

    private static final String API_URL = InfoBot.CBU_API_URL;


    public static List<Currency> getJson() {

        try {

            HttpClient client = HttpClient.newHttpClient();

            URI uri = URI.create(API_URL);

            HttpRequest request = HttpRequest.newBuilder().uri(uri).build();

            HttpResponse.BodyHandler<String> responseBodyHandler = HttpResponse.BodyHandlers.ofString();

            HttpResponse<String> send = client.send(request, responseBodyHandler);

            String jsonFile = send.body();

            Gson gson = new Gson();

            System.out.println(send.statusCode());

            TypeToken<List<Currency>> typeToken = new TypeToken<>() {
            };

            return gson.fromJson(jsonFile, typeToken);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
