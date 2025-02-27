package co.edu.escuelaing.arep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;

/**
 * @author Juan Pablo Daza Pereira
 */
public class ReflectiveChatGPT {

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(45000);
                System.out.println("Started on port 45000");
            } catch (IOException e) {
                System.err.println("Could not listen on port:" + e.getMessage());
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
                    if (requestURI.getPath().equals("/compreflex") && requestURI.getQuery() != null) {
                        String comando = requestURI.getQuery().split("=")[1];
                        JsonObject response = processCommand(comando);
                        outputLine = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "\r\n" +
                                response.toString();
                    } else {
                        outputLine = "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Type: text/html\r\n" +
                                "\r\n" +
                                "<!DOCTYPE html><html><body><h1>404 Not Found</h1></body></html>";
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

    private static JsonObject processCommand(String comando) {
        try {
            comando = java.net.URLDecoder.decode(comando, "UTF-8");

            String commandName = comando.split("\\(")[0].trim();
            String paramsString = comando.substring(comando.indexOf("(") + 1, comando.lastIndexOf(")")).trim();
            String[] params = paramsString.split(",");

            switch (commandName) {
                case "class":
                    return handleClassCommand(params);
                case "invoke":
                    return handleInvokeCommand(params);
                case "unaryInvoke":
                    return handleUnaryInvokeCommand(params);
                case "binaryInvoke":
                    return handleBinaryInvokeCommand(params);
                default:
                    JsonObject errorResponse = new JsonObject();
                    errorResponse.addProperty("error", "Unknown command: " + commandName);
                    return errorResponse;
            }
        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return errorResponse;
        }
    }

    private static JsonObject handleClassCommand(String[] params) throws ClassNotFoundException {
        if (params.length < 1) {
            throw new IllegalArgumentException("Class name is required");
        }
        String className = params[0].trim();
        Class<?> clazz = Class.forName(className);
        return getDeclaredFieldsAndMethods(clazz);
    }

    private static JsonObject handleInvokeCommand(String[] params) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (params.length < 2) {
            throw new IllegalArgumentException("Class name and method name are required");
        }
        String className = params[0].trim();
        String methodName = params[1].trim();

        Class<?> clazz = Class.forName(className);
        Method method = clazz.getMethod(methodName);

        return invokeMethod(method, null);
    }

    private static JsonObject handleUnaryInvokeCommand(String[] params) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (params.length < 4) {
            throw new IllegalArgumentException("Class name, method name, parameter type, and parameter value are required");
        }

        String className = params[0].trim();
        String methodName = params[1].trim();
        String paramType = params[2].trim();
        String paramValue = params[3].trim();

        Class<?> clazz = Class.forName(className);
        Class<?> paramClass = getParamClass(paramType);
        Method method = clazz.getMethod(methodName, paramClass);

        Object[] args = new Object[] { convertParam(paramType, paramValue) };
        return invokeMethod(method, args);
    }

    private static JsonObject handleBinaryInvokeCommand(String[] params) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (params.length < 6) {
            throw new IllegalArgumentException("Class name, method name, parameter types, and parameter values are required");
        }

        String className = params[0].trim();
        String methodName = params[1].trim();
        String paramType1 = params[2].trim();
        String paramValue1 = params[3].trim();
        String paramType2 = params[4].trim();
        String paramValue2 = params[5].trim();

        Class<?> clazz = Class.forName(className);
        Class<?> paramClass1 = getParamClass(paramType1);
        Class<?> paramClass2 = getParamClass(paramType2);

        Method method = clazz.getMethod(methodName, paramClass1, paramClass2);

        Object[] args = new Object[] {
                convertParam(paramType1, paramValue1),
                convertParam(paramType2, paramValue2)
        };

        return invokeMethod(method, args);
    }

    private static Class<?> getParamClass(String paramType) {
        switch (paramType.toLowerCase()) {
            case "int":
                return int.class;
            case "double":
                return double.class;
            case "string":
                return String.class;
            default:
                throw new IllegalArgumentException("Unsupported parameter type: " + paramType);
        }
    }

    private static Object convertParam(String paramType, String paramValue) {
        switch (paramType.toLowerCase()) {
            case "int":
                return Integer.parseInt(paramValue);
            case "double":
                return Double.parseDouble(paramValue);
            case "string":
                return paramValue;
            default:
                throw new IllegalArgumentException("Unsupported parameter type: " + paramType);
        }
    }

    private static JsonObject invokeMethod(Method method, Object[] args) throws IllegalAccessException, InvocationTargetException {
        JsonObject response = new JsonObject();
        Object result = method.invoke(null, args);
        response.addProperty("result", result != null ? result.toString() : "null");
        return response;
    }

    private static JsonObject getDeclaredFieldsAndMethods(Class<?> clazz) {
        JsonObject response = new JsonObject();
        Method[] methods = clazz.getDeclaredMethods();
        Field[] fields = clazz.getDeclaredFields();
        List<String> methodsList = new ArrayList<>();
        List<String> fieldsList = new ArrayList<>();

        for (Method method : methods) {
            methodsList.add(method.getName());
        }
        for (Field field : fields) {
            fieldsList.add(field.getName());
        }

        response.addProperty("className", clazz.getName());
        response.addProperty("methodsList", methodsList.toString());
        response.addProperty("fieldsList", fieldsList.toString());
        return response;
    }
}
