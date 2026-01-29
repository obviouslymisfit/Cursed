package obviouslymisfit.cursed.team;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import obviouslymisfit.cursed.state.GameState;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the vanilla scoreboard's team membership as a pure projection of {@link GameState}.
 *
 * Invariant:
 * - {@link GameState} is the single source of truth.
 * - Scoreboard teams are derived from state and may be recreated/updated at any time.
 *
 * Why this exists:
 * - Team colors and membership are used as an immediate visual debugging aid.
 * - M2 requires restart-safe restoration without regenerating gameplay content.
 *
 * Naming:
 * - Scoreboard team IDs are internal and deterministic: "curse_team_1".."curse_team_N".
 * - Player-visible identity comes from color (and optionally prefix/suffix later).
 *
 * Safety:
 * - This method is idempotent: calling it repeatedly produces the same scoreboard result.
 * - It does not mutate {@link GameState}.
 */
public final class TeamScoreboardSync {

    private TeamScoreboardSync() {
        // Utility class (no instances).
    }

    /**
     * Synchronizes scoreboard teams and player membership from the given {@link GameState}.
     *
     * Expected call sites:
     * - immediately after state changes are persisted (e.g., after /curse teams set/assign/unassign).
     * - after loading persisted state on server start (once that lifecycle hook is wired).
     */
    public static void sync(MinecraftServer server, GameState state) {
        Scoreboard scoreboard = server.getScoreboard();

        // If teams are disabled, remove all CURSED scoreboard teams and clear memberships.
        if (!state.teamsEnabled || state.teamCount <= 0) {
            removeAllCursedTeams(scoreboard);
            return;
        }

        // Ensure required teams exist (1..teamCount) with deterministic colors.
        for (int teamIdx = 1; teamIdx <= state.teamCount; teamIdx++) {
            PlayerTeam team = getOrCreateTeam(scoreboard, teamIdx);
            team.setColor(colorForTeamIndex(teamIdx));
        }

        // Remove any leftover teams beyond teamCount (e.g., after reducing team count).
        removeCursedTeamsAbove(scoreboard, state.teamCount);

        // Build a set of players that should be assigned per state (name -> teamIdx).
        // Scoreboard membership uses player names, while state uses UUIDs.
        Map<UUID, Integer> assignments = state.playerTeams;

        // Track which player names should be in which team.
        // We apply this by:
        // 1) ensuring each assigned player is in the correct team
        // 2) removing any player from CURSED teams if they are unassigned in state
        Set<String> assignedNames = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            Integer teamIdx = assignments.get(uuid);

            if (teamIdx == null) {
                continue;
            }

            // Hard guard: ignore impossible assignments (should not happen if commands validate).
            if (teamIdx < 1 || teamIdx > state.teamCount) {
                continue;
            }

            String name = player.getScoreboardName();
            assignedNames.add(name);

            PlayerTeam desired = getOrCreateTeam(scoreboard, teamIdx);
            PlayerTeam current = scoreboard.getPlayersTeam(name);

            // Only change membership if needed.
            if (current == null || current != desired) {
                scoreboard.addPlayerToTeam(name, desired);
            }
        }

        // Remove online players from any CURSED team if they are not assigned in state.
        // (We only touch CURSED teams to avoid interfering with other mods/server setups.)
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String name = player.getScoreboardName();

            if (assignedNames.contains(name)) {
                continue;
            }

            PlayerTeam current = scoreboard.getPlayersTeam(name);
            if (current != null && isCursedTeamId(current.getName())) {
                scoreboard.removePlayerFromTeam(name, current);
            }
        }
    }

    private static PlayerTeam getOrCreateTeam(Scoreboard scoreboard, int teamIdx) {
        String id = cursedTeamId(teamIdx);
        PlayerTeam team = scoreboard.getPlayerTeam(id);
        if (team == null) {
            team = scoreboard.addPlayerTeam(id);
        }
        return team;
    }

    private static void removeCursedTeamsAbove(Scoreboard scoreboard, int maxTeamIdx) {
        // Scoreboard doesn't provide "list teams by prefix" directly, so we attempt known IDs.
        // Teams are capped at 8 by commands, but we keep this generic.
        for (int teamIdx = maxTeamIdx + 1; teamIdx <= 64; teamIdx++) {
            String id = cursedTeamId(teamIdx);
            PlayerTeam team = scoreboard.getPlayerTeam(id);
            if (team == null) {
                // Once we hit a gap, continue anyway; we want a clean guarantee.
                continue;
            }
            scoreboard.removePlayerTeam(team);
        }
    }

    private static void removeAllCursedTeams(Scoreboard scoreboard) {
        // Remove a bounded range of possible team IDs (commands cap at 8, but this is safe).
        for (int teamIdx = 1; teamIdx <= 64; teamIdx++) {
            String id = cursedTeamId(teamIdx);
            PlayerTeam team = scoreboard.getPlayerTeam(id);
            if (team != null) {
                scoreboard.removePlayerTeam(team);
            }
        }
    }

    private static String cursedTeamId(int teamIdx) {
        return "curse_team_" + teamIdx;
    }

    private static boolean isCursedTeamId(String id) {
        return id != null && id.startsWith("curse_team_");
    }

    private static ChatFormatting colorForTeamIndex(int teamIdx) {
        // Fixed, deterministic mapping for consistent debugging and identity.
        return switch (teamIdx) {
            case 1 -> ChatFormatting.RED;
            case 2 -> ChatFormatting.BLUE;
            case 3 -> ChatFormatting.GREEN;
            case 4 -> ChatFormatting.YELLOW;
            case 5 -> ChatFormatting.AQUA;
            case 6 -> ChatFormatting.LIGHT_PURPLE;
            case 7 -> ChatFormatting.GOLD;
            case 8 -> ChatFormatting.DARK_GRAY;
            default -> ChatFormatting.WHITE;
        };
    }
}
