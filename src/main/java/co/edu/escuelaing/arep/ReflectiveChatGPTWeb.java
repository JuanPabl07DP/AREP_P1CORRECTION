package co.edu.escuelaing.arep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import com.google.gson.JsonObject;

public class ReflectiveChatGPTWeb {
    public static void main(String[] args) {
        try {

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(35000);
            } catch (IOException e) {
                System.err.println("Could not listen on port: 35000 " + e.getMessage());
                System.exit(1);
            }

            boolean running = true;
            while (running) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    System.err.println("Accept failed " + e.getMessage());
                    System.exit(1);
                }
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine, outputLine;

                boolean firstLine = true;
                String requestStringURI = "";


                while ((inputLine = in.readLine()) != null) {
                    if (firstLine) {
                        System.out.println("Received: " + inputLine);
                        requestStringURI = inputLine.split(" ")[1];
                        firstLine = false;
                        continue;
                    }
                    if (!in.ready()) {
                        break;
                    }
                }

                URI requestURI = new URI(requestStringURI);

                if (requestURI.getPath().startsWith("/cliente")) {
                    outputLine = getClientResponse(requestURI);
                } else if (requestURI.getPath().startsWith("/consulta")) {
                    outputLine = getConsultResponse(requestURI);
                } else {
                    outputLine = getErrorPage();
                }

                out.println(outputLine);
                out.close();
                in.close();
                clientSocket.close();
            }
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getConsultResponse(URI requestURI) throws IOException {
        JsonObject response = ReflectiveChatGPTService.getInstance().getReflectiveChatCommand(requestURI.getQuery());
        return "HTTP/1.1 200 OK\n" +
                "Content-Type: text/html\n" +
                "\n" +
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>Consult</title>\n" +
                "</head>" +
                "<body>" +
                response.toString() +
                "</body>" +
                "</html>";
    }
    private static String getClientResponse(URI requestURI) {
        return null;
    }

    private static String getErrorPage() {
        return "HTTP/1.1 404 Not Found\n" +
                "Content-Type: text/html\n" +
                "\n" +
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>Not Found</title>\n" +
                "</head>" +
                "<body>" +
                "<h1>404 Not Found</h1>" +
                "</body>" +
                "</html>";
    }
}