package asia.coolapp.chat.mms;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.UnitModelLoader;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.StreamGifDecoder;

import org.signal.glide.apng.decode.APNGDecoder;
import asia.coolapp.chat.badges.models.Badge;
import asia.coolapp.chat.blurhash.BlurHash;
import asia.coolapp.chat.blurhash.BlurHashModelLoader;
import asia.coolapp.chat.blurhash.BlurHashResourceDecoder;
import asia.coolapp.chat.contacts.avatars.ContactPhoto;
import asia.coolapp.chat.crypto.AttachmentSecret;
import asia.coolapp.chat.crypto.AttachmentSecretProvider;
import asia.coolapp.chat.giph.model.ChunkedImageUrl;
import asia.coolapp.chat.glide.BadgeLoader;
import asia.coolapp.chat.glide.ChunkedImageUrlLoader;
import asia.coolapp.chat.glide.ContactPhotoLoader;
import asia.coolapp.chat.glide.OkHttpUrlLoader;
import asia.coolapp.chat.glide.cache.ApngBufferCacheDecoder;
import asia.coolapp.chat.glide.cache.ApngFrameDrawableTranscoder;
import asia.coolapp.chat.glide.cache.ApngStreamCacheDecoder;
import asia.coolapp.chat.glide.cache.EncryptedApngCacheEncoder;
import asia.coolapp.chat.glide.cache.EncryptedBitmapResourceEncoder;
import asia.coolapp.chat.glide.cache.EncryptedCacheDecoder;
import asia.coolapp.chat.glide.cache.EncryptedCacheEncoder;
import asia.coolapp.chat.glide.cache.EncryptedGifDrawableResourceEncoder;
import asia.coolapp.chat.mms.AttachmentStreamUriLoader.AttachmentModel;
import asia.coolapp.chat.mms.DecryptableStreamUriLoader.DecryptableUri;
import asia.coolapp.chat.stickers.StickerRemoteUri;
import asia.coolapp.chat.stickers.StickerRemoteUriLoader;
import asia.coolapp.chat.stories.StoryTextPostModel;
import asia.coolapp.chat.util.ConversationShortcutPhoto;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * The core logic for {@link SignalGlideModule}. This is a separate class because it uses
 * dependencies defined in the main Gradle module.
 */
public class SignalGlideComponents implements RegisterGlideComponents {

  @Override
  public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    AttachmentSecret attachmentSecret = AttachmentSecretProvider.getInstance(context).getOrCreateAttachmentSecret();
    byte[]           secret           = attachmentSecret.getModernKey();

    registry.prepend(File.class, File.class, UnitModelLoader.Factory.getInstance());

    registry.prepend(InputStream.class, new EncryptedCacheEncoder(secret, glide.getArrayPool()));

    registry.prepend(Bitmap.class, new EncryptedBitmapResourceEncoder(secret));
    registry.prepend(File.class, Bitmap.class, new EncryptedCacheDecoder<>(secret, new StreamBitmapDecoder(new Downsampler(registry.getImageHeaderParsers(), context.getResources().getDisplayMetrics(), glide.getBitmapPool(), glide.getArrayPool()), glide.getArrayPool())));

    registry.prepend(GifDrawable.class, new EncryptedGifDrawableResourceEncoder(secret));
    registry.prepend(File.class, GifDrawable.class, new EncryptedCacheDecoder<>(secret, new StreamGifDecoder(registry.getImageHeaderParsers(), new ByteBufferGifDecoder(context, registry.getImageHeaderParsers(), glide.getBitmapPool(), glide.getArrayPool()), glide.getArrayPool())));

    ApngBufferCacheDecoder apngBufferCacheDecoder = new ApngBufferCacheDecoder();
    ApngStreamCacheDecoder apngStreamCacheDecoder = new ApngStreamCacheDecoder(apngBufferCacheDecoder);

    registry.prepend(InputStream.class, APNGDecoder.class, apngStreamCacheDecoder);
    registry.prepend(ByteBuffer.class, APNGDecoder.class, apngBufferCacheDecoder);
    registry.prepend(APNGDecoder.class, new EncryptedApngCacheEncoder(secret));
    registry.prepend(File.class, APNGDecoder.class, new EncryptedCacheDecoder<>(secret, apngStreamCacheDecoder));
    registry.register(APNGDecoder.class, Drawable.class, new ApngFrameDrawableTranscoder());

    registry.prepend(BlurHash.class, Bitmap.class, new BlurHashResourceDecoder());
    registry.prepend(StoryTextPostModel.class, Bitmap.class, new StoryTextPostModel.Decoder());

    registry.append(StoryTextPostModel.class, StoryTextPostModel.class, UnitModelLoader.Factory.getInstance());
    registry.append(ConversationShortcutPhoto.class, Bitmap.class, new ConversationShortcutPhoto.Loader.Factory(context));
    registry.append(ContactPhoto.class, InputStream.class, new ContactPhotoLoader.Factory(context));
    registry.append(DecryptableUri.class, InputStream.class, new DecryptableStreamUriLoader.Factory(context));
    registry.append(AttachmentModel.class, InputStream.class, new AttachmentStreamUriLoader.Factory());
    registry.append(ChunkedImageUrl.class, InputStream.class, new ChunkedImageUrlLoader.Factory());
    registry.append(StickerRemoteUri.class, InputStream.class, new StickerRemoteUriLoader.Factory());
    registry.append(BlurHash.class, BlurHash.class, new BlurHashModelLoader.Factory());
    registry.append(Badge.class, InputStream.class, BadgeLoader.createFactory());
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }
}
