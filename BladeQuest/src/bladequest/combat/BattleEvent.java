package bladequest.combat;

import java.util.ArrayList;
import java.util.List;

import bladequest.battleactions.BattleAction;
import bladequest.battleactions.BattleAction.State;
import bladequest.battleactions.BattleActionPatterns;
import bladequest.battleactions.BattleActionRunner;
import bladequest.battleactions.DelegatingAction;
import bladequest.battleactions.TargetedAction;
import bladequest.battleactions.bactDamage;
import bladequest.battleactions.bactSlash;
import bladequest.battleactions.bactTryEscape;
import bladequest.combat.BattleCalc.MovePriority;
import bladequest.statuseffects.StatusEffect;
import bladequest.world.Ability;
import bladequest.world.DamageTypes;
import bladequest.world.Global;
import bladequest.world.PlayerCharacter;
import bladequest.world.TargetTypes;
import bladequest.world.PlayerCharacter.Action;

public class BattleEvent 
{
	private static final long actTimerLength = 150;//milliseconds
	public static int frameFromActIndex(int index){return (int)(index*actTimerLength);}	
	
	private PlayerCharacter source;
	private List<PlayerCharacter> targets;
	
	private BattleActionRunner actionRunner;
	
	private boolean running, done, interrupted;
	private long startTime;
	
	PlayerCharacter.Action action;
	Ability ability;
	
	private List<StatusEffect> endTurnStatuses;
	private List<DamageMarker> markers;
	private boolean dontRunStatus;
	
	enum ActionType
	{
		Chosen,
		Special
	}
	
	ActionType type;
	
	public BattleEvent(PlayerCharacter.Action action, Ability ability, PlayerCharacter source, List<PlayerCharacter> targets, List<DamageMarker> markers)
	{
		this.action = action;
		this.ability = ability;
		this.source = source;
		this.targets = new ArrayList<PlayerCharacter>(targets);
		this.markers = markers;
		type = ActionType.Chosen;
		dontRunStatus = false;
		interrupted = false;
	}
	
	public BattleEvent(PlayerCharacter.Action action, Ability ability, PlayerCharacter source, List<PlayerCharacter> targets, List<DamageMarker> markers, ActionType type)
	{
		this.action = action;
		this.ability = ability;
		this.source = source;
		this.targets = new ArrayList<PlayerCharacter>(targets);
		this.markers = markers;
		this.type = type;
		dontRunStatus = false;
		interrupted = false;
	}	
	public Ability getAbility() {return ability;}
	public PlayerCharacter.Action getAction() {return action;}
	public ActionType getType(){return type;}
	public PlayerCharacter getSource() { return source; }
	public List<PlayerCharacter> getTargets() { return targets;}	
	public boolean isDone(){ return done;}
	public boolean runningStatus() { return endTurnStatuses != null;}
	
	public MovePriority getPriority()
	{
		switch(action)
		{
		case Attack:
			return MovePriority.Regular;
		case Ability:
			return ability.getPriority();
		case CombatAction:
			//combat actions... all regular for now
			return MovePriority.Regular;
		case Item:
			return MovePriority.Item;
		case Run:
			return MovePriority.Regular;
		case Guard: case Skipped:
		default:
			return MovePriority.Low;
		}
	}
	
	boolean special = false;
	public void setSpecial()
	{
		special = true;
	}
	public boolean isSpecial()
	{
		return special;
	}
	
	public void setActionType(PlayerCharacter.Action action)
	{
		//mainly to reset to skipped, possibly for berserk??/
		this.action = action;
	}
	
	public void dontRunStatusEffects()
	{
		dontRunStatus = true;
	}
	
	private BattleEventBuilder makeBattleEventBuilder()
	{
		return new BattleEventBuilder()
		{	 
			BattleEvent ev;
			BattleEventBuilder initializer(BattleEvent ev)
			{
				this.ev = ev;
				return this;
			}
			@Override				 
			public List<PlayerCharacter> getTargets()
			{
				return targets;
			}
			@Override				 
			public PlayerCharacter getSource()
		    {
				return source;
			}
			@Override
			public int getCurrentBattleFrame()
			{
				return ev.getCurrentFrame();	
			}
			@Override
			public BattleAction getLast() {
				return ev.actionRunner.getLast();
			}
			@Override
			public void addEventObject(BattleAction eventObj) {
				ev.actionRunner.addAction(eventObj);
			}
			@Override
			public void addMarker(DamageMarker marker) {
				markers.add(marker);
			}
		}.initializer(this);	
	}
	
	public void setTargets(List<PlayerCharacter> targets)
	{
		this.targets = targets;
		init();
	}
	public static BattleEventBuilderObject abilityToBattleEventBuilder(Ability ability)
	{
		return new BattleEventBuilderObject()
		{
			Ability ability;
			public BattleEventBuilderObject initialize(Ability ability)
			{
				this.ability = ability;
				return this;
			}
			@Override
			public void buildEvents(BattleEventBuilder builder) {
				for (BattleAction action : ability.getActions())
				{
					builder.addEventObject(action);
				}
			}
			
		}.initialize(ability);
	}
	
