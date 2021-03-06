package com.amazing.eye.detial

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.res.Configuration
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import android.widget.ImageView
import com.amazing.eye.bean.VideoBean
import com.amazing.eye.databinding.ActivityVideoDetailBinding
import com.amazing.eye.utils.loadNormalImage
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import kotlinx.android.synthetic.main.activity_video_detail.*
import androidx.core.app.ActivityOptionsCompat
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.amazing.eye.ApplicationContext
import com.amazing.eye.R
import com.amazing.eye.adapter.CommonViewpagerAdapter

class VideoDetailActivity : AppCompatActivity() {

    lateinit var videoDetailBean: VideoBean
    lateinit var orientationUtils: OrientationUtils
    var isPlay: Boolean = false
    var isPause: Boolean = false
    var showPosition = 0

    companion object {
        fun intentThere(
            context: AppCompatActivity, videoBean: VideoBean, showPosition: Int, mImgView: View
        ) {
            val intent = Intent(context, VideoDetailActivity::class.java)
            intent.putExtra("videoDetailBean", videoBean)
            intent.putExtra("showPosition", showPosition)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                context, mImgView,
                ApplicationContext.getString(R.string.transitionName_video)
            )
            context.startActivity(intent, options.toBundle())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityVideoDetailBinding>(
            this,
            R.layout.activity_video_detail
        )
        videoDetailBean = intent.getSerializableExtra("videoDetailBean") as VideoBean
        showPosition = intent.getIntExtra("showPosition", 0)

        val fragmentList: MutableList<Fragment> = mutableListOf(
            DetailInfosFragment.newInstance(videoDetailBean),
            CommentListFragment.newInstance(videoDetailBean.videoId)
        )
        val adapter = CommonViewpagerAdapter(
            supportFragmentManager,
            FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
            fragmentList
        )
        vp_detail_activity.offscreenPageLimit = fragmentList.size
        vp_detail_activity.adapter = adapter
        vp_detail_activity.currentItem = showPosition

        //外部辅助的旋转，帮助全屏
        orientationUtils = OrientationUtils(this, video_detail_activity)
        //初始化不打开外部的旋转
        orientationUtils.isEnable = false

        val imageView = ImageView(this)
        loadNormalImage(imageView, videoDetailBean.feed)
        val gsyVideoOption = GSYVideoOptionBuilder()
        gsyVideoOption.setThumbImageView(imageView)
            .setIsTouchWiget(true)
            .setRotateViewAuto(false)
            .setLockLand(false)
            .setAutoFullWithSize(true)
            .setShowFullAnimation(false)
            .setNeedLockFull(true)
            .setUrl(videoDetailBean.playUrl)
            .setCacheWithPlay(false)
            .setVideoTitle(videoDetailBean.title)
            .setVideoAllCallBack(object : GSYSampleCallBack() {
                override fun onPrepared(url: String?, vararg objects: Any) {
                    super.onPrepared(url, *objects)
                    //开始播放了才能旋转和全屏
                    orientationUtils.isEnable = true
                    isPlay = true
                }

                override fun onQuitFullscreen(url: String?, vararg objects: Any) {
                    super.onQuitFullscreen(url, *objects)
                    orientationUtils.backToProtVideo()
                }
            }).setLockClickListener { view, lock ->
                orientationUtils.isEnable = !lock
            }.build(video_detail_activity)
//        video_detail_activity.thumbImageView.transitionName = "shareVideoImg"
        video_detail_activity.backButton.setOnClickListener { onBackPressed() }
        video_detail_activity.fullscreenButton.setOnClickListener {
            //直接横屏
            orientationUtils.resolveByClick()

            //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
            video_detail_activity.startWindowFullscreen(this@VideoDetailActivity, true, true)
        }
        //定位播放进度
        if (videoDetailBean.isPlaying) {
            video_detail_activity.seekOnStart = videoDetailBean.currentPosition
            video_detail_activity.startPlayLogic()
        }
    }

    fun toCommentListFragment() {
        vp_detail_activity.currentItem = 1
    }

    override fun onBackPressed() {
        orientationUtils.backToProtVideo()
        if (GSYVideoManager.backFromWindowFull(this)) {
            return
        }
        supportFinishAfterTransition()
    }


    override fun onPause() {
        video_detail_activity.currentPlayer.onVideoPause()
        super.onPause()
        isPause = true
    }

    override fun onResume() {
        video_detail_activity.currentPlayer.onVideoResume(false)
        super.onResume()
        isPause = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isPlay) {
            video_detail_activity.currentPlayer.release()
        }
        orientationUtils.releaseListener()
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //如果旋转了就全屏
        if (isPlay && !isPause) {
            video_detail_activity.onConfigurationChanged(
                this,
                newConfig,
                orientationUtils,
                true,
                true
            )
        }
    }
}
