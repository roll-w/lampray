package tech.lamprism.lampray.system.console.shell.command;

import space.lingu.Nullable;

/**
 * @author RollW
 */
public final class ConfirmationHelper {

    /**
     * Confirm the input is "yes" (case-insensitive) or "Y".
     *
     * @param input the input string
     * @return true if confirmed, false otherwise
     */
    public static boolean confirm(@Nullable String input) {
        return input != null && (input.equalsIgnoreCase("yes") || input.equals("Y"));
    }

    private ConfirmationHelper() {
    }
}
