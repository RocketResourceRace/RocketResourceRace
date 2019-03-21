package util;

import json.JSONManager;
import player.Player;
import processing.core.PApplet;
import processing.core.PImage;
import processing.sound.SoundFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.logging.Level;

import static processing.core.PApplet.*;
import static util.Logging.LOGGER_MAIN;

public class Util {
    public static PApplet papplet;
    public static String RESOURCES_ROOT = "data/";
    public static String loadingName;
    private static HashMap<String, SoundFile> sfx;

    public static PImage loadImage(String s) {
        return papplet.loadImage(s);
    }

    public static float between(float lower, float v, float upper) {
        return max(min(upper, v), lower);
    }

    public static int color(float r, float g, float b) {
        return papplet.color(r, g, b);
    }

    public static int brighten(int old, int off) {
        return color(between(0, red(old)+off, 255), between(0, green(old)+off, 255), between(0, blue(old)+off, 255));
    }

    public static float red(int old) {
        return papplet.red(old);
    }

    public static float green(int old) {
        return papplet.green(old);
    }

    public static float blue(int old) {
        return papplet.blue(old);
    }

    public static String roundDp(String val, int dps) {
        return (new BigDecimal(""+val).divide(new BigDecimal("1"), dps, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString();
    }

    public static String roundDpTrailing(String val, int dps){
        return (new BigDecimal(""+val).divide(new BigDecimal("1"), dps, BigDecimal.ROUND_HALF_EVEN)).toPlainString();
    }

    public static float noise(float v1, float v2) {
        return papplet.noise(v1, v2);
    }

    public static float noise(float v1, float v2, float v3) {
        return papplet.noise(v1, v2, v3);
    }

    public static float random(float v1) {
        return papplet.random(v1);
    }
    public static float random(float v1, float v2) {
        return papplet.random(v1, v2);
    }

    public static void noiseSeed(long v) {
        papplet.noiseSeed(v);
    }
    public static void noiseDetail(int v, float f) {
        papplet.noiseDetail(v, f);
    }


    public static boolean playerExists(Player[] players, String name) {
        for (Player p: players) {
            if (p.name.trim().equals(name.trim())){
                return true;
            }
        }
        return false;
    }


    public static Player getPlayer(Player[] players, String name){
        for (Player p: players) {
            if (p.name.trim().equals(name.trim())){
                return p;
            }
        }
        LOGGER_MAIN.severe(String.format("Tried to find player: %s but this player was not found. Returned first player, this will likely cause problems", name));
        return players[0];
    }



    public static float sum(float[] l) {
        float c=0;
        for (float v : l) c += v;
        return c;
    }

    public static void quitGame() {
        LOGGER_MAIN.info("Exitting game...");
        papplet.exit();
    }
    public static void loadSounds() {
        try {
            if (JSONManager.loadBooleanSetting("sound on")) {
                sfx = new HashMap<>();
                sfx.put("click3", new SoundFile(papplet, RESOURCES_ROOT +"wav/click3.wav"));
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Something wrong with loading sounds", e);
            throw e;
        }
    }

    public static void setFrameRateCap() {
        LOGGER_MAIN.finer("Setting framerate cap");
        if (JSONManager.loadBooleanSetting("framerate cap")) {
            papplet.frameRate(60);
        } else {
            papplet.frameRate(1000);
        }
    }

    public static void setVolume() {
        try {
            for (SoundFile fect : sfx.values()) {
                fect.amp(JSONManager.loadFloatSetting("volume"));
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Something wrong with setting volume", e);
            throw e;
        }
    }
}
