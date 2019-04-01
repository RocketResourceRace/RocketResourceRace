package util;

import processing.core.PFont;

import java.util.HashMap;
import java.util.logging.Level;

import static processing.core.PApplet.round;
import static util.Logging.LOGGER_MAIN;
import static util.Util.papplet;

public class Font {
    private static HashMap<Integer, PFont> fonts = new HashMap<>();
    public static PFont getFont(float size) {
        try {
            PFont f=fonts.get(round(size));
            if (f == null) {
                fonts.put(round(size), papplet.createFont("GillSans", size));
                return fonts.get(round(size));
            } else {
                return f;
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Something wrong with loading font", e);
            throw e;
        }
    }
}
