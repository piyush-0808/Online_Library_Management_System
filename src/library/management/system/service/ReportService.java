package library.management.system.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import library.management.system.dao.ReportDAO;
import library.management.system.exception.DatabaseException;
import library.management.system.item.LibraryItem;
import library.management.system.model.ItemCategory;

/** Business-facing report operations; SQL lives in ReportDAO. */
public class ReportService {

    private final ReportDAO reportDAO = new ReportDAO();
    private final BookService bookService = new BookService();

    /** Loads one detailed report; grouping uses typed maps for stable output. */
    public ReportTable getDetailedReport(
            String reportType, String status, String search)
            throws DatabaseException {
        return switch (reportType) {
            case "CURRENTLY_ISSUED" -> circulationReport(
                    "Currently Issued Books", "ISSUED", search);
            case "OVERDUE" -> circulationReport("Overdue Books", "OVERDUE", search);
            case "FINE_COLLECTION" -> fineCollectionReport(status, search);
            case "INVENTORY_BY_CATEGORY" -> inventoryByCategory();
            case "FINE_TOTALS" -> fineTotals();
            default -> throw new IllegalArgumentException(
                    "Unknown report type: " + reportType);
        };
    }

    private ReportTable circulationReport(String title, String status, String search)
            throws DatabaseException {
        List<Object[]> rows = new ArrayList<>();
        for (ReportDAO.CirculationRow row :
                reportDAO.findCirculationRows(status, search)) {
            rows.add(new Object[]{row.issueId(), row.memberId(), row.memberName(),
                    row.bookId(), row.bookTitle(), row.issueDate(),
                    row.dueDate(), row.status()});
        }
        return new ReportTable(title,
                List.of("Issue ID", "Member ID", "Member", "Book ID",
                        "Book", "Issue Date", "Due Date", "Status"), rows);
    }

    private ReportTable fineCollectionReport(String status, String search)
            throws DatabaseException {
        List<Object[]> rows = new ArrayList<>();
        for (ReportDAO.FineCollectionRow row :
                reportDAO.findFineCollectionRows(status, search)) {
            rows.add(new Object[]{row.fineId(), row.issueId(), row.memberId(),
                    row.memberName(), row.bookTitle(), row.daysLate(),
                    row.amount(), row.status(), row.createdAt(), row.paidAt()});
        }
        return new ReportTable("Fine Collection",
                List.of("Fine ID", "Issue ID", "Member ID", "Member", "Book",
                        "Days Late", "Amount", "Status", "Created", "Paid"), rows);
    }

    private ReportTable inventoryByCategory() throws DatabaseException {
        Map<ItemCategory, Long> counts = new EnumMap<>(ItemCategory.class);
        for (ItemCategory category : ItemCategory.values()) {
            counts.put(category, 0L);
        }
        for (LibraryItem item : bookService.findAll()) {
            counts.merge(item.getCategory(), 1L, Long::sum);
        }
        List<Object[]> rows = counts.entrySet().stream()
                .map(entry -> new Object[]{entry.getKey().getDisplayName(), entry.getValue()})
                .toList();
        return new ReportTable("Inventory by Category",
                List.of("Category", "Titles"), rows);
    }

    private ReportTable fineTotals() throws DatabaseException {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (ReportDAO.FineCollectionRow fine :
                reportDAO.findFineCollectionRows("ALL", "")) {
            totals.merge(fine.status(), fine.amount(), BigDecimal::add);
        }
        Map<String, BigDecimal> sortedTotals = new TreeMap<>(totals);
        List<Object[]> rows = sortedTotals.entrySet().stream()
                .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                .toList();
        return new ReportTable("Fine Totals by Status",
                List.of("Status", "Amount"), rows);
    }

    public record ReportTable(String title, List<String> columns, List<Object[]> rows) { }

