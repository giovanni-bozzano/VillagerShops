package de.dosmike.sponge.vshop.shops;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;

public class ShopEntity {
    private Location<World> loc;
    private Vector3d rot;
    private ShopMenuManager menu;
    private UUID lastEntity = null;
    private EntityType npcType = EntityTypes.VILLAGER;
    private String variantName;
    private FieldResolver.KeyAttacher variant;
    private Text displayName;
    private final UUID ident; //for configs;
    UUID playershopholder = null;
    Location<World> playershopcontainer = null;

    private int lookAroundTicks = 0;

    public ShopEntity(UUID identifier) {
        ident = identifier;
    }

    public UUID getIdentifier() {
        return ident;
    }

    public Location<World> getLocation() {
        return loc;
    }

    /**
     * function to move existing shop for convenience as API
     */
    public void move(Location<World> newLocation) {
        Optional<Chunk> c = loc.getExtent().getChunkAtBlock(loc.getBiomePosition());
        if (!c.isPresent())
            throw new RuntimeException("Chunk for shop not available!");
        Chunk chunk = c.get();
        if (!chunk.isLoaded()) {
            if (!chunk.loadChunk(false))
                throw new RuntimeException("Unable to load chunk for shop to remove old entity");
        }
        getEntity().ifPresent(le->le.setLocation(newLocation));
        loc = newLocation;
    }

    public void setLocation(Location<World> location) {
        this.loc = new Location<>(location.getExtent(), location.getBlockX() + 0.5, location.getY(), location.getBlockZ() + 0.5);
        VillagerShops.getInstance().markShopsDirty();
    }

    public Vector3d getRotation() {
        return rot;
    }

    public void setRotation(Vector3d rotation) {
        this.rot = rotation;
    }

    public ShopMenuManager getMenu() {
        return menu;
    }

    /**
     * recounts items in the shop container for playershops
     */
    public void updateStock() {
        getStockInventory().ifPresent(inv -> menu.updateStock(inv));
    }

    public void setMenu(ShopMenuManager menu) {
        this.menu = menu;
        menu.updateMenu(true);
        VillagerShops.getInstance().markShopsDirty();
    }

    public EntityType getNpcType() {
        return npcType;
    }

    public void setNpcType(EntityType npcType) {
        if (npcType.equals(EntityTypes.PLAYER)) npcType = EntityTypes.HUMAN;
        this.npcType = npcType;
        VillagerShops.getInstance().markShopsDirty();
    }

    public void setDisplayName(Text name) {
        displayName = name;
        VillagerShops.getInstance().markShopsDirty();
    }

    public Text getDisplayName() {
        return displayName;
    }

    /**
     * MAY NOT BE CALLED BEFORE setNpcType()
     * @param magicVariant is a magic string that represents a variant, skin, career or similar id
     *                  and is automatically resolved into a CatalogType for the entity type of
     *                  this shop. If no matching "skin" can be found, no variant will be picked
     */
    public void setVariant(String magicVariant) {
        try {
            if (npcType.equals(EntityTypes.PLAYER)) { //entity type player will shit at you, and then laugh at you for you don't know you're supposed to use sponge:human
                npcType = EntityTypes.HUMAN;
            }
            if (magicVariant.equalsIgnoreCase("none")) {
                variant = null;
            } else if (npcType.equals(EntityTypes.HUMAN)) {
                variant = FieldResolver.PLAYER_SKIN.validate(magicVariant);
            } else if (npcType.equals(EntityTypes.HORSE)) {
                variant = FieldResolver.HORSE_VARIANT.validate(magicVariant);
            } else if (npcType.equals(EntityTypes.OCELOT)) {
                variant = FieldResolver.OCELOT_VARIANT.validate(magicVariant);
            } else if (npcType.equals(EntityTypes.VILLAGER)) {
                variant = FieldResolver.VILLAGER_VARIANT.validate(magicVariant);
            } else if (npcType.equals(EntityTypes.LLAMA)) {
                variant = FieldResolver.LLAMA_VARIANT.validate(magicVariant);
            } else if (npcType.equals(EntityTypes.RABBIT)) {
                variant = FieldResolver.RABBIT_VARIANT.validate(magicVariant);
            } else if (npcType.equals(EntityTypes.PARROT)) {
                variant = FieldResolver.PARROT_VARIANT.validate(magicVariant);
            } else {
                variantName = "NONE";
                variant = null;
            }
        } catch (Exception e) { //ignore non existant fields due to lower version
            e.printStackTrace();
            variantName = "NONE";
            variant = null;
        }
        if (variant != null) {
            variantName = variant.toString();
        } else
            variantName = magicVariant; //maybe valid later?

        VillagerShops.getInstance().markShopsDirty();
    }

    public FieldResolver.KeyAttacher getVariant() {
        return variant;
    }

    public String getVariantName() {
        return variantName;
    }

    /** @return the actual entity if loaded */
    public Optional<Entity> getEntity() {
        return loc.getExtent().getEntity(lastEntity);
    }
    public Optional<UUID> getEntityUniqueID() {
        return Optional.ofNullable(lastEntity);
    }

