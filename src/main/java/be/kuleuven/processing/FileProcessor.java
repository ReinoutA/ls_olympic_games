package be.kuleuven.processing;

import org.json.*;

import java.util.*;

import java.io.*;

import be.kuleuven.model.*;

public class FileProcessor {
    private JSONObject data;

    public FileProcessor(String path) {
        File file = new File(path);

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            StringBuilder jsonContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            data = new JSONObject(jsonContent.toString());
            System.out.println("Data ingelezen");

        } catch (IOException e) {
            System.out.println("Inputfile not found");
            e.printStackTrace();
        }
    }

    public Data process() {
        Map<String, Double> weights = new HashMap<>();
        String[] weightNames = new String[] { "travelDistanceWeight", "genderBalanceWeight", "taskTypeAdequacyWeight" };

        for (String weightName : weightNames) {
            weights.put(weightName, data.getJSONObject("weights").getDouble(weightName));
        }

        Set<String> skills = new HashSet<>();
        JSONArray array = data.getJSONArray("skills");
        for (int i = 0; i < array.length(); i++) {
            skills.add(array.getString(i));
        }

        Set<String> taskTypes = new HashSet<>();
        array = data.getJSONArray("taskTypes");
        for (int i = 0; i < array.length(); i++) {
            taskTypes.add(array.getString(i));
        }

        Map<String, Location> locations = new HashMap<>();
        array = data.getJSONArray("locations");
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            Location l = new Location(o);
            locations.put(l.id, l);
        }

        List<Task> tasks = new ArrayList<>();
        array = data.getJSONArray("tasks");
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            Task t = new Task(Task.AMOUNT_OF_TASKS, o, locations);
            Task.AMOUNT_OF_TASKS++;
            tasks.add(t);
        }

        List<Volunteer> volunteers = new ArrayList<>();
        array = data.getJSONArray("volunteers");
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);

            Volunteer v = new Volunteer(Volunteer.AMOUNT_OF_VOLUNTEERS, o, locations);
            Volunteer.AMOUNT_OF_VOLUNTEERS++;
            JSONObject skillsObject = o.getJSONObject("skills");
            Iterator<String> keys = skillsObject.keys();

            while(keys.hasNext()) {
                String key = keys.next();
                v.skills.put(key, skillsObject.getInt(key));
            }
            
            volunteers.add(v);
        }

        Set<Volunteer> presourcedVolunteers = new HashSet<>();
        Set<Volunteer> nonPresourcedVolunteers = new HashSet<>();
        for (Volunteer v : volunteers) {
            if (v.isPresourced)
                presourcedVolunteers.add(v);
            else
                nonPresourcedVolunteers.add(v);

            for (Task t : tasks) {
                if (v.preferredLocations.containsKey(t.location.id) && v.availableDays >= t.days && v.taskTypes.get(t.taskTypeId) != 0) {
                    boolean geschikt = true;
                    // :S
                    for(SkillRequirement sr: t.hardRequirements) {
                        if(v.getSkill(sr.skillId) < sr.minProficiency && (sr.proportion == 1.0 || sr.proportion > (1 - (double) 1 / t.demand))){
                            geschikt = false;
                        }
                    }

                    if(geschikt){
                        v.addTask(t);
                        t.allowVolunteer(v);
                    }
                }
            }

        }

        return new Data(weights, skills, locations, tasks, volunteers, presourcedVolunteers, nonPresourcedVolunteers);
    }
}
