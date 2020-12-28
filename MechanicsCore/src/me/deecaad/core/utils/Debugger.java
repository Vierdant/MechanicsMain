package me.deecaad.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Logger;

public class Debugger {

    // Which logger to use to log information,
    // no logical reason to be able to change this
    private final Logger logger;
    private int level;
    private boolean isPrintTraces;
    private int errors;

    public String msg = "MechanicsPlugin had %s error(s) in console.";
    public String permission = "mechanicscore.errorlog";
    public long updateTime = 300L;
    private BukkitRunnable warningTask;
    private boolean hasStarted;

    public Debugger(Logger logger, int level) {
        this(logger, level, false);
    }

    public Debugger(Logger logger, int level, boolean isPrintTraces) {
        this.logger = logger;
        this.level = level;
        this.isPrintTraces = isPrintTraces;

        warningTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (errors > 0) {
                    boolean alertedPlayer = false;

                    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                        if (!player.hasPermission(permission)) {
                            continue;
                        }

                        alertedPlayer = true;
                        player.sendMessage(ChatColor.RED + String.format(msg, errors));
                    }

                    if (alertedPlayer) {
                        errors = 0;
                    }
                }
            }
        };
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Gets the current logging level
     *
     * @return Integer logging level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets the logger level, should be called after
     * reloading the plugin that instantiated this class
     *
     * @param level The integer level to log at
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Determines if the given level can be logged
     *
     * @param level Level to test for
     * @return true if can be logged
     */
    public boolean canLog(LogLevel level) {
        return level.shouldPrint(this.level);
    }

    /**
     * Shorthand to log at debugging level
     *
     * @see LogLevel#DEBUG
     * @param msg Messages to log
     */
    public void debug(String...msg) {
        if (canLog(LogLevel.DEBUG)) log(LogLevel.DEBUG, msg);
    }

    /**
     * Shorthand to log at debugging level
     *
     * @see LogLevel#INFO
     * @param msg Messages to log
     */
    public void info(String...msg) {
        if (canLog(LogLevel.INFO)) log(LogLevel.INFO, msg);
    }

    /**
     * Shorthand to log at debugging level
     *
     * @see LogLevel#WARN
     * @param msg Messages to log
     */
    public void warn(String...msg) {
        if (canLog(LogLevel.WARN)) log(LogLevel.WARN, msg);
    }

    /**
     * Shorthand to log at debugging level
     *
     * @see LogLevel#ERROR
     * @param msg Messages to log
     */
    public void error(String...msg) {
        if (canLog(LogLevel.ERROR)) log(LogLevel.ERROR, msg);
    }

    /**
     * Logs all given messaged at the given
     * <code>LogLevel</code>. Each message is
     * logged on a new line.
     *
     * @param level The level to log at
     * @param msg The messages
     */
    public void log(LogLevel level, String...msg) {
        if (!canLog(level)) return;

        for (String str : msg) {
            logger.log(level.getParallel(), str);
        }

        // Used if we want to find the origin of an error
        if (level == LogLevel.ERROR) {
            if (isPrintTraces) {
                log(level, new Throwable());
            }

            errors++;
        }
    }

    /**
     * Logs an error. Useful for debugging and not showing
     * users error messages
     *
     * @param level The level to log at
     * @param error Error
     */
    public void log(LogLevel level, Throwable error) {
        if (!canLog(level)) return;

        logger.log(level.getParallel(), "", error);
    }

    /**
     * Logs the given error and gives the user the
     * given message.
     *
     * @param level Level to log at
     * @param msg Message to send
     * @param error Error
     */
    public void log(LogLevel level, String msg, Throwable error) {
        if (!canLog(level)) return;

        logger.log(level.getParallel(), msg, error);
    }

    /**
     * Shorthand for asserting true with an <code>ERROR</code>
     * <code>LoggingLevel</code>.
     *
     * @param bool What to assert
     * @param messages Messages to log if assertion failed
     */
    public void validate(boolean bool, String...messages) {
        if (!bool) {
            log(LogLevel.ERROR, messages);
        }
    }

    /**
     * Easy way to assert variables cleanly. If <code>bool</code>
     * is true, than there is no error, the method exits. If it
     * is false, than there is an error, and the given messages
     * should be logged at the given level so the user can be
     * aware of possible errors.
     *
     * @param level Level to log at
     * @param bool What to assert
     * @param messages Messages to log if assertion failed
     */
    public void validate(LogLevel level, boolean bool, String...messages) {
        if (!bool) {
            log(level, messages);
        }
    }

    public synchronized void start(Plugin plugin) {
        if (hasStarted) return;

        warningTask.runTaskTimerAsynchronously(plugin, 10L, updateTime);

        hasStarted = true;
    }
}
