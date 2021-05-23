package com.mmaarten.mmcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Use this builder to construct a {@link MmCommandHandler}
 * instance when
 * you need to set configuration options other than the default. For an
 * {@link MmCommandHandler} instance with default configuration, you
 * can simply use {@code new MmCommandHandler()}.
 * </p>
 *
 * <p>
 * The following shows an example of how to use the
 * {@link MmCommandHandlerBuilder} to construct an
 * {@link MmCommandHandler} instance:
 * </p>
 *
 * <pre>
 * MmCommandHandler mmCommandHandler = new MmCommandHandlerBuilder()
 *     .enableHelpSubCommand()
 *     .generateTabCompletions()
 *     .runLastAllowed()
 *     .setHelpHeader("--------------Help---------------")
 *     .setAmountOfCommandsPerHelpPage(15)
 *     .build();
 * </pre>
 *
 * NOTE: the order of invocation of the configuration methods does not
 * matter.
 * @author Mmaarten
 * @author TheSummerGrinch
 */
public final class MmCommandHandlerBuilder {

    protected final @NotNull List<MmCommand> commands = new ArrayList<>();
    protected int commandsPerPage = 5;
    protected boolean useHelp = false;
    protected boolean generateTabCompletions = false;
    protected boolean runLastAllowed = false;
    @NotNull protected String helpHeader = "----Help---- page: %page%";
    @NotNull protected String helpCommandPrefix = "&d";
    @NotNull protected String helpCommandArgumentSpacer = " &5";
    @NotNull protected String helpArgumentDescriptionSpacer = " &8> &7";
    @NotNull protected String helpPropertyPrefix = "&5";
    @NotNull protected String helpPropertyValueSpacer = " &8> &7";
    private boolean commandHandlerBuilt = false;

    /**
     * Creates an MmCommandBuilder instance, preloaded with the default
     * configuration, that can be used to build a {@link MmCommandHandler}
     * instance with a custom configuration. MmCommandHandlerBuilder is
     * typically used by first invoking various configuration methods to set
     * desired options, and finally calling {@link #build()}.
     */
    public MmCommandHandlerBuilder() {}

    /**
     * Constructs a MmCommandHandlerBuilder instance from an existing
     * MmCommandHandler. The newly constructed MmCommandHandlerBuilder will
     * be created with the same configuration as the existing
     * MmCommandHandler instance.
     * @param commandHandler the instance of which the configuration will be
     *                       applied to the new MmCommandHandlerBuilder.
     */
    MmCommandHandlerBuilder(MmCommandHandler commandHandler) {
        this.commands.addAll(commandHandler.commands);
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
     * Enable the help feature for this handler.
     * This will allow the use of {@code /<basecommand> help}
     * which will invoke {@link MmCommandHandler#printHelp(CommandSender, String, List, int)}.
     * <p>
     * When enabled, all first-level subcommands are forbidden to use "help"
     * as name or alias.
     *
     * @return this builder
     */
    public @NotNull MmCommandHandlerBuilder enableHelpSubCommand() {
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
    public @NotNull MmCommandHandlerBuilder generateTabCompletions() {
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
    public @NotNull MmCommandHandlerBuilder runLastAllowed() {
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
     * The page number is available through the placeholder %page%
     * Default: "----Help----"
     * <p>
     * ! Only used in the default implementation of {@link MmCommandHandler#printHelp(CommandSender, String, List, int)} !
     *
     * @param header the header
     * @return this builder
     */
    public @NotNull MmCommandHandlerBuilder setHelpHeader(@NotNull String header) {
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
    public @NotNull MmCommandHandlerBuilder setAmountOfCommandsPerHelpPage(int amount) {
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
    public @NotNull MmCommandHandlerBuilder setHelpCommandPrefix(@NotNull String commandPrefix) {
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
    public @NotNull MmCommandHandlerBuilder setHelpArgumentSpacer(@NotNull String argumentSpacer) {
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
    public @NotNull MmCommandHandlerBuilder setHelpDescriptionSpacer(@NotNull String descriptionSpacer) {
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
    public @NotNull MmCommandHandlerBuilder setHelpPropertyPrefix(@NotNull String helpPropertyPrefix) {
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
    public @NotNull MmCommandHandlerBuilder setHelpPropertyValueSpacer(@NotNull String helpPropertyValueSpacer) {
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
    public @NotNull MmCommandHandlerBuilder addCommand(@NotNull MmCommand command) {
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
