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

    protected final @NotNull List<MmCommand> commands;
    protected final int commandsPerPage;
    protected final boolean useHelp;
    protected final boolean generateTabCompletions;
    protected final boolean runLastAllowed;
    protected final @NotNull String helpHeader;
    protected final @NotNull String helpCommandPrefix;
    protected final @NotNull String helpCommandArgumentSpacer;
    protected final @NotNull String helpArgumentDescriptionSpacer;
    protected final @NotNull String helpPropertyPrefix;
    protected final @NotNull String helpPropertyValueSpacer;


    /**
     * Instantiates a new MmCommand handler.
     */
    public MmCommandHandler() {
        this(Collections.<MmCommand>emptyList(), 5, false, false, false, "----Help" +
                "---- " +
                "page: " +
            "%page%", "&d", " &5", " &8> &7", "&5", " &8> &7");
    }

    MmCommandHandler(List<MmCommand> commands, int commandsPerPage,
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
     * The help menu consists of the {@link #helpHeader(String) header}
     * followed by {@link #amountOfCommandsPerHelpPage(int) a specified amount}
     * of help entries.
     * Each entry follows this pattern:
     * {@link #helpCommandPrefix(String) commandPrefix} {@code /label [subcommand(s)]}
     * {@link #helpArgumentSpacer(String) argumentSpacer} {@link MmCommandSignature#arguments() arguments}
     * {@link #helpDescriptionSpacer(String) descriptionSpacer} {@link MmCommandSignature#description() description}
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
     * The help menu consists of the {@link #helpHeader(String) header}
     * followed by a summary of the associated {@link MmCommandSignature signature}
     * followed by all help entries related to the provided basecommand
     * Each entry follows this pattern:
     * {@link #helpCommandPrefix(String) commandPrefix} {@code /label [subcommand(s)]}
     * {@link #helpArgumentSpacer(String) argumentSpacer} {@link MmCommandSignature#arguments() arguments}
     * {@link #helpDescriptionSpacer(String) descriptionSpacer} {@link MmCommandSignature#description() description}
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
}
