package be.kuleuven.localsearch;

import be.kuleuven.model.*;
import gurobi.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import org.json.*;

public class Main {

    private int NUM_LS_RUNS_1 = 1;
    private int MAX_LS_COMPLEXITY_1 = 3;
    private int K_1 = 13000;
    private double POW_1 = 2.5;

    private int NUM_LS_RUNS_2 = 10000;
    private int MAX_LS_COMPLEXITY_2 = 1;
    private int K_2 = 18000;
    private double POW_2 = 1.2;

    private static final boolean DEBUG_LOGS = true;
    private final boolean OUTPUTLOG = true;
    private boolean IGNORE_OBJECTIVE_2 = false;

    private int CALCULATION_TIME_MINUTES = 1000;
    public String INPUT_FILE_PATH;
    public String OUTPUT_FILE_PATH;
    public int SEED;
    private int AMOUNT_OF_THREADS = 2;

    public final LocalSearch localSearch;
    public String inputType;

    public Main(String INPUT_FILE_PATH){
        //localSearch = new LocalSearch("/p0_200t_5000v.json");
        //localSearch = new LocalSearch("/i0_200t_5000v.json");
        //localSearch = new LocalSearch("/i1_600t_40000v.json");

        //localSearch = new LocalSearch("/p2_781t_40000v.json");
        //localSearch = new LocalSearch("/i2_781t_40000v.json");

        //localSearch = new LocalSearch("/i3_781t_100000v.json");

        //localSearch = new LocalSearch("/p4_781t_140765v.json");
        //localSearch = new LocalSearch("/i4_781t_140765v.json");

        this.INPUT_FILE_PATH = INPUT_FILE_PATH;
        localSearch = new LocalSearch(INPUT_FILE_PATH);
        calculateParametersLNS_objective1();
    }

    public void init() throws GRBException {
        long startTime = System.currentTimeMillis();
        solve();
        long endTime = System.currentTimeMillis();
        long timeDifference = endTime - startTime;
        System.out.println("Calculation Finished... (" + timeDifference/1000 + " sec)");
    }

    public void calculateParametersLNS_objective1() {
        int complexity = localSearch.volunteers.size() * localSearch.tasks.size();
        // i0 en p0 (incl. wat marge)
        if(complexity <= 6000 * 200) {
            inputType = "T1";
            System.out.println("Chose i0/p0 like");
            this.NUM_LS_RUNS_1 = 1;
            this.MAX_LS_COMPLEXITY_1 = 1;
            this.K_1 = 15000;
            this.POW_1 = 1;
        }

        //  i1 & i2 & p2
        if(complexity > 200 * 6000 && complexity <= 781 * 41000) {
            inputType = "T2";
            System.out.println("Chose i1/i2/p2 like");
            this.NUM_LS_RUNS_1 = 2;
            this.MAX_LS_COMPLEXITY_1 = 1;
            this.K_1 = 17000;
            this.POW_1 = 1;
        }

        // i3
        // Server versie
        if(complexity > 781 * 41000 && complexity < 781 * 101000) {
            inputType = "T3";
            System.out.println("Chose i3 like");
            this.NUM_LS_RUNS_1 = 2;
            this.MAX_LS_COMPLEXITY_1 = 2;
            this.K_1 = 28000;
            this.POW_1 = 1.3;
        }

        // i4 en p4
        if(complexity > 781 * 101000) {
            inputType = "T4";
            System.out.println("Chose i4/p4 like");
            this.NUM_LS_RUNS_1 = 1;
            this.MAX_LS_COMPLEXITY_1 = 2;
            this.K_1 = 13000;
            this.POW_1 = 3.5;
        }
    }

