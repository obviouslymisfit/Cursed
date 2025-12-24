package obviouslymisfit.cursed.config;

/**
 * Debug configuration (dev/testing only).
 * This MUST be OFF by default in shipped configs.
 */
public final class DebugConfig {
    /** Master switch for all debug-only behavior (commands, verbose logs, forced states). */
    public boolean enabled = false;

    public DebugConfig() {}
}
