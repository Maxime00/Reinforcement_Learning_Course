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
	public double costPerKm;

	/* the planning class */
	Algorithm algorithm;
	
	//Define State class
	public class State{
		public State previousState;
		public Task task;
		public City currentCity;
		public double cost;
		public String taskStatus;
		public int weight;
		public boolean action;
		
		public State(State prev, City current, String status, int w, boolean a, Task t, double c) {
			this.previousState = prev;
			this.currentCity = current;
			this.taskStatus = new String(status);
			this.weight = w;
			this.action = a;
			this.task = t;
			this.cost = c;
			
			
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + weight;
			long temp;
			temp = Double.doubleToLongBits(cost);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + (action ? 1231 : 1237);
			result = prime * result + ((currentCity == null) ? 0 : currentCity.hashCode());
			result = prime * result + ((taskStatus == null) ? 0 : taskStatus.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			State other = (State) obj;
			if (weight != other.weight)
				return false;
			if (Double.doubleToLongBits(cost) != Double.doubleToLongBits(other.cost))
				return false;
			if (action != other.action)
				return false;
			if (currentCity == null) {
				if (other.currentCity != null)
					return false;
			} else if (!currentCity.equals(other.currentCity))
				return false;
			if (taskStatus == null) {
				if (other.taskStatus != null)
					return false;
			} else if (!taskStatus.equals(other.taskStatus))
				return false;
			return true;
		}
		
	}
	
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		this.capacity = agent.vehicles().get(0).capacity();
		this.costPerKm = agent.vehicles().get(0).costPerKm();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		
		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = BFSPlan(currentCity, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		
		System.out.println(tasks);
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
	
	private Plan BFSPlan (City firstCity, TaskSet tasks) {
		
		System.out.println("BFS algorithm start");
		long startTime = System.currentTimeMillis();
		
		Plan bestPlan = new Plan(firstCity);
		double minimumFinalCost = Double.POSITIVE_INFINITY;
		State goalState = null;
		
		int numTasks = tasks.size();
		Task[] allTasks = tasks.toArray(new Task[numTasks]);
		
		String initialTaskStatus = "";
		for (int i = 0; i < numTasks; i++) {
			initialTaskStatus = initialTaskStatus + REMAINING;
		}
		
		HashSet<State> StateSet = new HashSet<State>();
		
		int iteration = 0;
		
		State initialState = new State(null, firstCity, initialTaskStatus, 0, PICKUP, null, 0);
		LinkedList<State> Qtable = new LinkedList<State>();
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
				
				if (!StateSet.contains(tempState)) {
					City currentC = tempState.currentCity;
					double currentCost = tempState.cost;
					int currentWeight = tempState.weight;
					StateSet.add(tempState);

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
							double cost = currentCost + currentC.distanceTo(currentCity)*costPerKm;
							char[] taskStat = tempTaskStatus.toCharArray();
							taskStat[i] = DELIVERED;
							
							int weight = currentWeight - task.weight;
							
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
	
	/*private Plan AStarPlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan bestPlan = new Plan(current);
		costPerKm = vehicle.costPerKm();
		capacity = agent.vehicles().get(0).capacity();
		
		List<State> Qtable = new ArrayList<State>();
		List<State> Ctable = new ArrayList<State>();
		List<State> successors = new ArrayList<State>();
		
		State initialState = new State();
		State tempState = new State();
		State goalState = new State();
		double minimumCost = 999999;
		boolean comp = false;
		
		initialState.previousState = null;
		initialState.currentCity = current;
		//for(Task task:tasks)
			//initialState.remainingTasks.add(task);
		initialState.action = NOACTION;
		initialState.task = null;
		initialState.cost = 0; 
		
		Qtable.add(initialState);
		int i = 0;
		
		do{
			if(Qtable.isEmpty()){
				System.out.println("FAILURE");
				System.exit(1);
			}
			i++;
			System.out.println("i = "+i+"");
			
			tempState = Qtable.get(0);
			Qtable.remove(0);
			
			if( tempState.carriedTasks.isEmpty() && tempState.remainingTasks.isEmpty()) {
				System.out.println("Boom");
				if(tempState.cost < minimumCost) {
					minimumCost = tempState.cost;
					goalState = tempState;
					break;
				}
				
				
			}
			
			
			for(State state : Ctable) {
				if(tempState.currentCity == state.currentCity && tempState.previousState == state.previousState && tempState.carriedTasks.size() == state.carriedTasks.size() && tempState.remainingTasks.size() == state.remainingTasks.size() && tempState.cost < state.cost) 
					comp = true;
			}
			
			//If tempState is not in C or has a lower cost than its copy in C
			if(!Ctable.contains(tempState)) {
				Ctable.add(tempState);
				//successors = findSuccessors(tempState);
				
				Collections.sort(successors, new StateComparator());
				Qtable.addAll(successors);
				
				for(State state : successors)
				
				Collections.sort(Qtable, new StateComparator());
			}
			
		}while(!Qtable.isEmpty());
		
		retrievePlan(goalState, bestPlan);
		System.out.println("plan = "+bestPlan+"");
		System.out.println("cost = "+minimumCost+"");
		
		return bestPlan;
	}*/

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
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
	
	/*private double longestPath(State a) {
		
		double longestPath = 0;
		
		for( Task task : a.carriedTasks ) {
			if(a.currentCity.distanceTo(task.deliveryCity) > longestPath) 
				longestPath = a.currentCity.distanceTo(task.deliveryCity);				
		}
		
		for( Task task : a.remainingTasks ) {
			if(a.currentCity.distanceTo(task.pickupCity)+ task.pathLength() > longestPath) 
				longestPath = a.currentCity.distanceTo(task.pickupCity)+ task.pathLength();			
		}
		

		return longestPath;
	}*/
	
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
	
	/*
	public class StateComparator implements Comparator<State>{
		
		@Override
		public int compare(State a, State b) {
			double difference = (a.cost +longestPath(a)*costPerKm) - (b.cost+longestPath(b)*costPerKm);
			
			if(difference > 0)
				return 1;
			if(difference == 0)
				return 0;
			else
				return -1;
		}
	}*/

}