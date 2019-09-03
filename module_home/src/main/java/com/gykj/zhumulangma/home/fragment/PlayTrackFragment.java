package com.gykj.zhumulangma.home.fragment;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.arch.lifecycle.ViewModelProvider;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.gykj.zhumulangma.common.AppConstants;
import com.gykj.zhumulangma.common.bean.NavigateBean;
import com.gykj.zhumulangma.common.event.EventCode;
import com.gykj.zhumulangma.common.event.KeyCode;
import com.gykj.zhumulangma.common.event.common.BaseActivityEvent;
import com.gykj.zhumulangma.common.mvvm.BaseMvvmFragment;
import com.gykj.zhumulangma.common.util.ZhumulangmaUtil;
import com.gykj.zhumulangma.common.util.log.TLog;
import com.gykj.zhumulangma.common.widget.TScrollView;
import com.gykj.zhumulangma.home.R;
import com.gykj.zhumulangma.home.adapter.AlbumAdapter;
import com.gykj.zhumulangma.home.dialog.PlaySchedulePopup;
import com.gykj.zhumulangma.home.mvvm.ViewModelFactory;
import com.gykj.zhumulangma.home.mvvm.viewmodel.PlayTrackViewModel;
import com.lxj.xpopup.XPopup;
import com.ninetripods.sydialoglib.SYDialog;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;
import com.wuhenzhizao.titlebar.widget.CommonTitleBar;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.advertis.Advertis;
import com.ximalaya.ting.android.opensdk.model.advertis.AdvertisList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;
import com.ximalaya.ting.android.opensdk.player.advertis.IXmAdsStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.IXmPlayerStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerException;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import me.yokeyword.fragmentation.ISupportFragment;

