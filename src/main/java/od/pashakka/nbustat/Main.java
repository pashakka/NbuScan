package od.pashakka.nbustat;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Properties properties = readProperties();
        NbuSiteParser nbuSiteParser = new NbuSiteParser(properties);
        try {
            nbuSiteParser.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Properties readProperties() {
        Properties properties = new Properties();
        try {
            properties.load(new FileReader("NbuScan.properties"));
        } catch (IOException e) {
            throw new IllegalStateException("err_load_properties", e);
        }
        return properties;
    }
}