    public Map<Volunteer, Task> solve() throws GRBException {
        long startTime = System.currentTimeMillis();
        int bestObjectiveValue;
        Map<Volunteer, Task> bestVolunteerToTaskAssignment;
            Map<Task, Integer> taskToNumAssignmentsUB = new HashMap<>();
            Map<Volunteer, Task> feasibleVolunteerToTaskAssignment = findFeasibleSolution(taskToNumAssignmentsUB, localSearch.initialVolunteerCount, localSearch.initialTaskCount);
            if(DEBUG_LOGS) {
                System.out.println("Initial Solution Size: " + feasibleVolunteerToTaskAssignment.size());
            }
            Map<Task, Integer> taskToSlack = new HashMap<>();
            for (Task t : localSearch.tasks) {
                int maxTaskCapacity = t.demand;
                int numEligibleVolunteers = t.allowedVolunteers.size();
                int slack = numEligibleVolunteers - maxTaskCapacity;
                taskToSlack.put(t, slack);
            }

            List<Task> tasksSortedBySlack = taskToSlack.entrySet().stream().sorted(Comparator.comparingInt(entry -> entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<Task> sortedTasks = new ArrayList<>(tasksSortedBySlack);
            List<Integer> objectiveValueHistory = new ArrayList<>();
            // Zet initiele oplossing als beste oplossing
            bestObjectiveValue = feasibleVolunteerToTaskAssignment.size();
            bestVolunteerToTaskAssignment = new HashMap<>(feasibleVolunteerToTaskAssignment);

            // Objective 1
            for (int complexity = 1; complexity <= MAX_LS_COMPLEXITY_1; complexity++) {
                if(DEBUG_LOGS) {
                    System.out.println("COMPLEXITY CHANGED TO: " + complexity + " AT " + (System.currentTimeMillis() - startTime) / 1000 + " SEC");
                }
                for (int run = 0; run < NUM_LS_RUNS_1; run++) {
                    if(complexity == 1 && (inputType.equals("T3") || inputType.equals("T4"))) {
                        run = NUM_LS_RUNS_1;
                    }
                    if(DEBUG_LOGS) {
                        System.out.println("COMPLEXITY " + complexity + " LS RUN " + run + " AT " + (System.currentTimeMillis() - startTime) / 1000 + " SEC");
                    }
                    Map<Volunteer, Task> volunteerToTaskAssignment = new HashMap<>(bestVolunteerToTaskAssignment);
                    Set<Task> batch = new HashSet<>();
                    Set<Volunteer> availableBatchVolunteers = new HashSet<>();
                    int iteration = 0;
                    for (Task t : sortedTasks) {
                        Map<Volunteer, Task> finalVolunteerToTaskAssignment = volunteerToTaskAssignment;
                        Set<Volunteer> availableTaskVolunteers = t.allowedVolunteers.stream().filter(j -> !finalVolunteerToTaskAssignment.containsKey(j) || finalVolunteerToTaskAssignment.get(j).equals(t)).collect(Collectors.toSet());

                        if (availableTaskVolunteers.isEmpty()) {
                            continue;
                        }

                        batch.add(t);
                        availableBatchVolunteers.addAll(availableTaskVolunteers);
                        if (availableBatchVolunteers.size() >= Math.pow(complexity, POW_1) * K_1 || t == sortedTasks.get(localSearch.tasks.size() - 1)) {
                            iteration++;
                            //if (DEBUG_LOGS) {
                                System.out.println("------------------------------\n" + "Iteration #" + iteration + " (" + batch.size() + " tasks, " + availableBatchVolunteers.size() + " volunteers)");
                            //}
                            volunteerToTaskAssignment = solveBatch(volunteerToTaskAssignment, batch, availableBatchVolunteers, false, taskToNumAssignmentsUB, true, 0, localSearch.initialVolunteerCount, localSearch.initialTaskCount, false, -1);
                            if (System.currentTimeMillis() - startTime >= CALCULATION_TIME_MINUTES * 60 * 1000) {
                                System.out.println("Time limit reached. Exiting the loop.");
                                writeToJson(volunteerToTaskAssignment, OUTPUT_FILE_PATH, 0);
                                return null;
                            }
                            batch.clear();
                            availableBatchVolunteers.clear();
                        }
                    }
                    int numActualAssignments = volunteerToTaskAssignment.size();
                    if(DEBUG_LOGS) {
                        System.out.println("\n===========================\n" + numActualAssignments);
                    }
                    objectiveValueHistory.add(numActualAssignments);
                    //if(DEBUG_LOGS) {
                        System.out.println("ObjectiveValue: " + numActualAssignments);
                    //}
                    if (numActualAssignments > bestObjectiveValue) {
                        bestObjectiveValue = numActualAssignments;
                        bestVolunteerToTaskAssignment = volunteerToTaskAssignment;
                    }
                    if (System.currentTimeMillis() - startTime >= CALCULATION_TIME_MINUTES * 60 * 1000) {
                        System.out.println("Time limit reached. Exiting the loop.");
                        writeToJson(bestVolunteerToTaskAssignment, OUTPUT_FILE_PATH, 0);
                        return null;
                    }

                    Collections.shuffle(sortedTasks);
                }
            }

            writeToJson(bestVolunteerToTaskAssignment, OUTPUT_FILE_PATH, 0);

            int numAssignmentsObjective1 = bestVolunteerToTaskAssignment.size();

            if(!IGNORE_OBJECTIVE_2) {
                if(DEBUG_LOGS) {
                    System.out.println("STARTING OBJECTIVE 2 LS AT " + (System.currentTimeMillis() - startTime) / 1000 + " SEC");
                }
                writeToJson(bestVolunteerToTaskAssignment, "result_obj1.json", 0);
                // Objective 2
                for (int complexity = 1; complexity <= MAX_LS_COMPLEXITY_2; complexity++) {
                    if(DEBUG_LOGS) {
                        System.out.println("COMPLEXITY CHANGED TO: " + complexity);
                    }
                    for (int run = 0; run < NUM_LS_RUNS_2; run++) {
                        if(DEBUG_LOGS) {
                            System.out.println("COMPLEXITY " + complexity + " LS RUN " + run + " AT " + (System.currentTimeMillis() - startTime) / 1000 + " SEC");
                        }
                        Map<Volunteer, Task> volunteerToTaskAssignment = new HashMap<>(bestVolunteerToTaskAssignment);
                        Set<Task> batch = new HashSet<>();
                        Set<Volunteer> availableBatchVolunteers = new HashSet<>();
                        int iteration = 0;
                        for (Task t : sortedTasks) {
                            Map<Volunteer, Task> finalVolunteerToTaskAssignment = volunteerToTaskAssignment;
                            Set<Volunteer> availableTaskVolunteers = t.allowedVolunteers.stream().filter(j -> !finalVolunteerToTaskAssignment.containsKey(j) || finalVolunteerToTaskAssignment.get(j).equals(t)).collect(Collectors.toSet());

                            if (availableTaskVolunteers.isEmpty()) {
                                continue;
                            }

                            batch.add(t);
                            availableBatchVolunteers.addAll(availableTaskVolunteers);
                            if (availableBatchVolunteers.size() >= Math.pow(complexity, POW_2) * K_2 || t == sortedTasks.get(localSearch.tasks.size() - 1)) {
                                iteration++;
                                if (DEBUG_LOGS) {
                                    System.out.println("------------------------------\n" + "Iteration #" + iteration + " (" + batch.size() + " tasks, " + availableBatchVolunteers.size() + " volunteers)");
                                }
                                volunteerToTaskAssignment = solveBatch(volunteerToTaskAssignment, batch, availableBatchVolunteers, false, taskToNumAssignmentsUB, true, 0, localSearch.initialVolunteerCount, localSearch.initialTaskCount, true, numAssignmentsObjective1);
                                if (System.currentTimeMillis() - startTime >= CALCULATION_TIME_MINUTES * 60 * 1000) {
                                    System.out.println("Time limit reached. Exiting the loop.");
                                    writeToJson(volunteerToTaskAssignment, OUTPUT_FILE_PATH, 0);
                                    return null;
                                }
                                batch.clear();
                                availableBatchVolunteers.clear();
                            }
                        }

                        bestVolunteerToTaskAssignment = volunteerToTaskAssignment;
                        if (System.currentTimeMillis() - startTime >= CALCULATION_TIME_MINUTES * 60 * 1000) {
                            System.out.println("Time limit reached. Exiting the loop.");
                            writeToJson(bestVolunteerToTaskAssignment, OUTPUT_FILE_PATH, 0);
                            return null;
                        }
                        Collections.shuffle(sortedTasks);
                    }
                }
            }
        // Both Objectives done
        System.out.println("Best Objective: " + bestObjectiveValue);
        if(!IGNORE_OBJECTIVE_2) {
            writeToJson(bestVolunteerToTaskAssignment, OUTPUT_FILE_PATH, 0);
        }
        return bestVolunteerToTaskAssignment;
    }

    public Map<Volunteer, Task> solveBatch(Map<Volunteer, Task> volunteerToTaskAssignment, Set<Task> batch, Set<Volunteer> availableBatchVolunteers, boolean isRelaxed, Map<Task, Integer> taskToNumAssignmentsUB, boolean warmStart, int timeLimit,  int initialVolunteerCount, int initialTaskCount, boolean isObjective2, int numAssignmentsObjective1) throws GRBException {
        int objective1Batch = 0;
        for(Volunteer v : availableBatchVolunteers) {
            if(volunteerToTaskAssignment.containsKey(v) && volunteerToTaskAssignment.get(v) != null) {
                objective1Batch++;
            }
        }

        Map<Volunteer, Task> newVolunteerToTaskAssignment = new HashMap<>(volunteerToTaskAssignment);

        GRBEnv env = new GRBEnv();
        env.set(GRB.IntParam.Threads, AMOUNT_OF_THREADS);
        if(OUTPUTLOG) {
            env.set(GRB.IntParam.OutputFlag, 1);
            env.set(GRB.IntParam.LogToConsole, 1);
        }else{
            env.set(GRB.IntParam.OutputFlag, 0);
            env.set(GRB.IntParam.LogToConsole, 0);
        }

        env.start();
        GRBModel model = new GRBModel(env);
        if(DEBUG_LOGS) {
            System.out.println("Objective 1: " + newVolunteerToTaskAssignment.size());
        }
        // DECISION VARIABLES
        GRBVar[][] x_vt = new GRBVar[initialVolunteerCount][initialTaskCount];
        for (Volunteer v : availableBatchVolunteers) {
            for (Task t : v.allowedTasks) {
                if (batch.contains(t) && newVolunteerToTaskAssignment.containsKey(v) && warmStart) {
                    if(volunteerToTaskAssignment.get(v) == t) {
                        x_vt[v.number][t.number] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + v.number + "_" + t.number);
                        x_vt[v.number][t.number].set(GRB.DoubleAttr.VarHintVal, 1.0);
                    }else{
                        x_vt[v.number][t.number] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + v.number + "_" + t.number);
                        x_vt[v.number][t.number].set(GRB.DoubleAttr.VarHintVal, 0.0);
                    }
                    newVolunteerToTaskAssignment.remove(v);
                }else if(batch.contains(t) && availableBatchVolunteers.contains(v)){
                    x_vt[v.number][t.number] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + v.number + "_" + t.number);
                }
            }
        }

