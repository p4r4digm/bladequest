package bladequest.world;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;
import android.util.Log;
import bladequest.battleactions.BattleAction;
import bladequest.bladescript.ParserException;
import bladequest.bladescript.ScriptVar;
import bladequest.combat.DamageComponent;
import bladequest.combat.DamageMarker;
import bladequest.graphics.BattleAnim;
import bladequest.graphics.WeaponSwing;
import bladequest.graphics.WeaponSwingDrawable;

public class Item 
{
	private String displayName, shortName, icon, description, shortDescription;
	private int count, useCount;
	private Type type;
	private int power, value;
	private boolean twoHanded, equipped, sellable;
	private int[] statMods;
	
	private String swingModel;
	private String swingAnim;
	private BattleAnim battleAnim;
	private int[] swingColorsBase, swingColorsSlash;
	private WeaponSwingDrawable weaponSwing;
	
	private static int id_ = 0;
	private int id;
	
	public String idName;
	
	private List<BattleAction> actions;
	private TargetTypes targetType;
	
	private List<DamageComponent> damageComponents;
	
	private  List<ScriptVar> onEquippedBattleStart;
	
	public Item(String idName, String name, Type type, String icon, String description)
	{
		this.idName = idName;
		this.displayName = name;
		this.icon = icon;
		this.description = description;
		count = 1;
		id = id_++;
		actions = new ArrayList<BattleAction>();
		statMods = new int[Stats.count()];
		damageComponents = new ArrayList<DamageComponent>();
		this.type = type;
		power = 0;
		equipped = false;
		useCount = 0;
		sellable = true;		
		onEquippedBattleStart = new ArrayList<ScriptVar>();
		twoHanded = false;
	}
	
	public Item(Item i)
	{
		this.idName = i.idName;
		this.id = i.id;
		this.displayName = i.displayName;
		this.description = i.description;
		this.shortDescription = i.shortDescription;
		this.icon = i.icon;
		this.count = 1;
		
		this.value = i.value;
		this.sellable = i.sellable;
		
		this.actions = i.actions;
		this.targetType = i.targetType;
		this.type = i.type;
		this.power = i.power;
		this.equipped = i.equipped;
		
		this.swingAnim = i.swingAnim;
		this.damageComponents = new ArrayList<DamageComponent>(i.damageComponents);
		
		this.twoHanded = i.twoHanded;
		
		//copy swing model data
		if(i.swingModel != null)
		{
			this.initSwingData(i.swingModel, i.swingAnim);
			for(int j = 0; j < 8; ++j)
			{
				this.swingColorsBase[j] = i.swingColorsBase[j];
				this.swingColorsSlash[j] = i.swingColorsSlash[j];
			}
			
		}
		
		//copy over stat mods
		statMods = new int[Stats.count()];	
		for(int j = 0; j < Stats.count(); ++j)
			statMods[j] = i.statMods[j];
		
		this.useCount = 0;
		this.onEquippedBattleStart = i.onEquippedBattleStart;
	}
	
	//merchant
	public int getValue() { return value; }
	public void setValue(int v) { value = v; }
	
	public boolean isSellable() { return sellable; }
	public void setSellable(boolean b) { sellable = b; }
	
	public boolean isWeapon()
	{
		return type == Type.Dagger || type == Type.Sword || type == Type.Staff;
	}
	
	public void setTwoHanded(boolean twoHanded)
	{
		this.twoHanded = twoHanded;
	}
	public boolean isTwoHanded()
	{
		return twoHanded;
	}
	
	public List<BattleAction> getActions() { return actions;}
	public void addAction(BattleAction action){actions.add(action);}	
	public String getDisplayName(){return displayName;}	
	public String getDescription(){return description;}
	public String getShortDescription() { return shortDescription == null ? description : shortDescription;}
	public void setShortDescription(String desc) { shortDescription = desc;}
	public String getShortName() { return shortName == null ? displayName : shortName;}
	public void setShortName(String desc) { shortName = desc;}
	public int getCount(){return count - useCount;}	
	public void setCount(int i){count = i; useCount = 0;}
	public int getId(){return id;}
	public Type getType(){return type;}
	public void clearUseCount() {useCount = 0;}
	public boolean isUsable(UseTypes useType)
	{
		switch(type)
		{
		case Usable:
			return true;
		case UsableBattleOnly:
			return useType == UseTypes.Battle;
		case UsableWorldOnly:
			return useType == UseTypes.World;
		case UsableSavePointOnly:
			return useType == UseTypes.World && Global.party.SavingAllowed();
		default:
			return false;
		
		}
	}
	
