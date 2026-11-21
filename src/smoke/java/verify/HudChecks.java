package verify;

import de.resourcebuilder.category.PvPItem;
import de.resourcebuilder.pack.LivePackManager;
import de.resourcebuilder.pack.StyleAssets;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

final class HudChecks {
    private static final List<PvPItem> METERS =
            List.of(PvPItem.HEARTS, PvPItem.HUNGER, PvPItem.XP_BAR);

    static void apply(LivePackManager packs) {
        for (int i = 0; i < METERS.size(); i++) {
            PvPItem meter = METERS.get(i);
            packs.selectVariant(meter, packs.getVariants(meter).get(i + 1), i == METERS.size() - 1);
        }
    }

    static void verify(Minecraft client, LivePackManager packs) throws IOException {
        try (var exported = new ZipFile(packs.exportMix().toFile())) {
            for (int i = 0; i < METERS.size(); i++) {
                PvPItem meter = METERS.get(i);
                var selected = packs.getActiveSelection(meter);
                if (selected != packs.getVariants(meter).get(i + 1))
                    throw new AssertionError("Lost independent selection for " + meter);
                for (String texture : StyleAssets.CATALOG.get(meter.name())) {
                    String path = StyleAssets.ROOT + texture + ".png";
                    var entry = exported.getEntry(path);
                    if (entry == null) throw new AssertionError("HUD export missing " + path);
                    var id = Identifier.withDefaultNamespace("textures/" + texture + ".png");
                    try (var live = client.getResourceManager().open(id);
                            var zip = exported.getInputStream(entry)) {
                        byte[] expected = selected.files().get(path);
                        if (expected == null
                                || !Arrays.equals(expected, live.readAllBytes())
                                || !Arrays.equals(expected, zip.readAllBytes()))
                            throw new AssertionError("Incorrect HUD texture " + path);
                    }
                }
            }
        }
    }

    private HudChecks() {}
}
