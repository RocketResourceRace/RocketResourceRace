package util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Logging {
    public static final Logger LOGGER_MAIN = Logger.getLogger("RocketResourceRaceMain"); // Most logs belong here INCLUDING EXCEPTION LOGS. Also I have put saving logs here rather than game
    public static final Logger LOGGER_GAME = Logger.getLogger("RocketResourceRaceGame"); // For game algorithm related logs (not exceptions here, just things like party moving or ai making decision)
    public static final Level FILELOGLEVEL = Level.FINEST;
}
