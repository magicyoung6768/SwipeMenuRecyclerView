package com.magicyoung.swiperecyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.BuildConfig;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

/**
 * 滑动抽屉封装的View 以FrameLayout实现，支持层叠展示，核心原理修改 swipe_content 与 swipe_menu 的layout，
 * 同时封装ScrollerCompat 处理展开与收起动画
 *
 * @author magicyoung6768
 * @see SwipeMenuRecyclerView
 */
public class SwipeMenuLayout extends FrameLayout {
    /**
     * 关闭状态
     */
    private static final int STATE_CLOSE = 0;
    /**
     * 开启状态
     */
    private static final int STATE_OPEN = 1;
    private static final boolean OVER_API_11 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    private int mSwipeDirection;
    /**
     * 盖于上层的view
     */
    private View mContentView;
    /**
     * 下层，抽屉view
     */
    private View mMenuView;
    private int mDownX;
    private int state = STATE_CLOSE;
    private GestureDetectorCompat mGestureDetector;
    private OnGestureListener mGestureListener;
    private boolean isFling;
    private ScrollerCompat mOpenScroller;
    private ScrollerCompat mCloseScroller;
    private int mBaseX;
    private Interpolator mCloseInterpolator;
    private Interpolator mOpenInterpolator;
    private ViewConfiguration mViewConfiguration;
    private boolean mSwipeEnable = true;
    private int mAnimDuration;
    private boolean mChangeMenuView = false;

    public SwipeMenuLayout(Context context) {
        this(context, null);
    }

