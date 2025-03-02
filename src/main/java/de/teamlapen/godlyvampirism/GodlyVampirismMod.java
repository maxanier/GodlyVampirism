package de.teamlapen.godlyvampirism;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.teamlapen.vampirism.config.BalanceBuilder;
import de.teamlapen.vampirism.config.BalanceConfig;
import de.teamlapen.vampirism.config.VampirismConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

@Mod(GodlyVampirismMod.MODID)
public class GodlyVampirismMod {

    public static final String MODID = "godly_vampirism";
    private static final Logger LOGGER = LogManager.getLogger();
    public static GodlyVampirismMod instance;

    private static final Set<Spec> defaultValues = new LinkedHashSet<>();
    private static List<Supplier<Optional<Spec>>> specSuppliers = new ArrayList<>();

    public GodlyVampirismMod(IEventBus modEventBus) {
        instance = this;
        NeoForge.EVENT_BUS.addListener(this::onCommandsRegister);
        modEventBus.addListener(this::onSetup);

        //Specify changes
        addInteger("arrowVampireKillerMaxHealth", 80);
        addInteger("holyWaterSplashDamage", 7);
        addDouble("holyWaterTierDamageInc", 3);
        addDouble("skillPointsPerLevel", 3);
        addDouble("skillPointsPerLordLevel", 3);

        addBoolean("allowInfiniteSpecialArrows", true);
        addInteger("haDisguiseInvisibleSQ", 128);
        addDouble("hsMajorAttackSpeedModifier", 0.7);
        addDouble("vsBloodThirstReduction1", -0.95);
        addDouble("vpHealthMaxMod", 20);
        addDouble("vpBloodExhaustionFactor", 0.3);
        addDouble("vpSundamage", 1d);
        addBoolean("vpSundamageNausea", false);
        addBoolean("vpFireResistanceReplace", false);
        addDouble("vpFireVulnerabilityMod", 1.5);
        addInteger("vaTeleportCooldown", 1);
        addInteger("vaTeleportMaxDistance", 150);
        addInteger("vaRageDurationIncrease", 10);
        addInteger("miMinionPerLordLevel", 2);
        addInteger("vpMaxYellowBorderPercentage", 15);
        addInteger("vpNeonatalDuration",2);
        addInteger("vpDbnoDuration",30);
        addInteger("vpNaturalArmorRegenDuration",2);
        addInteger("miEquipmentRepairAmount", 20);
        addInteger("vaFreezeDuration", 10);

    }

    private <Q> void create(String name, Q value) {
        specSuppliers.add(() -> {
            try {
                Field f = BalanceConfig.class.getDeclaredField(name);
                ModConfigSpec.ConfigValue<Q> conf = (ModConfigSpec.ConfigValue<Q>) f.get(VampirismConfig.BALANCE);
                return Optional.of(new Spec<Q>(conf, value));
            } catch (NoSuchFieldException e) {
                LOGGER.error("Cannot find config field as expected", e);
            } catch (IllegalAccessException e) {
                LOGGER.error("Failed to access config field as expected", e);
            }
            return Optional.empty();
        });
    }

    public void onCommandsRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("godly-vampirism").then(Commands.literal("apply").requires(context -> !(context.getServer() instanceof DedicatedServer) || context.hasPermission(3)).executes(context -> forceApplyConfiguration(context.getSource()))));
    }

    private void addBoolean(String key, boolean def) {
        VampirismConfig.addBalanceModification(key, conf -> {
            try {
                ((BalanceBuilder.BoolConf) conf).setDefaultValue(def);
                conf.comment(conf.getComment() + ". Modified by godly_vampirism");
            } catch (Exception e) {
                LOGGER.error("Failed to set "+key,e);
            }
        });
        create(key, def);
    }

    private void addDouble(String key, double def) {
        VampirismConfig.addBalanceModification(key, conf -> {
            try {
                ((BalanceBuilder.DoubleConf) conf).setDefaultValue(def);
                conf.comment(conf.getComment() + ". Modified by godly_vampirism");
            } catch (Exception e) {
                LOGGER.error("Failed to set "+key,e);
            }
        });
        create(key, def);
    }

    private void addInteger(String key, int def) {
        VampirismConfig.addBalanceModification(key, conf -> {
            try {
                ((BalanceBuilder.IntConf) conf).setDefaultValue(def);
                conf.comment(conf.getComment() + ". Modified by godly_vampirism");
            } catch (Exception e) {
                LOGGER.error("Failed to set "+key,e);
            }
        });
        create(key, def);
    }

    private int forceApplyConfiguration(CommandSourceStack source) {
        Iterator<Spec> it = defaultValues.iterator();
        Spec<?> v = null;
        while (it.hasNext()) {
            v = it.next();
            v.resetValue(false);
        }
        if (v != null) v.resetValue(true);
        source.sendSuccess( () -> Component.literal("Success!"), true);
        return 0;
    }

    private void onSetup(FMLCommonSetupEvent event) {
        specSuppliers.stream().map(Supplier::get).forEach(o -> o.ifPresent(defaultValues::add));
        specSuppliers = null;
    }

    private static class Spec<T> {
        private final T defaultValue;
        private final ModConfigSpec.ConfigValue<T> configSpec;

        private Spec(ModConfigSpec.ConfigValue<T> config, T value) {
            this.configSpec = config;
            this.defaultValue = value;
        }

        public void resetValue(boolean save) {
            this.configSpec.set(defaultValue);
            if (save) this.configSpec.save();
        }
    }


}
