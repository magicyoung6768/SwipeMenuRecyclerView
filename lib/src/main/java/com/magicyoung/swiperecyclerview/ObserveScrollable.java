package com.magicyoung.swiperecyclerview;

import android.view.ViewGroup;

/**
 * 用于ObservableRecyclerView 滑动监听的接口
 *
 * @author magicyoung6768
 */
public interface ObserveScrollable {
    /**
     * Set a callback listener.<br>
     *
     * @param listener Listener to set.
     */
    @Deprecated
    void setScrollViewCallbacks(ObservableRecyclerView.ObservableScrollViewCallbacks listener);

    /**
     * Add a callback listener.
     */
    void addScrollViewCallbacks(ObservableRecyclerView.ObservableScrollViewCallbacks listener);

    /**
     * Remove a callback listener.
     */
    void removeScrollViewCallbacks(ObservableRecyclerView.ObservableScrollViewCallbacks listener);

    /**
     * Clear callback listeners.
     */
    void clearScrollViewCallbacks();

    /**
     * Scroll vertically to the absolute Y.<br>
     * Implemented classes are expected to scroll to the exact Y pixels from the top,
     * but it depends on the type of the widget.
     *
     * @param y Vertical position to scroll to.
     */
    void scrollVerticallyTo(int y);

    /**
     * Return the current Y of the scrollable view.
     *
     * @return Current Y pixel.
     */
    int getCurrentScrollY();

    /**
     * Set a touch motion event delegation ViewGroup.<br>
     * This is used to pass motion events back to parent view.
     * It's up to the implementation classes whether or not it works.
     *
     * @param viewGroup ViewGroup object to dispatch motion events.
     */
    void setTouchInterceptionViewGroup(ViewGroup viewGroup);
}
