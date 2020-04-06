package de.dosmike.sponge.vshop.commands;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.vshop.DependingSuggestionElement;
import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import de.dosmike.sponge.vshop.shops.FieldResolver;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class cmdCreate extends Command {

    static final Function<List<String>, Iterable<String>> CREATE_SkinSupplier = args->{
        for (Map.Entry<EntityType, Set<String>> entry : FieldResolver.getAutoTabCompleteMapping().entrySet()) {
            for (String arg : args) {
                if (arg.equalsIgnoreCase(entry.getKey().getId())) {
                    return entry.getValue().stream().map(s->s.indexOf(' ')>=0?String.format("\"%s\"",s):s).collect(Collectors.toList());
                }
            }
        }
        return new ArrayList<>(0);
    };

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .arguments(
                        GenericArguments.flags().valueFlag(
                                GenericArguments.string(Text.of("position")), "-at"
                        ).valueFlag(
                                DependingSuggestionElement.dependentSuggest(
                                        GenericArguments.string(Text.of("Skin")),
                                        /*Text.of("EntityType"),*/
                                        CREATE_SkinSupplier,
                                        false
                                ), "-skin"
                        ).buildWith(GenericArguments.seq(
                                GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of("EntityType"), EntityType.class)),
                                GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("Name")))
                        ))
                ).executor(new cmdCreate()).build();
    }

    @NotNull
    @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {

        if (!(src instanceof Player)) {
            throw new CommandException(localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
        }
        Player player = (Player) src;

        if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                !PermissionRegistra.PLAYER.hasPermission(player)) {
            throw new CommandException(Text.of(TextColors.RED,
                    localString("permission.missing").resolve(player).orElse("[permission missing]")));
        }
        boolean adminshop = PermissionRegistra.ADMIN.hasPermission(player);

        if (!adminshop) {
            Optional<String> option = player.getOption("vshop.option.playershop.limit");
            int limit = -1;
            try {
                limit = Integer.parseInt(option.orElse("-1"));
            } catch (Exception e) {/**/}
            if (limit >= 0) {
                int cnt = 0;
                UUID pid = player.getUniqueId();
                for (ShopEntity npc : VillagerShops.getShops())
                    if (npc.isShopOwner(pid)) cnt++;

                if (cnt >= limit) {
                    throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                            localString("cmd.create.playershop.limit").replace("%limit%", limit).resolve(player).orElse("[limit reached]")));
                }
            }
        }

        String var = (String) args.getOne("Skin").orElse("none");
        String name = (String) args.getOne("Name").orElse("VillagerShop");
        Text displayName = TextSerializers.FORMATTING_CODE.deserialize(name);

        ShopEntity npc = new ShopEntity(UUID.randomUUID());
        ShopMenuManager prep = new ShopMenuManager();
        npc.setNpcType((EntityType) args.getOne("EntityType").get()); // required argument
        if (npc.getNpcType() == null) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.create.invalidtype").resolve(player).orElse("[invalid type]")));
        }
        if (!adminshop) {
            String entityPermission = npc.getNpcType().getId();
            entityPermission = "vshop.create." + entityPermission.replace(':', '.').replace("_", "").replace("-", "");
            if (!player.hasPermission(entityPermission)) {
                throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                        localString("cmd.create.entitypermission").replace("%permission%", entityPermission).resolve(player).orElse("[entity permission missing]")));
            }
        }
        npc.setVariant(var);
        //var wanted, but none returned/found
        if (!"none".equalsIgnoreCase(var) &&
                (npc.getVariant()==null ||
                        "none".equalsIgnoreCase(npc.getVariant().toString()))) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.create.invalidvariant")
                            .replace("%variant%", npc.getNpcType().getTranslation().get(Utilities.playerLocale(player)))
                            .resolve(player).orElse("[invalid variant]")));
        }
        Location<World> createAt = player.getLocation();
        double rotateYaw = player.getHeadRotation().getY();
        if (args.hasAny("position")) try {
            String[] parts = args.<String>getOne("position").get().split("/");
            if (parts.length != 5) {
                throw new Exception();
            }
            Optional<World> w = Sponge.getServer().getWorld(parts[0]);
            if (!w.isPresent()) {
                throw new CommandException(Text.of(TextColors.RED, "[vShop] ", localString("cmd.create.invalidworld").resolve(player).orElse("[Invalid pos]")));
            }
            createAt = w.get().getLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
            rotateYaw = Double.parseDouble(parts[4]);
            while (rotateYaw > 180) rotateYaw -= 360;
            while (rotateYaw <= -180) rotateYaw += 360;
        } catch (Exception e) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ", localString("cmd.create.invalidpos").resolve(player).orElse("[Invalid pos]")));
        }

        npc.setDisplayName(displayName);
        npc.setMenu(prep);
        npc.setLocation(createAt);
        npc.setRotation(new Vector3d(0, rotateYaw, 0));
        boolean playershop = false;
        try {
            npc.setPlayerShop(player.getUniqueId());
            playershop = true;
        } catch (Exception e) {
            if (!adminshop) {
                throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                        localString("cmd.create.playershop.missingcontainer").resolve(player).orElse("[no chest below]")));
            }
        }
        VillagerShops.addShop(npc);

        src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                localText(playershop ? "cmd.create.playershop.success" : "cmd.create.success").replace("%name%", Text.of(TextColors.RESET, displayName)).resolve(player).orElse(Text.of("[success]"))));

        VillagerShops.audit("%s created a new shop %s", Utilities.toString(src), npc.toString());
        return CommandResult.success();
    }

}
