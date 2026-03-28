package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant;

import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.goals.AssistantAlertGoal;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.goals.AssistantPathfindGoal;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.goals.AssistantSniffGoal;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.goals.AssistantWanderGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class SeekerAssistantEntity extends Creeper {

    private final Plugin plugin;
    private final UUID seekerId;
    private final AssistantSharedState sharedState;

    public SeekerAssistantEntity(Plugin plugin, UUID seekerId, Location origin, Level level) {
        super(EntityType.CREEPER, level);
        this.plugin = plugin;
        this.seekerId = seekerId;
        this.sharedState = new AssistantSharedState(origin);
    }

    @Override
    protected void registerGoals() {

    }

    public void injectGoals() {
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AssistantSniffGoal(this, plugin, sharedState));
        this.goalSelector.addGoal(3, new AssistantPathfindGoal(this, plugin, sharedState));
        this.goalSelector.addGoal(4, new AssistantAlertGoal(this, plugin, sharedState));
        this.goalSelector.addGoal(5, new AssistantWanderGoal(this, plugin, sharedState));
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public UUID getSeekerId() {
        return seekerId;
    }
}
