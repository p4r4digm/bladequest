package bladequest.battleactions;

import java.util.ArrayList;
import java.util.List;

import bladequest.combat.BattleCalc;
import bladequest.combat.BattleEventBuilder;
import bladequest.combat.DamageComponent;
import bladequest.combat.DamageMarker;
import bladequest.world.DamageTypes;
import bladequest.world.Global;
import bladequest.world.PlayerCharacter;
import bladequest.world.Stats;

public class bactDamage extends BattleAction
{
	float power;
	DamageTypes type;
	List<DamageComponent> damageComponents;
	float customMiss;
	
	BattleCalc.AccuracyType accuracyType;
	
	public bactDamage(float power, DamageTypes type)
	{
		this.damageComponents = new ArrayList<DamageComponent>();
		this.power = power;
		this.type = type;
		this.customMiss = 0.0f;
		this.accuracyType = BattleCalc.AccuracyType.Regular;
	}
	
	public bactDamage(float power, DamageTypes type, BattleCalc.AccuracyType accuracy, float missChance)
	{
		this.damageComponents = new ArrayList<DamageComponent>();
		this.power = power;
		this.type = type;
		this.customMiss = missChance;
		this.accuracyType = accuracy;
	}	
	
	public void addDamageComponent(Stats affinity, float power)
	{
		damageComponents.add(new DamageComponent(affinity, power));
	}
	
	@Override
	public State run(BattleEventBuilder builder)
	{
		
		PlayerCharacter attacker = builder.getSource();
		for(PlayerCharacter t : builder.getTargets())
		{			
			int dmg = BattleCalc.calculatedDamage(attacker, t, power, type, damageComponents, customMiss, accuracyType);
			
			
			if (dmg > 0)
			{
				//CALLS GENERIC CODE OH SHIT OH FUCK
				t.getOnDamagedEvent().trigger();
				//WARNING WARNING DANGER DANGER GAME STATE DESTROYED
			}
			
			
			switch(BattleCalc.getDmgReturnType())
			{
			case Blocked:
				builder.addMarker(new DamageMarker("BLOCK", t));	
				break;
			case Critical:
				Global.screenFader.setFadeColor(255, 255, 255, 255);
				Global.screenFader.flash(0.25f);
			case Hit:
				if(dmg >= 0 && !t.isEnemy())
					t.showDamaged();
				t.modifyHP(-dmg, false);
				builder.addMarker(new DamageMarker(-dmg, t));	
				break;
			case Miss:
				builder.addMarker(new DamageMarker("MISS", t));	
				break;
			}	
		}
		return State.Finished;
	}
	
	@Override
	public void runOutsideOfBattle(PlayerCharacter attacker, List<PlayerCharacter> targets, List<DamageMarker> markers)
	{
		for(PlayerCharacter t : targets)
		{
			int dmg = BattleCalc.calculatedDamage(attacker, t, power, type, damageComponents, customMiss, accuracyType);
			switch(BattleCalc.getDmgReturnType())
			{
			case Blocked:
				markers.add(new DamageMarker("BLOCK", t));	
				break;
			case Critical:
				markers.add(new DamageMarker("CRIT", t));	
			case Hit:
				t.modifyHP(-dmg, false);
				markers.add(new DamageMarker(-dmg, t));	
				break;
			case Miss:
				markers.add(new DamageMarker("MISS", t));	
				break;
			}	
		}
	}
	
	@Override
	public boolean willAffectTarget(PlayerCharacter target) 
	{		
		return target.isInBattle() && 
				((target.getHP() < target.getStat(Stats.MaxHP) 
				&& power < 0) || power >= 0); 
	}

}
