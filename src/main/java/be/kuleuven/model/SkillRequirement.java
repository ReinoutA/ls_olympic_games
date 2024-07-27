package be.kuleuven.model;

import org.json.*;

public class SkillRequirement {
    public String skillId;
    public int minProficiency;
    public boolean isHard;
    public double proportion;
    public double weight;
    
    public SkillRequirement(String skillId, int minProficiency, boolean isHard, double proportion, double weight) {
        this.skillId = skillId;
        this.minProficiency = minProficiency;
        this.isHard = isHard;
        this.proportion = proportion;
        this.weight = weight;
    }

    public SkillRequirement(JSONObject data) {
        this.skillId = data.getString("skillId");
        this.minProficiency = data.getInt("minProficiency");
        this.isHard = data.getBoolean("isHard");
        this.proportion = data.getDouble("proportion");
        this.weight = data.getDouble("weight");
    }

    @Override
    public String toString() {
        return "SkillRequirement{" +
                "skillId='" + skillId + '\'' +
                ", minProficiency=" + minProficiency +
                ", isHard=" + isHard +
                ", proportion=" + proportion +
                ", weight=" + weight +
                '}';
    }
}