package bladequest.combat;

import java.util.ArrayList;
import java.util.List;

import bladequest.battleactions.BattleAction;
import bladequest.battleactions.bactDamage;
import bladequest.battleactions.bactTryEscape;
import bladequest.graphics.BattleAnim;
import bladequest.graphics.BattleSprite.faces;
import bladequest.statuseffects.StatusEffect;
import bladequest.world.Ability;
import bladequest.world.DamageTypes;
import bladequest.world.Global;
import bladequest.world.Item;
import bladequest.world.PlayerCharacter;

public class BattleEvent 
{
	private static final long actTimerLength = 150;//milliseconds
	public static int frameFromActIndex(int index){return (int)(index*actTimerLength);}	
	
	private PlayerCharacter source;
	private List<PlayerCharacter> targets;
	private List<BattleEventObject> objects;
	
	private boolean running, done;
	private long startTime;
	
	private BattleAnim anim;	
	private int animStartIndex;
	
	private List<StatusEffect> endTurnStatuses;
	
	public BattleEvent(PlayerCharacter source, List<PlayerCharacter> targets)
	{
		this.source = source;
		this.targets = new ArrayList<PlayerCharacter>(targets);	
		
		objects = new ArrayList<BattleEventObject>();
	}
	
	public PlayerCharacter getSource() { return source; }
	public List<PlayerCharacter> getTargets() { return targets;}	
	public boolean isDone(){ return done;}	
	
	private int syncToAnimationWithOffset(int frame)
	{
	  return anim.syncToAnimation(frame) + frameFromActIndex(animStartIndex);
	}
	
	private int syncToAnimationWithOffset(float framePercent)
	{
	  return anim.syncToAnimation(framePercent) + frameFromActIndex(animStartIndex);
	}
	
	
	//get the frameIndex that falls aftr the end of the animation
	private int getFinalAnimFrameIndex()
	{
		int finalFrame = syncToAnimationWithOffset(-1);
		int index = (int)(finalFrame / actTimerLength)+1;
		
		return index;
	}
	
	private void setBattleAnimation(BattleAnim newAnim, int animOffset)
	{
	  anim = newAnim;
	  animStartIndex = animOffset;
	  if (anim != null)
	  {
		  objects.add(new BattleEventObject(frameFromActIndex(animStartIndex), anim, source, targets));
	  }
	}
	
	private BattleEventBuilder makeBattleEventBuilder()
	{
		return new BattleEventBuilder()
		{	 
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
			public void setAnimation(BattleAnim anim, int frameOffset) {
				setBattleAnimation(anim, frameOffset);
			}
			@Override
			public void addEventObject(BattleEventObject eventObj) {
				objects.add(eventObj);
			}
		};	
	}
	
