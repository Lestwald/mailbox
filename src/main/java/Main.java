import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

public class Main {
    static List<Account> accounts = new ArrayList<>();
    static List<List<String>> headers = new ArrayList<>(new ArrayList<>());
    static Connection conn = null;
    static Statement stmt = null;

    public static void connectDB() {
        String url = "jdbc:derby:" + new File("").getAbsolutePath() + "\\src\\main\\resources\\mail-db;create=true";
        try {
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            DatabaseMetaData dbmd = conn.getMetaData();
            ResultSet res = dbmd.getTables(null, null, "MAIL", null);
            if (!res.next()) {
                stmt.executeUpdate("CREATE TABLE MAIL (MAIL_UID VARCHAR(70), USERNAME VARCHAR(255))");
            }
        } catch (SQLException e) {
            Util.log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void addAccount(String ip, int port, String user, String password) {
        Account account = new Account(ip, port, user, password);
        int res;
        if (port == 110) res = account.connect(false);
        else res = account.connect(true);
        if (res == 0) {
            res = account.login();
            if (res == 0) accounts.add(account);
        }
    }

    public static void listNewMessages() {
        headers.clear();
        for (Account account : accounts) {
            account.listNewMessages();
        }
        if (headers.isEmpty()) System.out.println("There are no new messages");
        else for (int i = 0; i < headers.size(); i++) {
            System.out.printf("%d. %s\n%s\n", i, headers.get(i).get(1), headers.get(i).get(0));
        }
    }

    public static void saveMessage(int msgIndex) {
        for (Account account : accounts) {
            if (account.getUser().equals(headers.get(msgIndex).get(1))) {
                String res = account.saveMessage(Integer.parseInt(headers.get(msgIndex).get(2)));
                if (!res.isEmpty()) {
                    Util.log.info(String.format("Message %s saved as '%s'", msgIndex, res));
                    System.out.println(String.format("Message %s saved as '%s'", msgIndex, res));
                }
                return;
            }
        }
    }

    public static void saveAll() {
        if (headers.isEmpty()) System.out.println("There are no messages to get");
        else for (Account account : accounts) {
            for (int i = 0; i < headers.size(); i++) {
                List<String> header = headers.get(i);
                if (account.getUser().equals(header.get(1))) {
                    String res = account.saveMessage(Integer.parseInt(header.get(2)));
                    if (!res.isEmpty()) {
                        Util.log.info(String.format("Message %s saved as '%s'", i, res));
                        System.out.println(String.format("Message %s saved as '%s'", i, res));
                    }
                }
            }
        }
    }

    public static void consoleIO() {
        Scanner scanner = new Scanner(System.in);
        String line;
        while (true) {
            line = scanner.nextLine();
            String[] tokens = line.split(" ");
            switch (tokens[0]) {
                case "add":
                    String ip = tokens[1];
                    int port = Integer.parseInt(tokens[2]);
                    String user = tokens[3];
                    String password = tokens[4];
                    addAccount(ip, port, user, password);
                    break;
                case "list":
                    listNewMessages();
                    break;
                case "get":
                    if (tokens[1].equals("all")) {
                        saveAll();
                    } else for (int i = 1; i < tokens.length; i++) {
                        try{
                            int msgNumber = Integer.parseInt(tokens[i]);
                            if (msgNumber >= headers.size()) {
                                System.err.println("Message doesn't exist");
                            } else saveMessage(msgNumber);
                        } catch(NumberFormatException e){
                            Util.log.log(Level.SEVERE, e.getMessage(), e);
                            System.err.println("Invalid argument type");
                        }
                    }
                    break;
                case "":
                    break;
                case "quit":
                    for (Account account : accounts) {
                        account.closeConnection();
                    }
                    return;
                default:
                    System.err.println("Invalid command");
                    break;
            }
        }
    }

    public static void main(String[] args) {
        Util.configureLog();
        connectDB();
        consoleIO();
    }
}
