package plantApplication;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class PlantData {
    private double temperature;
    private double humidity;
    private LocalDateTime createdAt;

    public PlantData(double temperature, double humidity, LocalDateTime createdAt) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.createdAt = createdAt;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
