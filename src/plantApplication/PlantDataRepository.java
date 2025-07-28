package plantApplication;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlantDataRepository {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/plantdb?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234";

    public PlantData findLatestByPlantId(int plantId) {
        String sql = "SELECT plant_id, temperature, humidity, created_at FROM plant_data_latest WHERE plant_id = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, plantId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new PlantData(
                        rs.getInt("plant_id"),
                        rs.getDouble("temperature"),
                        rs.getDouble("humidity"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
            }
        } catch (SQLException e) {
            System.err.println("실시간 데이터 조회 실패: " + e.getMessage());
        }
        return null;
    }

    public List<PlantData> findAllLatest() {
        List<PlantData> result = new ArrayList<>();
        String sql = "SELECT device_id, temperature, humidity, timestamp FROM plant_data_latest ORDER BY device_id";
        try (
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                PlantData data = new PlantData(
                        rs.getInt("device_id"),
                        rs.getDouble("temperature"),
                        rs.getDouble("humidity"),
                        rs.getTimestamp("timestamp").toLocalDateTime()
                );
                result.add(data);
            }
        } catch (SQLException e) {
            System.err.println("목록 데이터 조회 실패: " + e.getMessage());
        }
        return result;
    }


}
