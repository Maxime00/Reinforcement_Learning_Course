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

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	private static final int NOACTION = 0;
	private static final int PICKUP = 1;
	private static final int DELIVER = 2;
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;
	int costPerKm;

	/* the planning class */
	Algorithm algorithm;
	
	//Define State class
	public class State{
		public State previousState;
		public City currentCity;
		public List<Task> carriedTasks;
		public List<Task> remainingTasks;
		int action;
		Task task;
		double cost;
		
		public State() {
			previousState = null;
			currentCity = null;
			carriedTasks = new ArrayList<Task>();
			remainingTasks = new ArrayList<Task>();
			action = NOACTION;
			task = null;
			cost = 0;
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
		City current = vehicle.getCurrentCity();
		Plan bestPlan = new Plan(current);
		costPerKm = vehicle.costPerKm();
		capacity = agent.vehicles().get(0).capacity();
		
		List<State> Qtable = new ArrayList<State>();
		List<State> successors = new ArrayList<State>();
		List<State> neighbors = new ArrayList<State>();
		State initialState = new State();
		State tempState = new State();
		State goalState = new State();
		double minimumCost = 999999;
		int loading, i = 0; 
		boolean flag = true;
		
		
		System.out.println("capacity = "+capacity+"");
		initialState.previousState = null;
		initialState.currentCity = current;
		for(Task task:tasks)
			initialState.remainingTasks.add(task);
		initialState.action = NOACTION;
		initialState.task = null;
		initialState.cost = 0;
		
		Qtable.add(initialState);
		
		do {
			loading = i;
			i++;
			System.out.println("i = "+i+"");
			//System.out.println("Q size = "+Qtable.size()+"");
			
			tempState = Qtable.get(0);
			Qtable.remove(0);
			
			/*if(tempState.previousState != null)
				System.out.println("tempState.previousState.currentCity = "+tempState.previousState.currentCity+"");
			System.out.println("tempState.currentCity = "+tempState.currentCity+" , tempState.carriedTasks.size() = "+tempState.carriedTasks.size()+" , tempState.remainingTasks.size() = "+tempState.remainingTasks.size()+"");
			if(tempState.action != NOACTION)
				System.out.println("tempState.action = "+tempState.action+" , tempState.task.pickupCity = "+tempState.task.pickupCity+" , tempState.task.deliveryCity = "+tempState.task.deliveryCity+" , tempState.cost = "+tempState.cost+"");
			*/
			if(tempState.carriedTasks.isEmpty() && tempState.remainingTasks.isEmpty()) {
				System.out.println("Boom");
				if(tempState.cost < minimumCost) {
					minimumCost = tempState.cost;
					goalState = tempState;
				}
				/*
				neighbors = findSuccessors(goalState.previousState);
				for(State state : neighbors) {
					if(state.cost < goalState.cost)
						flag = false;					
				}
				
				if(flag)
					break;
				*/
			}
			if (tempState == null)
				break;
			
			successors = findSuccessors(tempState);
			System.out.println("S size = "+successors.size()+"");
			Qtable.addAll(successors);
			
		}while(!Qtable.isEmpty());
		
		retrievePlan(goalState, bestPlan);
		System.out.println("plan = "+bestPlan+"");
		
		return bestPlan;
	}
	
	private Plan AStarPlan(Vehicle vehicle, TaskSet tasks) {
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
		for(Task task:tasks)
			initialState.remainingTasks.add(task);
		initialState.action = NOACTION;
		initialState.task = null;
		initialState.cost = 0; 
		
		Qtable.add(initialState);
		int i = 0;
		
		do{
			/*if(Qtable.isEmpty()){
				System.out.println("FAILURE");
				System.exit(1);
			}*/
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
			
			
			/*for(State state : Ctable) {
				if(tempState.currentCity == state.currentCity && tempState.previousState == state.previousState && tempState.carriedTasks.size() == state.carriedTasks.size() && tempState.remainingTasks.size() == state.remainingTasks.size() && tempState.cost < state.cost) 
					comp = true;
			}*/
			
			//If tempState is not in C or has a lower cost than its copy in C
			if(!Ctable.contains(tempState)) {
				Ctable.add(tempState);
				successors = findSuccessors(tempState);
				
				Collections.sort(successors, new StateComparator());
				Qtable.addAll(successors);
				
				for(State state : successors)
				
				Collections.sort(Qtable, new StateComparator());
			}
			
		}while(!Qtable.isEmpty());
		
		retrievePlan(goalState, bestPlan);
		System.out.println("plan = "+bestPlan+"");
		
		return bestPlan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
	
	public List<State> findSuccessors(State currentState){
		List<State> successors = new ArrayList<State>();
		
		
		if(currentState.remainingTasks.size() != 0 && currentState.carriedTasks.size() == 0) {
			for(Task task:currentState.remainingTasks) {
				State stateToAdd = new State();
				stateToAdd.remainingTasks.clear();
				stateToAdd.carriedTasks.clear();
				stateToAdd.remainingTasks.addAll(currentState.remainingTasks);
				stateToAdd.carriedTasks.addAll(currentState.carriedTasks);
				stateToAdd.previousState = currentState;
				stateToAdd.currentCity = task.pickupCity;
				stateToAdd.carriedTasks.add(task);
				stateToAdd.remainingTasks.remove(task);
				stateToAdd.action = PICKUP;
				stateToAdd.task = task;
				stateToAdd.cost = currentState.cost + stateToAdd.currentCity.distanceTo(currentState.currentCity)*costPerKm;
				successors.add(stateToAdd);
			}
		}
		
		else if(currentState.remainingTasks.size() == 0 && currentState.carriedTasks.size() != 0) {
			for(Task task:currentState.carriedTasks) {
				State stateToAdd = new State();
				stateToAdd.remainingTasks.clear();
				stateToAdd.carriedTasks.clear();
				stateToAdd.remainingTasks.addAll(currentState.remainingTasks);
				stateToAdd.carriedTasks.addAll(currentState.carriedTasks);
				stateToAdd.previousState = currentState;
				stateToAdd.currentCity = task.deliveryCity;
				stateToAdd.carriedTasks.remove(task);
				stateToAdd.action = DELIVER;
				stateToAdd.task = task;
				stateToAdd.cost = currentState.cost + stateToAdd.currentCity.distanceTo(currentState.currentCity)*costPerKm;
				successors.add(stateToAdd);
			}
		}
		
		else if(currentState.remainingTasks.size() != 0 && currentState.carriedTasks.size() != 0) {
			for(Task task:currentState.remainingTasks) {
				
				if(carriedWeight(currentState.carriedTasks)+task.weight <= capacity) {
										
					State stateToAdd = new State();
					stateToAdd.remainingTasks.clear();
					stateToAdd.carriedTasks.clear();
					stateToAdd.remainingTasks.addAll(currentState.remainingTasks);
					stateToAdd.carriedTasks.addAll(currentState.carriedTasks);
					stateToAdd.previousState = currentState;
					stateToAdd.currentCity = task.pickupCity;
					stateToAdd.carriedTasks.add(task);
					stateToAdd.remainingTasks.remove(task);
					stateToAdd.action = PICKUP;
					stateToAdd.task = task;
					stateToAdd.cost = currentState.cost + stateToAdd.currentCity.distanceTo(currentState.currentCity)*costPerKm;
					successors.add(stateToAdd);
				}
			}
			
			for(Task task:currentState.carriedTasks) {
				State stateToAdd = new State();
			
				stateToAdd.carriedTasks.clear();
				stateToAdd.remainingTasks.addAll(currentState.remainingTasks);
				stateToAdd.carriedTasks.addAll(currentState.carriedTasks);
				stateToAdd.previousState = currentState;
				stateToAdd.currentCity = task.deliveryCity;
				stateToAdd.carriedTasks.remove(task);
				stateToAdd.action = DELIVER;
				stateToAdd.task = task;
				stateToAdd.cost = currentState.cost + stateToAdd.currentCity.distanceTo(currentState.currentCity)*costPerKm;
				successors.add(stateToAdd);
			}	
		}
		
		/*for(State state:successors) {
			if(state.previousState != null)
				System.out.println("succState.previousState.currentCity = "+state.previousState.currentCity+"");
			System.out.println("succState.currentCity = "+state.currentCity+" , succState.carriedTasks.size() = "+state.carriedTasks.size()+" , succState.remainingTasks.size() = "+state.remainingTasks.size()+"");
			if(state.action != NOACTION)
				System.out.println("succState.action = "+state.action+" , succState.task.pickupCity = "+state.task.pickupCity+" , succState.task.deliveryCity = "+state.task.deliveryCity+" , succState.cost = "+state.cost+"");
			
		}*/
		
		return successors;
	}
	
	public void retrievePlan(State goalState, Plan plan) {
		if(goalState.previousState != null) {
			retrievePlan(goalState.previousState, plan);
			for(City city:goalState.previousState.currentCity.pathTo(goalState.currentCity)) 
				plan.appendMove(city);
			if(goalState.action == PICKUP)
				plan.appendPickup(goalState.task);
			else if(goalState.action == DELIVER)
				plan.appendDelivery(goalState.task);
		}
	}
		
	public int carriedWeight(List<Task> tasks) {
		int sum = 0;
		
		for (Task task : tasks)
			sum += task.weight;
	
		return sum;
	}
	
	private double longestPath(State a) {
		
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
	}
	
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
	}

}