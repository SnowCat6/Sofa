/* This file is auto-generated from BrowseFragment.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sgottard.sofa.support;

import com.sgottard.sofa.ContentFragment;
import com.sgottard.sofa.R;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.transition.TransitionListener;
import android.support.v17.leanback.widget.BrowseFrameLayout;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowHeaderPresenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.TitleView;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import java.lang.ref.WeakReference;
import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * A fragment for creating Leanback browse screens. It is composed of a
 * RowsSupportFragment and a HeadersSupportFragment.
 * <p>
 * A BrowseSupportFragment renders the elements of its {@link ObjectAdapter} as a set
 * of rows in a vertical list. The elements in this adapter must be subclasses
 * of {@link Row}.
 * <p>
 * The HeadersSupportFragment can be set to be either shown or hidden by default, or
 * may be disabled entirely. See {@link #setHeadersState} for details.
 * <p>
 * By default the BrowseSupportFragment includes support for returning to the headers
 * when the user presses Back. For Activities that customize {@link
 * android.support.v4.app.FragmentActivity#onBackPressed()}, you must disable this default Back key support by
 * calling {@link #setHeadersTransitionOnBackEnabled(boolean)} with false and
 * use {@link android.support.v17.leanback.app.BrowseSupportFragment.BrowseTransitionListener} and
 * {@link #startHeadersTransition(boolean)}.
 * <p>
 * The recommended theme to use with a BrowseSupportFragment is
 * {@link R.style#Theme_Leanback_Browse}.
 * </p>
 */
public class BrowseSupportFragment extends BaseSupportFragment {

    // BUNDLE attribute for saving header show/hide status when backstack is used:
    static final String HEADER_STACK_INDEX = "headerStackIndex";
    // BUNDLE attribute for saving header show/hide status when backstack is not used:
    static final String HEADER_SHOW = "headerShow";

    private boolean mUseHeaderTitleWhenExpanded = false;
    private String mOriginalTitle;

    final class BackStackListener implements FragmentManager.OnBackStackChangedListener {
        int mLastEntryCount;
        int mIndexOfHeadersBackStack;

        BackStackListener() {
            mLastEntryCount = getFragmentManager().getBackStackEntryCount();
            mIndexOfHeadersBackStack = -1;
        }

        void load(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mIndexOfHeadersBackStack = savedInstanceState.getInt(HEADER_STACK_INDEX, -1);
                mShowingHeaders = mIndexOfHeadersBackStack == -1;
            } else {
                if (!mShowingHeaders) {
                    getFragmentManager().beginTransaction()
                            .addToBackStack(mWithHeadersBackStackName).commit();
                }
            }
        }

        void save(Bundle outState) {
            outState.putInt(HEADER_STACK_INDEX, mIndexOfHeadersBackStack);
        }


