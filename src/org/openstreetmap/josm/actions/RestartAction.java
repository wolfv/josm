// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Restarts JOSM as it was launched. Comes from "restart" plugin, originally written by Upliner.
 * <br><br>
 * Mechanisms have been improved based on #8561 discussions and
 * <a href="http://lewisleo.blogspot.jp/2012/08/programmatically-restart-java.html">this article</a>.
 * @since 5857
 */
public class RestartAction extends JosmAction {

    // AppleScript to restart OS X package
    private static final String RESTART_APPLE_SCRIPT =
              "tell application \"System Events\"\n"
            + "repeat until not (exists process \"JOSM\")\n"
            + "delay 0.2\n"
            + "end repeat\n"
            + "end tell\n"
            + "tell application \"JOSM\" to activate";

    /**
     * Constructs a new {@code RestartAction}.
     */
    public RestartAction() {
        super(tr("Restart"), "restart", tr("Restart the application."),
                Shortcut.registerShortcut("file:restart", tr("File: {0}", tr("Restart")), KeyEvent.VK_J, Shortcut.ALT_CTRL_SHIFT), false);
        putValue("help", ht("/Action/Restart"));
        putValue("toolbar", "action/restart");
        Main.toolbar.register(this);
        setEnabled(isRestartSupported());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // If JOSM has been started with property 'josm.restart=true' this means
        // it is executed by a start script that can handle restart.
        // Request for restart is indicated by exit code 9.
        String scriptRestart = System.getProperty("josm.restart");
        if ("true".equals(scriptRestart)) {
            Main.exitJosm(true, 9);
        }

        try {
            restartJOSM();
        } catch (IOException ex) {
            Main.error(ex);
        }
    }

    /**
     * Determines if restarting the application should be possible on this platform.
     * @return {@code true} if the mandatory system property {@code sun.java.command} is defined, {@code false} otherwise.
     * @since 5951
     */
    public static boolean isRestartSupported() {
        return System.getProperty("sun.java.command") != null;
    }

    /**
     * Restarts the current Java application
     * @throws IOException in case of any error
     */
    public static void restartJOSM() throws IOException {
        if (isRestartSupported() && !Main.exitJosm(false, 0)) return;
        final List<String> cmd;
        try {
            // special handling for OSX .app package
            if (Main.isPlatformOsx() && System.getProperty("java.library.path").contains("/JOSM.app/Contents/MacOS")) {
                cmd = getAppleCommands();
            } else {
                cmd = getCommands();
            }
            Main.info("Restart "+cmd);
            if (Main.isDebugEnabled() && Main.pref.getBoolean("restart.debug.simulation")) {
                Main.debug("Restart cancelled to get debug info");
                return;
            }
            // execute the command in a shutdown hook, to be sure that all the
            // resources have been disposed before restarting the application
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));
                    } catch (IOException e) {
                        Main.error(e);
                    }
                }
            });
            // exit
            System.exit(0);
        } catch (Exception e) {
            // something went wrong
            throw new IOException("Error while trying to restart the application", e);
        }
    }

    private static List<String> getAppleCommands() {
        final List<String> cmd = new ArrayList<>();
        cmd.add("/usr/bin/osascript");
        for (String line : RESTART_APPLE_SCRIPT.split("\n")) {
            cmd.add("-e");
            cmd.add(line);
        }
        return cmd;
    }

    private static List<String> getCommands() throws IOException {
        final List<String> cmd = new ArrayList<>();
        // java binary
        cmd.add(getJavaRuntime());
        // vm arguments
        addVMArguments(cmd);
        // Determine webstart JNLP file. Use jnlpx.origFilenameArg instead of jnlp.application.href,
        // because only this one is present when run from j2plauncher.exe (see #10795)
        final String jnlp = System.getProperty("jnlpx.origFilenameArg");
        // program main and program arguments (be careful a sun property. might not be supported by all JVM)
        final String javaCommand = System.getProperty("sun.java.command");
        String[] mainCommand = javaCommand.split(" ");
        if (javaCommand.endsWith(".jnlp") && jnlp == null) {
            // see #11751 - jnlp on Linux
            if (Main.isDebugEnabled()) {
                Main.debug("Detected jnlp without jnlpx.origFilenameArg property set");
            }
            cmd.addAll(Arrays.asList(mainCommand));
        } else {
            // look for a .jar in all chunks to support paths with spaces (fix #9077)
            StringBuilder sb = new StringBuilder(mainCommand[0]);
            for (int i = 1; i < mainCommand.length && !mainCommand[i-1].endsWith(".jar"); i++) {
                sb.append(' ').append(mainCommand[i]);
            }
            String jarPath = sb.toString();
            // program main is a jar
            if (jarPath.endsWith(".jar")) {
                // if it's a jar, add -jar mainJar
                cmd.add("-jar");
                cmd.add(new File(jarPath).getPath());
            } else {
                // else it's a .class, add the classpath and mainClass
                cmd.add("-cp");
                cmd.add("\"" + System.getProperty("java.class.path") + "\"");
                cmd.add(mainCommand[0]);
            }
            // add JNLP file.
            if (jnlp != null) {
                cmd.add(jnlp);
            }
        }
        // finally add program arguments
        cmd.addAll(Main.getCommandLineArgs());
        return cmd;
    }

    private static String getJavaRuntime() throws IOException {
        final String java = System.getProperty("java.home") + File.separator + "bin" + File.separator +
                (Main.isPlatformWindows() ? "java.exe" : "java");
        if (!new File(java).isFile()) {
            throw new IOException("Unable to find suitable java runtime at "+java);
        }
        return java;
    }

    private static void addVMArguments(Collection<String> cmd) {
        List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        if (Main.isDebugEnabled()) {
            Main.debug("VM arguments: "+arguments);
        }
        for (String arg : arguments) {
            // When run from jp2launcher.exe, jnlpx.remove is true, while it is not when run from javaws
            // Always set it to false to avoid error caused by a missing jnlp file on the second restart
            arg = arg.replace("-Djnlpx.remove=true", "-Djnlpx.remove=false");
            // if it's the agent argument : we ignore it otherwise the
            // address of the old application and the new one will be in conflict
            if (!arg.contains("-agentlib")) {
                cmd.add(arg);
            }
        }
    }

    /**
     * Returns a new {@code ButtonSpec} instance that performs this action.
     * @return A new {@code ButtonSpec} instance that performs this action.
     */
    public static ButtonSpec getRestartButtonSpec() {
        return new ButtonSpec(
                tr("Restart"),
                ImageProvider.get("restart"),
                tr("Restart the application."),
                ht("/Action/Restart"),
                isRestartSupported()
        );
    }

    /**
     * Returns a new {@code ButtonSpec} instance that do not perform this action.
     * @return A new {@code ButtonSpec} instance that do not perform this action.
     */
    public static ButtonSpec getCancelButtonSpec() {
        return new ButtonSpec(
                tr("Cancel"),
                ImageProvider.get("cancel"),
                tr("Click to restart later."),
                null /* no specific help context */
        );
    }

    /**
     * Returns default {@code ButtonSpec} instances for this action (Restart/Cancel).
     * @return Default {@code ButtonSpec} instances for this action.
     * @see #getRestartButtonSpec
     * @see #getCancelButtonSpec
     */
    public static ButtonSpec[] getButtonSpecs() {
        return new ButtonSpec[] {
                getRestartButtonSpec(),
                getCancelButtonSpec()
        };
    }
}