	public static BasicAttackBuilder attackBuilder;
	private class AttackBuilder implements BasicAttackBuilder 
	{
		BattleEventBuilder builder;
		AttackBuilder(BattleEventBuilder builder)
		{
			this.builder = builder;
		}
		@Override
		public BattleEventBuilder getEventBuilder() {
			return builder;
		}
		
	}
	public boolean running()
	{
		return this.running;
	}
	private void buildAttackSlash(BattleEventBuilder builder, PlayerCharacter.Hand hand, BattleAction prevAction)
	{
		
		builder.addEventObject(new DelegatingAction()
		{
			PlayerCharacter.Hand hand;
			DelegatingAction initialize(PlayerCharacter.Hand hand)
			{
				this.hand = hand;
				return this;
			}
			@Override
			protected void buildEvents(BattleEventBuilder builder) 
			{
				
				PlayerCharacter attacker = builder.getSource();
				List<PlayerCharacter> targets = Global.battle.getTargetable(attacker, builder.getTargets(), TargetTypes.Single);
				if (!targets.isEmpty())
				{
					PlayerCharacter target = targets.get(0);
					

					builder.addEventObject(new TargetedAction(target)
					{
						PlayerCharacter.Hand hand;
						TargetedAction initialize(PlayerCharacter.Hand hand)
						{
							this.hand = hand;
							return this;
						}

						@Override
						protected void buildEvents(BattleEventBuilder builder) {
							builder.addEventObject(new BattleAction()
							{
								public State run(BattleEventBuilder builder)
								{
									for (PlayerCharacter target : builder.getTargets()) {target.getOnPhysicalHitEvent().trigger();}
									return State.Finished;
								}
							});
							
							
							//figure out off-hand penalty
							float power = 1.0f;
							if (hand == PlayerCharacter.Hand.OffHand) power = 0.5f;
							
							PlayerCharacter attacker = builder.getSource();
							bactDamage damageAction = new bactDamage(power, DamageTypes.Physical);
							damageAction.setHand(hand);
							damageAction.onHitRunner().addEventObject(new BattleAction()
							{
								public State run(BattleEventBuilder builder)
								{
									builder.getSource().getOnPhysicalHitSuccessEvent().trigger();
									for (PlayerCharacter target : builder.getTargets()) {target.getOnPhysicalHitLandsEvent().trigger();}
									return State.Finished;
								}
							});
							
							boolean equipped = hand == PlayerCharacter.Hand.MainHand && attacker.hand1Equipped() ||
											   hand == PlayerCharacter.Hand.OffHand && attacker.hand2Equipped();
					 		
							if(equipped)
								for(DamageComponent dc : (hand == PlayerCharacter.Hand.MainHand ? attacker.hand1() : attacker.hand2()).getDamageComponents())
									damageAction.addDamageComponent(dc.getAffinity(), dc.getPower());
						    
							bactSlash slash = new bactSlash(damageAction, hand, 1.0f);
							
							builder.addEventObject(slash);
							
							
						}
					}.initialize(hand));
					
				}
			}
				
		}.initialize(hand).addDependency(prevAction));
	}
	public void init()
	{
		if (actionRunner != null)
		{
			actionRunner.reset();
		}
		actionRunner = new BattleActionRunner();
		BattleEventBuilder builder = makeBattleEventBuilder();
		done = running = false;
		
		switch(action)
		{
		case Attack:
			
			buildAttackSlash(builder, PlayerCharacter.Hand.MainHand, null);
			
			if (builder.getSource().hand2WeaponEquipped())
			{
				buildAttackSlash(builder, PlayerCharacter.Hand.OffHand, builder.getLast());	
			}
			attackBuilder = new AttackBuilder(builder);
			//TRIGGER WARNING
			builder.getSource().getOnAttackEvent().trigger();
			//TRIGGER WARNING
			
			break;
		case Ability:
			abilityToBattleEventBuilder(ability).buildEvents(builder);
			break;
		case CombatAction:
			source.getEventBuilder().buildEvents(builder);
			break;
		case Item:
			BattleActionPatterns.BuildItemUse(builder);
			break;
		case Run:
			builder.addEventObject(new bactTryEscape());
			break;
		case Guard: case Skipped:
//insta-fail
		default:
			break;
		}
		actionRunner.initialize();
	}
	private int getCurrentFrame()
	{
		if (!running || done) return 0;
		return (int)(System.currentTimeMillis() - startTime);
	}
	public void interrupt()
	{
		if (actionRunner != null)
		{		
			actionRunner.interrupt();
		}
		interrupted = true;
	}
	public boolean advances()
	{
		if (getAction() == Action.Ability) 
		{
			if (!ability.advances()) return false;
		}
		
		return !(getAction() == Action.Guard ||
			     getAction() == Action.Skipped ||
				 runningStatus() ||
				 interrupted());
	}
	public boolean interrupted()
	{
		return interrupted;
	}
	public void update(Battle battle)
	{
		if (done) return;
		if (!source.isInBattle() || battle.isBattleOver())
		{
			running = false;
			done = true;
			actionRunner.reset();
			return;
		}
		if(!running)
		{
			running = true;
			startTime = System.currentTimeMillis();
		}
		else
		{
			BattleEventBuilder builder = makeBattleEventBuilder();
			
			State state = actionRunner.run(builder);
			if (state  == State.Finished)
			{
				if (endTurnStatuses == null) //haven't resolved end turn statuses yet.
				{
					//player turn over, apply status.
					//clone the list, apply all, but don't apply newly added statuses.
					//e.g. double buffer.						
					endTurnStatuses = new ArrayList<StatusEffect>(source.getStatusEffects());
				}
				while (actionRunner.getActions().isEmpty())
				{
					if (endTurnStatuses.isEmpty() || dontRunStatus)
					{
						endTurnStatuses = null; //nothing more to do, we can end the turn.
						done  = true;
						return;
					}
					else  //try and apply a status effect.
					{
						StatusEffect firstStatus = endTurnStatuses.get(0);
						endTurnStatuses.remove(0);
						firstStatus.onTurn(builder);
					}						
				}				
			}
		}
	}
}
