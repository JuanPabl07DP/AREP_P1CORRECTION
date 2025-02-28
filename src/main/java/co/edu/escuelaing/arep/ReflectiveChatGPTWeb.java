package co.edu.escuelaing.arep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Juan Pablo Daza Pereira
 */
public class ReflectiveChatGPTWeb {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(35000);
                System.out.println("Reflective ChatGPT Web Facade started on port 35000");
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
                        String[] requestParts = inputLine.split(" ");
                        if (requestParts.length >= 2) {
                            requestStringURI = requestParts[1];
                        }
                        firstLine = false;
                    }
                    if (!in.ready()) {
                        break;
                    }
                }

                try {
                    URI requestURI = new URI(requestStringURI);

                    if (requestURI.getPath().startsWith("/cliente")) {
                        outputLine = getClientResponse();
                    } else if (requestURI.getPath().startsWith("/consulta") && requestURI.getQuery() != null) {
                        outputLine = getConsultResponse(requestURI);
                    } else {
                        outputLine = getErrorPage();
                    }
                } catch (Exception e) {
                    outputLine = "HTTP/1.1 500 Internal Server Error\r\n" +
                            "Content-Type: text/html\r\n" +
                            "\r\n" +
                            "<!DOCTYPE html><html><body><h1>500 Internal Server Error</h1><p>" + e.getMessage() + "</p></body></html>";
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

        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "\r\n" +
                response.toString();
    }

    private static String getClientResponse() {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                "<!DOCTYPE html>\r\n" +
                "<html>\r\n" +
                "<head>\r\n" +
                "    <meta charset=\"UTF-8\">\r\n" +
                "    <title>Reflective ChatGPT</title>\r\n" +
                "</head>\r\n" +
                "<body>\r\n" +
                "    <h1>Reflective ChatGPT</h1>\r\n" +
                "    \r\n" +
                "    <div>\r\n" +
                "        <select id=\"cmd\" onchange=\"updateForm()\">\r\n" +
                "            <option value=\"class\">class - Get class info</option>\r\n" +
                "            <option value=\"invoke\">invoke - Call static method</option>\r\n" +
                "            <option value=\"unaryInvoke\">unaryInvoke - Method with 1 param</option>\r\n" +
                "            <option value=\"binaryInvoke\">binaryInvoke - Method with 2 params</option>\r\n" +
                "        </select>\r\n" +
                "        \r\n" +
                "        <div>\r\n" +
                "            <label for=\"className\">Class Name:</label>\r\n" +
                "            <input id=\"className\" value=\"java.lang.String\" onchange=\"updatePreview()\">\r\n" +
                "        </div>\r\n" +
                "        \r\n" +
                "        <div id=\"methodDiv\" style=\"display:none;\">\r\n" +
                "            <label for=\"methodName\">Method Name:</label>\r\n" +
                "            <input id=\"methodName\" placeholder=\"valueOf\" onchange=\"updatePreview()\">\r\n" +
                "        </div>\r\n" +
                "        \r\n" +
                "        <div id=\"param1Div\" style=\"display:none;\">\r\n" +
                "            <label>Parameter 1:</label>\r\n" +
                "            <select id=\"param1Type\" onchange=\"updatePreview()\">\r\n" +
                "                <option value=\"int\">int</option>\r\n" +
                "                <option value=\"double\">double</option>\r\n" +
                "                <option value=\"String\">String</option>\r\n" +
                "            </select>\r\n" +
                "            <input id=\"param1Value\" onchange=\"updatePreview()\">\r\n" +
                "        </div>\r\n" +
                "        \r\n" +
                "        <div id=\"param2Div\" style=\"display:none;\">\r\n" +
                "            <label>Parameter 2:</label>\r\n" +
                "            <select id=\"param2Type\" onchange=\"updatePreview()\">\r\n" +
                "                <option value=\"int\">int</option>\r\n" +
                "                <option value=\"double\">double</option>\r\n" +
                "                <option value=\"String\">String</option>\r\n" +
                "            </select>\r\n" +
                "            <input id=\"param2Value\" onchange=\"updatePreview()\">\r\n" +
                "        </div>\r\n" +
                "        \r\n" +
                "        <button onclick=\"sendCommand()\">Execute</button>\r\n" +
                "    </div>\r\n" +
                "    \r\n" +
                "    <div>\r\n" +
                "        <label>Command:</label>\r\n" +
                "        <pre id=\"preview\">class(java.lang.String)</pre>\r\n" +
                "    </div>\r\n" +
                "    \r\n" +
                "    <div>\r\n" +
                "        <label>Response:</label>\r\n" +
                "        <div id=\"response\">\r\n" +
                "            <p>Response will appear here...</p>\r\n" +
                "        </div>\r\n" +
                "    </div>\r\n" +
                "    \r\n" +
                "    <script>\r\n" +
                "        function updateForm() {\r\n" +
                "            const cmd = document.getElementById('cmd').value;\r\n" +
                "            \r\n" +
                "            // Hide all param forms initially\r\n" +
                "            document.getElementById('methodDiv').style.display = 'none';\r\n" +
                "            document.getElementById('param1Div').style.display = 'none';\r\n" +
                "            document.getElementById('param2Div').style.display = 'none';\r\n" +
                "            \r\n" +
                "            // Show relevant forms\r\n" +
                "            if (cmd !== 'class') {\r\n" +
                "                document.getElementById('methodDiv').style.display = 'block';\r\n" +
                "            }\r\n" +
                "            \r\n" +
                "            if (cmd === 'unaryInvoke' || cmd === 'binaryInvoke') {\r\n" +
                "                document.getElementById('param1Div').style.display = 'block';\r\n" +
                "            }\r\n" +
                "            \r\n" +
                "            if (cmd === 'binaryInvoke') {\r\n" +
                "                document.getElementById('param2Div').style.display = 'block';\r\n" +
                "            }\r\n" +
                "            \r\n" +
                "            updatePreview();\r\n" +
                "        }\r\n" +
                "        \r\n" +
                "        function updatePreview() {\r\n" +
                "            const cmd = document.getElementById('cmd').value;\r\n" +
                "            const className = document.getElementById('className').value;\r\n" +
                "            let command = cmd + '(' + className;\r\n" +
                "            \r\n" +
                "            if (cmd !== 'class') {\r\n" +
                "                const methodName = document.getElementById('methodName').value;\r\n" +
                "                command += ', ' + methodName;\r\n" +
                "            }\r\n" +
                "            \r\n" +
                "            if (cmd === 'unaryInvoke' || cmd === 'binaryInvoke') {\r\n" +
                "                const paramType = document.getElementById('param1Type').value;\r\n" +
                "                const paramValue = document.getElementById('param1Value').value;\r\n" +
                "                command += ', ' + paramType + ', ' + paramValue;\r\n" +
                "            }\r\n" +
                "            \r\n" +
                "            if (cmd === 'binaryInvoke') {\r\n" +
                "                const paramType = document.getElementById('param2Type').value;\r\n" +
                "                const paramValue = document.getElementById('param2Value').value;\r\n" +
                "                command += ', ' + paramType + ', ' + paramValue;\r\n" +
                "            }\r\n" +
                "            \r\n" +
                "            command += ')';\r\n" +
                "            document.getElementById('preview').textContent = command;\r\n" +
                "        }\r\n" +
                "        \r\n" +
                "        function sendCommand() {\r\n" +
                "            const command = document.getElementById('preview').textContent;\r\n" +
                "            const responseDiv = document.getElementById('response');\r\n" +
                "            \r\n" +
                "            responseDiv.innerHTML = '<p>Loading...</p>';\r\n" +
                "            \r\n" +
                "            fetch('/consulta?comando=' + encodeURIComponent(command))\r\n" +
                "                .then(response => response.json())\r\n" +
                "                .then(data => {\r\n" +
                "                    let html = '';\r\n" +
                "                    \r\n" +
                "                    if (data.className) {\r\n" +
                "                        // Class info result\r\n" +
                "                        html = '<h3>Class: ' + data.className + '</h3>' +\r\n" +
                "                               '<h4>Fields:</h4>' +\r\n" +
                "                               '<pre>' + formatList(data.fieldsList) + '</pre>' +\r\n" +
                "                               '<h4>Methods:</h4>' +\r\n" +
                "                               '<pre>' + formatList(data.methodsList) + '</pre>';\r\n" +
                "                    } else if (data.result !== undefined) {\r\n" +
                "                        // Method result\r\n" +
                "                        html = '<h3>Result:</h3>' +\r\n" +
                "                               '<pre>' + data.result + '</pre>';\r\n" +
                "                    } else if (data.error) {\r\n" +
                "                        // Error\r\n" +
                "                        html = '<h3>Error:</h3>' +\r\n" +
                "                               '<pre>' + data.error + '</pre>';\r\n" +
                "                    } else {\r\n" +
                "                        // Default JSON display\r\n" +
                "                        html = '<pre>' + JSON.stringify(data, null, 2) + '</pre>';\r\n" +
                "                    }\r\n" +
                "                    \r\n" +
                "                    responseDiv.innerHTML = html;\r\n" +
                "                })\r\n" +
                "                .catch(error => {\r\n" +
                "                    responseDiv.innerHTML = '<p>Error: ' + error.message + '</p>';\r\n" +
                "                });\r\n" +
                "        }\r\n" +
                "        \r\n" +
                "        function formatList(listString) {\r\n" +
                "            try {\r\n" +
                "                return listString.replace(/[\\[\\]]/g, '').split(',')\r\n" +
                "                    .map(item => item.trim())\r\n" +
                "                    .filter(item => item)\r\n" +
                "                    .join('\\n');\r\n" +
                "            } catch (e) {\r\n" +
                "                return listString;\r\n" +
                "            }\r\n" +
                "        }\r\n" +
                "        \r\n" +
                "        updateForm();\r\n" +
                "    </script>\r\n" +
                "</body>\r\n" +
                "</html>";
    }

    private static String getErrorPage() {
        return "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                "<!DOCTYPE html>\r\n" +
                "<html>\r\n" +
                "<head>\r\n" +
                "    <meta charset=\"UTF-8\">\r\n" +
                "    <title>Not Found</title>\r\n" +
                "</head>\r\n" +
                "<body>\r\n" +
                "    <h1>404 Not Found</h1>\r\n" +
                "    <p>The requested resource was not found on this server.</p>\r\n" +
                "    <p><a href=\"/cliente\">Go to Reflective ChatGPT</a></p>\r\n" +
                "</body>\r\n" +
                "</html>";
    }
}