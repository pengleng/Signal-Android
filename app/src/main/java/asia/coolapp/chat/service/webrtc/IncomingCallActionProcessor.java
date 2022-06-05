package asia.coolapp.chat.service.webrtc;

import android.net.Uri;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.logging.Log;
import org.signal.ringrtc.CallException;
import org.signal.ringrtc.CallId;
import org.signal.ringrtc.CallManager;
import asia.coolapp.chat.database.RecipientDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.events.CallParticipant;
import asia.coolapp.chat.events.WebRtcViewModel;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.notifications.DoNotDisturbUtil;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.ringrtc.CallState;
import asia.coolapp.chat.ringrtc.RemotePeer;
import asia.coolapp.chat.service.webrtc.state.CallSetupState;
import asia.coolapp.chat.service.webrtc.state.VideoState;
import asia.coolapp.chat.service.webrtc.state.WebRtcServiceState;
import asia.coolapp.chat.util.NetworkUtil;
import asia.coolapp.chat.util.Util;
import asia.coolapp.chat.webrtc.locks.LockManager;
import org.webrtc.PeerConnection;

import java.util.List;
import java.util.Objects;

import static asia.coolapp.chat.webrtc.CallNotificationBuilder.TYPE_INCOMING_RINGING;

/**
 * Responsible for setting up and managing the start of an incoming 1:1 call. Transitioned
 * to from idle or pre-join and can either move to a connected state (user picks up) or
 * a disconnected state (remote hangup, local hangup, etc.).
 */
public class IncomingCallActionProcessor extends DeviceAwareActionProcessor {

  private static final String TAG = Log.tag(IncomingCallActionProcessor.class);

  private final ActiveCallActionProcessorDelegate activeCallDelegate;
  private final CallSetupActionProcessorDelegate  callSetupDelegate;

  public IncomingCallActionProcessor(@NonNull WebRtcInteractor webRtcInteractor) {
    super(webRtcInteractor, TAG);
    activeCallDelegate = new ActiveCallActionProcessorDelegate(webRtcInteractor, TAG);
    callSetupDelegate  = new CallSetupActionProcessorDelegate(webRtcInteractor, TAG);
  }

  @Override
  protected @NonNull WebRtcServiceState handleIsInCallQuery(@NonNull WebRtcServiceState currentState, @Nullable ResultReceiver resultReceiver) {
    return activeCallDelegate.handleIsInCallQuery(currentState, resultReceiver);
  }

