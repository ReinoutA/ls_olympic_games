package be.kuleuven.localsearch;

import java.util.*;

import be.kuleuven.model.*;
import be.kuleuven.processing.*;
import gurobi.GRBException;

public class LocalSearch {
    public List<Volunteer> volunteers;
    public List<Task> tasks;
    public Map<Volunteer, Boolean> assignedVolunteers;
    public Map<String, Location> locations;
    public Map<String, Double> weights;
    public Set<Volunteer> presourcers;
    public Set<Volunteer> nonPresourcers;
    public int initialVolunteerCount;
    public int initialTaskCount;
    public Data data;

    public LocalSearch(String path) {
        this.data = new FileProcessor(path).process();
        this.weights = data.weights;
        this.volunteers = data.volunteers;
        this.tasks = data.tasks;
        this.assignedVolunteers = new HashMap<>();
        this.locations = data.locations;
        this.presourcers = data.presourcedVolunteers;
        this.nonPresourcers = data.nonPresourcedVolunteers;
        initialTaskCount = tasks.size();
        initialVolunteerCount = volunteers.size();
    }

}