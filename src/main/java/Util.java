import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Util {
    static Logger log = Logger.getLogger("global");

    public static void configureLog() {
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler("logs.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            log.addHandler(fileHandler);
            log.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
