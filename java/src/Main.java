import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    static String USER = "mao";
    static String PASSWORD = "qwe123";

    static String DATA_TABLE = "DATA";
    static String DB_URL = "jdbc:postgresql://localhost/database";

    public static void main(String[] args) {
        String filename = "config.cfg";
        Properties properties = new Properties();

        try {
            properties.load(Files.newInputStream(Paths.get(filename)));
        } catch (IOException e) {
            File file = new File(filename);
            try {
                if (!file.createNewFile()) {
                    System.out.println("Не удалось создать файл конфигурации.");
                } else {
                    try (FileWriter writer = new FileWriter(filename, false)) {
                        writer.write("period = 1000");
                    }
                }
            } catch (IOException ex) {
                System.out.println("Не удалось создать файл конфигурации.");
            }
        }

        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + DATA_TABLE
                + "(CPU BIGINT NOT NULL, MEMORY_TOTAL BIGINT NOT NULL, MEMORY_USED BIGINT NOT NULL, DISK_USAGE BIGINT NOT NULL);";

        try (Connection connection = connectDataBase(); Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        int period = Integer.parseInt(properties.getProperty("period", "1000"));
        new Timer().schedule(

                new TimerTask() {
                    Connection connection = connectDataBase();

                    @Override
                    public void run() {
                        int CPU = receiveCPU();
                        int DiskUsage = receiveDisk();
                        HashMap<String, Integer> MEMORY = receiveMemory();

                        System.out.println("CPU- " + CPU + "%");
                        System.out.println(" Memory- " + MEMORY.get("used") + "/" + MEMORY.get("total"));
                        System.out.println("  DiskUsage- " + DiskUsage + "%");
                        System.out.println("------------------------------------");

                        String insertValueSQL = "INSERT INTO " + DATA_TABLE
                                + "(CPU, MEMORY_TOTAL, MEMORY_USED, DISK_USAGE)  VALUES (" + CPU + ","
                                + MEMORY.get("total") + "," + MEMORY.get("used") + "," + DiskUsage + ")";

                        try (Statement statement = connection.createStatement()) {
                            statement.execute(insertValueSQL);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                },

                0, period);
    }

    public static Connection connectDataBase() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver is not found. Include it in your library path ");
            e.printStackTrace();
        }

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Connection Failed");
        }

        return connection;
    }

    public static int receiveCPU() {
        ProcessBuilder builder = new ProcessBuilder("vmstat", "1", "2", "-w").redirectErrorStream(true);
        Process p;

        try {
            p = builder.start();
        } catch (IOException e) {
            return -1;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String str;
        String res = null;

        while (true) {
            try {
                str = r.readLine();
            } catch (IOException e) {
                return -1;
            }
            if (str == null) {
                break;
            }

            res = str;
        }

        assert res != null;
        String[] per = res.replaceAll("\\s+", " ").split(" ");

        return Integer.parseInt(per[13]);
    }

    public static HashMap<String, Integer> receiveMemory() {
        ProcessBuilder builder = new ProcessBuilder("free").redirectErrorStream(true);
        Process p;
        try {
            p = builder.start();
        } catch (IOException e) {
            return new HashMap<>();
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String str;
        String[] res = new String[3];
        int i = 0;

        while (true) {
            try {
                str = r.readLine();
            } catch (IOException e) {
                return new HashMap<>();
            }
            if (str == null) {
                break;
            }

            res[i] = str;
            i++;
        }

        String[] per = res[1].replaceAll("\\s+", " ").split(" ");

        return new HashMap<String, Integer>() {
            {
                put("total", Integer.parseInt(per[1]));
                put("used", Integer.parseInt(per[2]));
            }
        };
    }

    public static int receiveDisk() {
        ProcessBuilder builder = new ProcessBuilder("df", "-h", "--total").redirectErrorStream(true);
        Process p;
        try {
            p = builder.start();
        } catch (IOException e) {
            return -1;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String str;
        String res = null;

        while (true) {
            try {
                str = r.readLine();
            } catch (IOException e) {
                return -1;
            }
            if (str == null) {
                break;
            }

            res = str;
        }

        assert res != null;
        String[] per = res.replaceAll("\\s+", " ").split(" ");

        return Integer.parseInt(per[4].replaceAll("\\D", ""));
    }

}
