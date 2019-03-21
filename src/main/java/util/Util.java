package util;

import player.Player;
import processing.core.PApplet;
import processing.core.PImage;

import java.math.BigDecimal;

import static processing.core.PApplet.*;
import static util.Logging.LOGGER_MAIN;

public class Util {
    public static PApplet papplet = new PApplet();
    public static String RESOURCES_ROOT = "data/";
    public static String loadingName;
    public static PImage loadImage(String s) {
        return (new PApplet()).loadImage(s);
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

    public static float noise(float v) {
        return papplet.noise(v);
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
        for (int i=0; i<l.length; i++)
            c += l[i];
        return c;
    }

    public static void quitGame() {
        LOGGER_MAIN.info("Exitting game...");
        papplet.exit();
    }
}
