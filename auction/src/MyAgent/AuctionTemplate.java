package MyAgent;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import uchicago.src.sim.engine.HomeController;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private int numVehicles;
	private int numCities;
	private int ownNumTasks;
	private int adversNumTasks;
	private int round;
	private int myId;
	private int adversId;
	private boolean bigFlag = true;
	private boolean[] certainty;
	private Strategy ownFinalStrategy;
	private Strategy adversFinalStrategy;
	private Strategy ownTempStrategy;
	private Strategy adversTempStrategy;
	private Task auctionnedTask;
	private List<Task> ownWonTasks;
	private List<Task> adversWonTasks;
	private List<Task> ownTempTasks; 
	private List<Task> adversTempTasks;
	private List<VehicleType> ownVehicles;
	private List<VehicleType> adversVehicles;
	private List<City> cities;
	private List<City> possibleCities;
	private long totalTime;
	private double myMarginalCost = 0;
	private double adversMarginalCost = 0;
	private double adversPredictedBid = 0;
	private int reward = 0;
	private int adversReward = 0 ;															//NEW
	private Long[][] allBids = new Long[2][500];   														  //NEW
	private double[] allAdversRatios = new double[200];
	private long adversLowestBid = 9999999 ;										//NEW			
	private double adversRatio = 0;
	private boolean wonLastRound = false;
	private int count = 1;
	//private City currentCity;
	
	private static final int BID_MAX = 4000;
	

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
		System.out.println("Setup start");
		Random random = new Random();
		int randomCity;
		
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.myId = agent.id();
		this.ownNumTasks = 0;
		this.adversNumTasks = 0;
		this.cities = new ArrayList<City>();
		this.cities.addAll(topology.cities());
		this.possibleCities = new ArrayList<City>();
		this.numCities = topology.cities().size();
		this.numVehicles = agent.vehicles().size();
		this.ownVehicles = new ArrayList<VehicleType>();
		this.adversVehicles = new ArrayList<VehicleType>();
		this.ownWonTasks = new ArrayList<Task>();
		this.adversWonTasks = new ArrayList<Task>();
		this.ownTempTasks = new ArrayList<Task>();
		this.adversTempTasks = new ArrayList<Task>();
		this.ownTempStrategy = new Strategy(0, numVehicles);
		this.adversTempStrategy = new Strategy(0, numVehicles);
		this.ownFinalStrategy = new Strategy(0, numVehicles);
		this.adversFinalStrategy = new Strategy(0, numVehicles);
		this.round = 0;
		this.certainty = new boolean[numVehicles];
		Arrays.fill(certainty, false);
		
		
		if(myId == 0)
			this.adversId = 1;
		else
			this.adversId = 0;
		
		possibleCities.addAll(cities);
		for(int i = 0; i < numVehicles; i++) {
			this.ownVehicles.add(new VehicleType(agent.vehicles().get(i)));
			this.adversVehicles.add(new VehicleType(agent.vehicles().get(i)));
			possibleCities.remove(agent.vehicles().get(i).homeCity());
		}
		for(int i = 0; i < numVehicles; i++) {
			randomCity = random.nextInt(possibleCities.size()-1);
			adversVehicles.get(i).homeCity = possibleCities.get(randomCity);
			possibleCities.remove(randomCity);
		}
		
		for(int i = 0; i < numVehicles; i++)
			System.out.println("myVehicle["+i+"].homeCity = "+ownVehicles.get(i).homeCity.name+"");
		
		for(int i = 0; i < numVehicles; i++)
			System.out.println("adversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
		
		/*for(City city1:topology.cities()) {
			System.out.println("probablity(task in "+city1.name+") = "+(distribution.probability(city1, null))+"");
			for(City city2:topology.cities()) {
				if(!city1.equals(city2)) {
					System.out.println("probablity(task between "+city1.name+" and "+city2.name+") = "+distribution.probability(city1, city2)+""); 
				}
			}
		}*/
		
		//System.out.println("numVehicles = "+numVehicles+"");
		System.out.println("Setup done");
		//this.currentCity = vehicle.homeCity();

		//long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		//this.random = new Random(seed);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		System.out.println("---------------------------------------------------------");
		System.out.println("ROUND "+round+"");
		System.out.println("auctionResult start");
		if(round == 1) {
			for(int i = 0; i < bids.length; i++) {
				if(i != myId) {
					if(bids[i] != null) {
						
						adversId = i;
						break;
					}
				}
			}
			
		}


		allBids[0][round-1] = bids[myId];													//NEW					
		allBids[1][round-1] = bids[adversId];//NEW
		
		if(bids[adversId] > BID_MAX)
			allBids[1][round-1] = (long) BID_MAX;
		
		if(adversMarginalCost != 0)
			allAdversRatios[round-1] = bids[adversId]/adversMarginalCost;
		else
			allAdversRatios[round-1] = 0;
		System.out.println("adversLowestBid = "+adversLowestBid+"");
		if(bids[adversId] != null && bids[adversId] < adversLowestBid) {											//NEW
			adversLowestBid = bids[adversId];											//NEW
		}																				//NEW
		
		adversaryPositions(bids[adversId], previous);
		for(int i = 0; i < numVehicles; i++)
			System.out.println("adversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
		//System.out.println("auctionResult start");
		if (winner == agent.id()) {
		//currentCity = previous.deliveryCity;
			
			reward += bids[myId].intValue();
			ownNumTasks++;
			ownWonTasks.add(previous);
			ownFinalStrategy = new Strategy(ownTempStrategy, ownNumTasks, numVehicles);
			wonLastRound = true;
			//System.out.println(" finalCost = "+ownFinalStrategy.strategyCost+"");
			//ownFinalStrategy.print(ownFinalStrategy.nextAction, "ownfinalNextAction", ownNumTasks);
		}
		else {
			adversReward += bids[adversId].intValue();
			adversNumTasks++;
			adversWonTasks.add(previous);
			adversFinalStrategy = new Strategy(adversTempStrategy, adversNumTasks, numVehicles);
			wonLastRound = false;
			//adversFinalStrategy.print(adversFinalStrategy.nextAction, "adversFinalNextAction", adversNumTasks, numVehicles);
		}
		System.out.println("auctionResult done");
	}
	
	@Override
	public Long askPrice(Task task) {
		long startTime = System.currentTimeMillis();

		double ratioA = 1;
		double ratioB = 0.9;
		double ratioC = 0.9;
		double ratioD = 0.5;
		double ratioE = 0.5;
		int roundNumber = 4;
		
		double bid = 0;
		double lowerBound = 0;
		double upperBound = 0;
		double random1 = 0;
		double range = 0;
		double profitDifference = 0;

		
		//if(round == 5)
			//System.exit(1);
		round++;
		System.out.println("---------------------------------------------------------");
		System.out.println("ROUND "+round+"");
		System.out.println("askPrice start");
		
		
		if(round != 1) {
			adversRatio = 0;
			for(int i = 0; i < round-1; i++)
				adversRatio += allAdversRatios[i];
			adversRatio /= (double) (round-1);
	
		}
		System.out.println("adversRatio= "+adversRatio+"");
		
		auctionnedTask = task;
		myMarginalCost = ownMarginalCost(task);
	//	System.out.println("myMarginalCost for round "+round+" = "+myMarginalCost+"");
		adversMarginalCost = adversaryMarginalCost(task);
		if(round != 1) {
			if(adversMarginalCost != 0)
				adversPredictedBid = adversRatio*adversMarginalCost;
			else
				adversPredictedBid = adversLowestBid;
		}
		else {
			if(adversMarginalCost != 0)
				adversPredictedBid = 1.2 * adversMarginalCost;
			else
				adversPredictedBid = 100;
		}
		
		
		lowerBound = ratioA * myMarginalCost;	
		upperBound = ratioB * adversMarginalCost * adversRatio;
		range = Math.abs(upperBound - lowerBound) ;
		
		if(lowerBound < upperBound && round != 1) {
			
			if(wonLastRound) {
				if(ratioE < 1) {
					ratioE += (1-ratioE)/2;
				}
			}
			
			if(!wonLastRound) {
				ratioE = 0.5;
			}
			
			//random1 = (Math.random() *( range*(1-ratioE))) + lowerBound + range*ratioE;
			bid = lowerBound + range*ratioE + range*(1-ratioE)/2;
		}
		
		
		else if(lowerBound >= upperBound && round != 1) {
			
			bid = ratioC*myMarginalCost;
			
			/*
			profitDifference = (reward - ownFinalStrategy.strategyCost) - (adversReward - adversFinalStrategy.strategyCost) ;
			
			//this task doesn't make us lose
			if(profitDifference > upperBound )
				bid = myMarginalCost;
			
			//this task makes us lose
			if(profitDifference < upperBound && profitDifference > 0 )
				bid = ratioC * myMarginalCost;
			
			//We are losing : potential lower ratio
			if(profitDifference <= 0 )
				bid = ratioC * myMarginalCost;
			*/
		}
		
	
     	/*
		bid = ratioB * adversMarginalCost;
		
		if(bid < ratioC * myMarginalCost)
			bid = ratioC*myMarginalCost;
     	 */
		
		if(bid < adversLowestBid && round != 1)
			bid = Math.max(adversLowestBid -1 , 100);
		
		if( round < roundNumber)
			bid = ratioD * bid;
		
		if(round == 1)
			bid = ratioD*myMarginalCost ;
		
		
		//bid = Math.max(bid, 100);
		
		/*
		if(adversMarginalCost != 0) 
			adversPredictedBid = ratio * adversMarginalCost;
		else
			adversPredictedBid = 500;
		if(round !=1)
			bid = 0.99 * adversPredictedBid;
		else
			bid = ratio * myMarginalCost;
		
		if(bid <= myMarginalCost)
			bid = 1.1 * myMarginalCost;
		*/
		/*if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;*/
		
		System.out.println("askPrice done");
		long endTime = System.currentTimeMillis();
		totalTime += (endTime - startTime);
		//System.out.println("Bid Time = "+(endTime-startTime)+"");
		return (long) Math.round(bid);
	}
	
	private double ownMarginalCost(Task task) {
		double marginalCost = 0;

		ownTempTasks.clear();
		ownTempTasks.addAll(ownWonTasks);
		ownTempTasks.add(task);
		ownTempStrategy = new Strategy(ownFinalStrategy, ownNumTasks + 1, numVehicles);
		ownTempStrategy.updateStrategy(ownNumTasks + 1, numVehicles, ownTempTasks, ownVehicles);

		if(round == 1) {
			marginalCost = ownTempStrategy.strategyCost;
		}
		else {
			marginalCost = ownTempStrategy.strategyCost - ownFinalStrategy.strategyCost;
		}
		
		return marginalCost;
	}
	
	private double adversaryMarginalCost(Task task) {
		double marginalCost = 0;
		
		adversTempTasks.clear();
		adversTempTasks.addAll(adversWonTasks);
		adversTempTasks.add(task);
		
		if(round == 1) {
			adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
			adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
			marginalCost = adversTempStrategy.strategyCost;
		}
		else {
			adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
			adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
			marginalCost = adversTempStrategy.strategyCost - adversFinalStrategy.strategyCost;
		}
		
		return marginalCost;
	}
	
	private void adversaryPositions(Long adversBid, Task task) {
		System.out.println("adversary positions start");
		List<VehicleType> updatedVehicles = new ArrayList<VehicleType>();
		City tempCity;
		VehicleType tempVehicle;
		double marginalCost;
		boolean flag = false;
		int nv = numVehicles;
		int rand;
		int vcl = 0;
		Random random = new Random();
		Strategy testStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
		List<Task> testTasks = new ArrayList<Task>();
		testTasks.addAll(adversWonTasks);
		testTasks.add(task);
		
		System.out.println("adversPredictedBid = "+adversPredictedBid+"");
		System.out.println("adversBid = "+adversBid+"");
		for(int i = 0; i < numVehicles; i++)
			System.out.println("adversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
		adversTempStrategy.print(adversTempStrategy.nextAction, "adversTempStrategy", adversNumTasks+1, numVehicles);
		System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
		if(adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) {
			if(round == 1) {
				System.out.println("FIRST STEP");
				
				for(int v = 0; v < numVehicles; v++) {
					System.out.println("s3ib l7al2");
					tempVehicle = adversVehicles.get(v);
					updatedVehicles.clear();
					updatedVehicles.add(tempVehicle);
					testStrategy = new Strategy(adversNumTasks + 1, 1);
					testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
					System.out.println("strategyCost for "+tempVehicle.homeCity.name+" = "+testStrategy.strategyCost+"");
					if(testStrategy.strategyCost != 0 && (adversBid > 1.2*0.7*testStrategy.strategyCost && adversBid < 1.2*1.3*testStrategy.strategyCost)) {
						System.out.println("s3ib l7al3");
						System.out.println("v = "+v+"");
						possibleCities.remove(adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity);
						adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity = adversVehicles.get(v).homeCity;
						rand = random.nextInt(possibleCities.size());
						adversVehicles.get(v).homeCity = possibleCities.get(rand);
						possibleCities.remove(rand);
						flag = true;
						adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
						adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
						certainty[adversTempStrategy.vehicle[0]] = true;
						adversPredictedBid = 1.2*adversTempStrategy.strategyCost;
						break;
					}
				}
				for(int i = 0; i < numVehicles; i++)
					System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
				adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
				System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
				
				if(!flag) {
					System.out.println("SECOND STEP");
					
					for(City city:possibleCities) {
						tempVehicle = new VehicleType(adversVehicles.get(adversTempStrategy.vehicle[0]));
						tempVehicle.homeCity = city;
						updatedVehicles.clear();
						updatedVehicles.add(tempVehicle);
						testStrategy = new Strategy(adversNumTasks + 1, 1);
						testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
						System.out.println("strategyCost for "+city.name+" = "+testStrategy.strategyCost+"");
						if(testStrategy.strategyCost != 0 && (adversBid > 1.2*0.7*testStrategy.strategyCost && adversBid < 1.2*1.3*testStrategy.strategyCost)) {
							System.out.println("s3ib l7al4");
							possibleCities.add(adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity);
							adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity = city;
							possibleCities.remove(city);
							
							adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
							adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
							certainty[adversTempStrategy.vehicle[0]] = true;
							adversPredictedBid = 1.2*adversTempStrategy.strategyCost;
							
							for(int i = 0; i < numVehicles; i++)
								System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
							adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
							System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
							
							if(adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) {
								System.out.println("CORRECTION FOR FLAG FALSE");
								certainty[adversTempStrategy.vehicle[0]] = false;
								for(int i = 0; i < numVehicles; i++)
									System.out.println("adversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
								while((adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) && nv > 0) {
									nv--;
									for(int v = 0; v < numVehicles; v++) {
										System.out.println("s3ib l7al2");
										tempVehicle = adversVehicles.get(v);
										updatedVehicles.clear();
										updatedVehicles.add(tempVehicle);
										testStrategy = new Strategy(adversNumTasks + 1, 1);
										testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
										System.out.println("strategyCost for "+tempVehicle.homeCity.name+" = "+testStrategy.strategyCost+"");
										if(testStrategy.strategyCost != 0 && (adversBid > 1.2*0.7*testStrategy.strategyCost && adversBid < 1.2*1.3*testStrategy.strategyCost)) {
											System.out.println("s3ib l7al3");
											System.out.println("v = "+v+"");
											possibleCities.remove(adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity);
											adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity = adversVehicles.get(v).homeCity;
											rand = random.nextInt(possibleCities.size());
											adversVehicles.get(v).homeCity = possibleCities.get(rand);
											possibleCities.remove(rand);
											
											flag = true;
											adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
											adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
											certainty[adversTempStrategy.vehicle[0]] = true;
											adversPredictedBid = 1.2*adversTempStrategy.strategyCost;
											break;
										}
									}
									for(int i = 0; i < numVehicles; i++)
										System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
									adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
									System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
								}
							}
							break;
						}
					}
				}
				else {
					
					nv = numVehicles;
					if(adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid ){
						System.out.println("CORRECTION FOR FLAG TRUE");
						certainty[adversTempStrategy.vehicle[0]] = false;
						for(int i = 0; i < numVehicles; i++)
							System.out.println("adversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
						adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
						System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
						while((adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) && nv > 0) {
							nv--;
							for(int v = 0; v < numVehicles; v++) {
								System.out.println("s3ib l7al5");
								tempVehicle = adversVehicles.get(v);
								updatedVehicles.clear();
								updatedVehicles.add(tempVehicle);
								testStrategy = new Strategy(adversNumTasks + 1, 1);
								testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
								System.out.println("strategyCost for "+tempVehicle.homeCity.name+" = "+testStrategy.strategyCost+"");
								if(testStrategy.strategyCost != 0 && (adversBid > 1.2*0.7*testStrategy.strategyCost && adversBid < 1.2*1.3*testStrategy.strategyCost)) {
									System.out.println("s3ib l7al6");
									System.out.println("v = "+v+"");
									possibleCities.remove(adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity);
									adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity = adversVehicles.get(v).homeCity;
									rand = random.nextInt(possibleCities.size());
									adversVehicles.get(v).homeCity = possibleCities.get(rand);
									possibleCities.remove(rand);
									
									flag = true;
									adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
									adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
									certainty[adversTempStrategy.vehicle[0]] = true;
									adversPredictedBid = 1.2*adversTempStrategy.strategyCost;
									break;
								}
							}
							for(int i = 0; i < numVehicles; i++)
								System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
							adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
							System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
						}
					}
				}
			}
			else {
				if(adversNumTasks == 0) {
					System.out.println("FIRST STEP");
					
					for(int v = 0; v < numVehicles; v++) {
						if(!certainty[v]) {
							System.out.println("s3ib l7al2");
							tempVehicle = adversVehicles.get(v);
							updatedVehicles.clear();
							updatedVehicles.add(tempVehicle);
							testStrategy = new Strategy(adversNumTasks + 1, 1);
							testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
							System.out.println("strategyCost for "+tempVehicle.homeCity.name+" = "+testStrategy.strategyCost+"");
							if(testStrategy.strategyCost != 0 && (adversBid > adversRatio*0.7*testStrategy.strategyCost && adversBid < adversRatio*1.3*testStrategy.strategyCost)) {
								System.out.println("s3ib l7al3");
								System.out.println("v = "+v+"");
								possibleCities.remove(adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity);
								adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity = adversVehicles.get(v).homeCity;
								rand = random.nextInt(possibleCities.size());
								adversVehicles.get(v).homeCity = possibleCities.get(rand);
								possibleCities.remove(rand);
								flag = true;
								adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
								adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
								certainty[adversTempStrategy.vehicle[0]] = true;
								adversPredictedBid = adversRatio*(adversTempStrategy.strategyCost - adversFinalStrategy.strategyCost);
								break;
							}
						}
					}
					for(int i = 0; i < numVehicles; i++)
						System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
					adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
					System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
					
					if(!flag) {

						System.out.println("SECOND STEP");
						if(!certainty[adversTempStrategy.vehicle[0]]) {
							for(City city:possibleCities) {
								tempVehicle = new VehicleType(adversVehicles.get(adversTempStrategy.vehicle[0]));
								tempVehicle.homeCity = city;
								updatedVehicles.clear();
								updatedVehicles.add(tempVehicle);
								testStrategy = new Strategy(adversNumTasks + 1, 1);
								testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
								System.out.println("strategyCost for "+city.name+" = "+testStrategy.strategyCost+"");
								if(testStrategy.strategyCost != 0 && (adversBid > adversRatio*0.7*testStrategy.strategyCost && adversBid < adversRatio*1.3*testStrategy.strategyCost)) {
									System.out.println("s3ib l7al4");
									possibleCities.add(adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity);
									adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity = city;
									possibleCities.remove(city);
									
									adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
									adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
									certainty[adversTempStrategy.vehicle[0]] = true;
									adversPredictedBid = adversRatio*(adversTempStrategy.strategyCost - adversFinalStrategy.strategyCost);
									
									for(int i = 0; i < numVehicles; i++)
										System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
									adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
									System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
									
									if(adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) {
										System.out.println("CORRECTION FOR FLAG FALSE");
										certainty[adversTempStrategy.vehicle[0]] = false;
										for(int i = 0; i < numVehicles; i++)
											System.out.println("adversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
										while((adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) && nv > 0) {
											nv--;
											for(int v = 0; v < numVehicles; v++) {
												System.out.println("s3ib l7al2");
												tempVehicle = adversVehicles.get(v);
												updatedVehicles.clear();
												updatedVehicles.add(tempVehicle);
												testStrategy = new Strategy(adversNumTasks + 1, 1);
												testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
												System.out.println("strategyCost for "+tempVehicle.homeCity.name+" = "+testStrategy.strategyCost+"");
												if(testStrategy.strategyCost != 0 && (adversBid > adversRatio*0.7*testStrategy.strategyCost && adversBid < adversRatio*1.3*testStrategy.strategyCost)) {
													System.out.println("s3ib l7al3");
													System.out.println("v = "+v+"");
													possibleCities.remove(adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity);
													adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity = adversVehicles.get(v).homeCity;
													rand = random.nextInt(possibleCities.size());
													adversVehicles.get(v).homeCity = possibleCities.get(rand);
													possibleCities.remove(rand);
													
													flag = true;
													adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
													adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
													certainty[adversTempStrategy.vehicle[0]] = true;
													adversPredictedBid = adversRatio*(adversTempStrategy.strategyCost - adversFinalStrategy.strategyCost);
													break;
												}
											}
											for(int i = 0; i < numVehicles; i++)
												System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
											adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
											System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
										}
									}
									break;
								}
							}
						}
						else {
							while(vcl < numVehicles && !flag) {
								for(City city:possibleCities) {
									tempVehicle = new VehicleType(adversVehicles.get(vcl));
									tempVehicle.homeCity = city;
									updatedVehicles.clear();
									updatedVehicles.add(tempVehicle);
									testStrategy = new Strategy(adversNumTasks + 1, 1);
									testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
									System.out.println("strategyCost for "+city.name+" = "+testStrategy.strategyCost+"");
									if(testStrategy.strategyCost != 0 && (adversBid > adversRatio*0.7*testStrategy.strategyCost && adversBid < adversRatio*1.3*testStrategy.strategyCost)) {
										System.out.println("s3ib l7al4");
										possibleCities.add(adversVehicles.get(vcl).homeCity);
										adversVehicles.get(vcl).homeCity = city;
										possibleCities.remove(city);
										flag = true;
										adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
										adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
										certainty[vcl] = true;
										adversPredictedBid = adversRatio*(adversTempStrategy.strategyCost - adversFinalStrategy.strategyCost);
										
										for(int i = 0; i < numVehicles; i++)
											System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
										adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
										System.out.println("Vehicle responsible : "+adversVehicles.get(vcl).homeCity+"");
										
										if(adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) {
											System.out.println("CORRECTION FOR FLAG FALSE");
											certainty[vcl] = false;
											for(int i = 0; i < numVehicles; i++)
												System.out.println("adversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
											while((adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) && nv > 0) {
												nv--;
												for(int v = 0; v < numVehicles; v++) {
													System.out.println("s3ib l7al2");
													tempVehicle = adversVehicles.get(v);
													updatedVehicles.clear();
													updatedVehicles.add(tempVehicle);
													testStrategy = new Strategy(adversNumTasks + 1, 1);
													testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
													System.out.println("strategyCost for "+tempVehicle.homeCity.name+" = "+testStrategy.strategyCost+"");
													if(testStrategy.strategyCost != 0 && (adversBid > adversRatio*0.7*testStrategy.strategyCost && adversBid < adversRatio*1.3*testStrategy.strategyCost)) {
														System.out.println("s3ib l7al3");
														System.out.println("v = "+v+"");
														possibleCities.remove(adversVehicles.get(vcl).homeCity);
														adversVehicles.get(vcl).homeCity = adversVehicles.get(v).homeCity;
														rand = random.nextInt(possibleCities.size());
														adversVehicles.get(v).homeCity = possibleCities.get(rand);
														possibleCities.remove(rand);
														
														flag = true;
														adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
														adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
														certainty[vcl] = true;
														adversPredictedBid = adversRatio*(adversTempStrategy.strategyCost - adversFinalStrategy.strategyCost);
														break;
													}
												}
												for(int i = 0; i < numVehicles; i++)
													System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
												adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
												System.out.println("Vehicle responsible : "+adversVehicles.get(vcl).homeCity+"");
											}
										}
										break;
									}
								}//
								vcl++;
							}
						}
					}////
					else {
						
						nv = numVehicles;
						if(adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) {
							System.out.println("CORRECTION FOR FLAG TRUE");
							certainty[adversTempStrategy.vehicle[0]] = false;
							for(int i = 0; i < numVehicles; i++)
								System.out.println("adversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
							adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
							System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
							while((adversBid < 0.7*adversPredictedBid || adversBid > 1.3*adversPredictedBid) && nv > 0) {
								nv--;
								for(int v = 0; v < numVehicles; v++) {
									if(!certainty[v]){
										System.out.println("s3ib l7al5");
										tempVehicle = adversVehicles.get(v);
										updatedVehicles.clear();
										updatedVehicles.add(tempVehicle);
										testStrategy = new Strategy(adversNumTasks + 1, 1);
										testStrategy.updateStrategy(adversNumTasks + 1, 1, testTasks, updatedVehicles);
										System.out.println("strategyCost for "+tempVehicle.homeCity.name+" = "+testStrategy.strategyCost+"");
										if(testStrategy.strategyCost != 0 && (adversBid > adversRatio*0.7*testStrategy.strategyCost && adversBid < adversRatio*1.3*testStrategy.strategyCost)) {
											System.out.println("s3ib l7al6");
											System.out.println("v = "+v+"");
											possibleCities.remove(adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity);
											adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity = adversVehicles.get(v).homeCity;
											rand = random.nextInt(possibleCities.size());
											adversVehicles.get(v).homeCity = possibleCities.get(rand);
											possibleCities.remove(rand);
											
											flag = true;
											adversTempStrategy = new Strategy(adversFinalStrategy, adversNumTasks + 1, numVehicles);
											adversTempStrategy.updateStrategy(adversNumTasks + 1, numVehicles, adversTempTasks, adversVehicles);
											certainty[adversTempStrategy.vehicle[0]] = true;
											adversPredictedBid = adversRatio*(adversTempStrategy.strategyCost - adversFinalStrategy.strategyCost);
											break;
										}
									}
								}
								for(int i = 0; i < numVehicles; i++)
									System.out.println("updatedAdversVehicle["+i+"].homeCity = "+adversVehicles.get(i).homeCity.name+"");
								adversTempStrategy.print(adversTempStrategy.nextAction, "updatedAdversTempStrategy", adversNumTasks+1, numVehicles);
								System.out.println("Vehicle responsible : "+adversVehicles.get(adversTempStrategy.vehicle[0]).homeCity+"");
							}
						}
					}
				}
			}
		}
		else {
			System.out.println("khra dial Vehicle responsible : "+adversTempStrategy.vehicle[adversNumTasks]+"");
			if(!certainty[adversTempStrategy.vehicle[adversNumTasks]])
				certainty[adversTempStrategy.vehicle[adversNumTasks]] = true;
		}
		
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		Long startTime = System.currentTimeMillis();
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		Strategy possibleBetterStrategy = new Strategy(ownFinalStrategy, ownNumTasks, numVehicles);
		//Plan planVehicle1 = naivePlan(vehicle, tasks);
		List<Task> allTasks = new ArrayList<Task>();
		allTasks.addAll(tasks);
		//ownWonTasks.removeAll(allTasks);
		//if(ownWonTasks.size() != 0)
		//	System.out.println("s3ib l7al");
		SLS sls = new SLS(ownNumTasks, numVehicles, allTasks, ownVehicles, possibleBetterStrategy);
		List<Plan> plans = new ArrayList<Plan>();
		
		if(ownNumTasks != 0) {
			possibleBetterStrategy = sls.stochasticLocalSearch();
			if(possibleBetterStrategy.strategyCost < ownFinalStrategy.strategyCost) 
				plans = possibleBetterStrategy.strategyToPlans(numVehicles, ownNumTasks, vehicles, allTasks);
			else
				plans = ownFinalStrategy.strategyToPlans(numVehicles, ownNumTasks, vehicles, allTasks);
		}
		else
			plans = ownFinalStrategy.strategyToPlans(numVehicles, ownNumTasks, vehicles, allTasks);
		
	//	plans.add(planVehicle1);
	//	while (plans.size() < vehicles.size())
		//	plans.add(Plan.EMPTY);
		
		//System.out.println("------------------");
		//possibleBetterStrategy.print(possibleBetterStrategy.nextAction, "", ownNumTasks);
		

		
		for(int p = 0; p < numVehicles; p++)
			System.out.println("plan = "+plans.get(p)+"");

		Long duration = System.currentTimeMillis() - startTime;
		System.out.println(" ownfinalCost = "+ownFinalStrategy.strategyCost+" .... possibleBetterCost = "+possibleBetterStrategy.strategyCost+" .... totalBidTime = "+totalTime+" .... totalPlantTime = "+duration+" .... totalReward = "+reward+"");
		return plans;
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
}
