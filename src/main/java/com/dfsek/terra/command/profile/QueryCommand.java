package com.dfsek.terra.command.profile;

import com.dfsek.terra.Terra;
import com.dfsek.terra.api.gaea.command.WorldCommand;
import com.dfsek.terra.api.gaea.profiler.WorldProfiler;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class QueryCommand extends WorldCommand {
    public QueryCommand(com.dfsek.terra.api.gaea.command.Command parent) {
        super(parent);
    }

    @Override
    public boolean execute(@NotNull Player sender, @NotNull Command command, @NotNull String label, @NotNull String[] args, World world) {
        WorldProfiler profile = ((Terra) getMain()).getWorld(world).getProfiler();
        sender.sendMessage(profile.getResultsFormatted());
        return true;
    }

    @Override
    public String getName() {
        return "query";
    }

    @Override
    public List<com.dfsek.terra.api.gaea.command.Command> getSubCommands() {
        return Collections.emptyList();
    }

    @Override
    public int arguments() {
        return 0;
    }

    @Override
    public List<String> getTabCompletions(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
