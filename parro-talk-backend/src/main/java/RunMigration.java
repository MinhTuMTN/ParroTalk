import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class RunMigration {
    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection("jdbc:postgresql://100.114.196.98:5432/parrotalk", "admin", "123456");
                 Statement stmt = conn.createStatement()) {
                stmt.execute("create table if not exists vocabulary_sense_topics (\n" +
                             "    sense_id uuid not null references vocabulary_senses(id) on delete cascade,\n" +
                             "    topic_id uuid not null references vocabulary_topics(id) on delete cascade,\n" +
                             "    primary key (sense_id, topic_id)\n" +
                             ");");
                System.out.println("Migration applied successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
