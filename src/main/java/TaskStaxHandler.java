import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * Lightweight StAX-based handler for tasks XML. Uses streaming read/write
 * and writes via a temp file + atomic move for safety.
 */
public class TaskStaxHandler {
    private final String fileName;
    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();
    // Note: we avoid XMLOutputFactory / XMLStreamWriter for writes and do a manual
    // buffered UTF-8 writer to reduce per-element allocation and method-call overhead.
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    public TaskStaxHandler(String fileName) {
        this.fileName = fileName;
    }

    public void ensureFileExists() throws Exception {
        Path target = Paths.get(fileName);
        File parent = target.toAbsolutePath().getParent().toFile();
        if (!parent.exists()) parent.mkdirs();
        File f = target.toFile();
        if (!f.exists()) {
            setAllTasks(new ArrayList<>());
        }
    }

    public List<Task> parseAllTasks() throws Exception {
        List<Task> out = new ArrayList<>();
        File f = new File(fileName);
        if (!f.exists()) return out;
        try (InputStream is = new FileInputStream(f)) {
            XMLStreamReader r = INPUT_FACTORY.createXMLStreamReader(is, "UTF-8");
            Task current = null;
            String currentElement = null;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String name = r.getLocalName();
                    if ("task".equals(name)) {
                        String id = r.getAttributeValue(null, "id");
                        current = new Task(id, "", TaskType.CUSTOM, null, false, null, null, null);
                    } else {
                        currentElement = name;
                    }
                } else if (ev == XMLStreamConstants.CHARACTERS) {
                    if (current != null && currentElement != null) {
                        String txt = r.getText();
                        switch (currentElement) {
                            case "name": current.setName(txt); break;
                            case "type": try { current.setType(TaskType.valueOf(txt)); } catch (Exception ex) {} break;
                            case "checklistId": current.setChecklistId(txt); break;
                            case "parentId": current.setParentId(txt); break;
                            case "weekday": current.setWeekday(txt); break;
                            case "done": current.setDone(Boolean.parseBoolean(txt)); break;
                            case "note": current.setNote(txt); break;
                            case "doneDate":
                                if (txt != null && !txt.isEmpty()) {
                                    try {
                                        LocalDate ld = LocalDate.parse(txt, DATE_FMT);
                                        current.setDoneDate(java.util.Date.from(ld.atStartOfDay(ZONE).toInstant()));
                                    } catch (Exception ignore) { }
                                }
                                break;
                        }
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    String name = r.getLocalName();
                    if ("task".equals(name)) {
                        if (TaskXmlHandler.validateTask(current)) out.add(current);
                        current = null;
                    } else {
                        currentElement = null;
                    }
                }
            }
            r.close();
        }
        return out;
    }

    public void setAllTasks(List<Task> tasks) throws Exception {
        KMLOutput(tasks);
    }

    public void updateTasks(List<Task> tasks) throws Exception {
        // For simplicity: read all, apply updates/inserts, write full doc
        List<Task> current = parseAllTasks();
        java.util.Map<String, Task> map = new java.util.HashMap<>();
        for (Task t : current) map.put(t.getId(), t);
        for (Task t : tasks) map.put(t.getId(), t);
        List<Task> merged = new ArrayList<>(map.values());
        KMLOutput(merged);
    }

    public void addTask(Task task) throws Exception {
        List<Task> current = parseAllTasks();
        current.add(task);
        KMLOutput(current);
    }

    public void removeTask(Task task) throws Exception {
        List<Task> current = parseAllTasks();
        current.removeIf(t -> t.getId().equals(task.getId()));
        KMLOutput(current);
    }

    public void checkAndResetPastDoneDate(Task task, String today) throws java.text.ParseException {
        if (task.isDone() && task.getDoneDate() != null && !task.getDoneDate().trim().isEmpty()) {
            java.util.Date doneDate = task.getParsedDoneDate(); // Use lazy parsing
            if (doneDate == null) return; // Skip if parsing failed
            java.util.Date todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(today);
            if (doneDate.before(todayDate)) {
                task.setDone(false);
                task.setDoneDate(null);
            }
        }
    }

    private void KMLOutput(List<Task> tasks) throws Exception {
        Path target = Paths.get(fileName);
        Path parent = target.toAbsolutePath().getParent();
        if (parent == null) parent = Paths.get(".");
        Path tmp = parent.resolve(target.getFileName().toString() + ".tmp");

        try (OutputStream fos = new FileOutputStream(tmp.toFile()); OutputStream os = new BufferedOutputStream(fos, 32 * 1024)) {
            // write header
            writeUtf8(os, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tasks>\n");

            StringBuilder sb = new StringBuilder(256);
            for (Task t : tasks) {
                sb.setLength(0);
                sb.append("  <task id=\"");
                escapeXmlToBuilder(t.getId(), sb);
                sb.append("\">\n");

                // name
                sb.append("    <name>"); escapeXmlToBuilder(t.getName(), sb); sb.append("</name>\n");
                // type
                sb.append("    <type>"); sb.append(t.getType() == null ? "CUSTOM" : t.getType().name()); sb.append("</type>\n");
                if (t.getChecklistId() != null) { sb.append("    <checklistId>"); escapeXmlToBuilder(t.getChecklistId(), sb); sb.append("</checklistId>\n"); }
                if (t.getParentId() != null) { sb.append("    <parentId>"); escapeXmlToBuilder(t.getParentId(), sb); sb.append("</parentId>\n"); }
                if (t.getWeekday() != null) { sb.append("    <weekday>"); escapeXmlToBuilder(t.getWeekday(), sb); sb.append("</weekday>\n"); }
                sb.append("    <done>"); sb.append(t.isDone()); sb.append("</done>\n");
                sb.append("    <doneDate>"); if (t.getDoneDate() != null) escapeXmlToBuilder(t.getDoneDate(), sb); sb.append("</doneDate>\n");
                if (t.getNote() != null && !t.getNote().isEmpty()) { sb.append("    <note>"); escapeXmlToBuilder(t.getNote(), sb); sb.append("</note>\n"); }

                sb.append("  </task>\n");
                writeUtf8(os, sb.toString());
            }

            writeUtf8(os, "</tasks>\n");
            os.flush();
        }

        // Move into place atomically
        java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private void writeUtf8(OutputStream os, String s) throws java.io.IOException {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        os.write(b, 0, b.length);
    }

    private void escapeXmlToBuilder(String s, StringBuilder sb) {
        if (s == null) return;
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default: sb.append(c);
            }
        }
    }
}
