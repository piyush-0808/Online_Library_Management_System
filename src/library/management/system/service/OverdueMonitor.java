package library.management.system.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import library.management.system.exception.DatabaseException;

/** One dashboard-owned daemon scheduler for overdue status/fine refreshes. */
public final class OverdueMonitor implements AutoCloseable {
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "library-overdue-monitor");
                thread.setDaemon(true);
                return thread;
            });
    private final AtomicBoolean closed = new AtomicBoolean();
    private final IssueService issueService = new IssueService();
    private final FineService fineService = new FineService();

    public OverdueMonitor(Runnable dashboardRefresh) {
        scheduler.scheduleAtFixedRate(() -> {
            if (closed.get()) {
                return;
            }
            try {
                issueService.refreshOverdueStatuses();
                fineService.refreshOverdueFinesInParallel();
                SwingUtilities.invokeLater(() -> {
                    if (!closed.get()) {
                        dashboardRefresh.run();
                    }
                });
            } catch (DatabaseException | RuntimeException exception) {
                System.err.println("Overdue refresh failed: "
                        + exception.getMessage());
                exception.printStackTrace(System.err);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            scheduler.shutdownNow();
        }
    }
}
