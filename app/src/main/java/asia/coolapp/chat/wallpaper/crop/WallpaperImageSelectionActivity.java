package asia.coolapp.chat.wallpaper.crop;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import asia.coolapp.chat.R;
import asia.coolapp.chat.mediasend.Media;
import asia.coolapp.chat.mediasend.v2.gallery.MediaGalleryFragment;
import asia.coolapp.chat.recipients.RecipientId;

public final class WallpaperImageSelectionActivity extends AppCompatActivity
        implements MediaGalleryFragment.Callbacks
{
  private static final String EXTRA_RECIPIENT_ID = "RECIPIENT_ID";
  private static final int    CROP               = 901;

  @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
  public static Intent getIntent(@NonNull Context context,
                                 @Nullable RecipientId recipientId)
  {
    Intent intent = new Intent(context, WallpaperImageSelectionActivity.class);
    intent.putExtra(EXTRA_RECIPIENT_ID, recipientId);
    return intent;
  }

  @Override
  protected void attachBaseContext(@NonNull Context newBase) {
    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    super.attachBaseContext(newBase);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.wallpaper_image_selection_activity);

    getSupportFragmentManager().beginTransaction()
                               .replace(R.id.fragment_container, new MediaGalleryFragment())
                               .commit();
  }

  @Override
  public void onMediaSelected(@NonNull Media media) {
    startActivityForResult(WallpaperCropActivity.newIntent(this, getRecipientId(), media.getUri()), CROP);
  }

  private RecipientId getRecipientId() {
    return getIntent().getParcelableExtra(EXTRA_RECIPIENT_ID);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == CROP && resultCode == RESULT_OK) {
      setResult(RESULT_OK, data);
      finish();
    }
  }

  @Override
  public boolean isMultiselectEnabled() {
    return false;
  }

  @Override
  public void onMediaUnselected(@NonNull Media media) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void onSelectedMediaClicked(@NonNull Media media) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void onNavigateToCamera() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void onSubmit() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void onToolbarNavigationClicked() {
    setResult(RESULT_CANCELED);
    finish();
  }

  @Override
  public boolean isCameraEnabled() {
    return false;
  }
}
