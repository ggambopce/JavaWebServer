package plantApplication;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlantDataRepository {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/plantdb?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "your_password";

    public PlantData findLatestByPlantId(int plantId) {
        String sql = "SELECT temperature, humidity, created_at FROM plant_data WHERE plant_id = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, plantId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new PlantData(
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

    public List<Plant> findAllPlants() {
        List<Plant> result = new ArrayList<>();
        String sql = "SELECT id, name FROM plant";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new Plant(
                        rs.getInt("id"),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            System.err.println("식물 목록 조회 실패: " + e.getMessage());
        }
        return result;
    }


}
