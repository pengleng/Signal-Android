package asia.coolapp.chat.service.webrtc;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.ringrtc.CallManager;
import org.signal.ringrtc.GroupCall;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.ringrtc.CameraEventListener;
import asia.coolapp.chat.ringrtc.RemotePeer;
import asia.coolapp.chat.service.webrtc.state.WebRtcServiceState;
import asia.coolapp.chat.util.AppForegroundObserver;
import asia.coolapp.chat.webrtc.audio.AudioManagerCommand;
import asia.coolapp.chat.webrtc.audio.SignalAudioManager;
import asia.coolapp.chat.webrtc.locks.LockManager;
import org.whispersystems.signalservice.api.messages.calls.SignalServiceCallMessage;

import java.util.Collection;
import java.util.UUID;

/**
 * Serves as the bridge between the action processing framework as the WebRTC service. Attempts
 * to minimize direct access to various managers by providing a simple proxy to them. Due to the
 * heavy use of {@link CallManager} throughout, it was exempted from the rule.
 */
public class WebRtcInteractor {

  private final Context                        context;
  private final SignalCallManager              signalCallManager;
  private final LockManager                    lockManager;
  private final CameraEventListener            cameraEventListener;
  private final GroupCall.Observer             groupCallObserver;
  private final AppForegroundObserver.Listener foregroundListener;

  public WebRtcInteractor(@NonNull Context context,
                          @NonNull SignalCallManager signalCallManager,
                          @NonNull LockManager lockManager,
                          @NonNull CameraEventListener cameraEventListener,
                          @NonNull GroupCall.Observer groupCallObserver,
                          @NonNull AppForegroundObserver.Listener foregroundListener)
  {
    this.context             = context;
    this.signalCallManager   = signalCallManager;
    this.lockManager         = lockManager;
    this.cameraEventListener = cameraEventListener;
    this.groupCallObserver   = groupCallObserver;
    this.foregroundListener  = foregroundListener;
  }

  @NonNull Context getContext() {
    return context;
  }

  @NonNull CameraEventListener getCameraEventListener() {
    return cameraEventListener;
  }

  @NonNull CallManager getCallManager() {
    return signalCallManager.getRingRtcCallManager();
  }

  @NonNull GroupCall.Observer getGroupCallObserver() {
    return groupCallObserver;
  }

  @NonNull AppForegroundObserver.Listener getForegroundListener() {
    return foregroundListener;
  }

  void updatePhoneState(@NonNull LockManager.PhoneState phoneState) {
    lockManager.updatePhoneState(phoneState);
  }

  void postStateUpdate(@NonNull WebRtcServiceState state) {
    signalCallManager.postStateUpdate(state);
  }

  void sendCallMessage(@NonNull RemotePeer remotePeer, @NonNull SignalServiceCallMessage callMessage) {
    signalCallManager.sendCallMessage(remotePeer, callMessage);
  }

  void sendGroupCallMessage(@NonNull Recipient recipient, @Nullable String groupCallEraId) {
    signalCallManager.sendGroupCallUpdateMessage(recipient, groupCallEraId);
  }

  void updateGroupCallUpdateMessage(@NonNull RecipientId groupId, @Nullable String groupCallEraId, @NonNull Collection<UUID> joinedMembers, boolean isCallFull) {
    signalCallManager.updateGroupCallUpdateMessage(groupId, groupCallEraId, joinedMembers, isCallFull);
  }

  void setCallInProgressNotification(int type, @NonNull RemotePeer remotePeer) {
    WebRtcCallService.update(context, type, remotePeer.getRecipient().getId());
  }

  void setCallInProgressNotification(int type, @NonNull Recipient recipient) {
    WebRtcCallService.update(context, type, recipient.getId());
  }

  void retrieveTurnServers(@NonNull RemotePeer remotePeer) {
    signalCallManager.retrieveTurnServers(remotePeer);
  }

  void stopForegroundService() {
    WebRtcCallService.stop(context);
  }

  void insertMissedCall(@NonNull RemotePeer remotePeer, long timestamp, boolean isVideoOffer) {
    signalCallManager.insertMissedCall(remotePeer, true, timestamp, isVideoOffer);
  }

  void insertReceivedCall(@NonNull RemotePeer remotePeer, boolean isVideoOffer) {
    signalCallManager.insertReceivedCall(remotePeer, true, isVideoOffer);
  }

  boolean startWebRtcCallActivityIfPossible() {
    return signalCallManager.startCallCardActivityIfPossible();
  }

  void registerPowerButtonReceiver() {
    WebRtcCallService.changePowerButtonReceiver(context, true);
  }

  void unregisterPowerButtonReceiver() {
    WebRtcCallService.changePowerButtonReceiver(context, false);
  }

  void silenceIncomingRinger() {
    WebRtcCallService.sendAudioManagerCommand(context, new AudioManagerCommand.SilenceIncomingRinger());
  }

  void initializeAudioForCall() {
    WebRtcCallService.sendAudioManagerCommand(context, new AudioManagerCommand.Initialize());
  }

  void startIncomingRinger(@Nullable Uri ringtoneUri, boolean vibrate) {
    WebRtcCallService.sendAudioManagerCommand(context, new AudioManagerCommand.StartIncomingRinger(ringtoneUri, vibrate));
  }

  void startOutgoingRinger() {
    WebRtcCallService.sendAudioManagerCommand(context, new AudioManagerCommand.StartOutgoingRinger());
  }

  void stopAudio(boolean playDisconnect) {
    WebRtcCallService.sendAudioManagerCommand(context, new AudioManagerCommand.Stop(playDisconnect));
  }

  void startAudioCommunication() {
    WebRtcCallService.sendAudioManagerCommand(context, new AudioManagerCommand.Start());
  }

  public void setUserAudioDevice(@Nullable RecipientId recipientId, @NonNull SignalAudioManager.AudioDevice userDevice) {
    WebRtcCallService.sendAudioManagerCommand(context, new AudioManagerCommand.SetUserDevice(recipientId, userDevice));
  }

  public void setDefaultAudioDevice(@NonNull RecipientId recipientId, @NonNull SignalAudioManager.AudioDevice userDevice, boolean clearUserEarpieceSelection) {
    WebRtcCallService.sendAudioManagerCommand(context, new AudioManagerCommand.SetDefaultDevice(recipientId, userDevice, clearUserEarpieceSelection));
  }

  void peekGroupCallForRingingCheck(@NonNull GroupCallRingCheckInfo groupCallRingCheckInfo) {
    signalCallManager.peekGroupCallForRingingCheck(groupCallRingCheckInfo);
  }

  public void activateCall(RecipientId recipientId) {
    AndroidTelecomUtil.activateCall(recipientId);
  }

  public void terminateCall(RecipientId recipientId) {
    AndroidTelecomUtil.terminateCall(recipientId);
  }

  public boolean addNewIncomingCall(RecipientId recipientId, long callId, boolean remoteVideoOffer) {
    return AndroidTelecomUtil.addIncomingCall(recipientId, callId, remoteVideoOffer);
  }

  public void rejectIncomingCall(RecipientId recipientId) {
    AndroidTelecomUtil.reject(recipientId);
  }

  public boolean addNewOutgoingCall(RecipientId recipientId, long callId, boolean isVideoCall) {
    return AndroidTelecomUtil.addOutgoingCall(recipientId, callId, isVideoCall);
  }
}
