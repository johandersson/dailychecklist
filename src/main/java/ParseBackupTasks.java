/* Utility to parse a tasks.xml with the project's TaskXmlHandler and print summary */
public class ParseBackupTasks {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: ParseBackupTasks <path-to-tasks.xml>");
            System.exit(2);
        }
        String path = args[0];
        System.out.println("Parsing: " + path);
        TaskXmlHandler handler = new TaskXmlHandler(path);
        java.util.List<Task> tasks = handler.parseAllTasks();
        System.out.println("Parsed tasks: " + tasks.size());
        long parents = tasks.stream().filter(t -> t.getParentId() == null || t.getParentId().trim().isEmpty()).count();
        long subtasks = tasks.size() - parents;
        System.out.println("parents=" + parents + " subtasks=" + subtasks);
        System.out.println("Sample tasks (first 20):");
        for (int i = 0; i < Math.min(20, tasks.size()); i++) {
            Task t = tasks.get(i);
            System.out.println(String.format("%d: id=%s type=%s checklistId=%s name=%s", i, t.getId(), t.getType(), t.getChecklistId(), t.getName()));
        }
    }
}
