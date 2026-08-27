package dev.mcclient.launcher;

/**
 * Where long-running work reports to. Lets the same download/launch code drive either the console
 * or the GUI's progress bar without knowing which is listening.
 */
@FunctionalInterface
public interface Progress {

    void status(String message);

    /** Byte-level progress for downloads. {@code total} is -1 when the size isn't known up front. */
    default void bytes(long done, long total) {}

    Progress CONSOLE = System.out::println;

    Progress SILENT = message -> {};
}
