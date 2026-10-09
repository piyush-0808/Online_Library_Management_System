package library.management.system.util;

import javax.swing.SwingWorker;
import java.util.function.Consumer;

public abstract class BackgroundTask<T> extends SwingWorker<T, Void> {

    private final Consumer<T> successHandler;
    private final Consumer<Exception> errorHandler;

    protected BackgroundTask(
            Consumer<T> successHandler,
            Consumer<Exception> errorHandler) {

        this.successHandler = successHandler;
        this.errorHandler = errorHandler;
    }

    @Override
    protected final void done() {
        try {
            T result = get();

            if (successHandler != null) {
                successHandler.accept(result);
            }

        } catch (Exception ex) {
            if (errorHandler != null) {
                errorHandler.accept(ex);
            } else {
                ex.printStackTrace();
            }
        }
    }
}