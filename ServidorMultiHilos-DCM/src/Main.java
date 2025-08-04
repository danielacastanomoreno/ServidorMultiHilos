// Imports
import java.io.*;
// Socket y ServerSocket son utilizados para protocolo TCP. ¡OJO!
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;


public class Main {

    // Metodo de inicio del hilo (Thread)
    public void init() throws IOException {

        // El serverSocket es el elemento del dispatcher.
        // El socket es un elemento de la request.

        // Cada aplicacion tiene que ejecutarse por un  puerto diferente.
        ServerSocket server = new ServerSocket(8050);

        var isAlive = true;
        while(isAlive) {
            System.out.println("Esperando un cliente... ");
            // El servidor acepta una request http
            var socket = server.accept();
            System.out.println("Conectado!");
            dispatchWorker(socket);
        }

    }

    public void dispatchWorker(Socket socket) {
        // Un proceso diferente es el que sucede cuando a un hilo se le da start
        // y otro diferente es lo que esta dentro del hilo. ¡OJO!
        new Thread(
                () -> {
                    try {
                        handleRequest(socket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).start();
    }

    // Cada vez que llegue una request por medio de un cliente, el HandleRequest le asigna un hilo a cada cliente. ¡OJO!
    public void handleRequest(Socket socket) throws IOException {

        var in = socket.getInputStream();
        var reader = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            // 2. Process: etapa de procesamiento
            if(line.startsWith("GET")) {
                // Tarea: que imprima la extension
                // Solucion: nombre + extension
                var resource = line.split(" ")[1].replace("/", "");
                System.out.println("El cliente esta pidiendo:  " + resource);

                // 3. Enviar la response
                sendResponse(socket, resource);
            }

        }

    }

    // Metodo que maneja las extensiones de los archivos que se estan solicitando (MIME)
    // Los tipos MIME constan de un tipo y un subtipo (por ejemplo, texto/html, imagen/jpeg) que ayudan a los navegadores
    // a procesar y mostrar los archivos correctamente.
    private String  contentType(String nameOfResouce) {

        if(nameOfResouce.endsWith(".htm") || nameOfResouce.endsWith(".html")) {
            return "text/html";
        }

        else if(nameOfResouce.endsWith(".jpg") || nameOfResouce.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        else if(nameOfResouce.endsWith(".gif")) {
            return "image/gif";
        }

        else if(nameOfResouce.endsWith(".png")) {
            return "image/png";
        }
        else {
            return "application/octet-stream";
        }

    }

    // Metodo de respuesta al cliente
    public void sendResponse(Socket socket, String resource) throws IOException {

        // * Construccion de la respuesta HTTP *
        // La respuesta HTTP tiene tres partes: línea de estado, headers y cuerpo.
        // Si el archivo existe, se determina el tipo MIME y se envía el archivo.
        // Si no, se responde con 404 y un HTML de error.

        String statusLine = null;
        String headerLine = null;
        final String CRLF = "\r\n";

        var res = new File("resources/" + resource);

        var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        FileInputStream fis;
        BufferedReader br;

        statusLine = "200 OK";
        headerLine = "Content-type: " + contentType(resource) + CRLF;

        if(res.exists()) {

            if (contentType(resource).equals("text/html")) {


                // Envio linea de estado
                writer.write("HTTP/1.0 " + statusLine + CRLF);
                // Envio header
                writer.write(headerLine);
                // Envio el archivo
                fis = new FileInputStream(res);
                br = new BufferedReader(new InputStreamReader(fis));

                String line;
                var response = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }

                writer.write("Content-Length:" + response.length() + "\r\n");
                writer.write("Connection: close\r\n");
                writer.write("\r\n");
                writer.write(response.toString());
                writer.flush();

                // Cerramos todos los recursos que teniamos abiertos
                writer.close();
                socket.close();

                // Envio linea de estado
                writer.write("HTTP/1.0 " + statusLine + CRLF);
                // Envio header
                writer.write(headerLine);
                // Envio el archivo
                fis = new FileInputStream(res);
                br = new BufferedReader(new InputStreamReader(fis));

                while ((line = br.readLine()) != null) {
                    response.append(line);
                }

                writer.write("Content-Length:" + response.length() + "\r\n");
                writer.write("Connection: close\r\n");
                writer.write("\r\n");
                writer.write(response.toString());
                writer.flush();

                // Cerramos todos los recursos que teniamos abiertos
                writer.close();
                socket.close();

            }


            if (res.exists() && contentType(resource).equals("image/jpeg") || contentType(resource).equals("image/gif") || contentType(resource).equals("image/png")) {

                var responseMetadata = new StringBuilder();
                responseMetadata.append("HTTP/1.1 200 OK" + CRLF);
                responseMetadata.append("Content-Type: " + contentType(resource));

                var fileStream = new FileInputStream(res);

                responseMetadata.append("Content-Length: " + fileStream.available() + CRLF);
                responseMetadata.append(CRLF);

                var ouputStream = socket.getOutputStream();
                ouputStream.write(responseMetadata.toString().getBytes(StandardCharsets.UTF_8));

                try (fileStream) {
                    fileStream.transferTo(ouputStream);
                }

                writer.flush();

                // Cerramos todos los recursos que teniamos abiertos
                writer.close();
                socket.close();

                System.out.println("Envio imagen");
            }

        }

        else {

            // Cuando no encuentra el recurso el servidor responde con un 404 y un html de error.

            var  notFound = new File("resources/" + "404-NotFound.html");
            fis = new FileInputStream(notFound);
            br = new BufferedReader(new InputStreamReader(fis));

            String lineHtml;
            var resposeNotFound = new StringBuilder();
            while ((lineHtml = br.readLine()) != null) {
                resposeNotFound.append(lineHtml);
            }

            writer.write("HTTP/1.0 404 NOT FOUND\r\n");
            writer.write("Content-Type: text/html\r\n");
            writer.write("Content-Length:" + resposeNotFound.length() + "\r\n");
            writer.write("Connection: close\r\n");
            writer.write("\r\n");
            writer.write(resposeNotFound.toString());
            writer.flush();

            // Cerramos todos los recursos que teniamos abiertos
            writer.close();
            socket.close();

            System.out.println("No se encontro el archivo");

        }

    }

    public static void main(String[] args) throws IOException {

        Main main = new Main();
        main.init();

    }

}
