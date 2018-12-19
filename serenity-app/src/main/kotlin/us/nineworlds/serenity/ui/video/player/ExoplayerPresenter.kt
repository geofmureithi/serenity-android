package us.nineworlds.serenity.ui.video.player

import android.util.Log
import android.view.View
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.birbit.android.jobqueue.JobManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlaybackControlView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import toothpick.Toothpick
import us.nineworlds.serenity.common.annotations.InjectionConstants
import us.nineworlds.serenity.common.annotations.OpenForTesting
import us.nineworlds.serenity.common.rest.SerenityClient
import us.nineworlds.serenity.core.logger.Logger
import us.nineworlds.serenity.core.model.VideoContentInfo
import us.nineworlds.serenity.events.video.OnScreenDisplayEvent
import us.nineworlds.serenity.injection.ForVideoQueue
import us.nineworlds.serenity.jobs.video.StartPlaybackJob
import us.nineworlds.serenity.jobs.video.StopPlaybackJob
import us.nineworlds.serenity.jobs.video.UpdatePlaybackPostionJob
import us.nineworlds.serenity.jobs.video.WatchedStatusJob
import us.nineworlds.serenity.ui.video.player.ExoplayerContract.ExoplayerPresenter
import us.nineworlds.serenity.ui.video.player.ExoplayerContract.ExoplayerView
import java.lang.Exception
import java.util.LinkedList
import javax.inject.Inject

@OpenForTesting
@InjectViewState
@StateStrategyType(SkipStrategy::class)
class ExoplayerPresenter : MvpPresenter<ExoplayerContract.ExoplayerView>(), ExoplayerPresenter,
    PlaybackControlView.VisibilityListener, Player.EventListener {

  @Inject lateinit var logger: Logger

  @field:[Inject ForVideoQueue]
  internal lateinit var videoQueue: LinkedList<VideoContentInfo>

  @Inject
  internal lateinit var serenityClient: SerenityClient

  @Inject
  internal lateinit var eventBus: EventBus

  @Inject
  internal lateinit var jobManager: JobManager

  internal lateinit var video: VideoContentInfo

  private var onScreenControllerShowing: Boolean = false

  override fun attachView(view: ExoplayerView?) {
    Toothpick.inject(this, Toothpick.openScope(InjectionConstants.APPLICATION_SCOPE))
    super.attachView(view)
    eventBus.register(this)
  }

  override fun detachView(view: ExoplayerView?) {
    super.detachView(view)
    eventBus.unregister(this)
  }

  override fun updateWatchedStatus() {
    jobManager.addJobInBackground(WatchedStatusJob(video.id()))
  }

  override fun onSeekProcessed() {
  }

  override fun onPositionDiscontinuity(reason: Int) {
  }

  override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
  }

  override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
  }

  override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
  }

  override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
  }

  override fun onPlayerError(error: ExoPlaybackException) {
    logger.error("Play back error", Exception(error))
  }

  override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
    if (Player.STATE_ENDED == playbackState) {
      stopPlaying()
      viewState.playbackEnded()
      return
    }
  }

  override fun onLoadingChanged(isLoading: Boolean) {
  }

  override fun onRepeatModeChanged(repeatMode: Int) {
  }

  override fun videoId(): String = video.id()

  override fun stopPlaying() {
    jobManager.addJobInBackground(StopPlaybackJob(video.id()))
  }

  override fun startPlaying() {
    jobManager.addJobInBackground(StartPlaybackJob(video.id()))
  }

  override fun onVisibilityChange(visibility: Int) {
    if (visibility == View.GONE) {
      logger.debug("Controller View was hidden")
      onScreenControllerShowing = false
    }

    if (visibility == View.VISIBLE) {
      logger.debug("Controller View was shown")
      onScreenControllerShowing = true
    }
  }

  override fun isHudShowing(): Boolean = onScreenControllerShowing

  @Subscribe(threadMode = MAIN)
  fun onOnScreenDisplayEvent(event: OnScreenDisplayEvent) {
    if (event.isShowing) {
      viewState.hideController()
    } else {
      viewState.showController()
    }
  }

  override fun updateServerPlaybackPosition(currentPostion: Long) {
    video.resumeOffset = currentPostion.toInt()
    jobManager.addJobInBackground(UpdatePlaybackPostionJob(video))
  }

  override fun playBackFromVideoQueue() {
    if (videoQueue.isEmpty()) {
      return
    }

    this.video = videoQueue.poll()

    val videoUrl: String = transcoderUrl()

    viewState.initializePlayer(videoUrl, video.resumeOffset)
  }

  internal fun isDirectPlaySupportedForContainer(video: VideoContentInfo): Boolean {

    val isAudioCodecSupported = selectCodec("audio/" + video.audioCodec)

    Log.d("ExoPlayerPresenter", "Audio Codec:  " + video.audioCodec + " support returned " + isAudioCodecSupported)

    if (video.container.equals("avi") || !isAudioCodecSupported) {
      return false
    }
    return true
  }

  private fun transcoderUrl(): String {
    logger.debug("Exoplayer Activity: Container: ${video.container} Audio: ${video.audioCodec}")
    if (isDirectPlaySupportedForContainer(video)) {
      logger.debug("Exoplayer Activity: Direct playing ${video.directPlayUrl}")
      return video.directPlayUrl
    }

    val transcodingUrl = serenityClient.createTranscodeUrl(video.id(), video.resumeOffset);

    logger.debug("Exoplayer Activity: Transcoding Url: $transcodingUrl")
    return transcodingUrl
  }

  private fun selectCodec(mimeType: String): Boolean {
    var actualCode = mimeType;
    if (actualCode.contains("h264")) {
      actualCode = "video/avc"
    }
    val codec = MediaCodecUtil.getDecoderInfo(actualCode, false) ?: return false
    return codec.isCodecSupported(mimeType)
  }
}