        GRBVar y = model.addVar(0, availableBatchVolunteers.size(), 0.0, GRB.INTEGER, "y");

        // Constraint 1
        for (Volunteer v : availableBatchVolunteers) {
            GRBLinExpr assignmentConstraint = new GRBLinExpr();
            for (Task t : v.allowedTasks) {
                if(batch.contains(t)) {
                    assignmentConstraint.addTerm(1.0, x_vt[v.number][t.number]);
                }
            }
            model.addConstr(assignmentConstraint, GRB.LESS_EQUAL, 1.0, "AssignmentConstraint_" + v.number);
        }

        // Constraint 2
        if(!isRelaxed) {
            for (Volunteer v : availableBatchVolunteers) {
                GRBLinExpr constraint = new GRBLinExpr();
                for (Task t : v.allowedTasks) {
                    if(batch.contains(t)) {
                        if (localSearch.presourcers.contains(v)) {
                            constraint.addTerm(1.0, x_vt[v.number][t.number]);
                        }
                    }
                }
                if (constraint.size() > 0) {
                    model.addConstr(constraint, GRB.EQUAL, 1.0, "Constraint2_" + v.number);
                }
            }
        }

        // Constraint 3
        for (Task t : batch) {
            if(taskToNumAssignmentsUB == null){
                GRBLinExpr constraint = new GRBLinExpr();
                for (Volunteer v : availableBatchVolunteers) {
                    if(t.allowedVolunteers.contains(v)) {
                        constraint.addTerm(1.0, x_vt[v.number][t.number]);
                    }
                }
                model.addConstr(constraint, GRB.LESS_EQUAL, t.demand, "Constraint3_" + t.number);
            }else{
                GRBLinExpr constraint = new GRBLinExpr();
                for (Volunteer v : availableBatchVolunteers) {
                    if(t.allowedVolunteers.contains(v)) {
                        constraint.addTerm(1.0, x_vt[v.number][t.number]);
                    }
                }
                model.addConstr(constraint, GRB.LESS_EQUAL, t.demand, "Constraint3_" + t.number);
            }
        }

