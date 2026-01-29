public final class DebugLog {
    private DebugLog() {}

    public static void d(String fmt, Object... args) {
        String msg = args == null || args.length == 0 ? fmt : String.format(fmt, args);
        String ts = java.time.LocalTime.now().toString();
        System.out.println("[DEBUG] " + ts + " " + Thread.currentThread().getName() + " - " + msg);
    }
}
