package burgdsim.burglars;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


import burgdsim.burglary.Burglary;
import burgdsim.burglary.VictimChooser;
import burgdsim.environment.buildings.DrugDealer;
import burgdsim.environment.buildings.House;
import burgdsim.environment.buildings.Social;
import burgdsim.environment.buildings.Workplace;
import burgdsim.main.GlobalVars;
import burgdsim.pecs.Action;
import burgdsim.pecs.DoNothingV;
import burgdsim.pecs.DrugsV;
import burgdsim.pecs.Motive;
import burgdsim.pecs.SleepV;
import burgdsim.pecs.SocialV;
import burgdsim.pecs.StateVariable;

/** A child of BaseBurglar who implement their own Burglary action which has a high weight on the security of properties
 * (this Burglar doesn't like buildings with high levels of security).
 * 
 * Note that createBurglar() must also be overridden. This is because when the type of burglar to create is specified in
 * the scearios file the BurglarFactory calls the method to actually createa the burglar. So if this class doesn't override
 * the method the BaseBurglar version will be called which actually returns a BaseBurglar, not a DislikeSecurityBurglar ! 
 * 
 * @author Nick Malleson
 *
 */
public class DislikeSecurityBurglar extends BaseBurglar  {

	
	public DislikeSecurityBurglar(List<StateVariable> stateVariables) {
		super(stateVariables);
		GlobalVars.CACHES.add(this);
	}

	/** Returns a personalised Burglary action, one which gives a high weight to security when looking for a victim 
	 * (makes buildings with high security less attractive). */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Action> T getSpecificAction(Class<T> actionClass, Motive motive) {
		T obj = null;
		if (actionClass.isAssignableFrom(Burglary.class)) {
			
			// Define a weight map which gives a specific value to the security weight in VictimChooser (all other parameters will
			// keep default values).
			Map<GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER, Double> weightMap = 
				new Hashtable<GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER, Double> ();
			weightMap.put(
					GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.SEC_W,
					GlobalVars.BURGLARY_PARAMS.DISLIKE_SECURITY_BURGLARY_WEIGHT); // Special parameter for this scenario
			VictimChooser vc = new VictimChooser(this, weightMap);
			// Create a new burglary action and set the victim chooser.
			Burglary b = new Burglary(motive);
			b.setVictimChooser(vc);
			obj = (T) b;
		}
		return obj;
	}
	
	/**
	 *  Create the burglar. Normally this is all done in BurglarFactory, but in some cases the BaseBurglar can
	 *  be created by reading from the scenarios file. If this happens then BurglarFactory doesn't know anything
	 *  about the Burglar other than the name (BaseBurglar) so doesn't know which StateVariables to create etc. 
	 */
	public static Burglar createBurglar(House homePlace, Workplace workPlace, Social socialPlace, DrugDealer drugDealerPlace) {
		List<StateVariable> simpleList = new ArrayList<StateVariable>();
		Burglar burglar = new DislikeSecurityBurglar(simpleList);

		// Agent just drives around.
//		burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.CAR);
		
		// Agent has to use public transport
		burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.BUS);
		burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.TRAIN);
		
		GlobalVars.BURGLAR_ENVIRONMENT.add(burglar);
		burglar.setName("BaseBurglar"+Burglar.burglarNumber++);
		
		// Have to set up their homes etc first because these are required by actions. If these are null then set
		// them randomly.
		House home; Workplace work; Social social; DrugDealer drugDealer;
		home = homePlace == null ? (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class) : homePlace; 
		work = workPlace == null ? (Workplace) GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class) : workPlace;
		social = socialPlace == null ? (Social) GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Social.class) : socialPlace;
		drugDealer = drugDealerPlace == null ? (DrugDealer) GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(DrugDealer.class) : drugDealerPlace; 
		burglar.setHome(home);
		burglar.setWork(work);
		burglar.setDrugDealer(drugDealer);
		burglar.setSocial(social);

		GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
		burglar.initMemory();

		StateVariable sleepV = new SleepV(burglar, 0.5);
		simpleList.add(sleepV);
		StateVariable doNothingV = new DoNothingV(burglar);
		simpleList.add(doNothingV);
		StateVariable drugsV = new DrugsV(burglar, 1.0);
		simpleList.add(drugsV);
		StateVariable socialV = new SocialV(burglar, 1.0);
		simpleList.add(socialV);
		
//		System.out.println("BaseBurglar returning a new burglar: "+burglar.getName()+" with following: \n" +
//				"\thome: "+home.toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(home).toString()+"\n" +
//				"\twork: "+burglar.getWork().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getWork()).toString()+"\n" +
//				"\tsocial: "+burglar.getSocial().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getSocial()).toString()+"\n" +
//				"\tdrug: "+burglar.getDrugDealer().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getDrugDealer()).toString()+"\n");
		
		return burglar;
	}
	

}
