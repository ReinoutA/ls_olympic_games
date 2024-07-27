package be.kuleuven.model;

import org.json.*;
import java.util.*;

public class Task {
    public static int AMOUNT_OF_TASKS = 0;
    public int number;
    public String id;
    public Location location;
    public int demand;
    public int days;
    public String taskTypeId;
    public int numSufficient;

    public Map<String, SkillRequirement> skillRequirements;
    public Set<Volunteer> allowedVolunteers;
    public Set<Volunteer> perfectFitVolunteers;
    public Set<Volunteer> assignedVolunteers;
    public Set<SkillRequirement> hardRequirements;
    public Set<SkillRequirement> softRequirements;
    private final Map<SkillRequirement, List<Volunteer>> volunteersThatFullFillMinimumProficiencyForSkillRequirement;
    private final Map<SkillRequirement, List<Volunteer>> volunteersThatDontFullFillMinimumProficiencyForSkillRequirement;

    public Task(int number, String id, Location location, int demand, int days, String taskTypeId) {
        this.number = number;
        this.id = id;
        this.location = location;
        this.demand = demand;
        this.days = days;
        this.taskTypeId = taskTypeId;
        this.numSufficient = 0;
        this.skillRequirements = new HashMap<>();
        this.allowedVolunteers = new HashSet<>();
        this.assignedVolunteers = new HashSet<>();
        this.hardRequirements = new HashSet<>();
        this.softRequirements = new HashSet<>();
        this.perfectFitVolunteers = new HashSet<>();
        this.volunteersThatFullFillMinimumProficiencyForSkillRequirement = new HashMap<>();
        this.volunteersThatDontFullFillMinimumProficiencyForSkillRequirement = new HashMap<>();
    }

    public Task(int number, JSONObject data, Map<String, Location> locations) {
        this.number = number;
        this.id = data.getString("id");
        this.location = locations.get(data.getString("locationId"));
        this.demand = data.getInt("demand");
        this.days = data.getInt("days");
        this.taskTypeId = data.getString("taskTypeId");
        this.numSufficient = 0;
        this.skillRequirements = new HashMap<>();
        this.allowedVolunteers = new HashSet<>();
        this.assignedVolunteers = new HashSet<>();
        this.hardRequirements = new HashSet<>();
        this.softRequirements = new HashSet<>();
        this.perfectFitVolunteers = new HashSet<>();
        this.volunteersThatFullFillMinimumProficiencyForSkillRequirement = new HashMap<>();
        this.volunteersThatDontFullFillMinimumProficiencyForSkillRequirement = new HashMap<>();
        JSONArray requirements = data.getJSONArray("skillRequirements");
        assert requirements != null;

        for (Object r : requirements) {
            JSONObject rq = (JSONObject) r;
            SkillRequirement requirement = new SkillRequirement(rq);
            skillRequirements.put(requirement.skillId, requirement);

            if (requirement.isHard)
                hardRequirements.add(requirement);
            else
                softRequirements.add(requirement);
        }
    }

    public void allowVolunteer(Volunteer v) {
        allowedVolunteers.add(v);
    }

    public boolean isFeasible() {
        for(SkillRequirement sr : hardRequirements) {
            int numInsufficient = 0;
            for(Volunteer v : assignedVolunteers) {
                if (v.getSkill(sr.skillId) < sr.minProficiency) {
                    numInsufficient++;
                }
            }
            if(numInsufficient > sr.proportion * assignedVolunteers.size()){
                return false;
            }
        }
        return true;
    }

    public Map<SkillRequirement, List<Volunteer>> getVolunteersThatFullFillMinimumProficiencyForSkillRequirement() {
        return volunteersThatFullFillMinimumProficiencyForSkillRequirement;
    }

    public void createVolunteersThatFullFillMinimumProficiencyForSkillRequirement(Set<Volunteer> volunteers) {
        for (SkillRequirement skillRequirement : skillRequirements.values()) {
            List<Volunteer> volunteersThatMeetRequirement = new ArrayList<>();
            List<Volunteer> volunteersThatDontMeetRequirement = new ArrayList<>();
            for (Volunteer v : volunteers) {
                for (String skill : v.skills.keySet()) {
                    if (skill.equals(skillRequirement.skillId)) {
                        if (v.skills.get(skill) >= skillRequirement.minProficiency) {
                            volunteersThatMeetRequirement.add(v);
                        } else {
                            volunteersThatDontMeetRequirement.add(v);
                        }
                    }
                }
            }
            volunteersThatFullFillMinimumProficiencyForSkillRequirement.put(skillRequirement, volunteersThatMeetRequirement);
            volunteersThatDontFullFillMinimumProficiencyForSkillRequirement.put(skillRequirement, volunteersThatDontMeetRequirement);
        }
    }

    public Map<SkillRequirement, List<Volunteer>> getVolunteersThatDontFullFillMinimumProficiencyForSkillRequirement() {
        return volunteersThatDontFullFillMinimumProficiencyForSkillRequirement;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", locationId='" + location + '\'' +
                ", demand=" + demand +
                ", days=" + days +
                ", taskTypeId='" + taskTypeId + '\'' +
                ", skillRequirements={" + skillRequirements +
                "}}";
    }
}