        @Override
        public void onBackStackChanged() {
            if (getFragmentManager() == null) {
                Log.w(TAG, "getFragmentManager() is null, stack:", new Exception());
                return;
            }
            int count = getFragmentManager().getBackStackEntryCount();
            // if backstack is growing and last pushed entry is "headers" backstack,
            // remember the index of the entry.
            if (count > mLastEntryCount) {
                BackStackEntry entry = getFragmentManager().getBackStackEntryAt(count - 1);
                if (mWithHeadersBackStackName.equals(entry.getName())) {
                    mIndexOfHeadersBackStack = count - 1;
                }
            } else if (count < mLastEntryCount) {
                // if popped "headers" backstack, initiate the show header transition if needed
                if (mIndexOfHeadersBackStack >= count) {
                    mIndexOfHeadersBackStack = -1;
                    if (!mShowingHeaders) {
                        startHeadersTransitionInternal(true);
                    }
                }
            }
            mLastEntryCount = count;
        }
    }

    /**
     * Listener for transitions between browse headers and rows.
     */
    public static class BrowseTransitionListener {
        /**
         * Callback when headers transition starts.
         *
         * @param withHeaders True if the transition will result in headers
         *        being shown, false otherwise.
         */
        public void onHeadersTransitionStart(boolean withHeaders) {
        }
        /**
         * Callback when headers transition stops.
         *
         * @param withHeaders True if the transition will result in headers
         *        being shown, false otherwise.
         */
        public void onHeadersTransitionStop(boolean withHeaders) {
        }
    }

    private class SetSelectionRunnable implements Runnable {
        static final int TYPE_INVALID = -1;
        static final int TYPE_INTERNAL_SYNC = 0;
        static final int TYPE_USER_REQUEST = 1;

        private int mPosition;
        private int mType;
        private boolean mSmooth;

        SetSelectionRunnable() {
            reset();
        }

        void post(int position, int type, boolean smooth) {
            // Posting the set selection, rather than calling it immediately, prevents an issue
            // with adapter changes.  Example: a row is added before the current selected row;
            // first the fast lane view updates its selection, then the rows fragment has that
            // new selection propagated immediately; THEN the rows view processes the same adapter
            // change and moves the selection again.
            if (type >= mType) {
                mPosition = position;
                mType = type;
                mSmooth = smooth;
                mBrowseFrame.removeCallbacks(this);
                mBrowseFrame.post(this);
            }
        }

        @Override
        public void run() {
            setSelection(mPosition, mSmooth);
            reset();
        }

        private void reset() {
            mPosition = -1;
            mType = TYPE_INVALID;
            mSmooth = false;
        }
    }

    private static final String TAG = "BrowseSupportFragment";

    private static final String LB_HEADERS_BACKSTACK = "lbHeadersBackStack_";

    private static boolean DEBUG = false;

    /** The headers fragment is enabled and shown by default. */
    public static final int HEADERS_ENABLED = 1;

    /** The headers fragment is enabled and hidden by default. */
    public static final int HEADERS_HIDDEN = 2;

    /** The headers fragment is disabled and will never be shown. */
    public static final int HEADERS_DISABLED = 3;

    private WeakReference<? extends ContentFragment> currentFragmentRef;
    private RowsSupportFragment mRowsSupportFragment;
    protected HeadersSupportFragment mHeadersSupportFragment;

    private ObjectAdapter mAdapter;

    private int mHeadersState = HEADERS_ENABLED;
    private int mBrandColor = Color.TRANSPARENT;
    private boolean mBrandColorSet;

    private BrowseFrameLayout mBrowseFrame;
    private boolean mHeadersBackStackEnabled = true;
    private String mWithHeadersBackStackName;
    private boolean mShowingHeaders = true;
    private boolean mCanShowHeaders = true;
    private int mContainerListMarginStart;
    private int mContainerListAlignTop;
    private boolean mRowScaleEnabled = true;
    private OnItemViewSelectedListener mExternalOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;
    protected int mSelectedPosition = -1;

    private PresenterSelector mHeaderPresenterSelector;
    private final SetSelectionRunnable mSetSelectionRunnable = new SetSelectionRunnable();
    private final ToggleTitleRunnable mToggleTitleRunnable = new ToggleTitleRunnable();

    // transition related:
    private Object mSceneWithHeaders;
    private Object mSceneWithoutHeaders;
    private Object mSceneAfterEntranceTransition;
    private Object mHeadersTransition;
    private BackStackListener mBackStackChangedListener;
    private BrowseTransitionListener mBrowseTransitionListener;

    private static final String ARG_TITLE = BrowseSupportFragment.class.getCanonicalName() + ".title";
    private static final String ARG_BADGE_URI = BrowseSupportFragment.class.getCanonicalName() + ".badge";
    private static final String ARG_HEADERS_STATE =
        BrowseSupportFragment.class.getCanonicalName() + ".headersState";

    /**
     * Creates arguments for a browse fragment.
     *
     * @param args The Bundle to place arguments into, or null if the method
     *        should return a new Bundle.
     * @param title The title of the BrowseSupportFragment.
     * @param headersState The initial state of the headers of the
     *        BrowseSupportFragment. Must be one of {@link #HEADERS_ENABLED}, {@link
     *        #HEADERS_HIDDEN}, or {@link #HEADERS_DISABLED}.
     * @return A Bundle with the given arguments for creating a BrowseSupportFragment.
     */
    public static Bundle createArgs(Bundle args, String title, int headersState) {
        if (args == null) {
            args = new Bundle();
        }
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_HEADERS_STATE, headersState);
        return args;
    }

    /**
     * Sets the brand color for the browse fragment. The brand color is used as
     * the primary color for UI elements in the browse fragment. For example,
     * the background color of the headers fragment uses the brand color.
     *
     * @param color The color to use as the brand color of the fragment.
     */
    public void setBrandColor(@ColorInt int color) {
        mBrandColor = color;
        mBrandColorSet = true;

        if (mHeadersSupportFragment != null) {
            mHeadersSupportFragment.setBackgroundColor(mBrandColor);
        }
    }

    /**
     * Returns the brand color for the browse fragment.
     * The default is transparent.
     */
    @ColorInt
    public int getBrandColor() {
        return mBrandColor;
    }

    /**
     * Sets the adapter containing the rows for the fragment.
     *
     * <p>The items referenced by the adapter must be be derived from
     * {@link Row}. These rows will be used by the rows fragment and the headers
     * fragment (if not disabled) to render the browse rows.
     *
     * @param adapter An ObjectAdapter for the browse rows. All items must
     *        derive from {@link Row}.
     */
    public void setAdapter(ObjectAdapter adapter) {
        mAdapter = adapter;
        Object firstElement = mAdapter.get(0);

        if (firstElement instanceof ListRow
                && !(((ListRow) firstElement).getAdapter().get(0) instanceof RowsSupportFragment)
                && !(((ListRow) firstElement).getAdapter().get(0) instanceof ContentFragment)) {

            if (mRowsSupportFragment != null && mHeadersSupportFragment != null) {
                mHeadersSupportFragment.setAdapter(adapter);
                mRowsSupportFragment.setAdapter(adapter);
            }
        } else {
            mRowsSupportFragment = null;
            if (mHeadersSupportFragment != null) {
                mHeadersSupportFragment.setAdapter(adapter);
            }

            currentFragmentRef = new WeakReference<>(
                    (ContentFragment) ((ListRow) firstElement).getAdapter().get(0));
        }
    }

    /**
     * Returns the adapter containing the rows for the fragment.
     */
    public ObjectAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Sets an item selection listener.
     */
    public void setOnItemViewSelectedListener(OnItemViewSelectedListener listener) {
        mExternalOnItemViewSelectedListener = listener;
    }

    /**
     * Returns an item selection listener.
     */
    public OnItemViewSelectedListener getOnItemViewSelectedListener() {
        return mExternalOnItemViewSelectedListener;
    }

    /**
     * Get currently bound RowsSupportFragment or null if BrowseSupportFragment has not been created yet.
     * @return Currently bound RowsSupportFragment or null if BrowseSupportFragment has not been created yet.
     */
    public RowsSupportFragment getRowsSupportFragment() {
        return mRowsSupportFragment;
    }

    /**
     * Get currently bound HeadersSupportFragment or null if HeadersSupportFragment has not been created yet.
     * @return Currently bound HeadersSupportFragment or null if HeadersSupportFragment has not been created yet.
     */
    public HeadersSupportFragment getHeadersSupportFragment() {
        return mHeadersSupportFragment;
    }

    /**
     * Sets an item clicked listener on the fragment.
     * OnItemViewClickedListener will override {@link View.OnClickListener} that
     * item presenter sets during {@link Presenter#onCreateViewHolder(ViewGroup)}.
     * So in general,  developer should choose one of the listeners but not both.
     */
    public void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        mOnItemViewClickedListener = listener;
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.setOnItemViewClickedListener(listener);
        }
    }

    /**
     * Returns the item Clicked listener.
     */
    public OnItemViewClickedListener getOnItemViewClickedListener() {
        return mOnItemViewClickedListener;
    }

    /**
     * Starts a headers transition.
     *
     * <p>This method will begin a transition to either show or hide the
     * headers, depending on the value of withHeaders. If headers are disabled
     * for this browse fragment, this method will throw an exception.
     *
     * @param withHeaders True if the headers should transition to being shown,
     *        false if the transition should result in headers being hidden.
     */
    public void startHeadersTransition(boolean withHeaders) {
        if (!mCanShowHeaders) {
            throw new IllegalStateException("Cannot start headers transition");
        }
        if (isInHeadersTransition() || mShowingHeaders == withHeaders) {
            return;
        }
        startHeadersTransitionInternal(withHeaders);
    }

    /**
     * Returns true if the headers transition is currently running.
     */
    public boolean isInHeadersTransition() {
        return mHeadersTransition != null;
    }

    /**
     * Returns true if headers are shown.
     */
    public boolean isShowingHeaders() {
        return mShowingHeaders;
    }

    /**
     * Sets a listener for browse fragment transitions.
     *
     * @param listener The listener to call when a browse headers transition
     *        begins or ends.
     */
    public void setBrowseTransitionListener(BrowseTransitionListener listener) {
        mBrowseTransitionListener = listener;
    }

    /**
     * Enables scaling of rows when headers are present.
     * By default enabled to increase density.
     *
     * @param enable true to enable row scaling
     */
    public void enableRowScaling(boolean enable) {
        mRowScaleEnabled = enable;
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.enableRowScaling(mRowScaleEnabled);
        }
    }

    @Nullable
    private ContentFragment getCurrentFragment() {
        return currentFragmentRef == null ? null : currentFragmentRef.get();
    }

    private void startHeadersTransitionInternal(final boolean withHeaders) {
        if (getFragmentManager().isDestroyed()) {
            return;
        }
        mShowingHeaders = withHeaders;
        RowsSupportFragment target = null;
        if (mRowsSupportFragment != null) {
            target = mRowsSupportFragment;
        } else if (getCurrentFragment() != null && getCurrentFragment() instanceof RowsSupportFragment) {
            target = (RowsSupportFragment) getCurrentFragment();
        }

        Runnable transitionRunnable = new Runnable() {
            @Override
            public void run() {
                mHeadersSupportFragment.onTransitionPrepare();
                mHeadersSupportFragment.onTransitionStart();
                createHeadersTransition();
                if (mBrowseTransitionListener != null) {
                    mBrowseTransitionListener.onHeadersTransitionStart(withHeaders);
                }
                TransitionHelper.runTransition(withHeaders ? mSceneWithHeaders : mSceneWithoutHeaders,
                        mHeadersTransition);
                if (mHeadersBackStackEnabled) {
                    if (!withHeaders) {
                        getFragmentManager().beginTransaction()
                                .addToBackStack(mWithHeadersBackStackName).commit();
                    } else {
                        int index = mBackStackChangedListener.mIndexOfHeadersBackStack;
                        if (index >= 0) {
                            BackStackEntry entry = getFragmentManager().getBackStackEntryAt(index);
                            getFragmentManager().popBackStackImmediate(entry.getId(),
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        }
                    }
                }
            }
        };
        if (target != null) {
            target.onExpandTransitionStart(!withHeaders, transitionRunnable);
        } else {
            // used for custom fragments, just run the headers transition
            transitionRunnable.run();
        }
    }

    private boolean isVerticalScrolling() {
        // don't run transition
        boolean isScrolling = (mHeadersSupportFragment.getVerticalGridView().getScrollState()
                != HorizontalGridView.SCROLL_STATE_IDLE);
        ContentFragment currentFragment = getCurrentFragment();
        if (mRowsSupportFragment != null) {
            isScrolling = isScrolling || mRowsSupportFragment.getVerticalGridView().getScrollState()
                    != HorizontalGridView.SCROLL_STATE_IDLE;
        } else if (currentFragment != null) {
            isScrolling = isScrolling || currentFragment.isScrolling();
        }
        return isScrolling;
    }

    private final BrowseFrameLayout.OnFocusSearchListener mOnFocusSearchListener =
            new BrowseFrameLayout.OnFocusSearchListener() {
        @Override
        public View onFocusSearch(View focused, int direction) {
            // if headers is running transition,  focus stays
            if (mCanShowHeaders && isInHeadersTransition()) {
                return focused;
            }
            if (DEBUG) Log.v(TAG, "onFocusSearch focused " + focused + " + direction " + direction);

            if (getTitleView() != null && focused != getTitleView() &&
                    direction == View.FOCUS_UP) {
                return getTitleView();
            }
            if (getTitleView() != null && getTitleView().hasFocus() &&
                    direction == View.FOCUS_DOWN) {
                if (mCanShowHeaders && mShowingHeaders) {
                    return mHeadersSupportFragment.getVerticalGridView();
                } else {
                    ContentFragment currentFragment = getCurrentFragment();
                    if (mRowsSupportFragment != null) {
                        return mRowsSupportFragment.getVerticalGridView();
                    } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
                        return ((RowsSupportFragment) currentFragment).getVerticalGridView();
                    } else if (currentFragment != null) {
                        return currentFragment.getFocusRootView();
                    } else {
                        return null;
                    }
                }
            }

            boolean isRtl = ViewCompat.getLayoutDirection(focused) == ViewCompat.LAYOUT_DIRECTION_RTL;
            int towardStart = isRtl ? View.FOCUS_RIGHT : View.FOCUS_LEFT;
            int towardEnd = isRtl ? View.FOCUS_LEFT : View.FOCUS_RIGHT;
            if (mCanShowHeaders && direction == towardStart) {
                if (isVerticalScrolling() || mShowingHeaders) {
                    return focused;
                }
                return mHeadersSupportFragment.getVerticalGridView();
            } else if (direction == towardEnd) {
                if (isVerticalScrolling()) {
                    return focused;
                }
                ContentFragment currentFragment = getCurrentFragment();
                if (mRowsSupportFragment != null) {
                    return mRowsSupportFragment.getVerticalGridView();
                } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
                    return ((RowsSupportFragment) currentFragment).getVerticalGridView();
                } else if (currentFragment != null) {
                    return currentFragment.getFocusRootView();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    };

    private final BrowseFrameLayout.OnChildFocusListener mOnChildFocusListener =
            new BrowseFrameLayout.OnChildFocusListener() {

        @Override
        public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
            if (getChildFragmentManager().isDestroyed()) {
                return true;
            }
            // Make sure not changing focus when requestFocus() is called.
            if (mCanShowHeaders && mShowingHeaders) {
                if (mHeadersSupportFragment != null && mHeadersSupportFragment.getView() != null &&
                        mHeadersSupportFragment.getView().requestFocus(direction, previouslyFocusedRect)) {
                    return true;
                }
            }
            if (mRowsSupportFragment != null && mRowsSupportFragment.getView() != null &&
                    mRowsSupportFragment.getView().requestFocus(direction, previouslyFocusedRect)) {
                return true;
            }
            ContentFragment currentFragment = getCurrentFragment();
            if (currentFragment != null && currentFragment.getFocusRootView() != null &&
                    currentFragment.getFocusRootView().requestFocus(direction, previouslyFocusedRect)) {
                return true;
            }
            return getTitleView() != null && getTitleView().requestFocus(direction,
                    previouslyFocusedRect);
        }

                @Override
        public void onRequestChildFocus(View child, View focused) {
            if (getChildFragmentManager().isDestroyed()) {
                return;
            }
            if (!mCanShowHeaders || isInHeadersTransition()) return;
            int childId = child.getId();
            if (childId == R.id.browse_container_dock && mShowingHeaders) {
                startHeadersTransitionInternal(false);
            } else if (childId == R.id.browse_headers_dock && !mShowingHeaders) {
                startHeadersTransitionInternal(true);
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mBackStackChangedListener != null) {
            mBackStackChangedListener.save(outState);
        } else {
            outState.putBoolean(HEADER_SHOW, mShowingHeaders);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypedArray ta = getActivity().obtainStyledAttributes(R.styleable.LeanbackTheme);
        mContainerListMarginStart = (int) ta.getDimension(
                R.styleable.LeanbackTheme_browseRowsMarginStart, getActivity().getResources()
                .getDimensionPixelSize(R.dimen.lb_browse_rows_margin_start));
        mContainerListAlignTop = (int) ta.getDimension(
                R.styleable.LeanbackTheme_browseRowsMarginTop, getActivity().getResources()
                .getDimensionPixelSize(R.dimen.lb_browse_rows_margin_top));
        ta.recycle();

        readArguments(getArguments());

        if (mCanShowHeaders) {
            if (mHeadersBackStackEnabled) {
                mWithHeadersBackStackName = LB_HEADERS_BACKSTACK + this;
                mBackStackChangedListener = new BackStackListener();
                getFragmentManager().addOnBackStackChangedListener(mBackStackChangedListener);
                mBackStackChangedListener.load(savedInstanceState);
            } else {
                if (savedInstanceState != null) {
                    mShowingHeaders = savedInstanceState.getBoolean(HEADER_SHOW);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mBackStackChangedListener != null) {
            getFragmentManager().removeOnBackStackChangedListener(mBackStackChangedListener);
        }
        mBrowseFrame.removeCallbacks(mSetSelectionRunnable);
        mBrowseFrame.removeCallbacks(mToggleTitleRunnable);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (getChildFragmentManager().findFragmentById(R.id.browse_container_dock) == null) {
            mHeadersSupportFragment = new HeadersSupportFragment();
            ContentFragment currentFragment = getCurrentFragment();
            if (mRowsSupportFragment== null && currentFragment == null) {
                mRowsSupportFragment = new RowsSupportFragment();
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.browse_headers_dock, mHeadersSupportFragment)
                        .replace(R.id.browse_container_dock, mRowsSupportFragment).commit();
            } else {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.browse_headers_dock, mHeadersSupportFragment)
                        .replace(R.id.browse_container_dock, (Fragment) currentFragment).commit();
            }
        } else {
            mHeadersSupportFragment = (HeadersSupportFragment) getChildFragmentManager()
                    .findFragmentById(R.id.browse_headers_dock);
            Fragment fragment = getChildFragmentManager()
                    .findFragmentById(R.id.browse_container_dock);
            if (fragment instanceof RowsSupportFragment) {
                mRowsSupportFragment = (RowsSupportFragment) fragment;
            } else {
                currentFragmentRef = new WeakReference<>((ContentFragment) fragment);
            }
        }

        mHeadersSupportFragment.setHeadersGone(!mCanShowHeaders);

        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.setAdapter(mAdapter);
            mRowsSupportFragment.enableRowScaling(mRowScaleEnabled);
            mRowsSupportFragment.setOnItemViewSelectedListener(mRowViewSelectedListener);
            mRowsSupportFragment.setOnItemViewClickedListener(mOnItemViewClickedListener);
        }

        if (mHeaderPresenterSelector != null) {
            mHeadersSupportFragment.setPresenterSelector(mHeaderPresenterSelector);
        }
        mHeadersSupportFragment.setAdapter(mAdapter);
        mHeadersSupportFragment.setOnHeaderViewSelectedListener(mHeaderViewSelectedListener);
        mHeadersSupportFragment.setOnHeaderClickedListener(mHeaderClickedListener);

        View root = inflater.inflate(R.layout.lb_browse_fragment, container, false);

        setTitleView((TitleView) root.findViewById(R.id.browse_title_group));

        mBrowseFrame = (BrowseFrameLayout) root.findViewById(R.id.browse_frame);
        mBrowseFrame.setOnChildFocusListener(mOnChildFocusListener);
        mBrowseFrame.setOnFocusSearchListener(mOnFocusSearchListener);

        if (mBrandColorSet) {
            mHeadersSupportFragment.setBackgroundColor(mBrandColor);
        }

        mSceneWithHeaders = TransitionHelper.createScene(mBrowseFrame, new Runnable() {
            @Override
            public void run() {
                showHeaders(true);
            }
        });
        mSceneWithoutHeaders =  TransitionHelper.createScene(mBrowseFrame, new Runnable() {
            @Override
            public void run() {
                showHeaders(false);
            }
        });
        mSceneAfterEntranceTransition = TransitionHelper.createScene(mBrowseFrame, new Runnable() {
            @Override
            public void run() {
                setEntranceTransitionEndState();
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mHeadersSupportFragment != null) {
            mHeadersSupportFragment.setOnHeaderViewSelectedListener(null);
        }
    }

    private void createHeadersTransition() {
        mHeadersTransition = TransitionHelper.loadTransition(getActivity(),
                mShowingHeaders ?
                R.transition.lb_browse_headers_in : R.transition.lb_browse_headers_out);

        TransitionHelper.addTransitionListener(mHeadersTransition, new TransitionListener() {
            @Override
            public void onTransitionStart(Object transition) {
                mToggleTitleRunnable.post();
            }
            @Override
            public void onTransitionEnd(Object transition) {
                mHeadersTransition = null;
                ContentFragment currentFragment = getCurrentFragment();
                if (mRowsSupportFragment != null) {
                    mRowsSupportFragment.onTransitionEnd();
                } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
                    ((RowsSupportFragment) currentFragment).onTransitionEnd();
                }
                mHeadersSupportFragment.onTransitionEnd();
                if (mShowingHeaders) {
                    VerticalGridView headerGridView = mHeadersSupportFragment.getVerticalGridView();
                    if (headerGridView != null && !headerGridView.hasFocus()) {
                        headerGridView.requestFocus();
                    }
                } else {
                    VerticalGridView rowsGridView = null;
                    if (mRowsSupportFragment != null) {
                        rowsGridView = mRowsSupportFragment.getVerticalGridView();
                    } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
                        rowsGridView = ((RowsSupportFragment) currentFragment).getVerticalGridView();
                    } else if (currentFragment != null && currentFragment instanceof VerticalGridSupportFragment) {
                        rowsGridView = ((VerticalGridSupportFragment) currentFragment).getVerticalGridView();
                    }
                    if (rowsGridView != null && !rowsGridView.hasFocus()) {
                        rowsGridView.requestFocus();
                    }
                }

                if (mBrowseTransitionListener != null) {
                    mBrowseTransitionListener.onHeadersTransitionStop(mShowingHeaders);
                }
            }
        });
    }

    /**
     * Sets the {@link PresenterSelector} used to render the row headers.
     *
     * @param headerPresenterSelector The PresenterSelector that will determine
     *        the Presenter for each row header.
     */
    public void setHeaderPresenterSelector(PresenterSelector headerPresenterSelector) {
        mHeaderPresenterSelector = headerPresenterSelector;
        if (mHeadersSupportFragment != null) {
            mHeadersSupportFragment.setPresenterSelector(mHeaderPresenterSelector);
        }
    }

    private void setRowsAlignedLeft(boolean alignLeft) {
        ViewGroup.MarginLayoutParams lp;
        View containerList;
        ContentFragment currentFragment = getCurrentFragment();
        if (mRowsSupportFragment != null) {
            containerList = mRowsSupportFragment.getView();
            if (containerList != null) {
                lp = (ViewGroup.MarginLayoutParams) containerList.getLayoutParams();
                lp.setMarginStart(alignLeft ? 0 : mContainerListMarginStart);
                containerList.setLayoutParams(lp);
            }
        } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
            containerList = currentFragment.getView();
            if (containerList == null) {
                currentFragment.setExtraMargin(mContainerListAlignTop, mContainerListMarginStart);
            } else {
                lp = (ViewGroup.MarginLayoutParams) containerList.getLayoutParams();
                if (lp != null) {
                    lp.setMarginStart(alignLeft ? 0 : mContainerListMarginStart);
                    containerList.setLayoutParams(lp);
                }
            }
        } else if (currentFragment != null) {
            containerList = currentFragment.getView();
            if (containerList == null) {
                currentFragment.setExtraMargin(mContainerListAlignTop, mContainerListMarginStart);
            } else {
                lp = (ViewGroup.MarginLayoutParams) containerList.getLayoutParams();
                if (lp != null) {
                    lp.setMarginStart(alignLeft ? 0 : mContainerListMarginStart);
                    containerList.setLayoutParams(lp);
                }
            }
        }
    }

    private void setHeadersOnScreen(boolean onScreen) {
        MarginLayoutParams lp;
        View containerList;
        containerList = mHeadersSupportFragment.getView();
        lp = (MarginLayoutParams) containerList.getLayoutParams();
        lp.setMarginStart(onScreen ? 0 : -mContainerListMarginStart);
        containerList.setLayoutParams(lp);
    }

    protected void showHeaders(boolean show) {
        if (DEBUG) Log.v(TAG, "showHeaders " + show);
        mHeadersSupportFragment.setHeadersEnabled(show);
        setHeadersOnScreen(show);
        setRowsAlignedLeft(!show);
        ContentFragment currentFragment = getCurrentFragment();
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.setExpand(true);
        } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
            ((RowsSupportFragment) currentFragment).setExpand(true);
        }
    }

    private HeadersSupportFragment.OnHeaderClickedListener mHeaderClickedListener =
        new HeadersSupportFragment.OnHeaderClickedListener() {
            @Override
            public void onHeaderClicked(RowHeaderPresenter.ViewHolder viewHolder, Row row) {
                if (!mCanShowHeaders || !mShowingHeaders || isInHeadersTransition()) {
                    return;
                }
                ContentFragment currentFragment = getCurrentFragment();
                if (mRowsSupportFragment != null) {
                    startHeadersTransitionInternal(false);
                    mRowsSupportFragment.getVerticalGridView().requestFocus();
                } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
                    startHeadersTransitionInternal(false);
                    ((RowsSupportFragment) currentFragment).getVerticalGridView().requestFocus();
                } else if (currentFragment != null && currentFragment.getFocusRootView() != null) {
                    startHeadersTransitionInternal(false);
                    currentFragment.getFocusRootView().requestFocus();
                }
            }
        };

    private OnItemViewSelectedListener mRowViewSelectedListener = new OnItemViewSelectedListener() {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            int position = -1;
            ContentFragment currentFragment = getCurrentFragment();
            if (mRowsSupportFragment != null) {
                position = mRowsSupportFragment.getVerticalGridView().getSelectedPosition();
                onRowSelected(position);
            } else if (currentFragment != null
                    && currentFragment instanceof RowsSupportFragment
                    && ((RowsSupportFragment) currentFragment).getVerticalGridView() != null) {
                position = ((RowsSupportFragment) currentFragment).getVerticalGridView().getSelectedPosition();
                mToggleTitleRunnable.post();
            } else if (currentFragment != null
                    && currentFragment instanceof VerticalGridSupportFragment
                    && ((VerticalGridSupportFragment) currentFragment).getVerticalGridView()
                    != null) {
                VerticalGridView grid
                        = ((VerticalGridSupportFragment) currentFragment).getVerticalGridView();
                if (grid != null) {
                    position = grid.getSelectedPosition();
                }
                mToggleTitleRunnable.post();
            }
            if (DEBUG) Log.v(TAG, "row selected position " + position);
            if (mExternalOnItemViewSelectedListener != null) {
                mExternalOnItemViewSelectedListener.onItemSelected(itemViewHolder, item,
                        rowViewHolder, row);
            }
        }
    };

    private HeadersSupportFragment.OnHeaderViewSelectedListener mHeaderViewSelectedListener =
            new HeadersSupportFragment.OnHeaderViewSelectedListener() {
        @Override
        public void onHeaderSelected(RowHeaderPresenter.ViewHolder viewHolder, Row row) {
            int position = mHeadersSupportFragment.getSelectedPosition();
            if (DEBUG) Log.v(TAG, "header selected position " + position);

            if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed())
                return;

            // switch fragments (if needed)
            if (mRowsSupportFragment == null && mAdapter.size() > position
                    && ((ListRow) mAdapter.get(position)).getAdapter().size() > 0) {
                onMainHeaderPrepareSelection(position);
                Object listRowObject = ((ListRow) mAdapter.get(position)).getAdapter().get(0);
                ContentFragment nextFragment = null;
                if (listRowObject instanceof ContentFragment)
                    nextFragment = (ContentFragment) listRowObject;
                if (nextFragment == null)
                    return;
                FragmentManager cfManager = getChildFragmentManager();
                Fragment foundFragment = cfManager.findFragmentById(R.id.browse_container_dock);
                if (foundFragment == null || (foundFragment instanceof ContentFragment && !foundFragment.equals(nextFragment))) {
                    if (foundFragment instanceof RowsSupportFragment) {
                        ((RowsSupportFragment) foundFragment).setSelectedPosition(0, false);
                    } else if (foundFragment instanceof VerticalGridSupportFragment) {
                        ((VerticalGridSupportFragment) foundFragment).setSelectedPosition(0);
                    }
                    FragmentTransaction transaction = cfManager.beginTransaction();
                    transaction.replace(R.id.browse_container_dock, (Fragment) nextFragment, nextFragment.getTag());
                    transaction.commit();

                    currentFragmentRef = new WeakReference<>(nextFragment);
                    if (nextFragment instanceof RowsSupportFragment) {
                        ((RowsSupportFragment) nextFragment).setOnItemViewSelectedListener(mRowViewSelectedListener);
                        ((RowsSupportFragment) nextFragment).setOnItemViewClickedListener(mOnItemViewClickedListener);
                    } else if (nextFragment instanceof VerticalGridSupportFragment) {
                        ((VerticalGridSupportFragment) nextFragment).setOnItemViewSelectedListener(
                                mRowViewSelectedListener);
                        ((VerticalGridSupportFragment) nextFragment).setOnItemViewClickedListener(
                                mOnItemViewClickedListener);
                    }
                    showHeaders(mShowingHeaders);
                    onMainHeaderSelected(position);
                }
            } else {
                onRowSelected(position);
            }
        }
    };

    public void onMainHeaderSelected(int position) {
    }

    public void onMainHeaderPrepareSelection(int position) {
    }

    public void onRowSelected(int position) {
        if (position != mSelectedPosition) {
            mSetSelectionRunnable.post(
                    position, SetSelectionRunnable.TYPE_INTERNAL_SYNC, true);
            mToggleTitleRunnable.post();
        }
    }

    private void setSelection(int position, boolean smooth) {
        if (position != NO_POSITION) {
            if (mRowsSupportFragment != null) {
                mRowsSupportFragment.setSelectedPosition(position, smooth);
            }
            mHeadersSupportFragment.setSelectedPosition(position, smooth);
        }
        mSelectedPosition = position;
    }

    /**
     * Sets the selected row position with smooth animation.
     */
    public void setSelectedPosition(int position) {
        setSelectedPosition(position, true);
    }

    /**
     * Gets position of currently selected row.
     *
     * @return Position of currently selected row.
     */
    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    /**
     * Sets the selected row position.
     */
    public void setSelectedPosition(int position, boolean smooth) {
        mSetSelectionRunnable.post(
                position, SetSelectionRunnable.TYPE_USER_REQUEST, smooth);
    }

    /**
     * Selects a Row and perform an optional task on the Row. For example
     * <code>setSelectedPosition(10, true, new ListRowPresenterSelectItemViewHolderTask(5))</code>
     * scrolls to 11th row and selects 6th item on that row.  The method will be ignored if
     * RowsSupportFragment has not been created (i.e. before {@link #onCreateView(LayoutInflater,
     * ViewGroup, Bundle)}).
     *
     * @param rowPosition Which row to select.
     * @param smooth True to scroll to the row, false for no animation.
     * @param rowHolderTask Optional task to perform on the Row.  When the task is not null, headers
     * fragment will be collapsed.
     */
    public void setSelectedPosition(int rowPosition, boolean smooth,
            final Presenter.ViewHolderTask rowHolderTask) {
        ContentFragment currentFragment = getCurrentFragment();
        if (rowHolderTask != null && (mRowsSupportFragment != null || (currentFragment != null && (
                currentFragment instanceof RowsSupportFragment
                        || currentFragment instanceof VerticalGridSupportFragment)))) {
            startHeadersTransition(false);
        }
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.setSelectedPosition(rowPosition, smooth, rowHolderTask);
        } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
            ((RowsSupportFragment) currentFragment).setSelectedPosition(rowPosition, smooth, rowHolderTask);
        } else if (currentFragment != null && currentFragment instanceof VerticalGridSupportFragment) {
            ((VerticalGridSupportFragment) currentFragment).setSelectedPosition(rowPosition);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ContentFragment currentFragment = getCurrentFragment();
        mHeadersSupportFragment.setWindowAlignmentFromTop(mContainerListAlignTop);
        mHeadersSupportFragment.setItemAlignment();
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.setWindowAlignmentFromTop(mContainerListAlignTop);
            mRowsSupportFragment.setItemAlignment();
            mRowsSupportFragment.setScalePivots(0, mContainerListAlignTop);
        } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment && !(
                (RowsSupportFragment) currentFragment).isAdded()) {
            ((RowsSupportFragment) currentFragment).setWindowAlignmentFromTop(mContainerListAlignTop);
            ((RowsSupportFragment) currentFragment).setItemAlignment();
            ((RowsSupportFragment) currentFragment).setScalePivots(0, mContainerListAlignTop);
            ((RowsSupportFragment) currentFragment).setOnItemViewSelectedListener(mRowViewSelectedListener);
            ((RowsSupportFragment) currentFragment).setOnItemViewClickedListener(mOnItemViewClickedListener);
        } else if (currentFragment != null && currentFragment instanceof VerticalGridSupportFragment && !(
                (VerticalGridSupportFragment) currentFragment).isAdded()) {
            ((VerticalGridSupportFragment) currentFragment).setOnItemViewSelectedListener(mRowViewSelectedListener);
            ((VerticalGridSupportFragment) currentFragment).setOnItemViewClickedListener(mOnItemViewClickedListener);
        }

        if (mCanShowHeaders && mShowingHeaders && mHeadersSupportFragment.getView() != null) {
            mHeadersSupportFragment.getView().requestFocus();
        } else if (!mCanShowHeaders || !mShowingHeaders) {
            if (mRowsSupportFragment!= null && mRowsSupportFragment.getView() != null) {
                mRowsSupportFragment.getView().requestFocus();
            } else if (currentFragment != null && currentFragment.getFocusRootView() != null) {
                currentFragment.getFocusRootView().requestFocus();
            }
        }
        if (mCanShowHeaders) {
            showHeaders(mShowingHeaders);
        }
        if (isEntranceTransitionEnabled()) {
            setEntranceTransitionStartState();
        }
    }

    /**
     * Enables/disables headers transition on back key support. This is enabled by
     * default. The BrowseSupportFragment will add a back stack entry when headers are
     * showing. Running a headers transition when the back key is pressed only
     * works when the headers state is {@link #HEADERS_ENABLED} or
     * {@link #HEADERS_HIDDEN}.
     * <p>
     * NOTE: If an Activity has its own onBackPressed() handling, you must
     * disable this feature. You may use {@link #startHeadersTransition(boolean)}
     * and {@link BrowseTransitionListener} in your own back stack handling.
     */
    public final void setHeadersTransitionOnBackEnabled(boolean headersBackStackEnabled) {
        mHeadersBackStackEnabled = headersBackStackEnabled;
    }

    /**
     * Returns true if headers transition on back key support is enabled.
     */
    public final boolean isHeadersTransitionOnBackEnabled() {
        return mHeadersBackStackEnabled;
    }

    private void readArguments(Bundle args) {
        if (args == null) {
            return;
        }
        if (args.containsKey(ARG_TITLE)) {
            setTitle(args.getString(ARG_TITLE));
        }
        if (args.containsKey(ARG_HEADERS_STATE)) {
            setHeadersState(args.getInt(ARG_HEADERS_STATE));
        }
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        mOriginalTitle = title;
    }

    public void setUseHeaderTitleWhenExpanded(boolean useHeaderTitleWhenExpanded) {
        this.mUseHeaderTitleWhenExpanded = useHeaderTitleWhenExpanded;
    }

    void switchTitle() {
        if (mShowingHeaders) {
            if (mBadgeDrawable != null) {
                mTitleView.setBadgeDrawable(mBadgeDrawable);
                mTitleView.setTitle(null);
            } else if (mTitleView != null) {
                mTitleView.setTitle(mOriginalTitle);
                mTitleView.setBadgeDrawable(null);
            }
        } else if (mHeadersSupportFragment != null
                && mHeadersSupportFragment.getVerticalGridView() != null &&
                mHeadersSupportFragment.getAdapter() != null) {
            ListRow listRow = (ListRow) mHeadersSupportFragment.getAdapter()
                    .get(mHeadersSupportFragment.getVerticalGridView().getSelectedPosition());
            if (listRow.getHeaderItem() != null) {
                String headerTitle = listRow.getHeaderItem().getName();
                if (!TextUtils.isEmpty(headerTitle) && mTitleView != null) {
                    mTitleView.setBadgeDrawable(null);
                    mTitleView.setTitle(headerTitle);
                }
            }
        }
    }

    public boolean selectFirstItemInRow() {
        int position = mHeadersSupportFragment.getVerticalGridView().getSelectedPosition();
        if (DEBUG)
            Log.v(TAG, "header selected position " + position);

        // switch fragments (if needed)
        if (mRowsSupportFragment == null) {
            FragmentManager cfManager = getChildFragmentManager();
            Fragment foundFragment = cfManager.findFragmentById(R.id.browse_container_dock);
            if (foundFragment != null) {
                if (foundFragment instanceof RowsSupportFragment) {
                    int selectedRow
                            = ((RowsSupportFragment) foundFragment).getSelectedPosition();
                    int selectedpositionInRow
                            = ((RowsSupportFragment) foundFragment).getSelectedPositionInRow(selectedRow);
                    if (selectedpositionInRow == 0) {
                        return false;
                    }
                    ListRowPresenter.SelectItemViewHolderTask selectItemViewHolderTask = new
                            ListRowPresenter.SelectItemViewHolderTask(0);
                    selectItemViewHolderTask.setSmoothScroll(false);
                    setSelectedPosition(selectedRow, false,
                            selectItemViewHolderTask);
                } else if (foundFragment instanceof VerticalGridSupportFragment) {
                    int selectedColumn
                            = ((VerticalGridSupportFragment) foundFragment).getSelectedColumn();
                    if (selectedColumn == 0) {
                        return false;
                    }
                    int selectedRow
                            = ((VerticalGridSupportFragment) foundFragment).getSelectedRow();
                    setSelectedPosition(
                            ((VerticalGridSupportFragment) foundFragment).getFirstPositionInRow(
                                    selectedRow), false);
                }
            }
        } else {
            int selectedRow = mRowsSupportFragment.getSelectedPosition();
            ListRowPresenter.SelectItemViewHolderTask selectItemViewHolderTask = new
                    ListRowPresenter.SelectItemViewHolderTask(0);
            selectItemViewHolderTask.setSmoothScroll(false);
            setSelectedPosition(selectedRow, false, selectItemViewHolderTask);
        }
        return true;
    }

    /**
     * Sets the state for the headers column in the browse fragment. Must be one
     * of {@link #HEADERS_ENABLED}, {@link #HEADERS_HIDDEN}, or
     * {@link #HEADERS_DISABLED}.
     *
     * @param headersState The state of the headers for the browse fragment.
     */
    public void setHeadersState(int headersState) {
        if (headersState < HEADERS_ENABLED || headersState > HEADERS_DISABLED) {
            throw new IllegalArgumentException("Invalid headers state: " + headersState);
        }
        if (DEBUG) Log.v(TAG, "setHeadersState " + headersState);

        if (headersState != mHeadersState) {
            mHeadersState = headersState;
            switch (headersState) {
                case HEADERS_ENABLED:
                    mCanShowHeaders = true;
                    mShowingHeaders = true;
                    break;
                case HEADERS_HIDDEN:
                    mCanShowHeaders = true;
                    mShowingHeaders = false;
                    break;
                case HEADERS_DISABLED:
                    mCanShowHeaders = false;
                    mShowingHeaders = false;
                    break;
                default:
                    Log.w(TAG, "Unknown headers state: " + headersState);
                    break;
            }
            if (mHeadersSupportFragment != null) {
                mHeadersSupportFragment.setHeadersGone(!mCanShowHeaders);
            }
        }
    }

    /**
     * Returns the state of the headers column in the browse fragment.
     */
    public int getHeadersState() {
        return mHeadersState;
    }

    @Override
    protected Object createEntranceTransition() {
        return TransitionHelper.loadTransition(getActivity(),
                R.transition.lb_browse_entrance_transition);
    }

    @Override
    protected void runEntranceTransition(Object entranceTransition) {
        TransitionHelper.runTransition(mSceneAfterEntranceTransition, entranceTransition);
    }

    @Override
    protected void onEntranceTransitionPrepare() {
        mHeadersSupportFragment.onTransitionPrepare();
        ContentFragment currentFragment = getCurrentFragment();
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.onTransitionPrepare();
        } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
            ((RowsSupportFragment) currentFragment).onTransitionPrepare();
        }
    }

    @Override
    protected void onEntranceTransitionStart() {
        mHeadersSupportFragment.onTransitionStart();
        ContentFragment currentFragment = getCurrentFragment();
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.onTransitionEnd();
        } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
            ((RowsSupportFragment) currentFragment).onTransitionStart();
        }
    }

    @Override
    protected void onEntranceTransitionEnd() {
        mRowsSupportFragment.onTransitionEnd();
        mHeadersSupportFragment.onTransitionEnd();
    }

    void setSearchOrbViewOnScreen(boolean onScreen) {
        View searchOrbView = getTitleView().getSearchAffordanceView();
        MarginLayoutParams lp = (MarginLayoutParams) searchOrbView.getLayoutParams();
        lp.setMarginStart(onScreen ? 0 : -mContainerListMarginStart);
        searchOrbView.setLayoutParams(lp);
    }

    void setEntranceTransitionStartState() {
        setHeadersOnScreen(false);
        setSearchOrbViewOnScreen(false);
        ContentFragment currentFragment = getCurrentFragment();
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.setEntranceTransitionState(false);
        } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
            ((RowsSupportFragment) currentFragment).setEntranceTransitionState(false);
        }
    }

    void setEntranceTransitionEndState() {
        setHeadersOnScreen(mShowingHeaders);
        setSearchOrbViewOnScreen(true);
        ContentFragment currentFragment = getCurrentFragment();
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.setEntranceTransitionState(true);
        } else if (currentFragment != null && currentFragment instanceof RowsSupportFragment) {
            ((RowsSupportFragment) currentFragment).setEntranceTransitionState(true);
        }
    }

    // this has been exposed to the developer, mainly to allow control over the title block
    // for custom fragments
    public void toggleTitle(boolean show) {
        showTitle(show);
    }

    public void toggleTitle() {
        if (mHeadersSupportFragment == null
                || mHeadersSupportFragment.getVerticalGridView() == null)
            return;
        VerticalGridView headerVerticalGridView = mHeadersSupportFragment.getVerticalGridView();
        int rowsPosition = 0;
        ContentFragment currentFragment = getCurrentFragment();
        if (mRowsSupportFragment != null && mRowsSupportFragment.getVerticalGridView() != null) {
            rowsPosition = mRowsSupportFragment.getVerticalGridView().getSelectedPosition();
        } else if (currentFragment != null
                && currentFragment instanceof RowsSupportFragment
                && ((RowsSupportFragment) currentFragment).getVerticalGridView() != null) {
            rowsPosition = ((RowsSupportFragment) currentFragment).getVerticalGridView()
                    .getSelectedPosition();
        } else if (currentFragment != null
                && currentFragment instanceof VerticalGridSupportFragment
                && ((VerticalGridSupportFragment) currentFragment).getVerticalGridView() != null) {
            rowsPosition = ((VerticalGridSupportFragment) currentFragment).getSelectedRow();
        }
        boolean isFirstChildIntersectWithTitle = getTitleView() != null
                && headerVerticalGridView.getChildCount() != 0 &&
                headerVerticalGridView.getChildAt(0).getTop() < getTitleView().getHeight();
        if ((!mShowingHeaders && rowsPosition == 0) || (mShowingHeaders
                && !isFirstChildIntersectWithTitle && rowsPosition == 0)) {
            showTitle(true);
        } else {
            showTitle(false);
        }
    }

    private class ToggleTitleRunnable implements Runnable {

        @Override
        public void run() {
            toggleTitle();
            if (mUseHeaderTitleWhenExpanded && mShowingTitle)
                switchTitle();
        }

        void post() {
            if (mBrowseFrame != null) {
                mBrowseFrame.removeCallbacks(this);
                mBrowseFrame.postDelayed(this, 100);
            }
        }
    }
}

