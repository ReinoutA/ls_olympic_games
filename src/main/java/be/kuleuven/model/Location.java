package be.kuleuven.model;

import org.json.*;

public class Location {
    public String id;
    public double lat;
    public double lon;

    public Location(String id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }

    public Location(JSONObject data) {
        this.id = data.getString("id");
        this.lat = data.getDouble("lat");
        this.lon = data.getDouble("lon");
    }

    public int distTo(Location other) {
        double dLon = Math.toRadians(this.lon - other.lon);
        double dLat = Math.toRadians(this.lat - other.lat);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(other.lat)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        int r = 6371;
        
        return (int) Math.ceil(c * r);
    }

    @Override
    public String toString() {
        return "Location{" +
                "id='" + id + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
