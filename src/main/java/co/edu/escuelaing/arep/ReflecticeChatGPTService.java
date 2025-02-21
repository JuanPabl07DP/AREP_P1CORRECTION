package co.edu.escuelaing.arep;

import java.net.*;
import java.io.*;
public class ReflecticeChatGPTService {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(35000);
        }catch (IOException e){
             System.err.println("Error, no se escucha por el puerto 35000");
             System.exit(1);
        }

        Socket clientsocket = null;
        try {
            System.out.println("Listo para recibir por el puerto... " + serverSocket);
            clientsocket = serverSocket.accept();
        }catch (IOException e){
            System.err.println("Error, falló");
            System.exit(1);
        }


    }
}