	public List<DamageComponent> getDamageComponents(){return damageComponents;}
	public void addDamageComponent(Stats affinity, float power){damageComponents.add(new DamageComponent(affinity, power));}
	
	public String getIcon() { return icon; }
	
	public void setTargetType(TargetTypes t){targetType = t;}
	public TargetTypes getTargetType() { return targetType; }
	
	public void setPower(int i){ power = i;}
	public int Power(){ return power; }
	
	public boolean isEquipped() { return equipped; }
	
	public void use(){useCount += 1;}
	public void unuse(){useCount = Math.max(0, useCount - 1);}
	public boolean isUsed() { return useCount >= count; }
	
	public void addStatMod(Stats stat, int amt){ statMods[stat.ordinal()] = amt;}
	public void addStatMod(int stat, int amt){ statMods[stat] = amt;}
	public int getStatMod(int stat){return statMods[stat]; }
	
	public String getAnimName(){return swingAnim;}
	
	
	
	public void initSwingData(String swingModel, String swingAnim)
	{
		this.swingModel = swingModel;
		this.swingAnim = swingAnim;
		swingColorsBase = new int[8];
		swingColorsSlash = new int[8];
	}
	
	public void swingColor(boolean slash, int index, int c)
	{
		(slash ? swingColorsSlash : swingColorsBase)[index] = c;
	}
	
	public void generateSwing()
	{
		if(weaponSwing != null)weaponSwing.release();	
		
		WeaponSwing model = Global.weaponSwingModels.get(swingModel);
		
		weaponSwing = model.genSwingDrawable(swingColorsBase, swingColorsSlash);
		battleAnim = weaponSwing.genAnim();
	}

	public void playAnimation(Point src, Point tar)
	{
		Global.playAnimation(battleAnim, src, tar);
	}
	
	public BattleAnim getAnim() { return battleAnim;}
	
	
	public WeaponSwingDrawable getSwing() { return weaponSwing; }
	
	public void equip(PlayerCharacter c) 
	{ 
		equipped = true; 
		for(int j = 0; j < Stats.count(); ++j)
			c.modStat(j, statMods[j]);

	}
	public void unequip(PlayerCharacter c) 
	{ 
		equipped = false; 
		for(int j = 0; j < Stats.count(); ++j)
			c.modStat(j, -statMods[j]);
	}
	
	public void executeOutOfBattle(PlayerCharacter attacker, List<PlayerCharacter> targets, List<DamageMarker> markers)
	{
		for(int i = 0; i < actions.size(); ++i)
			actions.get(i).runOutsideOfBattle(attacker, targets, markers);
	}
	
	public boolean willAffect(PlayerCharacter c)
	{
		for(BattleAction ba : actions)
			if(!ba.willAffectTarget(c))
				return false;
		
		return true;
	}
	
	public void addStartBattleEquippedScript(ScriptVar fn)
	{
		onEquippedBattleStart.add(fn);
	}
	public void runStartBattleScripts(PlayerCharacter c)
	{
		for (ScriptVar fn : onEquippedBattleStart)
		{
			try {
				fn.apply(ScriptVar.toScriptVar(Global.battle)).apply(ScriptVar.toScriptVar(c));
			} catch (ParserException e) {
				e.printStackTrace();
				Log.d("Parser", e.what());
			}
		}
	}
	
	public void modifyCount(int i)
	{
		count += i;
		if(count <= 0)
			count = 0;
		if(count > 99)
			count = 99;			
	}
	
	public enum UseTypes
	{
		Battle,
		World
	}
	
	public enum Type
	{
		Usable,
		UsableBattleOnly,
		UsableWorldOnly,
		UsableSavePointOnly,
		Dagger,
		Sword,
		Staff,
		Shield,
		Helmet,
		Torso,
		Accessory,
		Key
	}

}
