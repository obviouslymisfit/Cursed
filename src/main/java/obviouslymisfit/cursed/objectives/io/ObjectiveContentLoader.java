package obviouslymisfit.cursed.objectives.io;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import obviouslymisfit.cursed.objectives.model.*;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ObjectiveContentLoader {
    private static final Gson GSON = new Gson();

    private static final String ROOT = "data/cursed/objectives";
    private static final String DIR_POOLS = ROOT + "/pools";
    private static final String DIR_QTY = ROOT + "/quantity_rules";
    private static final String DIR_GEN = ROOT + "/generator_rules";
    private static final String DIR_CONS = ROOT + "/constraints";

    // in-memory cache (simple for now)
    private static volatile ObjectiveContent CACHED;

    private ObjectiveContentLoader() {}

    public static ObjectiveContent getCachedOrNull() {
        return CACHED;
    }

    public static ObjectiveContent loadAll(Logger log, String modId) {
        log.info("[CURSED] Loading objective content from: {}", ROOT);

        List<String> poolFiles = listJsonFiles(log, modId, DIR_POOLS);
        List<String> qtyFiles = listJsonFiles(log, modId, DIR_QTY);
        List<String> genFiles = listJsonFiles(log, modId, DIR_GEN);
        List<String> conFiles = listJsonFiles(log, modId, DIR_CONS);

        int ok = 0, bad = 0;
        for (String p : concat(poolFiles, qtyFiles, genFiles, conFiles)) {
            if (validateSchema(log, modId, p)) ok++; else bad++;
        }

        // Parse
        Map<String, PoolFile> poolsById = new LinkedHashMap<>();
        for (String p : poolFiles) {
            PoolFile pf = readAs(log, modId, p, PoolFile.class);
            if (pf != null && pf.id() != null) poolsById.put(pf.id(), pf);
        }

        Map<String, QuantityRuleFile> qtyById = new LinkedHashMap<>();
        for (String p : qtyFiles) {
            QuantityRuleFile qf = readAs(log, modId, p, QuantityRuleFile.class);
            if (qf != null && qf.id() != null) qtyById.put(qf.id(), qf);
        }

        Map<Integer, GeneratorPhaseRuleFile> genByPhase = new LinkedHashMap<>();
        for (String p : genFiles) {
            GeneratorPhaseRuleFile gf = readAs(log, modId, p, GeneratorPhaseRuleFile.class);
            if (gf != null) genByPhase.put(gf.phase(), gf);
        }

        // constraints: expect exactly one for now
        HardConstraintsFile hard = null;
        if (!conFiles.isEmpty()) {
            hard = readAs(log, modId, conFiles.get(0), HardConstraintsFile.class);
        }

        // Minimal cross-checks (still not “generator” logic)
        for (GeneratorPhaseRuleFile g : genByPhase.values()) {
            if (g.primary() != null) {
                String qRuleId = g.primary().quantityRule();
                if (qRuleId != null && !qtyById.containsKey(qRuleId)) {
                    log.error("[CURSED] Generator phase {} references missing quantityRule id={}", g.phase(), qRuleId);
                }
                if (g.primary().itemPools() != null) {
                    for (String poolId : g.primary().itemPools()) {
                        if (!poolsById.containsKey(poolId)) {
                            log.error("[CURSED] Generator phase {} references missing pool id={}", g.phase(), poolId);
                        }
                    }
                }
            }
        }

        ObjectiveContent content = new ObjectiveContent(poolsById, qtyById, genByPhase, hard);
        CACHED = content;

        log.info("[CURSED] Objective content loaded. pools={}, quantity_rules={}, generator_rules={}, constraints={}, valid={}, invalid={}",
                poolFiles.size(), qtyFiles.size(), genFiles.size(), conFiles.size(), ok, bad);

        return content;
    }

    private static <T> T readAs(Logger log, String modId, String resPath, Class<T> clazz) {
        try (InputStream is = openResource(resPath)) {
            if (is == null) {
                log.error("[CURSED] Missing resource: {}", resPath);
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            return GSON.fromJson(br, clazz);
        } catch (Exception e) {
            log.error("[CURSED] Failed parsing {} as {}", resPath, clazz.getSimpleName(), e);
            return null;
        }
    }

    private static List<String> listJsonFiles(Logger log, String modId, String relativeDir) {
        List<String> found = new ArrayList<>();

        Optional<ModContainer> mcOpt = FabricLoader.getInstance().getModContainer(modId);
        if (mcOpt.isEmpty()) {
            log.error("[CURSED] ModContainer not found for modId={}", modId);
            return found;
        }

        Optional<Path> dirOpt = mcOpt.get().findPath(relativeDir);
        if (dirOpt.isEmpty()) {
            log.warn("[CURSED] Objective dir not found: {}", relativeDir);
            return found;
        }

        Path dir = dirOpt.get();
        try (var walk = Files.walk(dir, 1)) {
            walk.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> found.add(relativeDir + "/" + p.getFileName()));
        } catch (Exception e) {
            log.error("[CURSED] Failed walking objective dir: {}", relativeDir, e);
        }

        log.info("[CURSED] Found {} file(s) in {}", found.size(), relativeDir);
        return found;
    }

    private static boolean validateSchema(Logger log, String modId, String resPath) {
        try (InputStream is = openResource(resPath)) {
            if (is == null) {
                log.error("[CURSED] Missing resource: {}", resPath);
                return false;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            JsonObject obj = JsonParser.parseReader(br).getAsJsonObject();

            if (!obj.has("schema") || !obj.get("schema").isJsonPrimitive() || !obj.get("schema").getAsJsonPrimitive().isNumber()) {
                log.error("[CURSED] Invalid/missing schema in {}", resPath);
                return false;
            }
            int schema = obj.get("schema").getAsInt();
            if (schema != 1) {
                log.error("[CURSED] Unsupported schema={} in {}", schema, resPath);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("[CURSED] JSON validate failed for {}", resPath, e);
            return false;
        }
    }

    private static InputStream openResource(String resPath) {
        return ObjectiveContentLoader.class.getClassLoader().getResourceAsStream(resPath);
    }

    private static List<String> concat(List<String> a, List<String> b, List<String> c, List<String> d) {
        List<String> out = new ArrayList<>(a.size() + b.size() + c.size() + d.size());
        out.addAll(a); out.addAll(b); out.addAll(c); out.addAll(d);
        return out;
    }
}
