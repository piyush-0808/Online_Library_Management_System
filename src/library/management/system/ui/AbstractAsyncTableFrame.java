package library.management.system.ui;

import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import library.management.system.util.MessageUtil;

/** Template Method shared by data-table screens: fetch, then render on the EDT. */
public abstract class AbstractAsyncTableFrame<T> extends BaseFrame {
    private static final long serialVersionUID = 1L;
    protected AbstractAsyncTableFrame(String title, int width, int height) {
        super(title, width, height);
    }

    /** Fixed load algorithm; children supply only data access and rendering. */
    protected final void loadTableData() {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return fetchTableData();
            }

            @Override
            protected void done() {
                try {
                    renderTableData(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showTableError(exception);
                } catch (ExecutionException exception) {
                    showTableError(exception.getCause() == null
                            ? exception : exception.getCause());
                }
            }
        }.execute();
    }

    protected abstract T fetchTableData() throws Exception;

    protected abstract void renderTableData(T data);

    protected void showTableError(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = "Could not load table data.";
        }
        MessageUtil.showError(this, message);
        System.err.println("Table loading error: " + message);
    }
}
