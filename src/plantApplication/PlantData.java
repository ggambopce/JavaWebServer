package plantApplication;

import java.time.LocalDateTime;

public class PlantData {
    private int deviceId;
    private double temperature;
    private double humidity;
    private LocalDateTime createdAt;

    public PlantData(int deviceId, double temperature, double humidity, LocalDateTime createdAt) {
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.createdAt = createdAt;
    }

    public int getDeviceId() {
        return deviceId;
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

    @Override
    public String toString() {
        return "PlantData{" +
                "deviceId=" + deviceId +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", createdAt=" + createdAt +
                '}';
    }
}
