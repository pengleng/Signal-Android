package asia.coolapp.chat.ringrtc;

import androidx.annotation.NonNull;

public interface CameraEventListener {
  void onFullyInitialized();
  void onCameraSwitchCompleted(@NonNull CameraState newCameraState);
}
