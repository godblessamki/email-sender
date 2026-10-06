package utb.fai;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class EmailSender {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    /*
     * Constructor opens Socket to host/port. If the Socket throws an exception
     * during opening,
     * the exception is not handled in the constructor.
     */
    public EmailSender(String host, int port) throws UnknownHostException, IOException {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        // Read server greeting (220)
        String greeting = readResponse();
        if (getStatusCode(greeting) / 100 != 2) {
            throw new IOException("SMTP Server rejected connection: " + greeting.trim());
        }

        // Send HELO command to introduce client
        sendCommand("HELO localhost", 2);
    }

    /*
     * Sends email from an email address to an email address with some subject and
     * text.
     * If the Socket throws an exception during sending, the exception is not
     * handled by this method.
     */
    public void send(String from, String to, String subject, String text) throws IOException {
        sendCommand("MAIL FROM:<" + cleanAddress(from) + ">", 2);
        sendCommand("RCPT TO:<" + cleanAddress(to) + ">", 2);
        sendCommand("DATA", 3);

        // Send message headers
        writer.write("From: " + from + "\r\n");
        writer.write("To: " + to + "\r\n");
        writer.write("Subject: " + subject + "\r\n");
        writer.write("MIME-Version: 1.0\r\n");
        writer.write("Content-Type: text/plain; charset=utf-8\r\n");
        writer.write("\r\n");

        // Send message body with dot-stuffing (RFC 5321)
        if (text != null) {
            String[] lines = text.split("\r?\n", -1);
            for (String line : lines) {
                if (line.startsWith(".")) {
                    writer.write(".");
                }
                writer.write(line);
                writer.write("\r\n");
            }
        }

        // End of data marker
        writer.write(".\r\n");
        writer.flush();

        String dataResponse = readResponse();
        if (getStatusCode(dataResponse) / 100 != 2) {
            throw new IOException("Failed to send message content: " + dataResponse.trim());
        }
    }

    /*
     * Sends QUIT and closes the socket
     */
    public void close() {
        try {
            if (writer != null) {
                writer.write("QUIT\r\n");
                writer.flush();
                readResponse();
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception ignored) {}
            try {
                if (writer != null) writer.close();
            } catch (Exception ignored) {}
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
        }
    }

    private void sendCommand(String command, int expectedCodePrefix) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
        String response = readResponse();
        int code = getStatusCode(response);
        if (code / 100 != expectedCodePrefix) {
            throw new IOException("SMTP command failed: " + command + " -> " + response.trim());
        }
    }

    private String readResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\r\n");
            // RFC 5321: multiline reply has 3-digit code followed by '-'
            // Last line has 3-digit code followed by ' ' or end of line
            if (line.length() >= 3 && Character.isDigit(line.charAt(0))
                    && Character.isDigit(line.charAt(1)) && Character.isDigit(line.charAt(2))) {
                if (line.length() == 3 || line.charAt(3) != '-') {
                    break;
                }
            }
        }
        if (line == null) {
            throw new IOException("Connection closed by server unexpectedly.");
        }
        return response.toString();
    }

    private int getStatusCode(String response) {
        if (response != null && response.length() >= 3) {
            try {
                return Integer.parseInt(response.substring(0, 3));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private String cleanAddress(String address) {
        if (address == null) return "";
        int start = address.indexOf('<');
        int end = address.indexOf('>');
        if (start != -1 && end != -1 && end > start) {
            return address.substring(start + 1, end).trim();
        }
        return address.trim();
    }
}
