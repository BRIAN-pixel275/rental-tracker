package com.brian.rentaltracker.transport;

import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable numbered-list pagination used by every list screen in the app:
 * up to pageSize rows per page, with Next/Previous appearing only when those
 * pages actually exist, plus a Back option.
 */
public class Paginator<T> {

    private static final int PAGE_SIZE = 5;

    private final List<T> items;
    private final Consumer<T> renderRow;
    private int page = 0;

    public Paginator(List<T> items, Consumer<T> renderRow) {
        this.items = items;
        this.renderRow = renderRow;
    }

    public int totalPages() {
        return items.isEmpty() ? 1 : (int) Math.ceil(items.size() / (double) PAGE_SIZE);
    }

    public List<T> currentPageItems() {
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, items.size());
        return from >= items.size() ? List.of() : items.subList(from, to);
    }

    public boolean hasNext() { return page < totalPages() - 1; }
    public boolean hasPrevious() { return page > 0; }
    public void next() { if (hasNext()) page++; }
    public void previous() { if (hasPrevious()) page--; }

    /** Renders the current page: numbered rows, then Next/Previous/Back as applicable. */
    public void render(ConsoleIO io) {
        List<T> pageItems = currentPageItems();
        int number = 1;
        for (T item : pageItems) {
            io.printf("%d) ", number++);
            renderRow.accept(item);
        }
        if (hasNext()) {
            io.print(number + ") Next page");
            number++;
        }
        if (hasPrevious()) {
            io.print(number + ") Previous page");
            number++;
        }
        io.print(number + ") Back to menu");
    }
}