    public DashboardReport getDashboardReport() throws DatabaseException {
        ReportDAO.DashboardMetrics metrics = reportDAO.getDashboardMetrics();
        return new DashboardReport(
                metrics.totalBooks(), metrics.totalCopies(),
                metrics.availableCopies(), metrics.totalMembers(),
                metrics.activeMembers(), metrics.pendingMembers(),
                metrics.blockedMembers(), metrics.activeIssues(),
                metrics.overdueIssues(), metrics.returnedIssues(),
                metrics.unpaidFines(), metrics.paidFines(),
                metrics.unpaidFineCount(), metrics.totalReservations(),
                metrics.waitingReservations(), metrics.fulfilledReservations(),
                metrics.cancelledReservations());
    }

    public int getTotalBooks() throws DatabaseException {
        return reportDAO.getTotalBooks();
    }

    public int getTotalMembers() throws DatabaseException {
        return reportDAO.getTotalMembers();
    }

    public int getActiveIssues() throws DatabaseException {
        return reportDAO.getActiveIssues();
    }

    public int getOverdueIssues() throws DatabaseException {
        return reportDAO.getOverdueIssues();
    }

    public double getTotalUnpaidFines() throws DatabaseException {
        return reportDAO.getTotalUnpaidFines();
    }

    public static final class DashboardReport {
        private final int totalBooks;
        private final int totalCopies;
        private final int availableCopies;
        private final int totalMembers;
        private final int activeMembers;
        private final int pendingMembers;
        private final int blockedMembers;
        private final int activeIssues;
        private final int overdueIssues;
        private final int returnedIssues;
        private final double unpaidFines;
        private final double paidFines;
        private final int unpaidFineCount;
        private final int totalReservations;
        private final int waitingReservations;
        private final int fulfilledReservations;
        private final int cancelledReservations;

        private DashboardReport(
                int totalBooks, int totalCopies, int availableCopies,
                int totalMembers, int activeMembers, int pendingMembers,
                int blockedMembers, int activeIssues, int overdueIssues,
                int returnedIssues, double unpaidFines, double paidFines,
                int unpaidFineCount, int totalReservations,
                int waitingReservations, int fulfilledReservations,
                int cancelledReservations) {
            this.totalBooks = totalBooks;
            this.totalCopies = totalCopies;
            this.availableCopies = availableCopies;
            this.totalMembers = totalMembers;
            this.activeMembers = activeMembers;
            this.pendingMembers = pendingMembers;
            this.blockedMembers = blockedMembers;
            this.activeIssues = activeIssues;
            this.overdueIssues = overdueIssues;
            this.returnedIssues = returnedIssues;
            this.unpaidFines = unpaidFines;
            this.paidFines = paidFines;
            this.unpaidFineCount = unpaidFineCount;
            this.totalReservations = totalReservations;
            this.waitingReservations = waitingReservations;
            this.fulfilledReservations = fulfilledReservations;
            this.cancelledReservations = cancelledReservations;
        }

        public int getTotalBooks() { return totalBooks; }
        public int getTotalCopies() { return totalCopies; }
        public int getAvailableCopies() { return availableCopies; }
        public int getIssuedCopies() { return totalCopies - availableCopies; }
        public int getTotalMembers() { return totalMembers; }
        public int getActiveMembers() { return activeMembers; }
        public int getPendingMembers() { return pendingMembers; }
        public int getBlockedMembers() { return blockedMembers; }
        public int getActiveIssues() { return activeIssues; }
        public int getOverdueIssues() { return overdueIssues; }
        public int getReturnedIssues() { return returnedIssues; }
        public double getUnpaidFines() { return unpaidFines; }
        public double getPaidFines() { return paidFines; }
        public int getUnpaidFineCount() { return unpaidFineCount; }
        public int getTotalReservations() { return totalReservations; }
        public int getWaitingReservations() { return waitingReservations; }
        public int getFulfilledReservations() { return fulfilledReservations; }
        public int getCancelledReservations() { return cancelledReservations; }
    }
}
