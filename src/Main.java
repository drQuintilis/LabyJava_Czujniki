import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Uruchamiamy aplikację w wątku obsługującym interfejs graficzny
        SwingUtilities.invokeLater(() -> {
            SensorMonitoringSystemSwing frame = new SensorMonitoringSystemSwing();
            frame.setVisible(true);
        });
    }
}