	public void setTargets(List<PlayerCharacter> targets)
	{
		this.targets = targets;
		objects.clear();
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
				PlayerCharacter source = builder.getSource();
				List<PlayerCharacter> targets = builder.getTargets();
				BattleAnim anim = ability.getAnimation();
				int animStartIndex = 3;
				
				int animStartTime = frameFromActIndex(animStartIndex);
				int finalIndex = animStartIndex;
				
				if (anim != null)
				{
					builder.setAnimation(anim, animStartIndex);
					finalIndex = (int)((frameFromActIndex(finalIndex) + anim.syncToAnimation(1.0f))/actTimerLength)+1;
					builder.addEventObject(new BattleEventObject(frameFromActIndex(animStartIndex), faces.Cast, 0, source));
				}
				
				for(BattleAction action : ability.getActions(builder))
				{
					int frame = action.getFrame();
					if (anim != null)
					{
						builder.addEventObject(new BattleEventObject(animStartTime + anim.syncToAnimation(frame), action, source, targets));
					}
					else
					{
						builder.addEventObject(new BattleEventObject(frameFromActIndex(frame), action, source, targets));
					}
				}
				
				if (anim != null)
				{
					builder.addEventObject(new BattleEventObject(frameFromActIndex(finalIndex), faces.Ready, 0, source));
				}
				builder.addEventObject(new BattleEventObject(frameFromActIndex(finalIndex+2)));
			}
			
		}.initialize(ability);
	}
	public void init()
	{
		int finalIndex;
		done = running = false;
		
		switch(source.getAction())
		{
		case Attack:
			anim = source.getWeaponAnimation();
			animStartIndex = 3;
			objects.add(new BattleEventObject(frameFromActIndex(0), faces.Ready, 0, source));
			objects.add(new BattleEventObject(frameFromActIndex(2), faces.Attack, 0, source));
			objects.add(new BattleEventObject(frameFromActIndex(3), faces.Attack, 1, source));
			objects.add(new BattleEventObject(frameFromActIndex(4), faces.Attack, 2, source));	
			objects.add(new BattleEventObject(frameFromActIndex(5), faces.Ready, 0, source));
			objects.add(new BattleEventObject(frameFromActIndex(animStartIndex), source.getWeaponAnimation(), source, targets));
			objects.add(new BattleEventObject(syncToAnimationWithOffset(0.5f), new bactDamage(0, 1.0f, DamageTypes.Physical), source, targets));
			objects.add(new BattleEventObject(frameFromActIndex(7)));
			break;
		case Ability:
			Ability ab = source.getAbilityToUse();
			abilityToBattleEventBuilder(ab).buildEvents(makeBattleEventBuilder());
			break;
		case CombatAction:
			source.getEventBuilder().buildEvents(makeBattleEventBuilder());
			break;
		case Item:
			Item itm = source.getItemToUse();
			anim = new BattleAnim(Global.battleAnims.get(itm.getAnimName()));
			animStartIndex = 3;			
			finalIndex = getFinalAnimFrameIndex();
			
			//resets item count and removes from inventory
			source.useItem();
			
			objects.add(new BattleEventObject(frameFromActIndex(animStartIndex), faces.Use, 0, source));
			objects.add(new BattleEventObject(frameFromActIndex(animStartIndex), anim, source, targets));
			for(BattleAction action : itm.getActions())
				objects.add(new BattleEventObject(syncToAnimationWithOffset(action.getFrame()), action, source, targets));
			objects.add(new BattleEventObject(frameFromActIndex(finalIndex), faces.Ready, 0, source));
			objects.add(new BattleEventObject(frameFromActIndex(finalIndex+2)));
			
			break;
		case Run:
			animStartIndex = 3;
			objects.add(new BattleEventObject(frameFromActIndex(animStartIndex), new bactTryEscape(animStartIndex, makeBattleEventBuilder()), source, targets));
			break;
		case Guard:
			objects.add(new BattleEventObject(0));  //insta-fail.
		default:
			break;
		}
		
	}
	
	public void update(Battle battle, List<DamageMarker> markers)
	{
		if (!source.isInBattle())
		{
			running = false;
			done = true;
			return;
		}
		if(!running)
		{
			running = true;
			startTime = System.currentTimeMillis();
		}
		else
		{
			long frame = System.currentTimeMillis() - startTime;
			BattleEventObject rmObj = null;
			//int actIndex = (int)(frame / actTimerLength);
			for(BattleEventObject obj : objects)
			{
				if(obj.Frame() <= frame)
				{
					obj.execute(battle, markers);
					rmObj = obj;
					break;					
				}
			}
			
			if(rmObj != null)
			{
				objects.remove(rmObj);			
				if(objects.size() == 0)
				{
					running = false;					
					if (endTurnStatuses == null) //haven't resolved end turn statuses yet.
					{
						//player turn over, apply status.
						//clone the list, apply all, but don't apply newly added statuses.
						//e.g. double buffer.						
						endTurnStatuses = new ArrayList<StatusEffect>(source.getStatusEffects());
					}
					while (objects.isEmpty())
					{
						if (endTurnStatuses.isEmpty())
						{
							endTurnStatuses = null; //nothing more to do, we can end the turn.
							done  = true;
							return;
						}
						else  //try and apply a status effect.
						{
							StatusEffect firstStatus = endTurnStatuses.get(0);
							endTurnStatuses.remove(0);
							firstStatus.onTurn(makeBattleEventBuilder());
						}						
					}
				}
			}			
		}
	}
}
