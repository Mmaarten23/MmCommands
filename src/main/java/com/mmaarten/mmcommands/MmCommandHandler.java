package com.mmaarten.mmcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
    private int commandsPerPage = 5;
    private boolean useHelp = false;
    private boolean generateTabCompletions = false;
    private boolean runLastAllowed = false;
    private @NotNull String helpHeader = "----Help----";
    private @NotNull String helpCommandPrefix = "&d";
    private @NotNull String commandArgumentSpacer = "&5";
    private @NotNull String argumentDescriptionSpacer = "&8> &7";


    /**
     * Instantiates a new MmCommand handler.
     */
    public MmCommandHandler() {
        this.commands = new ArrayList<>();
    }

    /*
     * CREATION
     */

    /**
     * Enable the help feature for this handler.
     * This will allow the use of {@code /<basecommand> help}
     * which will invoke {@link #printHelp(CommandSender, String, List, int)}.
     * <p>
     * When enabled, all first-level subcommands are forbidden to use "help"
     * as name or alias.
     *
     * @return this handler
     */
    public @NotNull MmCommandHandler useHelp() {
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
     * @return this handler
     */
    public @NotNull MmCommandHandler generateTabCompletions() {
        if (this.commands.size() != 0)
            throw new RuntimeException("Options cannot be modified after commands have been added");
        this.generateTabCompletions = true;
        return this;
    }

    /**
     * Enable partial running for this handler.
     * <p>
     * When enabled, the handler will call the last correctly matched
     * subcommand even if it is followed by a subcommand the {@link CommandSender}
     * does not have the required permissions for. Example given
     *
     * <p>
     * User enters the command {@code /myplugin say bold Hello World!}.
     * The user has the permission to the say subcommand but is lacking the
     * permission for the {@code bold} subcommand.
     * <ul>
     *     <li>
     *         If partial running is not enabled on the handler for the {@code /myplugin} command,
     *         {@link #noPermission(CommandSender, MmCommandSignature)} will be called
     *         and command execution will be cancelled.
     *     </li>
     *     <li>
     *         If partial running is enabled, the {@code say} subcommand will be executed
     *         with arguments {@code ["bold","Hello", "World!"]}
     *     </li>
     * </ul>
     *
     * @return this handler
     */
    //TODO: check if a missing permission message will be displayed if a partial command is ran
    public @NotNull MmCommandHandler runLastAllowed() {
        if (this.commands.size() != 0)
            throw new RuntimeException("Options cannot be modified after commands have been added");
        this.runLastAllowed = true;
        return this;
    }

    /**
     * Set the header to be added to every help menu page.
     * <p>
     * Minecraft color codes can be used by using the &amp; character as alternative
     * color character.
     * Multiple lines can be used by using the java newline character.
     * Default: "----Help----"
     * <p>
     * ! Only used in the default implementation of {@link #printHelp(CommandSender, String, List, int)} !
     *
     * @param header the header
     * @return this handler
     */
    public @NotNull MmCommandHandler helpHeader(@NotNull String header) {
        if (this.commands.size() != 0)
            throw new RuntimeException("Options cannot be modified after commands have been added");
        this.helpHeader = header;
        return this;
    }

    /**
     * Set the amount of help entries listed on each help page.
     * Default: 5
     * <p>
     * ! Only used in the default implementation of {@link #printHelp(CommandSender, String, List, int)} !
     *
     * @param amount the amount
     * @return this handler
     */
    public @NotNull MmCommandHandler amountOfCommandsPerHelpPage(int amount) {
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
     * help entry, not in front of the {@link #helpHeader(String) header}.
     * The last {@link ChatColor} code in this string will be the color
     * used to display the {@link MmCommandSignature#name() command}.
     * Default: "&d"
     * <p>
     * ! Only used in the default implementation of {@link #printHelp(CommandSender, String, List, int)} !
     *
     * @param commandPrefix the command color
     * @return this handler
     */
    public @NotNull MmCommandHandler helpCommandPrefix(@NotNull String commandPrefix) {
        if (this.commands.size() != 0)
            throw new RuntimeException("Options cannot be modified after commands have been added");
        this.helpCommandPrefix = commandPrefix;
        return this;
    }

    /**
     * Set the spacer to use in between the {@link MmCommandSignature#name() command}
     * with possible {@link MmCommand#getSubcommands() subcommands}
     * and the {@link MmCommandSignature#arguments() arguments}. The string will be used
     * once in every help entry, not in the {@link #helpHeader(String) header}.
     * <p>
     * The last {@link ChatColor} code in this string will be the color
     * used to display the {@link MmCommandSignature#arguments() arguments}.
     * Default: "&5"
     * <p>
     * ! Only used in the default implementation of {@link #printHelp(CommandSender, String, List, int)} !
     *
     * @param argumentSpacer the argument spacer
     * @return this handler
     */
    public @NotNull MmCommandHandler helpArgumentSpacer(@NotNull String argumentSpacer) {
        if (this.commands.size() != 0)
            throw new RuntimeException("Options cannot be modified after commands have been added");
        this.commandArgumentSpacer = argumentSpacer;
        return this;
    }

    /**
     * Set the spacer to use in between the {@link MmCommandSignature#arguments() arguments}
     * and the {@link MmCommandSignature#description() description}.
     * The string will be used once in every help entry,
     * not in the {@link #helpHeader(String) header}.
     * <p>
     * The last {@link ChatColor} code in this string will be the color
     * used to display the {@link MmCommandSignature#description() description}.
     * Default: "&5"
     * <p>
     * ! Only used in the default implementation of {@link #printHelp(CommandSender, String, List, int)} !
     *
     * @param descriptionSpacer the description spacer
     * @return this handler
     */
    public @NotNull MmCommandHandler helpDescriptionSpacer(@NotNull String descriptionSpacer) {
        if (this.commands.size() != 0)
            throw new RuntimeException("Options cannot be modified after commands have been added");
        this.argumentDescriptionSpacer = descriptionSpacer;
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
     * @return this handler
     */
    public @NotNull MmCommandHandler addCommand(MmCommand command) {
        if (command.getClass().getAnnotation(MmCommandSignature.class) == null)
            throw new IllegalArgumentException(
                    "Class " + command.getClass().getName() + " must be annotated by a " + MmCommandSignature.class.getName() + " annotation!"
            );
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
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // No subcommand provided
        if (args.length == 0) {
            if (noArguments(sender)) {
                printHelp(sender, label, new ArrayList<>(this.commands), 0);
            }
            return true;
        }

        // Check for the "help" subcommand
        if (useHelp)
            if (args[0].equalsIgnoreCase("help")) {
                printHelp(sender, label, new ArrayList<>(this.commands), args);
                return true;
            }

        // Find matching command
        @Nullable MmCommand cmd = getCommand(args[0]);
        if (cmd == null) {
            if (noSuchCommand(sender, args[0]))
                printHelp(sender, label, new ArrayList<>(this.commands), 0);
            return true;
        }

        // Check type and permissions
        if (failsChecks(sender, cmd, label)) return true;


        SubCommandWrapper wrapper = getSubCommand(args, cmd, sender, label);
        int i = wrapper.getDepth();
        cmd = wrapper.getSub();
        if (cmd == null) return true;

        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments = arguments.subList(Math.min(i, args.length), args.length);
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
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = getAllowedCommands(sender, label)
                    .stream()
                    .map(mmCommand -> mmCommand.getClass().getAnnotation(MmCommandSignature.class).name())
                    .collect(Collectors.toList());
            if (this.useHelp) completions.add("help");
            return completions;
        }


        // TODO: add per command help
        if (useHelp && args.length == 2)
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
        if (failsChecks(sender, cmd, label, true)) return MmCommandHandler.NO_COMPLETIONS;

        // Check current subcommand
        SubCommandWrapper wrapper = getSubCommand(args, cmd, sender, label, true);
        int i = wrapper.getDepth();
        cmd = wrapper.getSub();
        if (cmd == null) return MmCommandHandler.NO_COMPLETIONS;

        // Pass arguments
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments.subList(Math.min(i + 1, args.length), args.length);

        // Append future subcommands if needed and allowed
        List<String> completions = cmd.onTabComplete(sender, command, label, args);
        if (this.generateTabCompletions) {
            if (completions == null) completions = new ArrayList<>();
            completions.addAll(cmd.getSubcommands()
                    .stream()
                    .filter(mmCommand -> !failsChecks(sender, mmCommand, label, true))
                    .map(mmCommand -> mmCommand.getClass().getAnnotation(MmCommandSignature.class).name())
                    .collect(Collectors.toList())
            );
        }
        return completions;
    }

    //TODO: subcommands only need a permission check. Type is not needed
    private boolean failsChecks(CommandSender sender, MmCommand cmd, String label) {
        return failsChecks(sender, cmd, label, false);
    }

    private boolean failsChecks(CommandSender sender, MmCommand cmd, String label, boolean suppressHelp) {
        // Check type
        MmCommandSignature info = cmd.getClass().getAnnotation(MmCommandSignature.class);
        switch (info.type()) {
            case PLAYER_ONLY:
                if (!(sender instanceof Player)) {

                    if (!suppressHelp && noValidType(sender, info))
                        printHelp(sender, label, new ArrayList<>(this.commands), 0);
                    return true;
                }
                break;
            case CONSOLE_ONLY:
                if (!(sender instanceof ConsoleCommandSender)) {
                    if (!suppressHelp && noValidType(sender, info))
                        printHelp(sender, label, new ArrayList<>(this.commands), 0);
                    return true;
                }
                break;
            case PLAYER_CONSOLE:
                if (!(sender instanceof ConsoleCommandSender) && !(sender instanceof Player)) {
                    if (!suppressHelp && noValidType(sender, info))
                        printHelp(sender, label, new ArrayList<>(this.commands), 0);
                    return true;
                }
        }

        // Check permission
        if (!sender.hasPermission(info.permission())) {
            if (!suppressHelp && noPermission(sender, info))
                printHelp(sender, label, new ArrayList<>(this.commands), 0);
            return true;
        }
        return false;
    }

    private @NotNull List<MmCommandSignature> getSignatures() {
        return this.commands.stream().map(command -> command.getClass().getAnnotation(MmCommandSignature.class)).collect(Collectors.toList());
    }

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

    private @NotNull List<MmCommand> getAllowedCommands(@NotNull CommandSender sender, @NotNull String label) {
        return this.commands.stream().filter(command -> !failsChecks(sender, command, label, true)).collect(Collectors.toList());
    }

    private @NotNull SubCommandWrapper getSubCommand(@NotNull String[] args, @NotNull MmCommand cmd, @NotNull CommandSender sender, @NotNull String label) {
        return getSubCommand(args, cmd, sender, label, false);
    }

    private @NotNull SubCommandWrapper getSubCommand(@NotNull String[] args, @NotNull MmCommand cmd, @NotNull CommandSender sender, @NotNull String label, boolean suppressHelp) {
        int i = 1;
        while (i < args.length) {
            int finalI = i;
            MmCommand temp = cmd.getSubcommands().stream().filter(sub -> {
                MmCommandSignature subInfo = sub.getClass().getAnnotation(MmCommandSignature.class);
                return subInfo.name().equalsIgnoreCase(args[finalI]) ||
                        Arrays.stream(subInfo.aliases()).anyMatch(alias -> alias.equalsIgnoreCase(args[0]));
            })
                    .findFirst()
                    .orElse(null);
            if (temp == null) break;
            if (failsChecks(sender, temp, label, suppressHelp)) {
                if (!this.runLastAllowed) cmd = null;
                break;
            }
            cmd = temp;
            i++;
        }
        return new SubCommandWrapper(cmd, i);
    }

    private boolean commandExists(@NotNull MmCommand command) {
        return this.commands.stream().anyMatch(command1 ->
                command1.getClass().getAnnotation(MmCommandSignature.class).name().equals(command.getClass().getAnnotation(MmCommandSignature.class).name())
        );
    }

    private boolean checkAliasNameConflict(@NotNull MmCommand command) {
        String[] aliases = command.getClass().getAnnotation(MmCommandSignature.class).aliases();
        Set<String> existingNames = this.commands
                .stream()
                .map(subcommand -> subcommand.getClass().getAnnotation(MmCommandSignature.class).name())
                .collect(Collectors.toSet());
        return Arrays.stream(aliases).anyMatch(existingNames::contains);
    }

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

    private boolean checkHelpConflict(@NotNull MmCommand command) {
        String name = command.getClass().getAnnotation(MmCommandSignature.class).name();
        String[] aliases = command.getClass().getAnnotation(MmCommandSignature.class).aliases();
        return name.equalsIgnoreCase("help") || Arrays.stream(aliases).anyMatch(alias -> alias.equalsIgnoreCase("help"));
    }

    private void printHelp(@NotNull CommandSender commandSender, @NotNull String label, @NotNull List<MmCommand> commands, String[] args) {
        int pageNr = 0;
        if (args.length >= 2) {
            try {
                pageNr = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        printHelp(commandSender, label, commands, pageNr);
    }

    private @NotNull List<String> generateHelpStrings(@NotNull CommandSender sender, @NotNull MmCommand currentCmd, @NotNull String label, @NotNull String prefix) {
        MmCommandSignature current = currentCmd.getClass().getAnnotation(MmCommandSignature.class);
        prefix = prefix + " " + current.name();
        List<String> results = new ArrayList<>();

        if (!current.arguments().equalsIgnoreCase("") || currentCmd.getSubcommands().size() == 0)
            results.add(prefix + " " + this.commandArgumentSpacer + current.arguments() + " " + this.argumentDescriptionSpacer + current.description());
        for (MmCommand sub : currentCmd.getSubcommands()) {
            if (!failsChecks(sender, sub, label, true))
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
    protected boolean noSuchCommand(@NotNull CommandSender sender, @NotNull String attemptedCommand) {
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
     * the help menu should be printed.
     *
     * @param sender    the sender
     * @param signature the signature of the invoked command
     * @return whether or not to print the help menu to the player
     */
    protected boolean noValidType(@NotNull CommandSender sender, @NotNull MmCommandSignature signature) {
        return true;
    }

    /**
     * Code to be ran when a player executes a command
     * they do not have the required permission node for.
     * <p>
     * The return value determines whether or not
     * the help menu should be printed.
     *
     * @param sender    the sender
     * @param signature the signature of the invoked command
     * @return whether or not to print the help menu to the player
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
     *
     * A help entry will be added for all {@link MmCommand}s that match at least one of the following conditions:
     * <ul>
     *     <li>The command signature has a non-empty string for the arguments property</li>
     *     <li>The command does not have any subcommands</li>
     * </ul>
     *
     * @param commandSender the command sender
     * @param label         the label
     * @param commands      the commands
     * @param page          the page
     */
    protected void printHelp(@NotNull CommandSender commandSender, @NotNull String label, @NotNull List<MmCommand> commands, int page) {
        List<MmCommand> allowedCommands = getAllowedCommands(commandSender, label);
        List<String> helpMenu = new ArrayList<>();
        String prefix = this.helpCommandPrefix + "/" + label;
        for (MmCommand allowedCommand : allowedCommands) {
            helpMenu.addAll(generateHelpStrings(commandSender, allowedCommand, label, prefix));
        }

        page = Math.max(page, 0);
        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.helpHeader));
        for (int index = page * commandsPerPage; index < Math.min((page + 1) * commandsPerPage, helpMenu.size()); index++) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMenu.get(index)));
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
        public SubCommandWrapper(@Nullable MmCommand sub, int depth) {
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
