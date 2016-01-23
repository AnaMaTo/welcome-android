package com.stephentuso.welcome.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.stephentuso.welcome.R;
import com.stephentuso.welcome.WelcomeCompletedEvent;
import com.stephentuso.welcome.WelcomeFailedEvent;
import com.stephentuso.welcome.WelcomeScreenHelper;
import com.stephentuso.welcome.ui.view.WelcomeScreenBackgroundView;
import com.stephentuso.welcome.ui.view.WelcomeScreenViewPagerIndicator;
import com.stephentuso.welcome.util.SharedPreferencesHelper;
import com.stephentuso.welcome.util.WelcomeScreenConfiguration;
import com.stephentuso.welcome.util.WelcomeUtils;

import de.greenrobot.event.EventBus;

public abstract class WelcomeActivity extends AppCompatActivity {

    ViewPager mViewPager;
    WelcomeFragmentPagerAdapter mAdapter;
    WelcomeScreenConfiguration mConfiguration;

    WelcomeScreenItemList mItems = new WelcomeScreenItemList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mConfiguration = configuration();
        //overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.setTheme(mConfiguration.getThemeResId());
        /* Passing null for savedInstanceState fixes issue with fragments in list not matching
           the displayed ones after the screen was rotated. (Parallax animations would stop working)
           TODO: Look into this more
         */
        super.onCreate(null);
        setContentView(R.layout.activity_welcome);

        mAdapter = new WelcomeFragmentPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(mAdapter);

        SkipButton skip = new SkipButton(findViewById(R.id.button_skip), mConfiguration.getCanSkip());
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeWelcomeScreen();
            }
        });

        NextButton next = new NextButton(findViewById(R.id.button_next));
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollToNextPage();
            }
        });

        DoneButton done = new DoneButton(findViewById(R.id.button_done));
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeWelcomeScreen();
            }
        });

        WelcomeScreenViewPagerIndicator indicator = (WelcomeScreenViewPagerIndicator) findViewById(R.id.pager_indicator);
        WelcomeScreenBackgroundView background = (WelcomeScreenBackgroundView) findViewById(R.id.background_view);

        WelcomeScreenHider hider = new WelcomeScreenHider(findViewById(R.id.root));
        hider.setOnViewHiddenListener(new WelcomeScreenHider.OnViewHiddenListener() {
            @Override
            public void onViewHidden() {
                completeWelcomeScreen();
            }
        });

        mItems = new WelcomeScreenItemList(background, indicator, skip, next, done, hider);
        mViewPager.addOnPageChangeListener(mItems);
        mViewPager.addOnPageChangeListener(mConfiguration.getPages());
        mViewPager.setCurrentItem(mConfiguration.firstPageIndex());
        mItems.setup(mConfiguration);
        mItems.onPageSelected(mViewPager.getCurrentItem());
    }

    private boolean scrollToNextPage() {
        if (!canScrollToNextPage())
            return false;
        mViewPager.setCurrentItem(getNextPageIndex());
        return true;
    }

    private boolean scrollToPreviousPage() {
        if (!canScrollToPreviousPage())
            return false;
        mViewPager.setCurrentItem(getPreviousPageIndex());
        return true;
    }

    private int getNextPageIndex() {
        return mViewPager.getCurrentItem() + (mConfiguration.isRtl() ? -1 : 1);
    }

    private int getPreviousPageIndex() {
        return mViewPager.getCurrentItem() + (mConfiguration.isRtl() ? 1 : -1);
    }

    private boolean canScrollToNextPage() {
        return mConfiguration.isRtl() ? getNextPageIndex() >= mConfiguration.lastViewablePageIndex() : getNextPageIndex() <= mConfiguration.lastViewablePageIndex();
    }

    private boolean canScrollToPreviousPage() {
        return mConfiguration.isRtl() ? getPreviousPageIndex() <= mConfiguration.firstPageIndex() : getPreviousPageIndex() >= mConfiguration.firstPageIndex();
    }

    protected void completeWelcomeScreen() {
        SharedPreferencesHelper.storeWelcomeCompleted(this, getKey());
        sendBroadcast(WelcomeScreenHelper.ACTION_WELCOME_COMPLETED);
        EventBus.getDefault().post(new WelcomeCompletedEvent(getKey()));
        super.finish();
        if (mConfiguration.getExitAnimation() != WelcomeScreenConfiguration.NO_ANIMATION_SET)
            overridePendingTransition(R.anim.none, mConfiguration.getExitAnimation());
    }

    private void sendBroadcast(String action) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(action);
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onBackPressed() {

        if (mConfiguration.getCanSkip() && (mConfiguration.getBackButtonSkips() || !scrollToPreviousPage())) {
            completeWelcomeScreen();
        } else if (!mConfiguration.getCanSkip() && !scrollToPreviousPage()){
            EventBus.getDefault().post(new WelcomeFailedEvent(getKey()));
            finish();
        }

    }

    private String getKey() {
        return WelcomeUtils.getKey(this.getClass());
    }

    protected abstract WelcomeScreenConfiguration configuration();

    private class WelcomeFragmentPagerAdapter extends FragmentPagerAdapter {

        public WelcomeFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mConfiguration.getFragment(position);
        }

        @Override
        public int getCount() {
            return mConfiguration.pageCount();
        }
    }
}