        // Constraint 4
        for (Task t : batch) {
            Map<SkillRequirement, List<Volunteer>> minProficiencyVolunteers = t.getVolunteersThatFullFillMinimumProficiencyForSkillRequirement();

            for (SkillRequirement sr : t.hardRequirements) {
                GRBLinExpr exprL = new GRBLinExpr();
                GRBLinExpr exprR = new GRBLinExpr();
                double fraction = sr.proportion;
                List<Volunteer> srVolunteers = minProficiencyVolunteers.get(sr);

                for (Volunteer v : t.allowedVolunteers) {
                    if (availableBatchVolunteers.contains(v)) {
                        if (srVolunteers != null && srVolunteers.contains(v)) {
                            exprL.addTerm(1.0, x_vt[v.number][t.number]);
                        }
                        exprR.addTerm(fraction, x_vt[v.number][t.number]);
                    }
                }
                model.addConstr(exprL, GRB.GREATER_EQUAL, exprR, "Constraint3");
            }
        }
        Map<Task, Map<Boolean, Integer>> taskToNumsGenderAssignments = getTaskToNumsGenderAssignments(newVolunteerToTaskAssignment);

        if(!isRelaxed) {
            int numFemaleAssignments = 0;
            for (Task t : taskToNumsGenderAssignments.keySet()) {
                if(taskToNumsGenderAssignments.get(t).get(false) == null){

                }else{
                    numFemaleAssignments += taskToNumsGenderAssignments.get(t).get(false);
                }
            }
            GRBLinExpr femaleArcLiterals = new GRBLinExpr();
            for (Volunteer v : availableBatchVolunteers) {
                for (Task t : v.allowedTasks) {
                    if (batch.contains(t)) {
                        if (!v.isMale) {
                            femaleArcLiterals.addTerm(100.0, x_vt[v.number][t.number]);
                        }
                    }
                }
            }
            femaleArcLiterals.addConstant(100*numFemaleAssignments);
            GRBLinExpr totalNumAssignmentsHigh = new GRBLinExpr();
            GRBLinExpr totalNumAssignmentsLow = new GRBLinExpr();
            for (Volunteer v : availableBatchVolunteers) {
                for (Task t : v.allowedTasks) {
                    if (batch.contains(t)) {
                        totalNumAssignmentsHigh.addTerm(55.0, x_vt[v.number][t.number]);
                        totalNumAssignmentsLow.addTerm(45.0, x_vt[v.number][t.number]);
                    }
                }
            }
            totalNumAssignmentsHigh.addConstant(55 * newVolunteerToTaskAssignment.size());
            totalNumAssignmentsLow.addConstant(45 * newVolunteerToTaskAssignment.size());

            model.addConstr(totalNumAssignmentsLow, GRB.LESS_EQUAL, femaleArcLiterals, "CONSTRAINT6");
            model.addConstr(femaleArcLiterals, GRB.LESS_EQUAL, totalNumAssignmentsHigh, "CONSTRAINT7");
        }

