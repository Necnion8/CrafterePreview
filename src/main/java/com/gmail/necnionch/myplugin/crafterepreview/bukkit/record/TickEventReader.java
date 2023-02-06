package com.gmail.necnionch.myplugin.crafterepreview.bukkit.record;

import java.util.NoSuchElementException;

public class TickEventReader {

    private final TickData[] events;
    private int currentIndex;

    public TickEventReader(TickData[] events) {
        this.events = events;
    }

    public TickData next() throws NoSuchElementException {
        try {
            return events[currentIndex++];
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

    public boolean hasNext() {
        return events.length > currentIndex + 1;
    }

    public TickData back() throws NoSuchElementException {
        try {
            return events[currentIndex--];
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

    public boolean hasBack() {
        return currentIndex > 0;
    }

    public int getCursor() {
        return currentIndex;
    }

    public TickData getCurrent() {
        return events[currentIndex];
    }

    public int getEventCount() {
        return events.length;
    }

    public TickData[] ticks() {
        return events;
    }

    public long getFirstTick() {
        return (events.length <= 0) ? 0 : events[0].getTick();
    }

    public long getLastTick() {
        return (events.length <= 0) ? 0 : events[events.length - 1].getTick();
    }
}
