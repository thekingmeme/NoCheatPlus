package fr.neatmonster.nocheatplus.checks.moving;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.checks.access.CheckDataFactory;
import fr.neatmonster.nocheatplus.checks.access.ICheckData;
import fr.neatmonster.nocheatplus.utilities.ActionFrequency;

/*
 * M"""""`'"""`YM                   oo                   M""""""'YMM            dP            
 * M  mm.  mm.  M                                        M  mmmm. `M            88            
 * M  MMM  MMM  M .d8888b. dP   .dP dP 88d888b. .d8888b. M  MMMMM  M .d8888b. d8888P .d8888b. 
 * M  MMM  MMM  M 88'  `88 88   d8' 88 88'  `88 88'  `88 M  MMMMM  M 88'  `88   88   88'  `88 
 * M  MMM  MMM  M 88.  .88 88 .88'  88 88    88 88.  .88 M  MMMM' .M 88.  .88   88   88.  .88 
 * M  MMM  MMM  M `88888P' 8888P'   dP dP    dP `8888P88 M       .MM `88888P8   dP   `88888P8 
 * MMMMMMMMMMMMMM                                    .88 MMMMMMMMMMM                          
 *                                               d8888P                                       
 */
/**
 * Player specific data for the moving checks.
 */
public class MovingData extends ACheckData {

	/** The factory creating data. */
	public static final CheckDataFactory factory = new CheckDataFactory() {
		@Override
		public final ICheckData getData(final Player player) {
			return MovingData.getData(player);
		}

		@Override
		public ICheckData removeData(final String playerName) {
			return MovingData.removeData(playerName);
		}

		@Override
		public void removeAllData() {
			clear();
		}
	};

    /** The map containing the data per players. */
    private static Map<String, MovingData> playersMap = new HashMap<String, MovingData>();

    /**
     * Gets the data of a specified player.
     * 
     * @param player
     *            the player
     * @return the data
     */
    public static MovingData getData(final Player player) {
        if (!playersMap.containsKey(player.getName()))
            playersMap.put(player.getName(), new MovingData());
        return playersMap.get(player.getName());
    }

    public static ICheckData removeData(final String playerName) {
		return playersMap.remove(playerName);
	}
    
    public static void clear(){
    	playersMap.clear();
    }

	// Violation levels.
    public double         creativeFlyVL            = 0D;
    public double         morePacketsVL            = 0D;
    public double         morePacketsVehicleVL     = 0D;
    public double         noFallVL                 = 0D;
    public double         survivalFlyVL            = 0D;

    // Data shared between the fly checks.
    public int            bunnyhopDelay;
    public double         horizontalFreedom;
    public double         jumpAmplifier;
    public double         verticalFreedom;
    public double         horizontalVelocityCounter;
    public double         verticalVelocity;
    public int            verticalVelocityCounter;
    /** Last from coordinates. */
    public double         fromX = Double.MAX_VALUE, fromY, fromZ;
    /** Last to coordinates. */
    public double 		  toX = Double.MAX_VALUE, toY, toZ;
    /** To/from was ground or web or assumed to be etc. */
    public boolean		  toWasReset, fromWasReset;

    // Data of the creative check.
    public boolean        creativeFlyPreviousRefused;

    // Data of the more packets check.
    public int            morePacketsBuffer        = 50;
    public long           morePacketsLastTime;
    public int            morePacketsPackets;
    public Location       morePacketsSetback;

    // Data of the more packets vehicle check.
    public int            morePacketsVehicleBuffer = 50;
    public long           morePacketsVehicleLastTime;
    public int            morePacketsVehiclePackets;
    public Location       morePacketsVehicleSetback;

    // Data of the no fall check.
    public float          noFallFallDistance;
//    public boolean        noFallOnGround;
//    public boolean        noFallWasOnGround;
    /** Last y coordinate from when the player was on ground. */
    public double         noFallMaxY;
    /** Indicate that NoFall should assume the player to be on ground. */
    public boolean noFallAssumeGround;
    /** Indicate that NoFall is not to use next damage event for checking on-ground properties. */ 
    public boolean noFallSkipAirCheck = false;
    // Passable check.
    public double 	      passableVL;

	// Data of the survival fly check.
	public double 		sfHorizontalBuffer;
	public int 			sfJumpPhase;
	// public double survivalFlyLastFromY;
	/**
	 * Last valid y distance covered by a move. Integer.MAX_VALUE indicates "not set".
	 */
	public double		sfLastYDist = Double.MAX_VALUE;
	public int			sfFlyOnIce;
	public long			sfCobwebTime;
	public double		sfCobwebVL;
	public long			sfVLTime;
    
    // Accounting info.
    // TODO: optimize later.
    public final ActionFrequency hDistSum = new ActionFrequency(3, 333);
    public final ActionFrequency vDistSum = new ActionFrequency(3, 333);
    public final ActionFrequency hDistCount = new ActionFrequency(3, 333);
    public final ActionFrequency vDistCount = new ActionFrequency(3, 333);

    // Locations shared between all checks.
    public Location       setBack;
    public Location       teleported;

	/**
	 * Clear the data of the fly checks (not more-packets).
	 */
	public void clearFlyData() {
		bunnyhopDelay = 0;
		sfJumpPhase = 0;
		jumpAmplifier = 0;
		setBack = null;
		sfLastYDist = Double.MAX_VALUE;
		fromX = toX = Double.MAX_VALUE;
		clearAccounting();
		clearNoFallData();
		sfHorizontalBuffer = 0;
		toWasReset = fromWasReset = false; // TODO: true maybe
	}

	/**
	 * Mildly reset the flying data without losing any important information.
	 * 
	 * @param setBack
	 */
	public void onSetBack(final Location setBack) {
		// Reset positions
		resetPositions(teleported);
		this.setBack = teleported;
		clearAccounting(); // Might be more safe to do this.
		// Keep no-fall data.
		// Fly data: problem is we don't remember the settings for the set back location.
		// Assume the player to start falling from there rather, or be on ground.
		// TODO: Check if to adjust some counters to state before setback? 
		// Keep jump amplifier
		// Keep bunny-hop delay (?)
		// keep jump phase.
		sfHorizontalBuffer = Math.min(0, sfHorizontalBuffer);
		toWasReset = fromWasReset = false; // TODO: true maybe
	}

    public void clearAccounting() {
        final long now = System.currentTimeMillis();
        hDistSum.clear(now);
        vDistSum.clear(now);
        hDistCount.clear(now);
        vDistCount.clear(now);
    }

    /**
     * Clear the data of the more packets checks.
     */
    public void clearMorePacketsData() {
        morePacketsSetback = null;
        morePacketsVehicleSetback = null;
    }

    /**
     * Clear the data of the new fall check.
     */
    public void clearNoFallData() {
//        noFallOnGround = noFallWasOnGround = true;
        noFallFallDistance = 0;
        noFallMaxY = 0D;
        noFallSkipAirCheck = false;
    }
    
    public void resetPositions(final Location loc){
        if (loc == null) resetPositions(Double.MAX_VALUE, 0, 0);
        else resetPositions(loc.getX(), loc.getY(), loc.getZ());
    }

    public void resetPositions(final double x, final double y, final double z) {
        fromX = toX = x;
        fromY = toY = y;
        fromZ = toZ = z;
        sfLastYDist = Double.MAX_VALUE;
    }
}
