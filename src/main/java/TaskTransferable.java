/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

public final class TaskTransferable implements Transferable {
    private static final DataFlavor TASK_IDS = new DataFlavor(
            DataFlavor.javaJVMLocalObjectMimeType + ";class=" + TaskIds.class.getName(),
            "Daily Checklist task IDs");

    public record TaskIds(List<String> ids) {
        public TaskIds {
            ids = List.copyOf(ids);
            if (ids.isEmpty() || ids.stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("A drag requires task IDs");
            }
        }
    }

    private final TaskIds payload;

    public TaskTransferable(List<String> ids) {
        payload = new TaskIds(ids);
    }

    public static boolean supports(Transferable transferable) {
        return transferable.isDataFlavorSupported(TASK_IDS);
    }

    public static List<String> readIds(Transferable transferable)
            throws UnsupportedFlavorException, IOException {
        Object data = transferable.getTransferData(TASK_IDS);
        if (!(data instanceof TaskIds ids)) {
            throw new IOException("Invalid task drag payload");
        }
        return ids.ids();
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { TASK_IDS };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return TASK_IDS.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return payload;
    }
}