    public SwipeMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeMenuLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!isInEditMode()) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeMenu, 0, defStyle);
            mAnimDuration = a.getInteger(R.styleable.SwipeMenu_anim_duration, 500);
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setClickable(true);
        mContentView = findViewById(R.id.swipe_content);
        if (mContentView == null) {
            if (BuildConfig.DEBUG) {
                throw new IllegalArgumentException("not find contentView by id swipe_content");
            }
        }
        mMenuView = findViewById(R.id.swipe_menu);
        if (mMenuView == null) {
            if (BuildConfig.DEBUG) {
                throw new IllegalArgumentException("not find menuView by id swipe_menu");
            }
        }
        mViewConfiguration = ViewConfiguration.get(getContext());
        init();
    }

    public void setSwipeDirection(int swipeDirection) {
        mSwipeDirection = swipeDirection;
    }

    public void init() {
        mGestureListener = new SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                isFling = false;
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (velocityX > mViewConfiguration.getScaledMinimumFlingVelocity() || velocityY > mViewConfiguration.getScaledMinimumFlingVelocity()) {
                    isFling = true;
                }
                return isFling;
            }
        };
        mGestureDetector = new GestureDetectorCompat(getContext(), mGestureListener);
        mCloseScroller = ScrollerCompat.create(getContext());
        mOpenScroller = ScrollerCompat.create(getContext());
    }

    public void setCloseInterpolator(Interpolator closeInterpolator) {
        mCloseInterpolator = closeInterpolator;
        if (mCloseInterpolator != null) {
            mCloseScroller = ScrollerCompat.create(getContext(),
                    mCloseInterpolator);
        }
    }

    public void setOpenInterpolator(Interpolator openInterpolator) {
        mOpenInterpolator = openInterpolator;
        if (mOpenInterpolator != null) {
            mOpenScroller = ScrollerCompat.create(getContext(),
                    mOpenInterpolator);
        }
    }

    public boolean onSwipe(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) event.getX();
                isFling = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int dis = (int) (mDownX - event.getX());
                if (state == STATE_OPEN) {
                    dis += mMenuView.getWidth() * mSwipeDirection;
                }
                swipe(dis);
                break;
            case MotionEvent.ACTION_UP:
                int upX = (int) event.getX();
                if ((isFling || Math.abs(mDownX - event.getX()) > 0) &&
                        Math.signum(mDownX - event.getX()) == mSwipeDirection || ignoreMenuCLick(upX)) {
                    smoothOpenMenu();
                } else {
                    if (isOpen()) {
                        smoothCloseMenu();
                    }
                    return false;
                }
                break;
        }
        return true;
    }

    private boolean ignoreMenuCLick(int upX) {
        if (mSwipeDirection == SwipeMenuRecyclerView.DIRECTION_LEFT) {
            return upX > mContentView.getMeasuredWidth() - mMenuView.getMeasuredWidth() && mDownX > mContentView.getMeasuredWidth() - mMenuView.getMeasuredWidth();
        } else {
            return upX < mMenuView.getMeasuredWidth() && mDownX < mMenuView.getMeasuredWidth();
        }
    }

    public boolean isOpen() {
        return state == STATE_OPEN;
    }

    private void swipe(int dis) {
        if (Math.signum(dis) != mSwipeDirection) {
            dis = 0;
        } else if (Math.abs(dis) > mMenuView.getWidth()) {
            dis = mMenuView.getWidth() * mSwipeDirection;
            state = STATE_OPEN;
        }

        LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
        int lGap = getPaddingLeft() + lp.leftMargin;
        mContentView.layout(lGap - dis, mContentView.getTop(),
                lGap + getSupportWidth(mContentView) - dis, mContentView.getBottom());

        if (mSwipeDirection == SwipeMenuRecyclerView.DIRECTION_LEFT) {
            if (mChangeMenuView) {
                mMenuView.layout(getMeasuredWidth() - dis, mMenuView.getTop(),
                        getMeasuredWidth() + getSupportWidth(mMenuView) - dis, mMenuView.getBottom());
            } else {
                mMenuView.layout(mContentView.getMeasuredWidth() - mMenuView.getMeasuredWidth(), mMenuView.getTop(),
                        mContentView.getMeasuredWidth(), mMenuView.getBottom());
            }
        } else {
            if (mChangeMenuView) {
                mMenuView.layout(-getSupportWidth(mMenuView) - dis, mMenuView.getTop(), -dis, mMenuView.getBottom());
            } else {
                mMenuView.layout(0, mMenuView.getTop(), mMenuView.getMeasuredWidth(), mMenuView.getBottom());
            }
        }
    }

    @Override
    public void computeScroll() {
        if (state == STATE_OPEN) {
            if (mOpenScroller.computeScrollOffset()) {
                swipe(mOpenScroller.getCurrX() * mSwipeDirection);
                postInvalidate();
            }
        } else {
            if (mCloseScroller.computeScrollOffset()) {
                swipe((mBaseX - mCloseScroller.getCurrX()) * mSwipeDirection);
                postInvalidate();
            }
        }
    }

    public void smoothCloseMenu() {
        closeOpenedMenu();
    }

    public void closeOpenedMenu() {
        state = STATE_CLOSE;
        if (mSwipeDirection == SwipeMenuRecyclerView.DIRECTION_LEFT) {
            mBaseX = -mContentView.getLeft();
            mCloseScroller.startScroll(0, 0, mMenuView.getWidth(), 0, mAnimDuration);
        } else {
            mBaseX = mContentView.getLeft();
            mCloseScroller.startScroll(0, 0, mMenuView.getWidth(), 0, mAnimDuration);
        }
        postInvalidate();
    }

    public void smoothOpenMenu() {
        state = STATE_OPEN;
        if (mSwipeDirection == SwipeMenuRecyclerView.DIRECTION_LEFT) {
            mOpenScroller.startScroll(-mContentView.getLeft(), 0, mMenuView.getWidth(), 0, mAnimDuration);
        } else {
            mOpenScroller.startScroll(mContentView.getLeft(), 0, mMenuView.getWidth(), 0, mAnimDuration);
        }
        postInvalidate();
    }

    public void closeMenu() {
        if (mCloseScroller.computeScrollOffset()) {
            mCloseScroller.abortAnimation();
        }
        if (state == STATE_OPEN) {
            state = STATE_CLOSE;
            swipe(0);
        }
    }

    public void openMenu() {
        if (state == STATE_CLOSE) {
            state = STATE_OPEN;
            swipe(mMenuView.getWidth() * mSwipeDirection);
        }
    }

    public View getMenuView() {
        return mMenuView;
    }

    public View getContentView() {
        return mContentView;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
        int lGap = getPaddingLeft() + lp.leftMargin;
        int tGap = getPaddingTop() + lp.topMargin;
        mContentView.layout(lGap, tGap, lGap + getSupportWidth(mContentView),
                tGap + getSupportWidth(mContentView));
        lp = (LayoutParams) mMenuView.getLayoutParams();
        tGap = getPaddingTop() + lp.topMargin;
        if (mSwipeDirection == SwipeMenuRecyclerView.DIRECTION_LEFT) {
            mMenuView.layout(getMeasuredWidth(), tGap,
                    getMeasuredWidth() + getSupportWidth(mMenuView),
                    tGap + mMenuView.getMeasuredHeightAndState());
        } else {
            mMenuView.layout(-getSupportWidth(mMenuView), tGap,
                    0, tGap + mMenuView.getMeasuredHeightAndState());
        }
    }

    public void setSwipeEnable(boolean swipeEnable) {
        this.mSwipeEnable = swipeEnable;
    }

    public boolean isSwipeEnable() {
        return mSwipeEnable;
    }

    public void setChangeMenuView(boolean changeMenuView) {
        this.mChangeMenuView = changeMenuView;
    }

    public boolean isChangeMenuView() {
        return mChangeMenuView;
    }

    private int getSupportWidth(View view) {
        return OVER_API_11 ? view.getMeasuredWidthAndState() : view.getMeasuredWidth();
    }
}
