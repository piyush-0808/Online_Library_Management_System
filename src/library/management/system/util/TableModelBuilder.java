package library.management.system.util;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.function.Function;

public final class TableModelBuilder {

    private TableModelBuilder() {
        // Utility class
    }

    public static <T> DefaultTableModel build(
            List<T> data,
            String[] columnNames,
            Function<T, Object[]> rowMapper) {

        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        if (data == null || rowMapper == null) {
            return model;
        }

        for (T item : data) {
            Object[] row = rowMapper.apply(item);
            model.addRow(row);
        }

        return model;
    }

    public static <T> void refresh(
            DefaultTableModel model,
            List<T> data,
            Function<T, Object[]> rowMapper) {

        model.setRowCount(0);

        if (data == null || rowMapper == null) {
            return;
        }

        for (T item : data) {
            model.addRow(rowMapper.apply(item));
        }
    }
}