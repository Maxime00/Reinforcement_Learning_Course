package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import template.DeliberativeTemplate.State;

import java.util.List;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.HashSet;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	private static final int NOACTION = 0;
	private static final boolean PICKUP = true;
	private static final boolean DELIVER = false;
	private static final char REMAINING = 'r';
	private static final char PICKEDUP = 'p';
	private static final char DELIVERED = 'd';
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	public int capacity;
	public int costPerKm;
	TaskSet transfercarriedTasks;   //NOUVEAU

	/* the planning class */
	Algorithm algorithm;
	
	//Define State class
	public class State{
		private State previousState;
		private City currentCity;
		private String taskStatus;
		private int weight;
		private boolean action;
		private Task task;
		private double cost;
		
		public State(State prev, City current, String status, int w, boolean a, Task t, double c) {
			previousState = prev;
			currentCity = current;
			taskStatus = new String(status);
			weight = w;
			action = a;
			task = t;
			cost = c;
		}
		
	}
	
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = AStarPlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = BFSPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
	
	private Plan BFSPlan (Vehicle vehicle, TaskSet tasks) {
		
		long startTime = System.currentTimeMillis();
		
		Plan bestPlan = new Plan(vehicle.getCurrentCity());
		double minimumFinalCost = 999999;
		State goalState = null;
		
		if (transfercarriedTasks != null) {                     //NOUVEAU
			tasks.addAll(transfercarriedTasks);                  //NOUVEAU
		}
		
		int numTasks = tasks.size();
		Task[] allTasks = tasks.toArray(new Task[tasks.size()]);
		
				
		State initialState = new State(null, vehicle.getCurrentCity(), "", 0, PICKUP, null, 0);
		for(int i = 0; i < allTasks.length; i++) {
			if(transfercarriedTasks != null && transfercarriedTasks.contains(allTasks[i]))                 //NOUVEAU
				initialState.taskStatus += PICKEDUP;                                       //NOUVEAU
			else                                                                          //NOUVEAU
				initialState.taskStatus += REMAINING;                                      //NOUVEAU
		}
			
		
		HashSet<State> searchStateSet = new HashSet<State>();
		
		int iteration = 0;
		
		LinkedList<State> Qtable = new LinkedList<State>();
		
		costPerKm = vehicle.costPerKm();
		capacity = agent.vehicles().get(0).capacity();
		
		Qtable.add(initialState);
		
		do {
	
			if (Qtable.isEmpty()) {
				break;
			}
			
			iteration++;
			
			State tempState = Qtable.pop();
			String tempTaskStatus = tempState.taskStatus;
			
			if(tempTaskStatus.replace("d", "").length() == 0) {
				
				double tempCost = tempState.cost;
				
				if(tempCost < minimumFinalCost) {
					minimumFinalCost = tempCost;
					goalState = tempState;
				}
				continue;
			}
			
			if (tempState == null)
				break;
			else {
				if (!searchStateSet.contains(tempState)) {
					
					City currentC = tempState.currentCity;
					double currentCost = tempState.cost;
					int currentWeight = tempState.weight;
					searchStateSet.add(tempState);

					for(int i=0; i < numTasks; i++) {
									
						if(tempTaskStatus.charAt(i) == REMAINING) {
							Task task = allTasks[i];
							int updatedWeight = task.weight + currentWeight;
							
							if (updatedWeight <= capacity) {
								City currentCity = task.pickupCity;
								double cost = currentCost + currentC.distanceTo(currentCity)*costPerKm;
								char[] taskStat = tempTaskStatus.toCharArray();
								taskStat[i] = PICKEDUP;
								
								State stateToAdd = new State(tempState, currentCity, new String(taskStat), updatedWeight, PICKUP, task, cost);
								Qtable.add(stateToAdd);
							}
						}
						
						else if(tempTaskStatus.charAt(i) == PICKEDUP) {
							Task task = allTasks[i];
							City currentCity = task.deliveryCity;
							double cost = tempState.cost + currentC.distanceTo(currentCity)*costPerKm;
							char[] taskStat = tempTaskStatus.toCharArray();
							taskStat[i] = DELIVERED;
							int weight = tempState.weight - task.weight;
							
							State stateToAdd = new State(tempState, currentCity, new String(taskStat), weight, DELIVER, task, cost);
							Qtable.add(stateToAdd);
						}
					}
				}
			}
			
		}while(true);
		
		retrievePlan(goalState, bestPlan);
		long endTime = System.currentTimeMillis();
		
		System.out.println("plan = "+bestPlan+"");
		System.out.println("cost = "+minimumFinalCost+"");
		System.out.println("iterations = "+iteration+"");
		System.out.println("Execution time: " + (endTime - startTime) + "");
		
		return bestPlan;
	}
	
	private Plan AStarPlan(Vehicle vehicle, TaskSet tasks) {

		long startTime = System.currentTimeMillis();
		
		Plan bestPlan = new Plan(vehicle.getCurrentCity());
		double minimumFinalCost = 999999;
		State goalState = null;
		
		costPerKm = vehicle.costPerKm();
		capacity = agent.vehicles().get(0).capacity();
		
		if (transfercarriedTasks != null) {                     //NOUVEAU
			tasks.addAll(transfercarriedTasks);                  //NOUVEAU
		}
		
		int numTasks = tasks.size();
		Task[] allTasks = tasks.toArray(new Task[tasks.size()]);
		
		
		
		State initialState = new State(null, vehicle.getCurrentCity(), "", 0, PICKUP, null, 0);
		for(int i = 0; i < allTasks.length; i++) {
			if(transfercarriedTasks != null && transfercarriedTasks.contains(allTasks[i]))                 //NOUVEAU
				initialState.taskStatus += PICKEDUP;                                       //NOUVEAU
			else                                                                          //NOUVEAU
				initialState.taskStatus += REMAINING;                                      //NOUVEAU
		}
		
		
		HashSet<State> searchStateSet = new HashSet<State>();
		StateComparator stateComparator = new StateComparator(allTasks);


		PriorityQueue<State> Qtable = new PriorityQueue<State>(100000, stateComparator);
		
		Qtable.add(initialState);
		int iteration = 0;
		
		do {
			
			if (Qtable.isEmpty()) {
				break;
			}
			
			iteration++;
			
			State tempState = Qtable.poll();
			
			String tempTaskStatus = tempState.taskStatus;

			if(tempTaskStatus.replace("d", "").length() == 0) {
				double tempCost = tempState.cost;
				
				if(tempCost < minimumFinalCost) {
					minimumFinalCost = tempCost;
					goalState = tempState;
				}
				continue;
				
				
				/*
				//THIS BREAK MIGHT CHANGE 
				minimumFinalCost = tempState.cost;
				goalState = tempState;
				break;
				*/
			}
			
			if (tempState == null)
				break;
			
			else {
				if (!searchStateSet.contains(tempState)) {
					
					City currentC = tempState.currentCity;
					double currentCost = tempState.cost;
					int currentWeight = tempState.weight;
					searchStateSet.add(tempState);

					for(int i=0; i < numTasks; i++) {
									
						if(tempTaskStatus.charAt(i) == REMAINING) {
							Task task = allTasks[i];
							int updatedWeight = task.weight + currentWeight;
							
							if (updatedWeight <= capacity) {
								City currentCity = task.pickupCity;
								double cost = currentCost + currentC.distanceTo(currentCity)*costPerKm;
								char[] taskStat = tempTaskStatus.toCharArray();
								taskStat[i] = PICKEDUP;
								
								State stateToAdd = new State(tempState, currentCity, new String(taskStat), updatedWeight, PICKUP, task, cost);
								Qtable.add(stateToAdd);
							}
						}
						
						else if(tempTaskStatus.charAt(i) == PICKEDUP) {
							Task task = allTasks[i];
							City currentCity = task.deliveryCity;
							double cost = tempState.cost + currentC.distanceTo(currentCity)*costPerKm;
							char[] taskStat = tempTaskStatus.toCharArray();
							taskStat[i] = DELIVERED;
							int weight = tempState.weight - task.weight;
							
							State stateToAdd = new State(tempState, currentCity, new String(taskStat), weight, DELIVER, task, cost);
							Qtable.add(stateToAdd);
						}
					}
				}
			}	
		}while(true);
		
		retrievePlan(goalState, bestPlan);
		long endTime = System.currentTimeMillis();
		
		System.out.println("plan = "+bestPlan+"");
		System.out.println("cost = "+minimumFinalCost+"");
		System.out.println("iterations = "+iteration+"");
		System.out.println("Execution time: " + (endTime - startTime) + "");
		
		return bestPlan;
	}
	
	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
			this.transfercarriedTasks = carriedTasks;
		}
	}
	
	public void findSuccessors(State currentState, Task[] tasks, List<State> Queue, int numTasks, String currentTaskStatus){
		
		int currentWeight = currentState.weight;
		double currentCost = currentState.cost;
		City currentC = currentState.currentCity;
		
		

		for(int i=0; i < numTasks; i++) {

			
			
			if(currentTaskStatus.charAt(i) == REMAINING) {
				Task task = tasks[i];
				int newWeight = currentWeight + task.weight;
				if (newWeight <= capacity) {
					City currentCity = task.pickupCity;
					char[] taskStat = currentState.taskStatus.toCharArray();
					taskStat[i] = PICKEDUP;
					double cost = currentCost + currentC.distanceTo(currentCity)*costPerKm;
					State stateToAdd = new State(currentState, currentCity, new String(taskStat), newWeight, PICKUP, task, cost);
					Queue.add(stateToAdd);
				}
			}
			
			else if(currentTaskStatus.charAt(i) == PICKEDUP) {
				Task task = tasks[i];
				City currentCity = task.deliveryCity;
				char[] taskStat = currentState.taskStatus.toCharArray();
				taskStat[i] = DELIVERED;
				int weight = currentState.weight - task.weight;
				double cost = currentState.cost + currentC.distanceTo(currentCity)*costPerKm;
				State stateToAdd = new State(currentState, currentCity, new String(taskStat), weight, DELIVER, task, cost);
				Queue.add(stateToAdd);
			}
		}
	}
	
	public void retrievePlan(State goalState, Plan plan) {
		State previousState = goalState.previousState;
		if(previousState != null) {
			retrievePlan(previousState, plan);
			for(City city:previousState.currentCity.pathTo(goalState.currentCity)) 
				plan.appendMove(city);
			if(goalState.action == PICKUP)
				plan.appendPickup(goalState.task);
			else if(goalState.action == DELIVER)
				plan.appendDelivery(goalState.task);
		}
	}
	
	
	public void filterQueue(List<State> Queue, State tempState) {
		List<State> queueCopy = new ArrayList<State>();
		queueCopy.addAll(Queue);
		for(State state:queueCopy) {
			if(tempState.cost <= state.cost)
				Queue.remove(state);
		}
	}
	
	public boolean areAllTasksDelivered(State state) {
		boolean flag;
		String ouf = new String(state.taskStatus);
		if(ouf.replace("d", "").length() == 0)
			flag = true;
		else
			flag = false;
			
		
		
		return flag;
	}
	
	public class StateComparator implements Comparator<State>{
		
		private Task[] allTasks;


		public StateComparator(Task[] allTasks) {
			this.allTasks = allTasks;
		
		}

		@Override
		public int compare(State a, State b) {
			
			double longestPatha = 0;
			double longestPathb = 0;
			int numTasksa = a.taskStatus.length();
			int numTasksb = b.taskStatus.length();
			
			for(int i= 0 ; i < numTasksa ; i++) {
				
				if(a.taskStatus.charAt(i) == PICKEDUP) {
					if(a.currentCity.distanceTo(allTasks[i].deliveryCity) > longestPatha) 
						longestPatha = a.currentCity.distanceTo(allTasks[i].deliveryCity);		
				}
				
				else if(a.taskStatus.charAt(i) == REMAINING) {
					if(a.currentCity.distanceTo(allTasks[i].pickupCity) + allTasks[i].pathLength() > longestPatha) 
						longestPatha = a.currentCity.distanceTo(allTasks[i].pickupCity) + allTasks[i].pathLength();		
				}
				
					
				
			}
			
			for(int i= 0 ; i < numTasksb ; i++) {
				
				if(b.taskStatus.charAt(i) == PICKEDUP) {
					if(b.currentCity.distanceTo(allTasks[i].deliveryCity) > longestPathb) 
						longestPathb = b.currentCity.distanceTo(allTasks[i].deliveryCity);		
				}
				
				else if(b.taskStatus.charAt(i) == REMAINING) {
					if(b.currentCity.distanceTo(allTasks[i].pickupCity) + allTasks[i].pathLength() > longestPathb) 
						longestPathb = b.currentCity.distanceTo(allTasks[i].pickupCity) + allTasks[i].pathLength();		
				}
			}
			
			
			double difference = (a.cost +longestPatha*costPerKm) - (b.cost+longestPathb*costPerKm);
			
			if(difference > 0)
				return 1;
			if(difference == 0)
				return 0;
			else
				return -1;
		}
	}

}