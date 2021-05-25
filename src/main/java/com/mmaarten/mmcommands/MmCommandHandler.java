/*
    Copyright (C) 2021 Maarten Magits

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.mmaarten.mmcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * The MmCommandHandler is an implementation of {@link TabExecutor}
 * which takes care of the repetitive tasks.
 * <p>
 * Use {@link org.bukkit.command.PluginCommand#setExecutor(CommandExecutor)}
 * to link an MmCommandHandler to your base command.
 * <p>
 * created on 29/04/2021 by Mmaarten. Project: MmCommands
 */
@SuppressWarnings("unused")
public class MmCommandHandler implements TabExecutor {
    private final static @NotNull List<String> NO_COMPLETIONS = new ArrayList<>();

    private final @NotNull List<MmCommand> commands;
    private final int commandsPerPage;
    private final boolean useHelp;
    private final boolean generateTabCompletions;
    private final boolean runLastAllowed;
    private final @NotNull String helpHeader;
    private final @NotNull String helpCommandPrefix;
    private final @NotNull String helpCommandArgumentSpacer;
    private final @NotNull String helpArgumentDescriptionSpacer;
    private final @NotNull String helpPropertyPrefix;
    private final @NotNull String helpPropertyValueSpacer;

    private MmCommandHandler(List<MmCommand> commands, int commandsPerPage,
                     boolean useHelp, boolean generateTabCompletions,
                     boolean runLastAllowed, String helpHeader,
                     String helpCommandPrefix,
                     String helpCommandArgumentSpacer,
                     String helpArgumentDescriptionSpacer,
                     String helpPropertyPrefix, String helpPropertyValueSpacer) {
        this.commands = commands;
        this.commandsPerPage = commandsPerPage;
        this.useHelp = useHelp;
        this.generateTabCompletions = generateTabCompletions;
        this.runLastAllowed = runLastAllowed;
        this.helpHeader = helpHeader;
        this.helpCommandPrefix = helpCommandPrefix;
        this.helpCommandArgumentSpacer = helpCommandArgumentSpacer;
        this.helpArgumentDescriptionSpacer = helpArgumentDescriptionSpacer;
        this.helpPropertyPrefix = helpPropertyPrefix;
        this.helpPropertyValueSpacer = helpPropertyValueSpacer;
    }

    /*
     * EXECUTION
     */