    /**
     * by setting a owner id this will try to turn into a player shop.<br>
     * If there's no chest below the shop this will throw a IllegalStateException.<br>
     * If the argument is null the playershop association is lifted
     */
    public void setPlayerShop(UUID ownerId) throws IllegalStateException {
        if (ownerId == null) {
            playershopholder = null;
            playershopcontainer = null;
            return;
        }
        Location<World> scan = getLocation().sub(0, 0.5, 0);
        Optional<TileEntity> te = scan.getTileEntity();
        if (!te.isPresent() || !(te.get() instanceof TileEntityCarrier) || ((TileEntityCarrier) te.get()).getInventory().capacity() < 27) {
            scan = scan.sub(0, 1, 0);
            te = scan.getTileEntity();
            if (!te.isPresent() || !(te.get() instanceof TileEntityCarrier) || ((TileEntityCarrier) te.get()).getInventory().capacity() < 27)
                throw new IllegalStateException("Shop is not placed above a chest");
        }

        playershopholder = ownerId;
        playershopcontainer = scan;
        VillagerShops.getInstance().markShopsDirty();
    }

    public boolean isShopOwner(UUID playerId) {
        return playershopholder != null && playershopholder.equals(playerId);
    }

    public Optional<Inventory> getStockInventory() {
        if (playershopholder == null) return Optional.empty();
        try {
            Optional<TileEntity> te = playershopcontainer.getTileEntity();
            if (!te.isPresent() || !(te.get() instanceof TileEntityCarrier) || ((TileEntityCarrier) te.get()).getInventory().capacity() < 27)
                throw new RuntimeException("ContainerBlock not Chest");
            return Optional.of(((TileEntityCarrier) te.get()).getInventory());
        } catch (Exception e) {
            VillagerShops.w("Could not receive container for Playershop at " + loc.getExtent().getName() + " " + loc.getBlockPosition());
            return Optional.empty();
        }
    }

    public Optional<Location<World>> getStockContainer() {
        return playershopholder == null ? Optional.empty() : Optional.of(playershopcontainer);
    }
    /** used for internals */
    public void setStockContainerRaw(Location<World> containerLocation) {
        playershopcontainer = containerLocation;
    }

    /**
     * if present this shop is a player-shop as defined by setPlayerShop
     */
    public Optional<UUID> getShopOwner() {
        return playershopholder == null ? Optional.empty() : Optional.of(playershopholder);
    }
    /** used for internals */
    public void setShopOwnerRaw(UUID shopOwnerRawId) {
        playershopholder = shopOwnerRawId;
    }

    public void tick() {
        getEntity().ifPresent(le->{
            if (le instanceof Living) {
                if ((++lookAroundTicks > 15 && VillagerShops.rng.nextInt(10) == 0) || lookAroundTicks > 100) {
                    Living mo = (Living) le;
                    lookAroundTicks = 0;
                    le.setLocationAndRotation(loc, rot);
                    mo.setHeadRotation(new Vector3d(VillagerShops.rng.nextFloat() * 30 - 14, rot.getY() + VillagerShops.rng.nextFloat() * 60 - 30, 0.0));
                }
            }
        });
    }
    public void findOrCreate() {
        Optional<Chunk> chunk = loc.getExtent().getChunk(loc.getChunkPosition());
        if (chunk.isPresent() && chunk.get().isLoaded()) {
            //first try to link the entity again:
            Entity shop = chunk.get().getNearbyEntities(loc.getPosition(), 0.4).stream()
                    //prevent picking up the player
                    .filter(ent->!ent.getType().equals(EntityTypes.PLAYER))
                    //either this IS this entity -> pass OR that entity was not yet assigned to a shop -> scoop up
                    .filter(ent->ent.getUniqueId().equals(lastEntity) || !VillagerShops.isEntityShop(ent))
                    .findFirst().orElseGet(this::spawn); // or create new one
            shop.setLocationAndRotation(loc, rot);
            lastEntity = shop.getUniqueId();
        }
    }
    public Entity spawn() {
        Entity shop = loc.getExtent().createEntity(npcType, loc.getPosition());
        shop.offer(Keys.AI_ENABLED, false);
        shop.offer(Keys.IS_SILENT, true);
        shop.offer(Keys.DISPLAY_NAME, displayName);

        //setting variant. super consistent ;D
        if (variant != null)
            try {
                variant.attach(shop);
            } catch (Exception e) {
                VillagerShops.l("Variant no longer supported! Did the EntityType change?");
            }

        if (loc.getExtent().spawnEntity(shop)) {
            lastEntity = shop.getUniqueId();
        } else {
            VillagerShops.w("Unable to spawn shop %s - Check spawn protection and chunk limits at %s %d %d %d!",
                    shop.getUniqueId().toString(),
                    shop.getLocation().getExtent().getName(),
                    shop.getLocation().getBlockX(),
                    shop.getLocation().getBlockY(),
                    shop.getLocation().getBlockZ());
        }
        return shop;
    }

    /** Custom toString name [is] { more } */
    @Override
    public String toString() {
        return String.format("%s [%s] { type: %s, entity: %s, skin: %s, location: %s°%.2f }",
                displayName.toPlain(), ident.toString(),
                playershopholder != null ? "playershop" : "adminshop",
                npcType.getId(), variantName,
                Utilities.toString(loc), rot.getY()
        );
    }
}
