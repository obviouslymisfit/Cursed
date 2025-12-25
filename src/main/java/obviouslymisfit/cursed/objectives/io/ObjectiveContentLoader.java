package obviouslymisfit.cursed.objectives.io;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ObjectiveContentLoader {
    private static final Gson GSON = new Gson();

    // IMPORTANT: this matches your locked folder layout under src/main/resources/data/...
    private static final String ROOT = "data/cursed/objectives";

    private ObjectiveContentLoader() {}

    public static LoadedContent loadAll(Logger log, String modId) {
        log.info("[CURSED] Loading objective content from: {}", ROOT);

        List<String> pools = new ArrayList<>();
        List<String> quantityRules = new ArrayList<>();
        List<String> generatorRules = new ArrayList<>();
        List<String> constraints = new ArrayList<>();

        // Load by walking the resources directory (works in dev; also works in-jar if the path resolves)
        pools.addAll(loadDir(log, modId, ROOT + "/pools"));
        quantityRules.addAll(loadDir(log, modId, ROOT + "/quantity_rules"));
        generatorRules.addAll(loadDir(log, modId, ROOT + "/generator_rules"));
        constraints.addAll(loadDir(log, modId, ROOT + "/constraints"));

        // Minimal validation: schema field exists and is int
        int ok = 0, bad = 0;
        for (String resPath : concat(pools, quantityRules, generatorRules, constraints)) {
            ValidationResult vr = validateSchema(log, modId, resPath);
            if (vr.ok) ok++; else bad++;
        }

        log.info("[CURSED] Objective content loaded. pools={}, quantity_rules={}, generator_rules={}, constraints={}, valid={}, invalid={}",
                pools.size(), quantityRules.size(), generatorRules.size(), constraints.size(), ok, bad);

        return new LoadedContent(pools, quantityRules, generatorRules, constraints);
    }

    private static List<String> loadDir(Logger log, String modId, String relativeDir) {
        List<String> found = new ArrayList<>();

        Optional<ModContainer> mcOpt = FabricLoader.getInstance().getModContainer(modId);
        if (mcOpt.isEmpty()) {
            log.error("[CURSED] ModContainer not found for modId={}", modId);
            return found;
        }

        ModContainer mc = mcOpt.get();
        Optional<Path> rootPathOpt = mc.findPath(relativeDir);
        if (rootPathOpt.isEmpty()) {
            // Not fatal: directory may be empty early on
            log.warn("[CURSED] Objective dir not found: {}", relativeDir);
            return found;
        }

        Path dirPath = rootPathOpt.get();
        try {
            // If in jar, dirPath may be inside a jar filesystem; Files.walk still works if FS is mounted
            try (var walk = Files.walk(dirPath, 1)) {
                walk.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            String fileName = p.getFileName().toString();
                            found.add(relativeDir + "/" + fileName);
                        });
            }
        } catch (Exception e) {
            log.error("[CURSED] Failed walking objective dir: {}", relativeDir, e);
        }

        log.info("[CURSED] Found {} file(s) in {}", found.size(), relativeDir);
        return found;
    }

    private static ValidationResult validateSchema(Logger log, String modId, String resPath) {
        try (InputStream is = openResource(modId, resPath)) {
            if (is == null) {
                log.error("[CURSED] Missing resource: {}", resPath);
                return new ValidationResult(false);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            JsonObject obj = JsonParser.parseReader(br).getAsJsonObject();

            if (!obj.has("schema") || !obj.get("schema").isJsonPrimitive() || !obj.get("schema").getAsJsonPrimitive().isNumber()) {
                log.error("[CURSED] Invalid/missing schema in {}", resPath);
                return new ValidationResult(false);
            }
            int schema = obj.get("schema").getAsInt();
            if (schema != 1) {
                log.error("[CURSED] Unsupported schema={} in {}", schema, resPath);
                return new ValidationResult(false);
            }
            return new ValidationResult(true);
        } catch (Exception e) {
            log.error("[CURSED] JSON parse/validate failed for {}", resPath, e);
            return new ValidationResult(false);
        }
    }

    private static InputStream openResource(String modId, String resPath) {
        // Fabric puts resources on the classpath; using the mod's classloader is safest
        return ObjectiveContentLoader.class.getClassLoader().getResourceAsStream(resPath);
    }

    private static List<String> concat(List<String> a, List<String> b, List<String> c, List<String> d) {
        List<String> out = new ArrayList<>(a.size() + b.size() + c.size() + d.size());
        out.addAll(a); out.addAll(b); out.addAll(c); out.addAll(d);
        return out;
    }

    private static final class ValidationResult {
        final boolean ok;
        ValidationResult(boolean ok) { this.ok = ok; }
    }

    public record LoadedContent(
            List<String> poolFiles,
            List<String> quantityRuleFiles,
            List<String> generatorRuleFiles,
            List<String> constraintFiles
    ) {}
}
