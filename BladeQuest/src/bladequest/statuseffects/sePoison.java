package bladequest.statuseffects;

import bladequest.battleactions.TargetedAction;
import bladequest.battleactions.bactDamage;
import bladequest.battleactions.bactRunAnimation;
import bladequest.combat.BattleEventBuilder;
import bladequest.graphics.BattleAnim;
import bladequest.world.DamageTypes;
import bladequest.world.Global;
import bladequest.world.PlayerCharacter;
import bladequest.world.States;
import bladequest.world.Stats;

public class sePoison extends StatusEffect
{
	float power;
	
	public sePoison(float power)
	{
		super("poison", true);
		this.power = power;
		icon = "poison";
		curable = true;
		removeOnDeath = true;
		battleOnly = false;
		hidden = false;
	}
	public StatusEffect clone() {return new sePoison(power);}
	public boolean weakens() {return true;}
	@Override
	public String saveLine() 
	{ 
		return "status " + sePoison.class.getSimpleName() + " " + power; 
	}
	public int calculateDamage(PlayerCharacter target)
	{
		int hp = Math.min(target.getStat(Stats.MaxHP), target.getUnModdedStat(Stats.MaxHP)); 
		
		return Math.max(1, (int)((float)hp*(power/100.0f)));
	}
	@Override
	public void onTurn(BattleEventBuilder builder) 
	{
		//set text, play animation damage, return.
		PlayerCharacter damageTarget =  builder.getSource();
		Global.battle.setInfoBarText(damageTarget.getDisplayName() + " is damaged by poison!");
		BattleAnim anim = Global.battleAnims.get("poison");
		
		int damage = calculateDamage(damageTarget);
		
		builder.addEventObject(new TargetedAction(damageTarget)
		{
			int damage;
			BattleAnim anim;
			TargetedAction initialize(int damage, BattleAnim anim)
			{
				this.anim = anim;
				this.damage = damage;
				return this;
			}
			@Override
			protected void buildEvents(BattleEventBuilder builder) {
				builder.addEventObject(new bactRunAnimation(anim));
				builder.addEventObject(new bactDamage(damage, DamageTypes.Fixed));	
			}
		}.initialize(damage, anim));
	}
	
	@Override
	public void onInflict(PlayerCharacter c) 
	{
		if(Global.GameState == States.GS_BATTLE)
			Global.playAnimation("poison", null, c.getPosition(true));
	}
	
	@Override
	public void onRemove(PlayerCharacter c) {}
	
	@Override
	public void worldEffect()
	{
		if(Global.party.getStepCount() % 3 == 0)
		{
			Global.screenShaker.shake(3, 0.1f, true);
			Global.screenFader.setFadeColor(64, 34, 177, 76);
			Global.screenFader.flash(0.25f);
		}		
	}
	
	@Override
	public void onStep(PlayerCharacter c) 
	{
		
		if(Global.party.getStepCount() % 3 == 0)
		{
			int damage = calculateDamage(c);
			int hp = c.getHP();
			
			damage = hp > damage ? damage : hp - 1;				

			c.modifyHP(-damage, false);		
		
		}
			
	}	

}
