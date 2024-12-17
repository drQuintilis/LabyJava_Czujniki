import javax.swing.*;
import java.awt.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SensorMonitoringSystemSwing extends JFrame {

    // deklaracje wspoldzielnych buforów, ktore służą do przechowywyania odczytów z czujników
    private final BlockingQueue<SensorData> temperatureBuffer = new LinkedBlockingQueue<>();
    private final BlockingQueue<SensorData> humidityBuffer = new LinkedBlockingQueue<>();
    private final BlockingQueue<SensorData> pressureBuffer = new LinkedBlockingQueue<>();
    /* Blokuje wątek próbujący pobrać element, jeśli kolejka jest pusta.
       Blokuje wątek próbujący dodać element, jeśli kolejka jest pełna */

    /* AtomicBoolean gwarantuje, że zmiana wartości (true lub false)
       jest wykonywana w sposób atomowy (jednolity i nieprzerwany).
       Wątki są początkowo zatrzymane (false) */
    private final AtomicBoolean running = new AtomicBoolean(false);

    // pola do wyświetlania wielu wierszy tekstu (odczytu naszych czujników)
    private final JTextArea temperatureArea = new JTextArea();
    private final JTextArea humidityArea = new JTextArea();
    private final JTextArea pressureArea = new JTextArea();

    // pola do wyświetlania ostatniego odczytu z czujników
    private final JLabel temperatureLastValue = new JLabel("Ostatni pomiar: --");
    private final JLabel humidityLastValue = new JLabel("Ostatni pomiar: --");
    private final JLabel pressureLastValue = new JLabel("Ostatni pomiar: --");

    // liczniki do numerowania odczytów
    private int temperatureCounter = 1;
    private int humidityCounter = 1;
    private int pressureCounter = 1;

    public SensorMonitoringSystemSwing() {
        setTitle("System Monitorowania Czujników");
        // Zamykanie okna powoduje zakończenie działania programu
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Ustawia odstępy wewnętrzne wokół komponentów
        gbc.insets = new Insets(10, 10, 10, 10);

        // Panel temperatury
        JPanel temperaturePanel = createSquareSensorPanel("Temperatura (°C)",
                temperatureArea, temperatureLastValue, 300);
        // komponent zostanie umieszczony w pierwszej kolumnie i pierwszym wierszu.
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(temperaturePanel, gbc);

        // Panel wilgotności
        JPanel humidityPanel = createSquareSensorPanel("Wilgotność (%)",
                humidityArea, humidityLastValue, 300);
        gbc.gridx = 1;
        mainPanel.add(humidityPanel, gbc);

        // Panel ciśnienia
        JPanel pressurePanel = createSquareSensorPanel("Ciśnienie (hPa)",
                pressureArea, pressureLastValue, 300);
        gbc.gridx = 2;
        mainPanel.add(pressurePanel, gbc);

        // Panel przycisków
        JPanel controlPanel = new JPanel();
        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        JButton resetButton = new JButton("Reset");

        // Wywoływanie działań po kliknięciu przycisków
        startButton.addActionListener(e -> startMonitoring());
        stopButton.addActionListener(e -> stopMonitoring());
        resetButton.addActionListener(e -> resetMonitoring());

        // Dodawanie przycisków do panelu
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(resetButton);

        // Dodawanie paneli do okna
        add(mainPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Ustawia rozmiar okna na podstawie rozmiarów komponentów
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createSquareSensorPanel(String title, JTextArea textArea,
                                           JLabel lastValueLabel, int size) {
        JPanel panel = new JPanel(new BorderLayout()) {
            // zwraca preferowany rozmiar panelu
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(size, size);
            }
        };
        // Ustawia ramkę z tytułem do panelu
        panel.setBorder(BorderFactory.createTitledBorder(title));

        // Wyłącza możliwość edycji tekstu przez użytkownika
        textArea.setEditable(false);

        // dodawanie do każego polu tekstowego paska przewijania
        JScrollPane scrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        // umieszczamy etykietę na dole panelu
        panel.add(lastValueLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void startMonitoring() {
        // Jeśli running ma wartość true, oznacza to, że monitorowanie już działa.
        if (running.get()) return;
        // Startowanie wątków
        running.set(true);

        // Tworzymy nowe wątki, które symulują odczyty z czujników i przyjmują burofy do przechowywania wyników
        new Thread(() -> simulateSensor(temperatureBuffer, -40, 40, 1000, temperatureArea, temperatureLastValue, "temperature")).start();
        new Thread(() -> simulateSensor(humidityBuffer, 0, 100, 1500, humidityArea, humidityLastValue, "humidity")).start();
        new Thread(() -> simulateSensor(pressureBuffer, 900, 1100, 2000, pressureArea, pressureLastValue, "pressure")).start();
    }

    //Po kliknięciu przycisku Stop, zmienna running jest ustawiana na false, żeby wątki się zatrzymały
    private void stopMonitoring() {
        running.set(false);
    }

    // funkcja wyczyszczająca wszystkie buforu i liczniki oraz pola tekstu
    private void resetMonitoring() {
        stopMonitoring();
        temperatureBuffer.clear();
        humidityBuffer.clear();
        pressureBuffer.clear();
        temperatureCounter = 1;
        humidityCounter = 1;
        pressureCounter = 1;

        SwingUtilities.invokeLater(() -> {
            temperatureArea.setText("");
            humidityArea.setText("");
            pressureArea.setText("");
            temperatureLastValue.setText("Ostatni pomiar: --");
            humidityLastValue.setText("Ostatni pomiar: --");
            pressureLastValue.setText("Ostatni pomiar: --");
        });
    }

    private void simulateSensor(BlockingQueue<SensorData> buffer, int min, int max, int interval,
                                JTextArea textArea, JLabel lastValueLabel, String sensorType) {
        // Każdy wątek wykonuje pracę w pętli dopóki running jest true
        while (running.get()) {
            try {
                // liczniki do numerowania odczytów
                int counter = switch (sensorType) {
                    case "temperature" -> temperatureCounter++;
                    case "humidity" -> humidityCounter++;
                    default -> pressureCounter++;
                };

                double value = min + Math.random() * (max - min);
                // Dodajemy nowy odczyt do kolejki bufora
                buffer.put(new SensorData(counter, value));

                // umieszczamy updateTextArea w kolejce zdarzeń wątku obsługującego interfejs graficzny
                SwingUtilities.invokeLater(() -> updateTextArea(textArea, buffer, lastValueLabel, value));
                // czekamy przez określony czas (1, 1,5 lub 2 sekundy)
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void updateTextArea(JTextArea textArea, BlockingQueue<SensorData> buffer,
                                JLabel lastValueLabel, double value) {
        JScrollPane scrollPane = (JScrollPane) textArea.getParent().getParent();
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();

        // Sprawdzamy, czy pasek przewijania znajduje się na dole
        boolean atBottom = verticalBar.getValue() + verticalBar.getVisibleAmount() >= verticalBar.getMaximum();

        // Dynamiczne budowanie tekstu
        StringBuilder sb = new StringBuilder();
        // Wyświetlamy wszystkie odczyty z bufora w interfejsie
        for (SensorData data : buffer) {
            sb.append(String.format("%d) %.2f\n", data.getNumber(), data.getValue()));
        }

        // Pobieramy aktualny stan paska przewijania
        int currentScroll = verticalBar.getValue();

        // wpisujemy tekst z ostatniego odczytu do etykiety
        textArea.setText(sb.toString());
        lastValueLabel.setText(String.format("Ostatni pomiar: %.2f", value));

        SwingUtilities.invokeLater(() -> {
            if (atBottom) {
                // Jeśli pasek przewijania jest na dole, przewijamy go znowu na sam dół
                verticalBar.setValue(verticalBar.getMaximum());
            } else {
                // W przeciwnym razie ustawiamy pasek przewijania na poprzednią wartość
                verticalBar.setValue(currentScroll);
            }
        });
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SensorMonitoringSystemSwing frame = new SensorMonitoringSystemSwing();
            frame.setVisible(true);
        });
    }
}

class SensorData {
    private final int number;
    private final double value;

    public SensorData(int number, double value) {
        this.number = number;
        this.value = value;
    }

    public int getNumber() {
        return number;
    }

    public double getValue() {
        return value;
    }
}
