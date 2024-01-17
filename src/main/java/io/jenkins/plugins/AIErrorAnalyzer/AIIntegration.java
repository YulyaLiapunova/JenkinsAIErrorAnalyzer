package io.jenkins.plugins.AIErrorAnalyzer;

import java.io.IOException;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class AIIntegration {
    private static Logger logger = Logger.getLogger(AIIntegration.class.getName());

    private String serviceName;

    private String apiKey;

    // private static final String API_KEY = "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";

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public AIIntegration(String serviceName, String apiKey) {
        this.serviceName = serviceName;
        this.apiKey = apiKey;
    }

    public String getResponse(String prompt) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(API_URL);
            request.setHeader("Authorization", "Bearer " + this.apiKey);
            request.setHeader("Content-Type", "application/json");

            // Create request body
            JSONObject body = new JSONObject();

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", "You are a helpful devops assistant."));
            messages.put(new JSONObject().put("role", "user").put("content", prompt));

            body.put("model", "gpt-3.5-turbo");
            body.put("messages", messages);

            request.setEntity(new StringEntity(body.toString()));

            // Execute and get the response
            HttpResponse response = httpClient.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            JSONObject responseObject = new JSONObject(jsonResponse);
            return responseObject
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (IOException e) {
            e.printStackTrace();
            return "Error occurred while generating response.";
        }
    }
}