    /**
     * Executes the given command, returning its success.
     * <br>
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true
     */
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String[] args
    ) {
        // No subcommand provided
        if (args.length == 0) {
            if (noArguments(sender)) {
                printHelp(sender, label, new ArrayList<>(this.commands), 0);
            }
            return true;
        }

        // Check for the "help" subcommand
        if (useHelp) {
            if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                printHelp(sender, label, new ArrayList<>(this.commands), args);
                return true;
            }
            if (args.length > 1) {
                @Nullable MmCommand cmd = getCommand(args[1]);
                if (cmd == null) {
                    printHelp(sender, label, new ArrayList<>(this.commands), args);
                    return true;
                }
                printPerCommandHelp(sender, label, cmd);
                return true;
            }
        }

        // Find matching command
        @Nullable MmCommand cmd = getCommand(args[0]);
        if (cmd == null) {
            if (noSuchCommand(sender, args[0]))
                printHelp(sender, label, new ArrayList<>(this.commands), 0);
            return true;
        }

        // Check type and permissions
        if (failsChecks(sender, cmd, label, true)) return true;

        // Find matching subcommand
        SubCommandWrapper wrapper = getSubCommand(Arrays.copyOfRange(args, 1, args.length), cmd, sender, label, !this.runLastAllowed);
        int i = wrapper.getDepth();
        cmd = wrapper.getSub();
        if (cmd == null) return true;

        // Remove first parameter and pass
        // the rest to the matched command
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments = arguments.subList(Math.min(i + 1, args.length), args.length);
        cmd.onCommand(sender, command, label, arguments.toArray(new String[0]));
        return true;
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside of a command block, this will be the player, not
     *                the command block.
     * @param command Command which was executed
     * @param label   The alias used
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed and command label
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String[] args
    ) {
        if (args.length == 1) {
            List<String> completions = getAllowedCommands(sender, label)
                    .stream()
                    .map(mmCommand -> mmCommand.getClass().getAnnotation(MmCommandSignature.class).name())
                    .collect(Collectors.toList());
            if (this.useHelp) completions.add("help");
            return completions;
        }


        if (this.useHelp && args.length == 2 && args[0].equalsIgnoreCase("help"))
            return getAllowedCommands(sender, label)
                    .stream()
                    .map(mmCommand -> mmCommand.getClass().getAnnotation(MmCommandSignature.class).name())
                    .collect(Collectors.toList());


        // Find matching subcommand
        @Nullable MmCommand cmd = getCommand(args[0]);
        if (cmd == null) {
            return MmCommandHandler.NO_COMPLETIONS;
        }

        // Check type and permissions
        if (failsChecks(sender, cmd, label)) return MmCommandHandler.NO_COMPLETIONS;

        // Check current subcommand
        SubCommandWrapper wrapper = getSubCommand(Arrays.copyOfRange(args, 1, args.length), cmd, sender, label, false);
        int i = wrapper.getDepth();
        cmd = wrapper.getSub();
        if (cmd == null) return MmCommandHandler.NO_COMPLETIONS;

        // Pass arguments
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments.subList(Math.min(i + 1, args.length), args.length);

        // Append future subcommands if needed and allowed
        List<String> completions = cmd.onTabComplete(sender, command, label, args);
        if (this.generateTabCompletions && i == args.length - 2) {
            if (completions == null) completions = new ArrayList<>();
            completions.addAll(cmd.getSubcommands()
                    .stream()
                    .filter(mmCommand -> !failsChecks(sender, mmCommand, label))
                    .map(mmCommand -> mmCommand.getClass().getAnnotation(MmCommandSignature.class).name())
                    .collect(Collectors.toList())
            );
        }
        return completions;
    }

    private boolean failsChecks(
            @NotNull CommandSender sender,
            @NotNull MmCommand cmd,
            @NotNull String label
    ) {
        return failsChecks(sender, cmd, label, false);
    }

    private boolean failsChecks(
            @NotNull CommandSender sender,
            @NotNull MmCommand cmd,
            @NotNull String label,
            boolean displayHelp
    ) {
        MmCommandSignature info = cmd.getClass().getAnnotation(MmCommandSignature.class);
        return failsType(sender, cmd, label, displayHelp) || failsPermission(sender, cmd, label, displayHelp);
    }

    private boolean failsType(
            @NotNull CommandSender sender,
            @NotNull MmCommand cmd,
            @NotNull String label,
            boolean displayHelp
    ) {
        MmCommandSignature info = cmd.getClass().getAnnotation(MmCommandSignature.class);

        switch (info.type()) {
            case PLAYER_ONLY:
                if (!(sender instanceof Player)) {

                    if (displayHelp && noValidType(sender, info))
                        printPerCommandHelp(sender, label, cmd);
                    return true;
                }
                break;
            case CONSOLE_ONLY:
                if (!(sender instanceof ConsoleCommandSender)) {
                    if (displayHelp && noValidType(sender, info))
                        printPerCommandHelp(sender, label, cmd);
                    return true;
                }
                break;
            case PLAYER_CONSOLE:
                if (!(sender instanceof ConsoleCommandSender) && !(sender instanceof Player)) {
                    if (displayHelp && noValidType(sender, info))
                        printPerCommandHelp(sender, label, cmd);
                    return true;
                }
        }
        return false;
    }

    private boolean failsPermission(
            @NotNull CommandSender sender,
            @NotNull MmCommand cmd,
            @NotNull String label,
            boolean displayHelp
    ) {
        MmCommandSignature info = cmd.getClass().getAnnotation(MmCommandSignature.class);
        if (!sender.hasPermission(info.permission())) {
            if (displayHelp && noPermission(sender, info))
                printPerCommandHelp(sender, label, cmd);
            return true;
        }
        return false;
    }

    //done
    private @Nullable MmCommand getCommand(@NotNull String name) {
        return this.commands
                .stream()
                .filter(filterCmd -> {
                    MmCommandSignature sig = filterCmd.getClass().getAnnotation(MmCommandSignature.class);
                    boolean isName = sig.name().equalsIgnoreCase(name);
                    boolean isAlias = Arrays.stream(sig.aliases()).anyMatch(alias -> alias.equalsIgnoreCase(name));
                    return isName || isAlias;
                })
                .findAny()
                .orElse(null);
    }

    //done
    private @NotNull List<MmCommand> getAllowedCommands(
            @NotNull CommandSender sender,
            @NotNull String label
    ) {
        return this.commands.stream().filter(command -> !failsChecks(sender, command, label)).collect(Collectors.toList());
    }

    //needs testing
    private @NotNull SubCommandWrapper getSubCommand(
            @NotNull String[] args,
            @NotNull MmCommand cmd,
            @NotNull CommandSender sender,
            @NotNull String label
    ) {
        return getSubCommand(args, cmd, sender, label, true);
    }

    //needs testing
    private @NotNull SubCommandWrapper getSubCommand(
            @NotNull String[] args,
            @NotNull MmCommand cmd,
            @NotNull CommandSender sender,
            @NotNull String label,
            boolean displayHelp
    ) {
        return getSubCommand(args, cmd, sender, label, displayHelp, 0);
    }

    //done
    private @NotNull SubCommandWrapper getSubCommand(
            @NotNull String[] args,
            @NotNull MmCommand cmd,
            @NotNull CommandSender sender,
            @NotNull String label,
            boolean displayHelp,
            int currentLevel
    ) {
        // last argument has been matched
        if (args.length == 0)
            return new SubCommandWrapper(cmd, currentLevel);

        // match next argument with subcommands
        // checking for both names and aliases
        MmCommand temp = cmd.getSubcommands().stream().filter(sub -> {
            MmCommandSignature subInfo = sub.getClass().getAnnotation(MmCommandSignature.class);
            return subInfo.name().equalsIgnoreCase(args[0]) ||
                    Arrays.stream(subInfo.aliases()).anyMatch(alias -> alias.equalsIgnoreCase(args[0]));
        })
                .findAny()
                .orElse(null);

        // next argument is not a subcommand
        // treating it as a regular argument
        if (temp == null)
            return new SubCommandWrapper(cmd, currentLevel);

        // argument is matched as a subcommand
        // but sender does not have permission
        if (failsPermission(sender, temp, label, displayHelp)) {
            if (!this.runLastAllowed)
                return new SubCommandWrapper(null, currentLevel);
            return new SubCommandWrapper(cmd, currentLevel);
        }

        // check next argument
        return getSubCommand(Arrays.copyOfRange(args, 1, args.length), temp, sender, label, displayHelp, currentLevel + 1);
    }

    //done
    private void printHelp(
            @NotNull CommandSender commandSender,
            @NotNull String label,
            @NotNull List<MmCommand> commands,
            String[] args
    ) {
        int pageNr = 0;
        if (args.length >= 2) {
            try {
                pageNr = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        printHelp(commandSender, label, commands, pageNr);
    }

    //done
    private @NotNull List<String> generateHelpStrings(
            @NotNull CommandSender sender,
            @NotNull MmCommand currentCmd,
            @NotNull String label,
            @NotNull String prefix
    ) {
        MmCommandSignature current = currentCmd.getClass().getAnnotation(MmCommandSignature.class);
        prefix = prefix + " " + current.name();
        List<String> results = new ArrayList<>();

        if (!current.description().equalsIgnoreCase("") || currentCmd.getSubcommands().size() == 0) {
            String b = this.helpCommandPrefix +
                    prefix +
                    (current.arguments().equals("") ? "" : this.helpCommandArgumentSpacer) +
                    current.arguments() +
                    this.helpArgumentDescriptionSpacer +
                    current.description();
            results.add(b);
        }
        for (MmCommand sub : currentCmd.getSubcommands()) {
            if (!failsPermission(sender, sub, label, false))
                results.addAll(generateHelpStrings(sender, sub, label, prefix));
        }
        return results;
    }

    /*
     * CUSTOMIZABLE
     */

    /**
     * Code to be ran when a player executes the
     * base command this handler is linked to
     * without any arguments.
     * <p>
     * The return value determines whether or not
     * the help menu should be printed.
     *
     * @param sender the sender
     * @return whether or not to print the help menu to the player
     */
    protected boolean noArguments(@NotNull CommandSender sender) {
        return true;
    }

    /**
     * Code to be ran when a player executes the
     * base command this handler is linked to
     * with arguments but the first argument
     * cannot be resolved to a {@link MmCommandSignature#name() command}
     * or to an {@link MmCommandSignature#aliases() alias}.
     * <p>
     * The return value determines whether or not
     * the help menu should be printed.
     *
     * @param sender           the sender
     * @param attemptedCommand the command the player attempted to invoke (first argument)
     * @return whether or not to print the help menu to the player
     */
    protected boolean noSuchCommand(
            @NotNull CommandSender sender,
            @NotNull String attemptedCommand
    ) {
        return true;
    }

    /**
     * Code to be ran when a player executes a command they
     * are not allowed to run because of the
     * {@link MmCommandSignature#type() commandtype}.
     * This will only be called on commands directly registered
     * to this handler as any subcommand is of type {@link MmCommandType#SUB_COMMAND}
     * which poses no restrictions.
     * <p>
     * The return value determines whether or not
     * the help menu for the command should be printed.
     *
     * @param sender    the sender
     * @param signature the signature of the invoked command
     * @return whether or not to print the command help menu to the player
     */
    protected boolean noValidType(@NotNull CommandSender sender, @NotNull MmCommandSignature signature) {
        return true;
    }

    /**
     * Code to be ran when a player executes a command
     * they do not have the required permission node for.
     * <p>
     * The return value determines whether or not
     * the help menu for the command should be printed.
     *
     * @param sender    the sender
     * @param signature the signature of the invoked command
     * @return whether or not to print the command help menu to the player
     */
    protected boolean noPermission(@NotNull CommandSender sender, @NotNull MmCommandSignature signature) {
        return true;
    }

    /**
     * Print the help menu to the provided commandsender.
     * The help menu consists of the {@link MmCommandHandler.Builder#setHelpHeader(String) header}
     * followed by {@link MmCommandHandler.Builder#setAmountOfCommandsPerHelpPage(int)
     * a specified amount} of help entries.
     * Each entry follows this pattern:
     * {@link MmCommandHandler.Builder#setHelpCommandPrefix(String) commandPrefix} {@code /label [subcommand(s)]}
     * {@link MmCommandHandler.Builder#setHelpArgumentSpacer(String) argumentSpacer} {@link MmCommandSignature#arguments() arguments}
     * {@link MmCommandHandler.Builder#setHelpDescriptionSpacer(String) descriptionSpacer} {@link MmCommandSignature#description() description}
     * <p>
     * A help entry will be added for all {@link MmCommand}s that match at least one of the following conditions:
     * <ul>
     *     <li>The command signature has a non-empty string for the descriptions property</li>
     *     <li>The command does not have any subcommands</li>
     * </ul>
     *
     * @param commandSender the command sender
     * @param label         the label
     * @param commands      the commands
     * @param page          the page to be displayed. Zero based.
     */
    protected void printHelp(
            @NotNull CommandSender commandSender,
            @NotNull String label,
            @NotNull List<MmCommand> commands,
            int page
    ) {
        List<MmCommand> allowedCommands = getAllowedCommands(commandSender, label);
        List<String> helpMenu = new ArrayList<>();
        String prefix = this.helpCommandPrefix + "/" + label;
        for (MmCommand allowedCommand : allowedCommands) {
            helpMenu.addAll(generateHelpStrings(commandSender, allowedCommand, label, prefix));
        }

        page = Math.max(page, 0);
        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.helpHeader.replace("%page%", String.valueOf(page))));
        for (int index = page * commandsPerPage; index < Math.min((page + 1) * commandsPerPage, helpMenu.size()); index++) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMenu.get(index)));
        }

    }

    /**
     * Print the help menu to the provided commandsender.
     * The help menu consists of the {@link MmCommandHandler.Builder#setHelpHeader(String) header}
     * followed by a summary of the associated {@link MmCommandSignature signature}
     * followed by all help entries related to the provided basecommand
     * Each entry follows this pattern:
     * {@link MmCommandHandler.Builder#setHelpCommandPrefix(String) commandPrefix} {@code /label [subcommand(s)]}
     * {@link MmCommandHandler.Builder#setHelpArgumentSpacer(String) argumentSpacer} {@link MmCommandSignature#arguments() arguments}
     * {@link MmCommandHandler.Builder#setHelpDescriptionSpacer(String) descriptionSpacer} {@link MmCommandSignature#description() description}
     * <p>
     * A help entry will be added for the {@link MmCommand} and its subcommands that match at least one of the following conditions:
     * <ul>
     *     <li>The command signature has a non-empty string for the description property</li>
     *     <li>The command does not have any subcommands</li>
     * </ul>
     *
     * @param sender the command sender
     * @param label  the label
     * @param cmd    the command to print the help menu for
     */
    protected void printPerCommandHelp(
            @NotNull CommandSender sender,
            @NotNull String label,
            @NotNull MmCommand cmd
    ) {
        MmCommandSignature sig = cmd.getClass().getAnnotation(MmCommandSignature.class);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.helpHeader).replace("%page", "0"));
        String summary = String.format("%sName%s%s\n", this.helpPropertyPrefix, this.helpPropertyValueSpacer, sig.name());
        summary += String.format("%sAliases%s%s\n", this.helpPropertyPrefix, this.helpPropertyValueSpacer, Arrays.toString(sig.aliases()));
        summary += String.format("%sType%s%s\n", this.helpPropertyPrefix, this.helpPropertyValueSpacer, sig.type());
        summary += String.format("%sPermission%s%s\n", this.helpPropertyPrefix, this.helpPropertyValueSpacer, sig.permission());
        summary += String.format("%sDescription%s%s\n", this.helpPropertyPrefix, this.helpPropertyValueSpacer, sig.description());

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', summary));
        for (String line : generateHelpStrings(sender, cmd, label, "/" + label)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }


    private static class SubCommandWrapper {
        private final @Nullable MmCommand sub;
        private final int depth;

        /**
         * Instantiates a new Sub command wrapper.
         *
         * @param sub   the sub
         * @param depth the depth
         */
        public SubCommandWrapper(
                @Nullable MmCommand sub,
                int depth
        ) {
            this.sub = sub;
            this.depth = depth;
        }

        /**
         * Gets the subcommand.
         *
         * @return the sub
         */
        public @Nullable MmCommand getSub() {
            return sub;
        }

        /**
         * Gets the depth at which the subcommand was found.
         *
         * @return the depth
         */
        public int getDepth() {
            return depth;
        }
    }

    /**
     * <p>
     *     Use this builder to construct an {@link MmCommandHandler} instance.
     *     To construct an MmCommandHandler instance with default configuration,
     *     simply use: {@code new MmCommandHandler.Builder().build()}
     * </p>
     * <p>
     *     The following shows an example of how to use the
     *     {@link MmCommandHandler.Builder Builder} to construct an
     *     {@link MmCommandHandler} instance:
     * </p>
     * <pre>
     * MmCommandHandler mmCommandHandler = new MmCommandHandler.Builder()
     *     .enableHelpSubCommand()
     *     .generateTabCompletions()
     *     .runLastAllowed()
     *     .setHelpHeader("--------------Help---------------")
     *     .setAmountOfCommandsPerHelpPage(15)
     *     .build();
     * </pre>
     * <p>
     *     NOTE: the order of invocation of the configuration methods does not
     *     matter.
     * </p>
     * @author Mmaarten
     * @author TheSummerGrinch
     */
    public static class Builder {
        private final @NotNull List<MmCommand> commands = new ArrayList<>();
        private int commandsPerPage = 5;
        private boolean useHelp = false;
        private boolean generateTabCompletions = false;
        private boolean runLastAllowed = false;
        @NotNull private String helpHeader = "----Help---- page: %page%";
        @NotNull private String helpCommandPrefix = "&d";
        @NotNull private String helpCommandArgumentSpacer = " &5";
        @NotNull private String helpArgumentDescriptionSpacer = " &8> &7";
        @NotNull private String helpPropertyPrefix = "&5";
        @NotNull private String helpPropertyValueSpacer = " &8> &7";
        private boolean commandHandlerBuilt = false;

        /**
         * <p>
         *     Constructs a {@link MmCommandHandler.Builder Builder} instance,
         *     preloaded with the default configuration, that can be used to
         *     build an {@link MmCommandHandler} instance with a custom configuration.
         * </p>
         *
         * <p>
         *     MmCommandHandlerBuilder is typically used by first invoking various
         *     configuration methods to set desired options, and finally calling
         *     {@link MmCommandHandler.Builder#build()}.
         * </p>
         */
        public Builder() {}

        /**
         * <p>
         *     Constructs a {@link MmCommandHandler.Builder Builder} instance
         *     from an existing {@link MmCommandHandler}.
         * </p>
         * <p>
         *     The newly constructed
         *     {@link MmCommandHandler.Builder Builder} will be created with the
         *     same configuration as the existing MmCommandHandler instance, but
         *     will not copy previously registered commands.
         * </p>
         * @param commandHandler the instance of which the configuration will be
         *                       applied to the new MmCommandHandlerBuilder.
         */
        public Builder(MmCommandHandler commandHandler) {
            this.commandsPerPage = commandHandler.commandsPerPage;
            this.useHelp = commandHandler.useHelp;
            this.generateTabCompletions = commandHandler.generateTabCompletions;
            this.runLastAllowed = commandHandler.runLastAllowed;
            this.helpHeader = commandHandler.helpHeader;
            this.helpCommandPrefix = commandHandler.helpCommandPrefix;
            this.helpCommandArgumentSpacer = commandHandler.helpCommandArgumentSpacer;
            this.helpArgumentDescriptionSpacer = commandHandler.helpArgumentDescriptionSpacer;
            this.helpPropertyPrefix = commandHandler.helpPropertyPrefix;
        }

        /**
         * <p>
         *     Enable the help feature for this handler.
         *     This will allow the use of {@code /<basecommand> help}, which
         *     will invoke {@link MmCommandHandler#printHelp(CommandSender, String, List, int)}.
         * </p>
         * <p>
         *     When enabled, all first-level subcommands are forbidden to use "help"
         *     as name or alias.
         * </p>
         *
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder enableHelpSubCommand() {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            this.useHelp = true;
            return this;
        }

        /**
         * Enable automatic tab completions for this handler.
         * When enabled, the name of subcommands are automatically added
         * to the completions. (If the {@link CommandSender} has access to
         * that specific subcommand)
         *
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder generateTabCompletions() {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            this.generateTabCompletions = true;
            return this;
        }

        /**
         * Enable partial running for this handler.
         * <p>
         *     When enabled, the handler will call the last correctly matched
         *     subcommand even if it is followed by a subcommand the {@link CommandSender}
         *     does not have the required permissions for. Example given
         * </p>
         * <p>
         *     User enters the command {@code /myplugin say bold Hello World!}.
         *     The user has the permission to the say subcommand but is lacking
         *     the permission for the {@code bold} subcommand.
         * </p>
         * <ul>
         *     <li>
         *         If partial running is not enabled on the handler for the {@code /myplugin} command,
         *         {@link MmCommandHandler#noPermission(CommandSender, MmCommandSignature)} will be called
         *         and command execution will be cancelled.
         *     </li>
         *     <li>
         *         If partial running is enabled, the {@code say} subcommand will be executed
         *         with arguments {@code ["bold","Hello", "World!"]}
         *     </li>
         * </ul>
         *
         * @return this builder
         */
        //TODO: check if a missing permission message will be displayed if a partial command is ran
        public @NotNull MmCommandHandler.Builder runLastAllowed() {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            this.runLastAllowed = true;
            return this;
        }

        /**
         * Set the header to be added to every help menu page.
         * <p>
         *     Minecraft color codes can be used by using the &amp; character as
         *     alternative color character. Multiple lines can be used by using
         *     the java newline character. The page number is available through
         *     the placeholder %page%
         * </p>
         * <p>
         *     Default: "----Help----"
         * </p>
         * <p>
         * ! Only used in the default implementation of {@link MmCommandHandler#printHelp(CommandSender, String, List, int)} !
         * </p>
         * @param header the header
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder setHelpHeader(@NotNull String header) {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            this.helpHeader = header;
            return this;
        }

        /**
         * Set the amount of help entries listed on each help page.
         * Default: 5
         * <p>
         * ! Only used in the default implementation of {@link MmCommandHandler#printHelp(CommandSender, String, List, int)} !
         *
         * @param amount the amount
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder setAmountOfCommandsPerHelpPage(int amount) {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            if (amount < 0)
                throw new IllegalArgumentException("Amount cannot be negative");
            if (amount > 100)
                throw new IllegalArgumentException("Amount cannot be greater than 100");
            this.commandsPerPage = amount;
            return this;
        }

        /**
         * Set the prefix to use in the help menu.
         * The string will be put in front of every
         * help entry, not in front of the {@link #setHelpHeader(String) header}.
         * The last {@link ChatColor} code in this string will be the color
         * used to display the {@link MmCommandSignature#name() command}.
         * Default: "&amp;d"
         * <p>
         * ! Only used in the default implementation of {@link MmCommandHandler#printHelp(CommandSender, String, List, int)} !
         *
         * @param commandPrefix the command color
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder setHelpCommandPrefix(@NotNull String commandPrefix) {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            this.helpCommandPrefix = commandPrefix;
            return this;
        }

        /**
         * Set the spacer to use in between the {@link MmCommandSignature#name() command}
         * with possible {@link MmCommand#getSubcommands() subcommands}
         * and the {@link MmCommandSignature#arguments() arguments}. The string will be used
         * once in every help entry, not in the {@link #setHelpHeader(String)
         * header}.
         * <p>
         * The last {@link ChatColor} code in this string will be the color
         * used to display the {@link MmCommandSignature#arguments() arguments}.
         * Default: "&amp;5"
         * <p>
         * ! Only used in the default implementation of {@link MmCommandHandler#printHelp(CommandSender, String, List, int)} !
         *
         * @param argumentSpacer the argument spacer
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder setHelpArgumentSpacer(@NotNull String argumentSpacer) {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            this.helpCommandArgumentSpacer = argumentSpacer;
            return this;
        }

        /**
         * Set the spacer to use in between the {@link MmCommandSignature#arguments() arguments}
         * and the {@link MmCommandSignature#description() description}.
         * The string will be used once in every help entry,
         * not in the {@link #setHelpHeader(String) header}.
         * <p>
         * The last {@link ChatColor} code in this string will be the color
         * used to display the {@link MmCommandSignature#description() description}.
         * Default: "&amp;5"
         * <p>
         * ! Only used in the default implementation of {@link MmCommandHandler#printHelp(CommandSender, String, List, int)} !
         *
         * @param descriptionSpacer the description spacer
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder setHelpDescriptionSpacer(@NotNull String descriptionSpacer) {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            this.helpArgumentDescriptionSpacer = descriptionSpacer;
            return this;
        }

        /**
         * Set the prefix to use in the per command help menu
         * The string will be used once in front of every property,
         * not in the {@link #setHelpHeader(String) header}.
         * <p>
         * The last {@link ChatColor} code in this string will be the color
         * used to display the property.
         * Default: "&amp;d"
         * <p>
         * ! Only used in the default implementation of {@link MmCommandHandler#printPerCommandHelp(CommandSender, String, MmCommand)} !
         *
         * @param helpPropertyPrefix the property prefix
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder setHelpPropertyPrefix(@NotNull String helpPropertyPrefix) {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            this.helpPropertyPrefix = helpPropertyPrefix;
            return this;
        }

        /**
         * Set the spacer to use in between the property and the value
         * in the per command help menu.
         * <p>
         * The last {@link ChatColor} code in this string will be the color
         * used to display the value.
         * Default: "&amp;5"
         * <p>
         * ! Only used in the default implementation of
         * {@link MmCommandHandler#printPerCommandHelp(CommandSender, String, MmCommand)} !
         *
         * @param helpPropertyValueSpacer the description spacer
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder setHelpPropertyValueSpacer(@NotNull String helpPropertyValueSpacer) {
            if (this.commands.size() != 0)
                throw new RuntimeException("Options cannot be modified after commands have been added");
            this.helpPropertyValueSpacer = helpPropertyValueSpacer;
            return this;
        }

        /**
         * Register a command to this handler.
         * The command class has to be annotated by the
         * {@link MmCommandSignature} annotation.
         * <p>
         * Both the {@link MmCommandSignature#name() name} and
         * the {@link MmCommandSignature#aliases() aliases}
         * must be unique.
         *
         * @param command the command to add to this handler
         * @return this builder
         */
        public @NotNull MmCommandHandler.Builder addCommand(@NotNull MmCommand command) {
            if (command.getClass().getAnnotation(MmCommandSignature.class) == null)
                throw new IllegalArgumentException("Class " + command.getClass().getName() + " must be annotated by a " + MmCommandSignature.class.getName() + " annotation!");

            MmCommandSignature sig = command.getClass().getAnnotation(MmCommandSignature.class);
            if (commandExists(command))
                throw new IllegalArgumentException("Cannot register " + sig.name() + ": Name already registered as command");
            if (checkAliasNameConflict(command))
                throw new IllegalArgumentException("Cannot register " + sig.name() + ": Registers an alias that is already being used as name for another command");
            if (checkAliasAliasConflict(command))
                throw new IllegalArgumentException("Cannot register " + sig.name() + ": Registers an alias that is already being used as alias for another command");
            if (checkNameAliasConflict(command))
                throw new IllegalArgumentException("Cannot register " + sig.name() + ": Name is already being used as alias for another command");
            if (this.useHelp && checkHelpConflict(command))
                throw new IllegalArgumentException("Cannot register " + sig.name() + ": Command name or alias conflicts with the help command");
            if (sig.type() == MmCommandType.SUB_COMMAND)
                throw new IllegalArgumentException("Cannot register " + sig.name() + ": Command type must not be" + MmCommandType.SUB_COMMAND);
            this.commands.add(command);
            return this;
        }

        //done
        private boolean commandExists(@NotNull MmCommand command) {
            return this.commands.stream().anyMatch(command1 ->
                    command1.getClass().getAnnotation(MmCommandSignature.class).name().equals(command.getClass().getAnnotation(MmCommandSignature.class).name())
            );
        }

        //done
        private boolean checkAliasNameConflict(@NotNull MmCommand command) {
            String[] aliases = command.getClass().getAnnotation(MmCommandSignature.class).aliases();
            Set<String> existingNames = this.commands
                    .stream()
                    .map(subcommand -> subcommand.getClass().getAnnotation(MmCommandSignature.class).name())
                    .collect(Collectors.toSet());
            return Arrays.stream(aliases).anyMatch(existingNames::contains);
        }

        //done
        private boolean checkAliasAliasConflict(@NotNull MmCommand command) {
            String[] aliases = command.getClass().getAnnotation(MmCommandSignature.class).aliases();
            Set<String> existingAliases = new HashSet<>();
            this.commands.forEach(subcommand ->
                    existingAliases.addAll(
                            Arrays.asList(
                                    subcommand.getClass().getAnnotation(MmCommandSignature.class).aliases()
                            )
                    )
            );
            return Arrays.stream(aliases).anyMatch(existingAliases::contains);
        }

        //done
        private boolean checkNameAliasConflict(@NotNull MmCommand command) {
            String name = command.getClass().getAnnotation(MmCommandSignature.class).name();
            Set<String> existingAliases = new HashSet<>();
            this.commands.forEach(subcommand ->
                    existingAliases.addAll(
                            Arrays.asList(
                                    subcommand.getClass().getAnnotation(MmCommandSignature.class).aliases()
                            )
                    )
            );
            return existingAliases.contains(name);
        }

        //done
        private boolean checkHelpConflict(@NotNull MmCommand command) {
            String name = command.getClass().getAnnotation(MmCommandSignature.class).name();
            String[] aliases = command.getClass().getAnnotation(MmCommandSignature.class).aliases();
            return name.equalsIgnoreCase("help") || Arrays.stream(aliases).anyMatch(alias -> alias.equalsIgnoreCase("help"));
        }

        /**
         * Creates a {@link MmCommandHandler} instance based on the current
         * configuration. This method can, but should not, be called multiple times.
         * @return an instance of MmCommandHandler with the configured options.
         */
        public @NotNull MmCommandHandler build() {
            return new MmCommandHandler(this.commands, this.commandsPerPage,
                    this.useHelp, this.generateTabCompletions,
                    this.runLastAllowed, this.helpHeader, this.helpCommandPrefix,
                    this.helpCommandArgumentSpacer,
                    this.helpArgumentDescriptionSpacer, this.helpPropertyPrefix,
                    this.helpPropertyValueSpacer);
        }
    }
}