@Route(path = AppConstants.Router.Home.F_PLAY_TRACK)
public class PlayTrackFragment extends BaseMvvmFragment<PlayTrackViewModel> implements
        TScrollView.OnScrollListener, View.OnClickListener, IXmPlayerStatusListener,
        BaseQuickAdapter.OnItemClickListener, IXmAdsStatusListener, OnSeekChangeListener, PlaySchedulePopup.onSelectedListener {

    private TScrollView msv;
    private CommonTitleBar ctbTrans;
    private CommonTitleBar ctbWhite;
    private View c;

    private ImageView whiteLeft;
    private ImageView whiteRight1;
    private ImageView whiteRight2;
    private TextView tvSchedule;
    private ImageView transLeft;
    private ImageView transRight1;
    private ImageView transRight2;
    private IndicatorSeekBar isbProgress;
    private LottieAnimationView lavPlayPause;
    private LottieAnimationView lavBuffering;
    private ImageView ivBg;
    private RecyclerView rvRelative;
    private AlbumAdapter mAlbumAdapter;
    private Track mTrack;
    private boolean isPlaying;

    private Handler mHandler;

    private PlaySchedulePopup mSchedulePopup;

    public PlayTrackFragment() {

    }


    @Override
    protected int onBindLayout() {
        return R.layout.home_fragment_play_track;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSwipeBackEnable(false);
    }

    @Override
    protected void initView(View view) {
        msv = fd(R.id.msv);
        ctbTrans = fd(R.id.ctb_trans);
        ctbWhite = fd(R.id.ctb_white);
        ivBg = fd(R.id.iv_bg);
        isbProgress = fd(R.id.ib_progress);
        c = fd(R.id.c);
        tvSchedule = fd(R.id.tv_schedule);
        lavPlayPause = fd(R.id.lav_play_pause);
        lavBuffering = fd(R.id.lav_buffering);
        rvRelative = fd(R.id.rv_relative);
        rvRelative.setLayoutManager(new LinearLayoutManager(mContext));
        mAlbumAdapter = new AlbumAdapter(R.layout.home_item_album);
        mAlbumAdapter.bindToRecyclerView(rvRelative);
        initBar();
        new Handler().postDelayed(() -> {
            if (XmPlayerManager.getInstance(mContext).isPlaying()) {
                if (XmPlayerManager.getInstance(mContext).isAdPlaying()) {
                    bufferingAnim();
                } else {
                    playingAnim();
                }
            }
        }, 100);
        mSchedulePopup = new PlaySchedulePopup(mContext, this);
    }


    private void initBar() {

        transLeft = ctbTrans.getLeftCustomView().findViewById(R.id.iv_left);
        transRight1 = ctbTrans.getRightCustomView().findViewById(R.id.iv1_right);
        transRight2 = ctbTrans.getRightCustomView().findViewById(R.id.iv2_right);


        transLeft.setImageResource(R.drawable.ic_common_titlebar_back);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            transLeft.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }
        transLeft.setRotation(-90);
        transLeft.setVisibility(View.VISIBLE);

        transRight1.setImageResource(R.drawable.ic_common_more);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            transRight1.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }
        transRight1.setVisibility(View.VISIBLE);

        transRight2.setImageResource(R.drawable.ic_common_share);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            transRight2.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }
        transRight2.setVisibility(View.VISIBLE);

        whiteLeft = ctbWhite.getLeftCustomView().findViewById(R.id.iv_left);
        whiteRight1 = ctbWhite.getRightCustomView().findViewById(R.id.iv1_right);
        whiteRight2 = ctbWhite.getRightCustomView().findViewById(R.id.iv2_right);
        TextView tvTitle = ctbWhite.getCenterCustomView().findViewById(R.id.tv_title);
        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setText("歌曲详情");

        whiteLeft.setImageResource(R.drawable.ic_common_titlebar_back);
        whiteLeft.setVisibility(View.VISIBLE);
        whiteLeft.setRotation(-90);
        whiteRight1.setImageResource(R.drawable.ic_common_more);
        whiteRight1.setVisibility(View.VISIBLE);
        whiteRight2.setImageResource(R.drawable.ic_common_share);
        whiteRight2.setVisibility(View.VISIBLE);
    }

    @Override
    public void initListener() {
        super.initListener();
        msv.setOnScrollListener(this);
        whiteLeft.setOnClickListener(this);
        transLeft.setOnClickListener(this);
        fd(R.id.iv_pre).setOnClickListener(this);
        fd(R.id.iv_next).setOnClickListener(this);
        fd(R.id.fl_play_pause).setOnClickListener(this);
        fd(R.id.cl_album).setOnClickListener(this);
        fd(R.id.tv_play_list).setOnClickListener(this);
        fd(R.id.iv_play_list).setOnClickListener(this);
        fd(R.id.iv_schedule).setOnClickListener(this);
        tvSchedule.setOnClickListener(this);
        mAlbumAdapter.setOnItemClickListener(this);
        isbProgress.setOnSeekChangeListener(this);
        XmPlayerManager.getInstance(mContext).addPlayerStatusListener(this);
        XmPlayerManager.getInstance(mContext).addAdsStatusListener(this);
        fd(R.id.tv_more_relative).setOnClickListener(view -> {
            Object o = ARouter.getInstance().build(AppConstants.Router.Home.F_ALBUM_LIST)
                    .withInt(KeyCode.Home.TYPE, AlbumListFragment.LIKE)
                    .withString(KeyCode.Home.TITLE, "更多推荐")
                    .navigation();
            EventBus.getDefault().post(new BaseActivityEvent<>(
                    EventCode.MainCode.NAVIGATE, new NavigateBean(AppConstants.Router.Home.F_ALBUM_LIST, (ISupportFragment) o)));

        });

    }

    @Override
    public void initData() {
        mHandler = new Handler();
        Track currSoundIgnoreKind = XmPlayerManager.getInstance(mContext).getCurrSoundIgnoreKind(true);
        if (null != currSoundIgnoreKind) {
            mTrack = currSoundIgnoreKind;
            Glide.with(this).load(currSoundIgnoreKind.getCoverUrlLarge()).into(ivBg);
            Glide.with(this).load(currSoundIgnoreKind.getAnnouncer().getAvatarUrl()).into((ImageView) fd(R.id.iv_announcer_cover));
            Glide.with(this).load(currSoundIgnoreKind.getAlbum().getCoverUrlMiddle()).into((ImageView) fd(R.id.iv_album_cover));

            ((TextView) fd(R.id.tv_track_name)).setText(currSoundIgnoreKind.getTrackTitle());
            ((TextView) fd(R.id.tv_announcer_name)).setText(currSoundIgnoreKind.getAnnouncer().getNickname());
            String vsignature = currSoundIgnoreKind.getAnnouncer().getVsignature();
            if (TextUtils.isEmpty(vsignature)) {
                fd(R.id.tv_vsignature).setVisibility(View.GONE);
            } else {
                ((TextView) fd(R.id.tv_vsignature)).setText(vsignature);
            }
            ((TextView) fd(R.id.tv_following_count)).setText(getString(R.string.following_count,
                    ZhumulangmaUtil.toWanYi(currSoundIgnoreKind.getAnnouncer().getFollowingCount())));
            fd(R.id.tv_vsignature).setVisibility(currSoundIgnoreKind.getAnnouncer().isVerified() ? View.VISIBLE : View.GONE);

            ((TextView) fd(R.id.tv_album_name)).setText(currSoundIgnoreKind.getAlbum().getAlbumTitle());
            ((TextView) fd(R.id.tv_track_intro)).setText(currSoundIgnoreKind.getTrackIntro());
            ((TextView) fd(R.id.tv_playcount_createtime)).setText(getString(R.string.playcount_createtime,
                    ZhumulangmaUtil.toWanYi(currSoundIgnoreKind.getPlayCount()),
                    TimeUtils.millis2String(currSoundIgnoreKind.getCreatedAt(), new SimpleDateFormat("yyyy-MM-dd"))));
            ((TextView) fd(R.id.tv_favorite_count)).setText(getString(R.string.favorite_count,
                    ZhumulangmaUtil.toWanYi(currSoundIgnoreKind.getFavoriteCount())));
            ((TextView) fd(R.id.tv_comment_count)).setText(getString(R.string.comment_count,
                    ZhumulangmaUtil.toWanYi(currSoundIgnoreKind.getCommentCount())));

            ((TextView) fd(R.id.tv_duration)).setText(ZhumulangmaUtil.secondToTimeE(currSoundIgnoreKind.getDuration()));
            mViewModel.getRelativeAlbums(String.valueOf(mTrack.getDataId()));
            isbProgress.setMax(currSoundIgnoreKind.getDuration());
            if (XmPlayerManager.getInstance(mContext).isPlaying()) {
                ((TextView) fd(R.id.tv_current)).setText(ZhumulangmaUtil.secondToTimeE(
                        XmPlayerManager.getInstance(mContext).getPlayCurrPositon() / 1000));
                isbProgress.setProgress((float) XmPlayerManager.getInstance(mContext).getPlayCurrPositon() / 1000);
            } else {
                ((TextView) fd(R.id.tv_current)).setText(ZhumulangmaUtil.secondToTimeE(0));
                isbProgress.setProgress(0);
            }
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> scheduleTime(), 0);

    }

    private void scheduleTime() {
        int type = SPUtils.getInstance().getInt(AppConstants.SP.PLAY_SCHEDULE_TYPE, 0);
        long time = SPUtils.getInstance().getLong(AppConstants.SP.PLAY_SCHEDULE_TIME, 0);

        if (type == 0) {
            tvSchedule.setText("定时");
            mHandler.removeCallbacksAndMessages(null);
        } else if (type == 1) {
            mHandler.postDelayed(() -> scheduleTime(), 1000);
            tvSchedule.setText(ZhumulangmaUtil.secondToTimeE(XmPlayerManager.getInstance(mContext).getDuration() / 1000 -
                    XmPlayerManager.getInstance(mContext).getPlayCurrPositon() / 1000));
        } else {
            if (System.currentTimeMillis() < time) {
                mHandler.postDelayed(() -> scheduleTime(), 1000);
                tvSchedule.setText(ZhumulangmaUtil.secondToTimeE((time - System.currentTimeMillis()) / 1000));
            } else {
                mHandler.removeCallbacksAndMessages(null);
                tvSchedule.setText("定时");
            }
        }
    }

    @Override
    public void initViewObservable() {
        mViewModel.getAlbumSingleLiveEvent().observe(this, albums -> mAlbumAdapter.setNewData(albums));
    }


    @Override
    protected boolean enableSimplebar() {
        return false;
    }

    @Override
    protected boolean lazyEnable() {
        return false;
    }

    @Override
    public void onScroll(int scrollY) {

        ctbTrans.setAlpha(ZhumulangmaUtil.unvisibleByScroll(scrollY, SizeUtils.dp2px(100), c.getTop() - SizeUtils.dp2px(80)));
        ctbWhite.setAlpha(ZhumulangmaUtil.visibleByScroll(scrollY, SizeUtils.dp2px(100), c.getTop() - SizeUtils.dp2px(80)));

    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (v == whiteLeft || v == transLeft) {
            pop();
        } else if (R.id.cl_album == id) {
            if (null != mTrack) {
                Object navigation = ARouter.getInstance().build(AppConstants.Router.Home.F_ALBUM_DETAIL)
                        .withLong(KeyCode.Home.ALBUMID, mTrack.getAlbum().getAlbumId())
                        .navigation();
                NavigateBean navigateBean = new NavigateBean(AppConstants.Router.Home.F_ALBUM_DETAIL, (ISupportFragment) navigation);
                navigateBean.launchMode = STANDARD;
                EventBus.getDefault().post(new BaseActivityEvent<>(
                        EventCode.MainCode.NAVIGATE, navigateBean));
            }
        } else if (R.id.iv_pre == id) {
            XmPlayerManager.getInstance(mContext).playPre();
        } else if (R.id.iv_next == id) {
            XmPlayerManager.getInstance(mContext).playNext();
        } else if (R.id.fl_play_pause == id) {
            if (XmPlayerManager.getInstance(mContext).isPlaying()) {
                XmPlayerManager.getInstance(mContext).pause();
            } else {
                XmPlayerManager.getInstance(mContext).play();
            }
        } else if (R.id.tv_schedule == id || R.id.iv_schedule == id) {
            new XPopup.Builder(getContext()).asCustom(mSchedulePopup).show();
        }else if (R.id.tv_schedule == id || R.id.iv_schedule == id) {
            new XPopup.Builder(getContext()).asCustom(mSchedulePopup).show();
        }
    }

    @Override
    public void onPlayStart() {
        playAnim();
        Log.d(TAG, "onPlayStart() called");
    }

    @Override
    public void onPlayPause() {
        pauseAnim();
        Log.d(TAG, "onPlayPause() called");
    }

    @Override
    public void onPlayStop() {
        pauseAnim();
        Log.d(TAG, "onPlayStop() called");
    }

    @Override
    public void onSoundPlayComplete() {

        if (SPUtils.getInstance().getInt(AppConstants.SP.PLAY_SCHEDULE_TYPE, 0) == 1) {
            SPUtils.getInstance().put(AppConstants.SP.PLAY_SCHEDULE_TYPE, 0);
            pauseAnim();
        }
        Log.d(TAG, "onSoundPlayComplete() called");
    }

    @Override
    public void onSoundPrepared() {
        Log.d(TAG, "onSoundPrepared() called");
    }

    @Override
    public void onSoundSwitch(PlayableModel playableModel, PlayableModel playableModel1) {
        Log.d(TAG, "onSoundSwitch() called with: playableModel = [" + playableModel + "], playableModel1 = [" + playableModel1 + "]");
        initData();
    }

    @Override
    public void onBufferingStart() {
        bufferingAnim();
        Log.d(TAG, "onBufferingStart() called");
    }

    @Override
    public void onBufferingStop() {
        TLog.d(XmPlayerManager.getInstance(mContext).isPlaying());
        if (XmPlayerManager.getInstance(mContext).isPlaying()) {
            playingAnim();
        } else {
            pauseAnim();
        }
        Log.d(TAG, "onBufferingStop() called");
    }

    @Override
    public void onBufferProgress(int i) {
        Log.d(TAG, "onBufferProgress() called with: i = [" + i + "]");
    }

    @Override
    public void onPlayProgress(int i, int i1) {
        ((TextView) fd(R.id.tv_current)).setText(ZhumulangmaUtil.secondToTimeE(i / 1000));
        if (!isTouch) {
            isbProgress.setProgress((float) i / 1000);
        }
    }

    @Override
    public boolean onError(XmPlayerException e) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        XmPlayerManager.getInstance(mContext).removePlayerStatusListener(this);
        XmPlayerManager.getInstance(mContext).removeAdsStatusListener(this);
    }

    @Override
    public Class<PlayTrackViewModel> onBindViewModel() {
        return PlayTrackViewModel.class;
    }

    @Override
    public ViewModelProvider.Factory onBindViewModelFactory() {
        return ViewModelFactory.getInstance(mApplication);
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        Object navigation = ARouter.getInstance().build(AppConstants.Router.Home.F_ALBUM_DETAIL)
                .withLong(KeyCode.Home.ALBUMID, mAlbumAdapter.getData().get(position).getId())
                .navigation();
        NavigateBean navigateBean = new NavigateBean(AppConstants.Router.Home.F_ALBUM_DETAIL, (ISupportFragment) navigation);
        navigateBean.launchMode = STANDARD;
        EventBus.getDefault().post(new BaseActivityEvent<>(
                EventCode.MainCode.NAVIGATE, navigateBean));
    }

    @Override
    public void onStartGetAdsInfo() {
        Log.d(TAG, "onStartGetAdsInfo() called");
    }

    @Override
    public void onGetAdsInfo(AdvertisList advertisList) {
        Log.d(TAG, "onGetAdsInfo() called with: advertisList = [" + advertisList + "]");
    }


    @Override
    public void onAdsStartBuffering() {
        bufferingAnim();
        Log.d(TAG, "onAdsStartBuffering() called");
    }

    @Override
    public void onAdsStopBuffering() {
     /*   if(XmPlayerManager.getInstance(mContext).isPlaying()){
            playingAnim();
        }*/
        Log.d(TAG, "onAdsStopBuffering() called");
    }


    @Override
    public void onStartPlayAds(Advertis advertis, int i) {
        bufferingAnim();
        Log.d(TAG, "onStartPlayAds() called with: advertis = [" + advertis + "], i = [" + i + "]");
    }

    @Override
    public void onCompletePlayAds() {
        playAnim();
        Log.d(TAG, "onCompletePlayAds() called");
    }

    @Override
    public void onError(int i, int i1) {
        Log.d(TAG, "onError() called with: i = [" + i + "], i1 = [" + i1 + "]");
    }

    @Override
    public void onSeeking(SeekParams seekParams) {

        TextView indicator = isbProgress.getIndicator().getTopContentView().findViewById(R.id.tv_indicator);
        indicator.setText(ZhumulangmaUtil.secondToTimeE(seekParams.progress)
                + "/" + ZhumulangmaUtil.secondToTimeE((long) seekParams.seekBar.getMax()));
    }

    @Override

    public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
        isTouch = true;
    }

    boolean isTouch;

    @Override
    public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
        XmPlayerManager.getInstance(mContext).seekTo(seekBar.getProgress() * 1000);
        isTouch = false;
    }

    private void playAnim() {
        if (!isPlaying) {

            lavBuffering.cancelAnimation();
            lavBuffering.setVisibility(View.GONE);
            lavPlayPause.setVisibility(View.VISIBLE);

            lavPlayPause.setMinAndMaxFrame(55, 90);
            lavPlayPause.loop(false);
            lavPlayPause.playAnimation();
            lavPlayPause.addAnimatorListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    playingAnim();
                    lavPlayPause.removeAnimatorListener(this);
                }
            });
        }
    }

    private void playingAnim() {
        lavPlayPause.removeAllAnimatorListeners();
        lavBuffering.cancelAnimation();
        lavBuffering.setVisibility(View.GONE);
        lavPlayPause.setVisibility(View.VISIBLE);
        isPlaying = true;
        lavPlayPause.setMinAndMaxFrame(90, 170);
        lavPlayPause.loop(true);
        lavPlayPause.playAnimation();
    }

    private void bufferingAnim() {

        lavPlayPause.cancelAnimation();
        isPlaying = false;
        lavPlayPause.setVisibility(View.GONE);
        lavBuffering.setVisibility(View.VISIBLE);
        lavBuffering.playAnimation();
    }

    private void pauseAnim() {
        lavBuffering.cancelAnimation();
        lavPlayPause.removeAllAnimatorListeners();
        lavBuffering.setVisibility(View.GONE);
        lavPlayPause.setVisibility(View.VISIBLE);
        isPlaying = false;
        lavPlayPause.setMinAndMaxFrame(180, 210);
        lavPlayPause.loop(false);
        lavPlayPause.playAnimation();
    }

    @Override
    public void onSupportVisible() {
        super.onSupportVisible();
        EventBus.getDefault().post(new BaseActivityEvent<>(EventCode.MainCode.HIDE_GP));
    }

    @Override
    public void onSupportInvisible() {
        super.onSupportInvisible();
        EventBus.getDefault().post(new BaseActivityEvent<>(EventCode.MainCode.SHOW_GP));
    }

    @Override
    public boolean onBackPressedSupport() {
        if (mSchedulePopup != null && mSchedulePopup.getPickerView() != null && mSchedulePopup.getPickerView().isShowing()) {
            mSchedulePopup.getPickerView().dismiss();
            return true;
        }
        return super.onBackPressedSupport();
    }

    @Override
    public void onSelected(int type, long time) {
        Log.d(TAG, "onSelected() called with: type = [" + type + "], time = [" + time + "]");

        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> scheduleTime(), 0);
    }

}