  @Override
  public @NonNull WebRtcServiceState handleTurnServerUpdate(@NonNull WebRtcServiceState currentState,
                                                            @NonNull List<PeerConnection.IceServer> iceServers,
                                                            boolean isAlwaysTurn)
  {
    RemotePeer activePeer = currentState.getCallInfoState().requireActivePeer();

    Log.i(TAG, "handleTurnServerUpdate(): call_id: " + activePeer.getCallId());

    currentState = currentState.builder()
                               .changeCallSetupState(activePeer.getCallId())
                               .iceServers(iceServers)
                               .alwaysTurn(isAlwaysTurn)
                               .build();

    return proceed(currentState);
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetTelecomApproved(@NonNull WebRtcServiceState currentState, long callId) {
    return proceed(super.handleSetTelecomApproved(currentState, callId));
  }

  private @NonNull WebRtcServiceState proceed(@NonNull WebRtcServiceState currentState) {
    RemotePeer     activePeer     = currentState.getCallInfoState().requireActivePeer();
    CallSetupState callSetupState = currentState.getCallSetupState(activePeer.getCallId());

    if (callSetupState.getIceServers().isEmpty() || (callSetupState.shouldWaitForTelecomApproval() && !callSetupState.isTelecomApproved())) {
      Log.i(TAG, "Unable to proceed without ice server and telecom approval" +
                 " iceServers: " + Util.hasItems(callSetupState.getIceServers()) +
                 " waitForTelecom: " + callSetupState.shouldWaitForTelecomApproval() +
                 " telecomApproved: " + callSetupState.isTelecomApproved());
      return currentState;
    }

    boolean         hideIp          = !activePeer.getRecipient().isSystemContact() || callSetupState.isAlwaysTurnServers();
    VideoState      videoState      = currentState.getVideoState();
    CallParticipant callParticipant = Objects.requireNonNull(currentState.getCallInfoState().getRemoteCallParticipant(activePeer.getRecipient()));

    try {
      webRtcInteractor.getCallManager().proceed(activePeer.getCallId(),
                                                context,
                                                videoState.getLockableEglBase().require(),
                                                AudioProcessingMethodSelector.get(),
                                                videoState.requireLocalSink(),
                                                callParticipant.getVideoSink(),
                                                videoState.requireCamera(),
                                                callSetupState.getIceServers(),
                                                hideIp,
                                                NetworkUtil.getCallingBandwidthMode(context),
                                                null,
                                                false);
    } catch (CallException e) {
      return callFailure(currentState, "Unable to proceed with call: ", e);
    }

    webRtcInteractor.updatePhoneState(LockManager.PhoneState.PROCESSING);
    webRtcInteractor.postStateUpdate(currentState);

    return currentState;
  }

  @Override
  protected @NonNull WebRtcServiceState handleDropCall(@NonNull WebRtcServiceState currentState, long callId) {
    return callSetupDelegate.handleDropCall(currentState, callId);
  }

  @Override
  protected @NonNull WebRtcServiceState handleAcceptCall(@NonNull WebRtcServiceState currentState, boolean answerWithVideo) {
    RemotePeer activePeer = currentState.getCallInfoState().requireActivePeer();

    Log.i(TAG, "handleAcceptCall(): call_id: " + activePeer.getCallId());

    SignalDatabase.sms().insertReceivedCall(activePeer.getId(), currentState.getCallSetupState(activePeer).isRemoteVideoOffer());

    currentState = currentState.builder()
                               .changeCallSetupState(activePeer.getCallId())
                               .acceptWithVideo(answerWithVideo)
                               .build();

    try {
      webRtcInteractor.getCallManager().acceptCall(activePeer.getCallId());
    } catch (CallException e) {
      return callFailure(currentState, "accept() failed: ", e);
    }

    return currentState;
  }

  protected @NonNull WebRtcServiceState handleDenyCall(@NonNull WebRtcServiceState currentState) {
    RemotePeer activePeer = currentState.getCallInfoState().requireActivePeer();

    if (activePeer.getState() != CallState.LOCAL_RINGING) {
      Log.w(TAG, "Can only deny from ringing!");
      return currentState;
    }

    Log.i(TAG, "handleDenyCall():");

    try {
      webRtcInteractor.rejectIncomingCall(activePeer.getId());
      webRtcInteractor.getCallManager().hangup();
      SignalDatabase.sms().insertMissedCall(activePeer.getId(), System.currentTimeMillis(), currentState.getCallSetupState(activePeer).isRemoteVideoOffer());
      return terminate(currentState, activePeer);
    } catch (CallException e) {
      return callFailure(currentState, "hangup() failed: ", e);
    }
  }

  protected @NonNull WebRtcServiceState handleLocalRinging(@NonNull WebRtcServiceState currentState, @NonNull RemotePeer remotePeer) {
    Log.i(TAG, "handleLocalRinging(): call_id: " + remotePeer.getCallId());

    RemotePeer activePeer = currentState.getCallInfoState().requireActivePeer();
    Recipient  recipient  = remotePeer.getRecipient();

    activePeer.localRinging();
    webRtcInteractor.updatePhoneState(LockManager.PhoneState.INTERACTIVE);

    boolean shouldDisturbUserWithCall = DoNotDisturbUtil.shouldDisturbUserWithCall(context.getApplicationContext(), recipient);
    if (shouldDisturbUserWithCall) {
      boolean started = webRtcInteractor.startWebRtcCallActivityIfPossible();
      if (!started) {
        Log.i(TAG, "Unable to start call activity due to OS version or not being in the foreground");
        ApplicationDependencies.getAppForegroundObserver().addListener(webRtcInteractor.getForegroundListener());
      }
    }

    if (shouldDisturbUserWithCall && SignalStore.settings().isCallNotificationsEnabled()) {
      Uri                            ringtone     = recipient.resolve().getCallRingtone();
      RecipientDatabase.VibrateState vibrateState = recipient.resolve().getCallVibrate();

      if (ringtone == null) {
        ringtone = SignalStore.settings().getCallRingtone();
      }

      webRtcInteractor.startIncomingRinger(ringtone, vibrateState == RecipientDatabase.VibrateState.ENABLED || (vibrateState == RecipientDatabase.VibrateState.DEFAULT && SignalStore.settings().isCallVibrateEnabled()));
    }

    webRtcInteractor.setCallInProgressNotification(TYPE_INCOMING_RINGING, activePeer);
    webRtcInteractor.registerPowerButtonReceiver();

    return currentState.builder()
                       .changeCallInfoState()
                       .callState(WebRtcViewModel.State.CALL_INCOMING)
                       .build();
  }

  protected @NonNull WebRtcServiceState handleScreenOffChange(@NonNull WebRtcServiceState currentState) {
    Log.i(TAG, "Silencing incoming ringer...");

    webRtcInteractor.silenceIncomingRinger();
    return currentState;
  }

  @Override
  protected @NonNull WebRtcServiceState handleRemoteVideoEnable(@NonNull WebRtcServiceState currentState, boolean enable) {
    return activeCallDelegate.handleRemoteVideoEnable(currentState, enable);
  }

  @Override
  protected @NonNull WebRtcServiceState handleScreenSharingEnable(@NonNull WebRtcServiceState currentState, boolean enable) {
    return activeCallDelegate.handleScreenSharingEnable(currentState, enable);
  }

  @Override
  protected @NonNull WebRtcServiceState handleReceivedOfferWhileActive(@NonNull WebRtcServiceState currentState, @NonNull RemotePeer remotePeer) {
    return activeCallDelegate.handleReceivedOfferWhileActive(currentState, remotePeer);
  }

  @Override
  protected @NonNull WebRtcServiceState handleEndedRemote(@NonNull WebRtcServiceState currentState, @NonNull CallManager.CallEvent endedRemoteEvent, @NonNull RemotePeer remotePeer) {
    return activeCallDelegate.handleEndedRemote(currentState, endedRemoteEvent, remotePeer);
  }

  @Override
  protected @NonNull WebRtcServiceState handleEnded(@NonNull WebRtcServiceState currentState, @NonNull CallManager.CallEvent endedEvent, @NonNull RemotePeer remotePeer) {
    return activeCallDelegate.handleEnded(currentState, endedEvent, remotePeer);
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetupFailure(@NonNull WebRtcServiceState currentState, @NonNull CallId callId) {
    return activeCallDelegate.handleSetupFailure(currentState, callId);
  }

  @Override
  public @NonNull WebRtcServiceState handleCallConnected(@NonNull WebRtcServiceState currentState, @NonNull RemotePeer remotePeer) {
    return callSetupDelegate.handleCallConnected(currentState, remotePeer);
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetEnableVideo(@NonNull WebRtcServiceState currentState, boolean enable) {
    return callSetupDelegate.handleSetEnableVideo(currentState, enable);
  }
}
