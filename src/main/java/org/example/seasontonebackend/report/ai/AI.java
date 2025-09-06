package org.example.seasontonebackend.report.ai;

import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class AI {

    private final String apiKey = "AIzaSyCZvT1kmnRkj9HNi8VoaAHz3JV37w4nviE"; // 발급받은 API 키

    // 프롬프트를 보내고 Gemini 텍스트를 반환하는 메서드
    public String getGeminiResponse(String prompt) {
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-goog-api-key", apiKey);
            conn.setDoOutput(true);

            String jsonInputString =
                    "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + prompt + "\" } ] } ] }";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 응답 읽기
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // text 추출
            return extractText(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred";
        }
    }

    // JSON에서 text만 추출
    private String extractText(String json) {
        String key = "\"text\":";
        int startIndex = json.indexOf(key);
        if (startIndex == -1) return "text not found";
        startIndex = json.indexOf("\"", startIndex + key.length()) + 1;
        int endIndex = json.indexOf("\"", startIndex);
        return json.substring(startIndex, endIndex);
    }
}
