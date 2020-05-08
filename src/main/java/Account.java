import java.net.*;
import javax.net.ssl.*;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Account {
    private String ip;
    private int port;
    private String user;
    private String password;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    public Account(String ip, int port, String user, String password) {
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    int connect(boolean ssl) {
        try {
            if (ssl) {
                SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = sslsocketfactory.createSocket(ip, port);
            } else {
                socket = new Socket(ip, port);
            }
            socket.setSoTimeout(5000);
            input = new BufferedReader(new InputStreamReader((socket.getInputStream())));
            output = new PrintWriter(socket.getOutputStream(), true);
            if (!isOK(input.readLine())) {
                Util.log.severe(String.format("Connection to %s:%d failed", ip, port));
                System.err.println(String.format("Connection to %s:%d failed", ip, port));
                return -1;
            } else {
                Util.log.info(String.format("Connection to %s:%d successful", ip, port));
                System.out.println(String.format("Connection to %s:%d successful", ip, port));
                return 0;
            }
        } catch (IOException e) {
            Util.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return -1;
    }

    void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            Util.log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    int login() {
        String res = send("USER ".concat(user));
        if (!isOK(res)) {
            System.err.println("Login failed.");
        } else {
            res = send("PASS ".concat(password));
            if (!isOK(res)) {
                Util.log.severe(String.format("Login to %s failed", user));
                System.err.println(String.format("Login to %s failed", user));
                return -1;
            } else {
                Util.log.info(String.format("Login to %s successful", user));
                System.out.println(String.format("Login to %s successful", user));
                return 0;
            }
        }
        return -1;
    }

    String send(String request) {
        output.println(request);
        Util.log.info("REQUEST TO SERVER: " + request);
        String respond = null;
        try {
            respond = input.readLine();
        } catch (IOException e) {
            Util.log.severe(e.getMessage());
        }
        Util.log.info("RESPOND FROM SERVER: " + respond);
        return respond;
    }

    String getUID(int id) {
        return (send("UIDL " + id).split(" "))[2];
    }

    public void addToDB(int id) {
        try {
            ResultSet res = Main.stmt.executeQuery(String.format("SELECT * FROM MAIL WHERE MAIL_UID = '%s' AND USERNAME = '%s'", getUID(id), user));
            if (!res.next()) {
                Main.stmt.executeUpdate(String.format("INSERT INTO MAIL VALUES ('%s', '%s')", getUID(id), user));
            }
        } catch (SQLException e) {
            Util.log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    String saveMessage(int id) {
        String filename = user + " " + getUID(id);
        File file = new File(new File("").getAbsolutePath() + "\\src\\main\\messages\\" + filename);
        if (!file.exists()) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                send("RETR " + id);
                String line;
                while (!(line = input.readLine()).equals(".")) {
                    writer.write(line + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Util.log.log(Level.SEVERE, e.getMessage(), e);
            }
            addToDB(id);
            return filename;
        }
        return "";
    }

    void listNewMessages() {
        int msgCount = Integer.parseInt((send("STAT ").split(" "))[1]);
        String uid = "";
        try {
            for (int i = 1; i <= msgCount; i++) {
                uid = getUID(i);
                ResultSet res = Main.stmt.executeQuery(String.format("SELECT * FROM MAIL WHERE MAIL_UID = '%s' AND USERNAME = '%s'", uid, user));
                if (!res.next()) {
                    Main.headers.add(List.of(getHeader(i), user, String.valueOf(i)));
                }
            }
        } catch (SQLException e) {
            Util.log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    String getHeader(int id) {
        StringBuilder msg = new StringBuilder();
        StringBuilder result = new StringBuilder();
        send("TOP " + id + " 0");
        String line;
        try {
            while (!(line = input.readLine()).equals(".")) {
                msg.append(line).append("\n");
                String[] tokens = line.split(":");
            }
        } catch (IOException e) {
            Util.log.log(Level.SEVERE, e.getMessage(), e);
        }
        String str = msg.toString();
        String pattern = "([A-Za-z-]+):\\s([^\\r\\n]+(?:\\r?\\n(?![A-Za-z-]+:\\s)[^\\r\\n]+)*)";
        Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
        Matcher m = p.matcher(str);
        Set<String> fields = new HashSet<>(Arrays.asList("Date", "From", "Subject"));
        while (m.find()) {
            String field = m.group(1);
            String value = m.group(2);
            if (fields.contains(field)) {
                String pattern1 = "=\\?.+\\?.+\\?(.+)\\?=(.*)";
                Pattern p1 = Pattern.compile(pattern1, Pattern.MULTILINE);
                Matcher m1 = p1.matcher(value);
                StringBuilder decodedString = new StringBuilder();
                while (m1.find()) {
                    String encodedString = m1.group(1);
                    byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
                    decodedString.append(new String(decodedBytes)).append(m1.group(2));
                }
                if (decodedString.length() != 0) value = decodedString.toString();
                result.append(field).append(": ").append(value).append("\n");
            }
        }
        return result.toString();
    }

    boolean isOK(String str) {
        return str.substring(0, 3).equals("+OK");
    }

    public String getUser() {
        return user;
    }
}
