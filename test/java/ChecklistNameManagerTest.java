import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.Assert.*;

public class ChecklistNameManagerTest {

    @Test
    public void getChecklists_whenMissing_returnsEmpty() throws Exception {
        Path tmp = Files.createTempDirectory("cnm-missing");
        String file = tmp.resolve("checklists.properties").toString();

        ChecklistNameManager mgr = new ChecklistNameManager(file);
        Set<Checklist> set = mgr.getChecklists();
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void addUpdateRemove_flow() throws Exception {
        Path tmp = Files.createTempDirectory("cnm-flow");
        String file = tmp.resolve("checklists.properties").toString();

        ChecklistNameManager mgr = new ChecklistNameManager(file);

        Checklist c = new Checklist("Initial", "id-1");
        mgr.addChecklist(c);

        assertEquals("Initial", mgr.getNameById("id-1"));
        Checklist byId = mgr.getChecklistById("id-1");
        assertNotNull(byId);

        mgr.updateChecklistName(byId, "Renamed");
        assertEquals("Renamed", mgr.getNameById("id-1"));

        mgr.removeChecklist(byId);
        assertNull(mgr.getChecklistById("id-1"));
    }
}
