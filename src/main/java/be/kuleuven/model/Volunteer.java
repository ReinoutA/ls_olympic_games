package be.kuleuven.model;

import java.util.*;
import org.json.*;

public class Volunteer {
    public static int AMOUNT_OF_VOLUNTEERS = 0;
    public String id;
    public boolean isMale;
    public boolean isPresourced;
    public Location location;
    public int availableDays;
    public int number;
    public Map<String, Location> preferredLocations;
    public Map<String, Integer> skills;
    public Map<String, Integer> taskTypes;

    public Set<Task> allowedTasks;
    public Set<Task> perfectFitTasks;
    public Volunteer(int number, String id, boolean isMale, boolean isPresourced, String locationId, int availableDays,
            Map<String, Location> locations) {
        this.number =  number;
        this.id = id;
        this.isMale = isMale;
        this.isPresourced = isPresourced;
        this.location = locations.get(locationId);
        this.availableDays = availableDays;
        this.preferredLocations = new HashMap<>();
        this.skills = new HashMap<>();
        this.taskTypes = new HashMap<>();
        this.allowedTasks = new HashSet<>();
        this.perfectFitTasks = new HashSet<>();
    }

    public Volunteer(int number, JSONObject data, Map<String, Location> locations) {
        this.number = number;
        this.id = data.getString("id");
        this.isMale = data.getBoolean("isMale");
        this.isPresourced = data.getBoolean("isPresourced");
        this.location = locations.get(data.getString("locationId"));
        this.availableDays = data.getInt("availableDays");
        this.preferredLocations = new HashMap<>();
        this.skills = new HashMap<>();
        this.taskTypes = new HashMap<>();

        this.allowedTasks = new HashSet<>();
        this.perfectFitTasks = new HashSet<>();
        JSONArray array = data.getJSONArray("preferredLocationIds");
        for (int i = 0; i < array.length(); i++) {
            String s = array.getString(i);
            preferredLocations.put(s, locations.get(s));
            if(locations.get(s) == null) {
                System.out.println("ERROR: LOCATION IS NULL");
            }
        }

        JSONObject types = data.getJSONObject("taskTypes");
        //for (String type : taskTypesGlobal) {
        for (String type : types.keySet()) {
            Integer i = types.getInt(type);
            taskTypes.put(type, i);
        }
    }

    public void addTask(Task t) {
        allowedTasks.add(t);
    }

    public int getSkill(String skillId) {
        return skills.getOrDefault(skillId, -1);

    }

    @Override
    public String toString() {
        return "Volunteer{" +
                "id=" + id +
                ", isMale=" + isMale +
                ", isPresourced=" + isPresourced +
                ", location=" + location +
                ", availableDays=" + availableDays +
                ", preferedLocations{" + preferredLocations + "}" +
                ", skills{" + skills + "}" +
                ", taskTypes{" + taskTypes + "}" +
                "}";
    }
}