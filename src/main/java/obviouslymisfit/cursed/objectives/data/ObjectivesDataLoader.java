package obviouslymisfit.cursed.objectives.data;

import obviouslymisfit.cursed.Cursed;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import obviouslymisfit.cursed.objectives.data.model.ItemPoolFile;
import obviouslymisfit.cursed.objectives.data.model.ObjectiveTemplateFile;
import obviouslymisfit.cursed.objectives.data.model.QuantityRuleFile;
import obviouslymisfit.cursed.objectives.data.model.GeneratorRuleFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;



/**
 * Loads and validates Baseline B objectives data from:
 *   data/cursed/objectives/
 *
 * Milestone 0 responsibilities:
 *  - validate folder structure exists
 *  - validate cross-file references later (JSON parsing comes next)
 *  - fail fast on startup if anything is invalid
 */
public final class ObjectivesDataLoader {

    private static final String ROOT_PATH = "data/cursed/objectives";

    private static final List<String> REQUIRED_SUBFOLDERS = List.of(
            "item_pools",
            "objective_templates",
            "quantity_rules",
            "generator_rules",
            "constraints"
    );

    private ObjectivesDataLoader() {
        // utility class
    }

    private static final Gson GSON = new GsonBuilder().create();

    private static Map<String, ItemPoolFile> loadAndValidateItemPools() {

        Cursed.LOGGER.info("CURSED: loading item pools");

        URL poolsUrl = ObjectivesDataLoader.class.getClassLoader().getResource(ROOT_PATH + "/item_pools");
        if (poolsUrl == null) {
            throw new IllegalStateException("CURSED: missing item_pools folder under " + ROOT_PATH);
        }

        Path poolsDir;
        try {
            poolsDir = Path.of(poolsUrl.toURI());
        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed to resolve item_pools path", e);
        }

        Map<String, ItemPoolFile> poolsById = new HashMap<>();

        try (var stream = Files.list(poolsDir)) {
            var poolFiles = stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                    .toList();

            if (poolFiles.isEmpty()) {
                throw new IllegalStateException("CURSED: no item pool .json files found in " + poolsDir);
            }

            for (Path file : poolFiles) {
                String filename = file.getFileName().toString();
                String expectedId = filename.substring(0, filename.length() - ".json".length());

                try (var in = Files.newInputStream(file);
                     var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                    ItemPoolFile pool = GSON.fromJson(reader, ItemPoolFile.class);

                    if (pool == null) {
                        throw new IllegalStateException("Pool parsed as null: " + filename);
                    }
                    if (pool.id == null || pool.id.isBlank()) {
                        throw new IllegalStateException("Missing id in " + filename);
                    }
                    if (!pool.id.equals(expectedId)) {
                        throw new IllegalStateException(
                                "id mismatch in " + filename + " (expected '" + expectedId + "', got '" + pool.id + "')"
                        );
                    }
                    if (pool.items == null || pool.items.isEmpty()) {
                        throw new IllegalStateException("Pool has no items: " + filename);
                    }

                    // Validate exact IDs: must be namespace:id (no '#', no spaces)
                    for (String id : pool.items) {
                        if (id == null || id.isBlank()) {
                            throw new IllegalStateException("Blank item id in pool " + pool.id);
                        }
                        if (id.contains("#")) {
                            throw new IllegalStateException("Tags are not allowed (found '#') in pool " + pool.id + ": " + id);
                        }
                        if (id.contains(" ")) {
                            throw new IllegalStateException("Spaces are not allowed in item id in pool " + pool.id + ": " + id);
                        }
                        int colon = id.indexOf(':');
                        if (colon <= 0 || colon == id.length() - 1) {
                            throw new IllegalStateException("Item id must be 'namespace:id' in pool " + pool.id + ": " + id);
                        }
                    }

                    if (poolsById.put(pool.id, pool) != null) {
                        throw new IllegalStateException("Duplicate id detected: " + pool.id);
                    }

                } catch (Exception e) {
                    throw new RuntimeException("CURSED: failed parsing item pool file: " + filename, e);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed while reading item pool files from " + poolsDir, e);
        }

        Cursed.LOGGER.info("CURSED: loaded {} item pools", poolsById.size());
        return poolsById;

    }

    private static Map<String, Path> indexConstraintsFiles() {
        URL constraintsUrl = ObjectivesDataLoader.class.getClassLoader().getResource(ROOT_PATH + "/constraints");
        if (constraintsUrl == null) {
            throw new IllegalStateException("CURSED: missing constraints folder under " + ROOT_PATH);
        }

        Path constraintsDir;
        try {
            constraintsDir = Path.of(constraintsUrl.toURI());
        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed to resolve constraints path", e);
        }

        Map<String, Path> byId = new HashMap<>();

        try (var stream = Files.list(constraintsDir)) {
            var files = stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                    .toList();

            if (files.isEmpty()) {
                throw new IllegalStateException("CURSED: no constraint .json files found in " + constraintsDir);
            }

            for (Path file : files) {
                String filename = file.getFileName().toString();
                String id = filename.substring(0, filename.length() - ".json".length());

                if (byId.put(id, file) != null) {
                    throw new IllegalStateException("CURSED: duplicate constraint id filename: " + id);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed while indexing constraint files", e);
        }

        Cursed.LOGGER.info("CURSED: indexed {} constraint files", byId.size());
        return byId;
    }

    private static Set<String> loadAndValidateObjectiveTemplates(
            Map<String, ItemPoolFile> poolsById,
            Map<String, Path> constraintsById
    ) {
        Cursed.LOGGER.info("CURSED: loading objective templates");

        URL templatesUrl = ObjectivesDataLoader.class.getClassLoader().getResource(ROOT_PATH + "/objective_templates");
        if (templatesUrl == null) {
            throw new IllegalStateException("CURSED: missing objective_templates folder under " + ROOT_PATH);
        }

        Path templatesDir;
        try {
            templatesDir = Path.of(templatesUrl.toURI());
        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed to resolve objective_templates path", e);
        }

        Map<String, ObjectiveTemplateFile> templatesById = new HashMap<>();

        try (var stream = Files.list(templatesDir)) {
            var templateFiles = stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                    .toList();

            if (templateFiles.isEmpty()) {
                throw new IllegalStateException("CURSED: no template .json files found in " + templatesDir);
            }

            for (Path file : templateFiles) {
                String filename = file.getFileName().toString();
                String expectedId = filename.substring(0, filename.length() - ".json".length());

                try (var in = Files.newInputStream(file);
                     var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                    ObjectiveTemplateFile t = GSON.fromJson(reader, ObjectiveTemplateFile.class);

                    if (t == null) throw new IllegalStateException("Template parsed as null: " + filename);

                    String resolvedId = (t.template_id != null && !t.template_id.isBlank()) ? t.template_id : t.id;

                    if (resolvedId == null || resolvedId.isBlank()) {
                        throw new IllegalStateException("Missing template id (template_id or id) in " + filename);
                    }
                    if (!resolvedId.equals(expectedId)) {
                        throw new IllegalStateException(
                                "Template id mismatch in " + filename + " (expected '" + expectedId + "', got '" + resolvedId + "')"
                        );
                    }

                    if (t.category == null || t.category.isBlank()) throw new IllegalStateException("Missing category in " + filename);
                    if (t.type == null || t.type.isBlank()) throw new IllegalStateException("Missing type in " + filename);

                    // Category/type legality
                    switch (t.category) {
                        case "PRIMARY" -> {
                            if (!t.type.equals("DELIVER")) {
                                throw new IllegalStateException("PRIMARY templates must be DELIVER: " + resolvedId);
                            }
                        }
                        case "SECONDARY" -> {
                            if (!(t.type.equals("DELIVER") || t.type.equals("TEAM_GATHER"))) {
                                throw new IllegalStateException("SECONDARY templates must be DELIVER or TEAM_GATHER: " + resolvedId);
                            }
                        }
                        case "TASK" -> {
                            if (!(t.type.equals("CRAFT") || t.type.equals("SMELT"))) {
                                throw new IllegalStateException("TASK templates must be CRAFT or SMELT: " + resolvedId);
                            }
                        }
                        default -> throw new IllegalStateException("Unknown category in " + resolvedId + ": " + t.category);
                    }

                    if (t.pool_refs == null || t.pool_refs.isEmpty()) {
                        throw new IllegalStateException("Missing/empty pool_refs in " + resolvedId);
                    }
                    for (String poolRef : t.pool_refs) {
                        if (!poolsById.containsKey(poolRef)) {
                            throw new IllegalStateException("Template " + resolvedId + " references missing pool: " + poolRef);
                        }
                    }

                    if (t.pick == null) throw new IllegalStateException("Missing pick in " + resolvedId);
                    if (t.pick.min < 1 || t.pick.max < 1 || t.pick.min > t.pick.max) {
                        throw new IllegalStateException("Invalid pick range in " + resolvedId + ": " + t.pick.min + ".." + t.pick.max);
                    }

                    if (t.constraints == null) {
                        throw new IllegalStateException("Missing constraints array in " + resolvedId + " (must be [] if none)");
                    }
                    for (String c : t.constraints) {
                        if (!constraintsById.containsKey(c)) {
                            throw new IllegalStateException("Template " + resolvedId + " references missing constraint file: " + c);
                        }
                    }

                    // Cohesion legality quick check (by naming convention for now)
                    if ((t.type.equals("CRAFT") || t.type.equals("SMELT"))) {
                        for (String c : t.constraints) {
                            if (c.startsWith("cohesion_")) {
                                throw new IllegalStateException("Tasks cannot have cohesion constraints: " + resolvedId + " -> " + c);
                            }
                        }
                    }

                    if (templatesById.put(resolvedId, t) != null) {
                        throw new IllegalStateException("Duplicate template id detected: " + resolvedId);
                    }

                } catch (Exception e) {
                    throw new RuntimeException("CURSED: failed parsing template file: " + filename, e);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed while reading templates from " + templatesDir, e);
        }

        Cursed.LOGGER.info("CURSED: loaded {} objective templates", templatesById.size());
        return templatesById.keySet();
    }

    private static void loadAndValidateQuantityRules() {
        Cursed.LOGGER.info("CURSED: loading quantity rules");

        URL rulesUrl = ObjectivesDataLoader.class.getClassLoader().getResource(ROOT_PATH + "/quantity_rules");
        if (rulesUrl == null) {
            throw new IllegalStateException("CURSED: missing quantity_rules folder under " + ROOT_PATH);
        }

        Path rulesDir;
        try {
            rulesDir = Path.of(rulesUrl.toURI());
        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed to resolve quantity_rules path", e);
        }

        // phase -> (category|type) -> rule
        Map<Integer, Map<String, QuantityRuleFile>> byPhase = new HashMap<>();

        try (var stream = Files.list(rulesDir)) {
            var files = stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                    .toList();

            if (files.isEmpty()) {
                throw new IllegalStateException("CURSED: no quantity rule .json files found in " + rulesDir);
            }

            for (Path file : files) {
                String filename = file.getFileName().toString();
                String expectedId = filename.substring(0, filename.length() - ".json".length());

                QuantityRuleFile r;
                try (var in = Files.newInputStream(file);
                     var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    r = GSON.fromJson(reader, QuantityRuleFile.class);
                } catch (Exception e) {
                    throw new RuntimeException("CURSED: failed parsing quantity rule file: " + filename, e);
                }

                if (r == null) throw new IllegalStateException("Quantity rule parsed as null: " + filename);

                String resolvedId = (r.rule_id != null && !r.rule_id.isBlank()) ? r.rule_id : r.id;
                if (resolvedId == null || resolvedId.isBlank()) {
                    throw new IllegalStateException("Missing rule id (rule_id or id) in " + filename);
                }
                if (!resolvedId.equals(expectedId)) {
                    throw new IllegalStateException(
                            "Rule id mismatch in " + filename + " (expected '" + expectedId + "', got '" + resolvedId + "')"
                    );
                }

                if (r.phase < 1 || r.phase > 5) {
                    throw new IllegalStateException("Invalid phase in " + resolvedId + ": " + r.phase);
                }
                if (r.category == null || r.category.isBlank()) {
                    throw new IllegalStateException("Missing category in " + resolvedId);
                }
                if (r.type == null || r.type.isBlank()) {
                    throw new IllegalStateException("Missing type in " + resolvedId);
                }
                if (r.roll_mode == null || r.roll_mode.isBlank()) {
                    throw new IllegalStateException("Missing roll_mode in " + resolvedId);
                }
                if (!r.roll_mode.equals("RANGE")) {
                    throw new IllegalStateException("Unsupported roll_mode in " + resolvedId + ": " + r.roll_mode);
                }

                // Basic legality
                switch (r.category) {
                    case "PRIMARY" -> {
                        if (!r.type.equals("DELIVER")) throw new IllegalStateException("PRIMARY rules must be DELIVER: " + resolvedId);
                    }
                    case "SECONDARY" -> {
                        if (!(r.type.equals("DELIVER") || r.type.equals("TEAM_GATHER")))
                            throw new IllegalStateException("SECONDARY rules must be DELIVER or TEAM_GATHER: " + resolvedId);
                    }
                    case "TASK" -> {
                        if (!(r.type.equals("CRAFT") || r.type.equals("SMELT")))
                            throw new IllegalStateException("TASK rules must be CRAFT or SMELT: " + resolvedId);
                    }
                    default -> throw new IllegalStateException("Unknown category in " + resolvedId + ": " + r.category);
                }

                if (r.min < 1 || r.max < 1 || r.min > r.max) {
                    throw new IllegalStateException("Invalid min/max in " + resolvedId + ": " + r.min + ".." + r.max);
                }
                if (r.step < 1) {
                    throw new IllegalStateException("Invalid step in " + resolvedId + ": " + r.step);
                }
                if (((r.max - r.min) % r.step) != 0) {
                    throw new IllegalStateException("Range not divisible by step in " + resolvedId + ": " + r.min + ".." + r.max + " step " + r.step);
                }

                String key = r.category + "|" + r.type;
                byPhase.computeIfAbsent(r.phase, p -> new HashMap<>());

                Map<String, QuantityRuleFile> phaseMap = byPhase.get(r.phase);
                if (phaseMap.put(key, r) != null) {
                    throw new IllegalStateException("Duplicate quantity rule for phase " + r.phase + " and " + key);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed while reading quantity rules from " + rulesDir, e);
        }

        // Coverage check
        Set<String> requiredKeys = Set.of(
                "PRIMARY|DELIVER",
                "SECONDARY|DELIVER",
                "SECONDARY|TEAM_GATHER",
                "TASK|CRAFT",
                "TASK|SMELT"
        );

        for (int phase = 1; phase <= 5; phase++) {
            Map<String, QuantityRuleFile> phaseMap = byPhase.get(phase);
            if (phaseMap == null) {
                throw new IllegalStateException("Missing all quantity rules for phase " + phase);
            }
            for (String req : requiredKeys) {
                if (!phaseMap.containsKey(req)) {
                    throw new IllegalStateException("Missing quantity rule for phase " + phase + " and " + req);
                }
            }
        }

        Cursed.LOGGER.info("CURSED: loaded quantity rules for phases 1-5 (coverage OK)");
    }

    private static void loadAndValidateGeneratorRules(Set<String> templateIds) {
        Cursed.LOGGER.info("CURSED: loading generator rules");

        URL rulesUrl = ObjectivesDataLoader.class.getClassLoader().getResource(ROOT_PATH + "/generator_rules");
        if (rulesUrl == null) {
            throw new IllegalStateException("CURSED: missing generator_rules folder under " + ROOT_PATH);
        }

        Path rulesDir;
        try {
            rulesDir = Path.of(rulesUrl.toURI());
        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed to resolve generator_rules path", e);
        }

        Map<Integer, GeneratorRuleFile> byPhase = new HashMap<>();

        try (var stream = Files.list(rulesDir)) {
            var files = stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                    .toList();

            if (files.isEmpty()) {
                throw new IllegalStateException("CURSED: no generator rule .json files found in " + rulesDir);
            }

            for (Path file : files) {
                String filename = file.getFileName().toString();
                String expected = filename.substring(0, filename.length() - ".json".length()); // phase1, phase2...

                int expectedPhase;
                try {
                    if (!expected.startsWith("phase")) throw new IllegalStateException("Invalid generator rule filename: " + filename);
                    expectedPhase = Integer.parseInt(expected.substring("phase".length()));
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid generator rule filename (expected phaseN.json): " + filename, e);
                }

                GeneratorRuleFile r;
                try (var in = Files.newInputStream(file);
                     var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    r = GSON.fromJson(reader, GeneratorRuleFile.class);
                } catch (Exception e) {
                    throw new RuntimeException("CURSED: failed parsing generator rule file: " + filename, e);
                }

                if (r == null) throw new IllegalStateException("Generator rule parsed as null: " + filename);
                if (r.phase != expectedPhase) {
                    throw new IllegalStateException("Phase mismatch in " + filename + " (expected " + expectedPhase + ", got " + r.phase + ")");
                }
                if (r.phase < 1 || r.phase > 5) {
                    throw new IllegalStateException("Invalid phase in " + filename + ": " + r.phase);
                }

                // Primary
                if (r.primary == null) throw new IllegalStateException("Missing primary block in phase " + r.phase);
                if (r.primary.count != 1) {
                    throw new IllegalStateException("Primary count must be 1 (locked) in phase " + r.phase);
                }
                validateEligibleTemplates("primary", r.phase, r.primary.eligible_templates, templateIds);

                // Secondary
                if (r.secondary == null) throw new IllegalStateException("Missing secondary block in phase " + r.phase);
                if (r.phase == 5) {
                    if (r.secondary.count != 0) throw new IllegalStateException("Phase 5 secondary.count must be 0");
                    if (r.secondary.eligible_templates == null || !r.secondary.eligible_templates.isEmpty())
                        throw new IllegalStateException("Phase 5 secondary.eligible_templates must be empty");
                } else {
                    if (r.secondary.count != 2) {
                        throw new IllegalStateException("Secondary count must be 2 (locked for phases 1-4) in phase " + r.phase);
                    }
                    validateEligibleTemplates("secondary", r.phase, r.secondary.eligible_templates, templateIds);
                }

                // Tasks
                if (r.tasks == null) throw new IllegalStateException("Missing tasks block in phase " + r.phase);
                if (r.tasks.count == null) throw new IllegalStateException("Missing tasks.count range in phase " + r.phase);

                if (r.phase == 5) {
                    if (r.tasks.count.min != 0 || r.tasks.count.max != 0) throw new IllegalStateException("Phase 5 tasks.count must be 0..0");
                    if (r.tasks.cap_per_team != 0) throw new IllegalStateException("Phase 5 tasks.cap_per_team must be 0");
                    if (r.tasks.eligible_templates == null || !r.tasks.eligible_templates.isEmpty())
                        throw new IllegalStateException("Phase 5 tasks.eligible_templates must be empty");
                } else {
                    if (r.tasks.count.min < 0 || r.tasks.count.max < 0 || r.tasks.count.min > r.tasks.count.max) {
                        throw new IllegalStateException("Invalid tasks.count range in phase " + r.phase + ": " + r.tasks.count.min + ".." + r.tasks.count.max);
                    }
                    if (r.tasks.cap_per_team < 0) throw new IllegalStateException("Invalid tasks.cap_per_team in phase " + r.phase + ": " + r.tasks.cap_per_team);
                    validateEligibleTemplates("tasks", r.phase, r.tasks.eligible_templates, templateIds);
                }

                // Retry budgets
                if (r.generation == null) throw new IllegalStateException("Missing generation block in phase " + r.phase);
                if (r.generation.retry_budget_total < 1) throw new IllegalStateException("Invalid retry_budget_total in phase " + r.phase);
                if (r.generation.retry_budget_per_slot < 1) throw new IllegalStateException("Invalid retry_budget_per_slot in phase " + r.phase);
                if (r.generation.retry_budget_per_slot > r.generation.retry_budget_total) {
                    throw new IllegalStateException("retry_budget_per_slot cannot exceed retry_budget_total in phase " + r.phase);
                }

                if (byPhase.put(r.phase, r) != null) {
                    throw new IllegalStateException("Duplicate generator rule for phase " + r.phase);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("CURSED: failed while reading generator rules from " + rulesDir, e);
        }

        // Coverage check: phases 1..5 must exist
        for (int phase = 1; phase <= 5; phase++) {
            if (!byPhase.containsKey(phase)) {
                throw new IllegalStateException("Missing generator rules for phase " + phase + " (phase" + phase + ".json)");
            }
        }

        Cursed.LOGGER.info("CURSED: loaded generator rules for phases 1-5 (coverage OK)");
    }

    private static void validateEligibleTemplates(
            String blockName,
            int phase,
            List<String> eligible,
            Set<String> templateIds
    ) {
        if (eligible == null || eligible.isEmpty()) {
            throw new IllegalStateException("Missing/empty " + blockName + ".eligible_templates in phase " + phase);
        }
        for (String id : eligible) {
            if (!templateIds.contains(id)) {
                throw new IllegalStateException("Phase " + phase + " " + blockName + " references missing template: " + id);
            }
        }
    }

    public static void loadAndValidate() {
        Cursed.LOGGER.info("CURSED: validating objectives data folder structure (Baseline B)");

        URL rootUrl = ObjectivesDataLoader.class
                .getClassLoader()
                .getResource(ROOT_PATH);

        if (rootUrl == null) {
            throw new IllegalStateException(
                    "CURSED: missing required resource folder: " + ROOT_PATH
            );
        }

        try {
            Path rootDir = Path.of(rootUrl.toURI());

            for (String subfolder : REQUIRED_SUBFOLDERS) {
                Path subPath = rootDir.resolve(subfolder);

                if (!Files.exists(subPath) || !Files.isDirectory(subPath)) {
                    throw new IllegalStateException(
                            "CURSED: missing required objectives subfolder: " + subPath
                    );
                }
            }
            for (String subfolder : REQUIRED_SUBFOLDERS) {
                Path subPath = rootDir.resolve(subfolder);

                try (var stream = Files.list(subPath)) {
                    boolean hasJson = stream.anyMatch(p ->
                            Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json")
                    );

                    if (!hasJson) {
                        throw new IllegalStateException(
                                "CURSED: objectives subfolder has no .json files: " + subPath
                        );
                    }
                }
            }


        } catch (Exception e) {
            throw new RuntimeException(
                    "CURSED: failed while validating objectives data structure",
                    e
            );
        }

        Map<String, ItemPoolFile> poolsById = loadAndValidateItemPools();
        Map<String, Path> constraintsById = indexConstraintsFiles();
        Set<String> templateIds = loadAndValidateObjectiveTemplates(poolsById, constraintsById);
        loadAndValidateQuantityRules();
        loadAndValidateGeneratorRules(templateIds);

        Cursed.LOGGER.info("CURSED: objectives data folder structure OK");
    }
}