        // Constr zodat objective 1 niet slechter wordt
        if(isObjective2) {
            GRBLinExpr constr = new GRBLinExpr();
            for(Volunteer v : availableBatchVolunteers) {
                for(Task t : batch) {
                    if(v.allowedTasks.contains(t)) {
                        constr.addTerm(1.0, x_vt[v.number][t.number]);
                    }
                }
            }
            model.addConstr(constr, GRB.GREATER_EQUAL,  objective1Batch, "NEW");
            model.update();
        }

        // Objectives
        if(!isObjective2) {
            GRBLinExpr objectiveFunction = new GRBLinExpr();
            for (Volunteer v : availableBatchVolunteers) {
                for (Task t : v.allowedTasks) {
                    if (v.allowedTasks.contains(t) && batch.contains(t)) {
                        objectiveFunction.addTerm(1.0, x_vt[v.number][t.number]);
                    }
                }
            }
            model.setObjectiveN(objectiveFunction, 0, 1, -1, 1e-6, 0, "Objective1");
        }else {
            GRBLinExpr objectiveFunction = new GRBLinExpr();
            GRBLinExpr expr1 = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();
            GRBLinExpr expr3 = new GRBLinExpr();

            // Expr 1
            for(Volunteer v : availableBatchVolunteers) {
                for(Task t : batch) {
                    if(t.allowedVolunteers.contains(v)) {
                        int f_vt = v.location.distTo(t.location);
                        int q_vnt = v.taskTypes.get(t.taskTypeId);
                        expr1.addTerm(localSearch.data.weights.get("travelDistanceWeight") * f_vt * 2 * t.days, x_vt[v.number][t.number]);
                        expr1.addTerm((-1) * localSearch.data.weights.get("taskTypeAdequacyWeight") * q_vnt, x_vt[v.number][t.number]);
                    }
                }
            }

            // Expr 2
            for(Task t : batch) {
                for(Volunteer v : availableBatchVolunteers) {
                    if(t.allowedVolunteers.contains(v)) {
                        for(SkillRequirement sr : t.softRequirements) {
                            Map<SkillRequirement, List<Volunteer>> m = t.getVolunteersThatDontFullFillMinimumProficiencyForSkillRequirement();
                            List<Volunteer> volunteerList = m.get(sr);
                            if(volunteerList.contains(v)) {
                                expr2.addTerm(sr.weight, x_vt[v.number][t.number]);
                            }
                        }
                    }
                }
            }

            // Expr 3
            expr3.addTerm(localSearch.data.weights.get("genderBalanceWeight"), y);
            objectiveFunction.add(expr1);
            objectiveFunction.add(expr2);
            objectiveFunction.add(expr3);
            model.setObjectiveN(objectiveFunction, 0, 1, 1, 1e-6, 0, "Objective2");
        }

