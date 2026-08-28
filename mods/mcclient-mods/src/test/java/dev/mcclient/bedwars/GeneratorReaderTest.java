package dev.mcclient.bedwars;

import dev.mcclient.hud.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Generator hologram parsing and the HUD's display helpers. */
class GeneratorReaderTest {

    @Test
    void readsADiamondGenerator() {
        // Hypixel splits this over several armour stands; the module joins them before parsing.
        GeneratorReader.Spawn spawn = GeneratorReader.parse(
                "§eTier §cII §b§lDiamond §7Spawns in §e12 §7seconds");

        assertEquals(GeneratorReader.Type.DIAMOND, spawn.type());
        assertEquals(12, spawn.seconds());
        assertEquals("II", spawn.tier());
        assertEquals("Diamond II", spawn.label());
    }

    @Test
    void readsAnEmeraldGenerator() {
        GeneratorReader.Spawn spawn = GeneratorReader.parse("§aEmerald §7Spawns in §e3 §7seconds");

        assertEquals(GeneratorReader.Type.EMERALD, spawn.type());
        assertEquals(3, spawn.seconds());
        assertEquals("Emerald", spawn.label(), "no tier shown means no numeral");
    }

    @Test
    void handlesSingularSecond() {
        assertEquals(1, GeneratorReader.parse("Diamond Spawns in 1 second").seconds());
    }

    @Test
    void ignoresHologramsThatAreNotGenerators() {
        assertNull(GeneratorReader.parse("§cSteve"));
        assertNull(GeneratorReader.parse(""));
        assertNull(GeneratorReader.parse("§aProtect your bed!"));
    }

    @Test
    void ignoresImplausiblyLargeCountdowns() {
        // Guards against latching onto an unrelated number in some other hologram.
        assertNull(GeneratorReader.parse("Spawns in 99999 seconds"));
    }

    @Test
    void formatsDurations() {
        assertEquals("1:30", Format.ticksToTime(30 * 20 + 60 * 20));
        assertEquals("0:05", Format.ticksToTime(5 * 20));
        assertEquals("0:01", Format.ticksToTime(3), "a sliver of a second still reads as one");
        assertEquals("0:00", Format.ticksToTime(0));
    }

    @Test
    void formatsAmplifiersTheWayTheGameDoes() {
        assertEquals("", Format.amplifier(0), "level I carries no numeral");
        assertEquals("II", Format.amplifier(1));
        assertEquals("III", Format.amplifier(2));
    }

    @Test
    void prettifiesEffectKeysWithoutALanguageFile() {
        assertEquals("Move Speed", Format.prettifyKey("potion.moveSpeed"));
        assertEquals("Invisibility", Format.prettifyKey("potion.invisibility"));
        assertEquals("Effect", Format.prettifyKey(null));
    }

    @Test
    void formatsFuses() {
        assertEquals("2.0s", Format.fuse(40));
        assertEquals("0.5s", Format.fuse(10));
        assertEquals("0.0s", Format.fuse(-1));
    }
}
