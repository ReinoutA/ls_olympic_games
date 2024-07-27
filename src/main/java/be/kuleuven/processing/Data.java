package be.kuleuven.processing;

import java.util.*;
import be.kuleuven.model.*;

public class Data {
    public Map<String, Double> weights;
    public Set<String> skills;
    public Map<String, Location> locations;
    public List<Task> tasks;
    public List<Volunteer> volunteers;
    public Set<Volunteer> presourcedVolunteers;
    public Set<Volunteer> nonPresourcedVolunteers;

    public Data(Map<String, Double> weights, Set<String> skills, Map<String, Location> locations,
            List<Task> tasks, List<Volunteer> volunteers, Set<Volunteer> presourcedVolunteers,
            Set<Volunteer> nonPresourcedVolunteers) {
        this.weights = weights;
        this.skills = skills;
        this.locations = locations;
        this.tasks = tasks;
        this.volunteers = volunteers;
        this.presourcedVolunteers = presourcedVolunteers;
        this.nonPresourcedVolunteers = nonPresourcedVolunteers;

        for (Task t : tasks) {
            t.createVolunteersThatFullFillMinimumProficiencyForSkillRequirement(new HashSet<>(t.allowedVolunteers));
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=======================================================================\n");
        sb.append("                                WEIGHTS\n");
        sb.append("=======================================================================\n");
        sb.append(weights + "\n");
        sb.append("=======================================================================\n");
        sb.append("                                 SKILLS\n");
        sb.append("=======================================================================\n");
        sb.append(skills + "\n");
        sb.append("=======================================================================\n");
        sb.append("                               LOCATIONS\n");
        sb.append("=======================================================================\n");
        sb.append(locations + "\n");
        sb.append("=======================================================================\n");
        sb.append("                                 TASKS\n");
        sb.append("=======================================================================\n");
        sb.append(tasks + "\n");
        sb.append("=======================================================================\n");
        sb.append("                               VOLUNTEERS\n");
        sb.append("=======================================================================\n");
        sb.append(volunteers + "\n");
        sb.append("=======================================================================\n");
        sb.append("                         PRESOURCED VOLUNTEERS\n");
        sb.append("=======================================================================\n");
        sb.append(presourcedVolunteers + "\n");
        sb.append("=======================================================================\n");
        sb.append("                       NON PRESOURCED VOLUNTEERS\n");
        sb.append("=======================================================================\n");
        sb.append(nonPresourcedVolunteers + "\n");
        return sb.toString();
    }
}
