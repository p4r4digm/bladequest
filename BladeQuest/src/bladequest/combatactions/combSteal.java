package bladequest.combatactions;

import java.util.List;

import android.graphics.Point;
import bladequest.battleactions.BattleAction;
import bladequest.battleactions.bactChangePosition;
import bladequest.battleactions.bactChangeVisibility;
import bladequest.battleactions.bactJumpHome;
import bladequest.battleactions.bactRunAnimation;
import bladequest.battleactions.bactSetFace;
import bladequest.battleactions.bactSpecialPosition;
import bladequest.battleactions.bactWait;
import bladequest.combat.BattleEvent;
import bladequest.combat.BattleEventBuilder;
import bladequest.combat.DamageMarker;
import bladequest.enemy.Enemy;
import bladequest.graphics.BattleAnim;
import bladequest.graphics.BattleSprite.faces;
import bladequest.math.PointMath;
import bladequest.world.DamageTypes;
import bladequest.world.Global;
import bladequest.world.PlayerCharacter;
import bladequest.world.TargetTypes;


public class combSteal extends CombatAction 
{
	public combSteal()
	{
		name = "Steal";
		type = DamageTypes.Physical;
		targetType = TargetTypes.SingleEnemy;
		actionText = " tries to steal!";
	}
	
	@Override
	public String getDescription() { return "Attempt to steal from an enemy.";}
	
//	
//	public BattleAnim getJumpToAnimation(PlayerCharacter source, PlayerCharacter target)
//	{
//		BattleAnim anim = new BattleAnim(60.0f);
//		
//		BattleSprite playerSprite =source.getBattleSprite();
//		
//		BattleAnimObject baObj = new BattleAnimObject(Types.Bitmap, false, playerSprite.getBmpName());
//		Rect srcRect = playerSprite.getFrameRect(faces.Use, 0);
//		
//		final float height = 1.0f / 6.0f;
//		
//	    final int steps = 10;
//	    final int stepTime = 2;
//	    
//	    List<Point> points = PointMath.getArc(source.getPosition(), target.getPosition(), steps, height);
//	    
//	    int step = 0;
//	    
//	    for (Point p : points)
//	    {
//	        BattleAnimObjState state = new BattleAnimObjState(stepTime * step++, PosTypes.Screen);
//			
//			state.setBmpSrcRect(srcRect.left, srcRect.top, srcRect.right, srcRect.bottom);
//			state.argb(255, 255, 255, 255);
//			state.pos1 = p;
//			state.size = new Point(playerSprite.getWidth(), playerSprite.getHeight());
//			baObj.addState(state);
//	    }
//	    
//	    Collections.reverse(points);
//	    
//	    for (Point p : points)
//	    {
//	        BattleAnimObjState state = new BattleAnimObjState(stepTime * step++, PosTypes.Screen);
//			
//			state.setBmpSrcRect(srcRect.left, srcRect.top, srcRect.right, srcRect.bottom);
//			state.argb(255, 255, 255, 255);
//			state.pos1 = p;
//			state.size = new Point(playerSprite.getWidth(), playerSprite.getHeight());
//			baObj.addState(state);
//	    }
//	    
//		anim.addObject(baObj);
//		
//		return anim;
//	}
//	
	@Override
	public void buildEvents(BattleEventBuilder builder)
	{
		List<PlayerCharacter> targets = builder.getTargets();
		
		PlayerCharacter source = builder.getSource();
		PlayerCharacter target = targets.get(0);
		
		BattleAnim jumpAnim = PointMath.buildJumpAnimation(source, source.getPosition(true), target.getPosition(true), 1.0f/4.0f, 3);
		
		
		
		Point jumpToPoint = target.getPosition(true);
		PointMath.toTopLeft(jumpToPoint, source);

		
		builder.addEventObject(new bactSpecialPosition(true));
		builder.addEventObject(new bactSetFace(faces.Ready, 0));
		builder.addEventObject(new bactWait(BattleEvent.frameFromActIndex(3)).addDependency(builder.getLast()));
		BattleAction startSteal = builder.getLast();
		builder.addEventObject(new bactChangeVisibility(false).addDependency(startSteal));		
		builder.addEventObject(new bactRunAnimation(jumpAnim).addDependency(startSteal));
		builder.addEventObject(new bactChangePosition(jumpToPoint).addDependency(builder.getLast()));
		builder.addEventObject(new bactJumpHome(4.0f, 3).addDependency(builder.getLast()));
		builder.addEventObject(new bactSpecialPosition(false).addDependency(builder.getLast()));
		
		builder.addEventObject(new BattleAction()
		{
			public State run(BattleEventBuilder builder)
			{
				Enemy target = (Enemy)builder.getTargets().get(0);
				
				if(target.holdingItem())
				{
					String item = target.stealItem();
					if(item == null)
					{
						builder.addMarker(new DamageMarker("MISS", target));	
						Global.battle.setInfoBarText("Failed to steal!");
					}
					else
					{
						Global.party.addItem(item, 1);
						
						builder.addMarker(new DamageMarker("STEAL", target));
						Global.battle.setInfoBarText("Stole a "+ Global.items.get(item).getDisplayName() +"!");
					}
				}
				else
				{
					builder.addMarker(new DamageMarker("FAIL", target));
					Global.battle.setInfoBarText("Doesn't have anything!");
				}
				return State.Finished;
			}
		}.addDependency(builder.getLast()));
		
		
		builder.addEventObject(new bactWait(BattleEvent.frameFromActIndex(8)).addDependency(builder.getLast()));
	}

}
