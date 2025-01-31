
// clasa reprezentuje pojedynczy odczyt z czujnika
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
