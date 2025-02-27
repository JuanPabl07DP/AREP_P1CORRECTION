package co.edu.escuelaing.arep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Juan Pablo Daza Pereira
 */
public class ReflectiveChatGPTService {

    private static ReflectiveChatGPTService instance;
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String GET_URL = "http://localhost:45000/compreflex?comando=";

    private ReflectiveChatGPTService() {
    }

    public static ReflectiveChatGPTService getInstance() {
        if (instance == null) {
            instance = new ReflectiveChatGPTService();
        }
        return instance;
    }

    public JsonObject getReflectiveChatCommand(String requestQuery) throws IOException {
        String command = requestQuery.split("=", 2)[1];

        String encodedCommand = URLEncoder.encode(command, "UTF-8");

        URL obj = new URL(GET_URL + encodedCommand);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        System.out.println("Backend Request: " + obj.toString());
        System.out.println("Backend Response Code: " + responseCode);

        JsonObject responseJson = new JsonObject();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String responseString = response.toString();
            System.out.println("Backend Response: " + responseString);

            try {
                JsonObject backendResponse = JsonParser.parseString(responseString).getAsJsonObject();
                responseJson = backendResponse;
            } catch (Exception e) {
                responseJson.addProperty("error", "Failed to parse backend response: " + e.getMessage());
                responseJson.addProperty("rawResponse", responseString);
            }
        } else {
            responseJson.addProperty("error", "Backend request failed with code: " + responseCode);
        }

        return responseJson;
    }
}