        model.update();
        model.set(GRB.IntParam.LPWarmStart, 2);
        model.optimize();

        if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
            System.out.println("Model is infeasible");
            model.dispose();
            env.dispose();
            return new HashMap<>(volunteerToTaskAssignment);
        } else {
            if(DEBUG_LOGS) {
                System.out.println("Model is feasible.");
            }
            int objective1Actually = 0;
            for (Volunteer v : availableBatchVolunteers) {
                for (Task t : v.allowedTasks) {
                    if(batch.contains(t)) {
                        double val = x_vt[v.number][t.number].get(GRB.DoubleAttr.X);
                        if (val > 0.5) {
                            newVolunteerToTaskAssignment.put(v, t);
                            objective1Actually++;
                        }
                    }
                }
            }

            if(isObjective2) {
                //if(DEBUG_LOGS) {
                    System.out.println("Objective 1: " + newVolunteerToTaskAssignment.size() + ", Objective 2: (batch) " + model.getObjective(0).getValue());
                //}
                if(objective1Batch != objective1Actually) {
                    System.out.println("ERROR OBJECTIVE 2");
                }
            }
            model.dispose();
            env.dispose();
            return newVolunteerToTaskAssignment;
        }
    }

    public Map<Volunteer, Task> findFeasibleSolution(Map<Task, Integer> taskToNumAssignmentsUB, int initialVolunteerCount, int initialTaskCount) throws GRBException {
        Map<Volunteer, Task> feasibleSolution = new HashMap<>();
        int numPresourcers = localSearch.presourcers.size();
        Set<Volunteer> volunteerSubset = new HashSet<>(localSearch.presourcers);
        List<Volunteer> volunteers = localSearch.volunteers;
        List<Task> tasks = localSearch.tasks;

        for(int i = 0; i < 0.5 * numPresourcers; i++){
            if(!volunteers.get(i).isPresourced) {
                volunteerSubset.add(volunteers.get(i));
            }
        }

        GRBEnv env = new GRBEnv();
        env.set(GRB.IntParam.Threads, AMOUNT_OF_THREADS);
        if(OUTPUTLOG) {
            env.set(GRB.IntParam.OutputFlag, 1);
            env.set(GRB.IntParam.LogToConsole, 1);
        }else{
            env.set(GRB.IntParam.OutputFlag, 0);
            env.set(GRB.IntParam.LogToConsole, 0);
        }
        env.start();
        GRBModel model = new GRBModel(env);
        model.set(GRB.DoubleParam.MIPGap, 10.0);
        model.set(GRB.IntParam.MIPFocus, 1);
        model.set(GRB.IntParam.SolutionLimit, 1);
        model.set(GRB.IntParam.NodeMethod, 0);

        GRBVar[][] x_vt = new GRBVar[initialVolunteerCount][initialTaskCount];
        for (Volunteer v : volunteerSubset) {
            for (Task t : v.allowedTasks) {
                x_vt[v.number][t.number] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + v.number + "_" + t.number);
            }
        }
        GRBVar y = model.addVar(0, volunteerSubset.size(), 0.0, GRB.INTEGER, "y");

        // CONSTRAINTS

        // Constraint 1
        for (Volunteer v : volunteerSubset) {
            GRBLinExpr assignmentConstraint = new GRBLinExpr();
            for (Task t : v.allowedTasks) {
                assignmentConstraint.addTerm(1.0, x_vt[v.number][t.number]);
            }
            model.addConstr(assignmentConstraint, GRB.LESS_EQUAL, 1.0, "AssignmentConstraint_" + v.number);
        }
        // Constraint 2
        for (Volunteer v : volunteerSubset) {
            GRBLinExpr constraint = new GRBLinExpr();
            for (Task t : v.allowedTasks) {
                if (localSearch.presourcers.contains(v)) {
                    constraint.addTerm(1.0, x_vt[v.number][t.number]);
                }
            }
            if (constraint.size() > 0) {
                model.addConstr(constraint, GRB.EQUAL, 1.0, "Constraint2_" + v.number);
            }
        }
        // Constraint 3
        for (Task t : tasks) {
            if(taskToNumAssignmentsUB == null){
                GRBLinExpr constraint = new GRBLinExpr();
                for (Volunteer v : volunteerSubset) {
                    if(t.allowedVolunteers.contains(v)) {
                        constraint.addTerm(1.0, x_vt[v.number][t.number]);
                    }
                }
                model.addConstr(constraint, GRB.LESS_EQUAL, t.demand, "Constraint3_" + t.number);
            }else{
                GRBLinExpr constraint = new GRBLinExpr();
                for (Volunteer v : volunteerSubset) {
                    if(t.allowedVolunteers.contains(v)) {
                        constraint.addTerm(1.0, x_vt[v.number][t.number]);
                    }
                }
                model.addConstr(constraint, GRB.LESS_EQUAL, t.demand, "Constraint3_" + t.number);
            }
        }
        // Constraint 4
        for (Task t : tasks) {
            for (SkillRequirement sr : t.hardRequirements) {
                GRBLinExpr exprL = new GRBLinExpr();
                GRBLinExpr exprR = new GRBLinExpr();
                double fraction = sr.proportion;
                for (Volunteer v : volunteerSubset) {
                    if (t.allowedVolunteers.contains(v)){
                        if (t.getVolunteersThatFullFillMinimumProficiencyForSkillRequirement().containsKey(sr)) {
                            List<Volunteer> vol = t.getVolunteersThatFullFillMinimumProficiencyForSkillRequirement().get(sr);
                            if (vol.contains(v)) {
                                exprL.addTerm(1.0, x_vt[v.number][t.number]);
                            }
                            if (t.allowedVolunteers.contains(v)) {
                                exprR.addTerm(fraction, x_vt[v.number][t.number]);
                            }
                        }
                    }
                }
                model.addConstr(exprL, GRB.GREATER_EQUAL, exprR, "Constraint4");
            }
        }
        // Constraint 5 and 6
        GRBLinExpr exprDiffer = new GRBLinExpr();
        GRBLinExpr ExprMale = new GRBLinExpr();
        GRBLinExpr exprHigh = new GRBLinExpr();
        GRBLinExpr exprLow = new GRBLinExpr();

        for (Volunteer v : volunteerSubset) {
            for (Task t : v.allowedTasks) {
                if (v.isMale) {
                    ExprMale.addTerm(1.0, x_vt[v.number][t.number]);
                    exprDiffer.addTerm(-1.0, x_vt[v.number][t.number]);
                }
                else {
                    exprDiffer.addTerm(1.0, x_vt[v.number][t.number]);
                }
                exprLow.addTerm(0.45, x_vt[v.number][t.number]);
                exprHigh.addTerm(0.55, x_vt[v.number][t.number]);
            }
        }
        model.addConstr(exprDiffer, GRB.LESS_EQUAL, 0.0, "ABS_DIFFERENCE_CONSTRAINT");
        model.addConstr(ExprMale, GRB.LESS_EQUAL, exprHigh, "CONSTRAINT6");
        model.addConstr(ExprMale, GRB.GREATER_EQUAL, exprLow, "CONSTRAINT7");

        model.update();
        model.optimize();
        if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
            System.out.println("Initial Solution is infeasible");
        } else {
            System.out.println("Initial Solution is feasible.");
            for (Volunteer v : volunteerSubset) {
                for (Task t : v.allowedTasks) {
                    double val = x_vt[v.number][t.number].get(GRB.DoubleAttr.X);
                    if (val > 0.5) {
                        feasibleSolution.put(v,t);
                    }

                }
            }
        }
        writeToJson(feasibleSolution, "initial_solution.json", 0);
        model.dispose();
        env.dispose();
        return feasibleSolution;
    }

    public void writeToJson(Map<Volunteer, Task> best, String fileName, double bestObjectiveValue) {
        JSONObject resultJSON = new JSONObject();
        resultJSON.put("assignedVolunteers", best.size());
        resultJSON.put("assignmentCost", bestObjectiveValue);

        JSONArray assignmentsArray = new JSONArray();

        for (Volunteer v : best.keySet()) {
            JSONObject assignmentJSON = new JSONObject();
            assignmentJSON.put("volunteerId", v.id);
            assignmentJSON.put("taskId", best.get(v).id);
            assignmentsArray.put(assignmentJSON);
        }
        resultJSON.put("assignments", assignmentsArray);
        String resultJSONString = resultJSON.toString();
        try {
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(resultJSONString);
            fileWriter.close();
            System.out.println("Result JSON is opgeslagen in " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Task, Map<Boolean, Integer>> getTaskToNumsGenderAssignments(Map<Volunteer, Task> volunteerToTaskAssignment) {
        Map<Task, Map<Boolean, Integer>> taskToNumsGenderAssignments = new HashMap<>();
        for (Volunteer v : volunteerToTaskAssignment.keySet()) {
            Task task = volunteerToTaskAssignment.get(v);
            taskToNumsGenderAssignments.putIfAbsent(task, new HashMap<>());
            boolean gender = v.isMale;
            taskToNumsGenderAssignments.get(task).merge(gender, 1, Integer::sum);
        }
        return taskToNumsGenderAssignments;
    }

    public static void main(String[] args) throws GRBException {
        System.out.println("Started Execution. V2.21");


        if (args.length != 5) {
            System.out.println("Gebruik: java -Xmx16g -jar algo.jar <pad naar invoerbestand> <pad naar oplossingsbestand> <seed> <tijdslimiet> <maximaal aantal gebruikte threads>");
            System.exit(1);
        } else{
            System.out.println("Aantal argumenten meegegeven: " + args.length);
        }

        Main main = new Main(args[0]);
        main.OUTPUT_FILE_PATH = args[1];
        main.SEED = Integer.parseInt(args[2]);
        main.CALCULATION_TIME_MINUTES = Integer.parseInt(args[3]);
        main.AMOUNT_OF_THREADS = Integer.parseInt(args[4]);
        if(DEBUG_LOGS) {
            System.out.println("INPUT_FILE_PATH: " + main.INPUT_FILE_PATH);
            System.out.println("OUTPUT_FILE_PATH: " + main.OUTPUT_FILE_PATH);
            System.out.println("SEED: " + main.SEED);
            System.out.println("CALCULATION_TIME_MINUTES: " + main.CALCULATION_TIME_MINUTES);
            System.out.println("AMOUNT_OF_THREADS: " + main.AMOUNT_OF_THREADS);
        }
        main.init();
    }

}
