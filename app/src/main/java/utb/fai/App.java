package utb.fai;

public class App {

    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("Chyba: Nedostatecny pocet argumentu. Ocekavano: <host> <port> <from> <to> <subject> <text>");
            return;
        }

        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Chyba: Neplatne cislo portu: " + args[1]);
            return;
        }

        String from = args[2];
        String to = args[3];
        String subject = args[4];
        String text = args[5];

        EmailSender sender = null;
        try {
            sender = new EmailSender(host, port);
            sender.send(from, to, subject, text);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sender != null) {
                sender.close();
            }
        }
    }
}
