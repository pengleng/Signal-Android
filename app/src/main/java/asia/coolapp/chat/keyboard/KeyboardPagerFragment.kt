package asia.coolapp.chat.keyboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import asia.coolapp.chat.R
import asia.coolapp.chat.components.emoji.MediaKeyboard
import asia.coolapp.chat.keyboard.emoji.EmojiKeyboardPageFragment
import asia.coolapp.chat.keyboard.gif.GifKeyboardPageFragment
import asia.coolapp.chat.keyboard.sticker.StickerKeyboardPageFragment
import asia.coolapp.chat.util.ThemedFragment.themeResId
import asia.coolapp.chat.util.ThemedFragment.themedInflate
import asia.coolapp.chat.util.ThemedFragment.withTheme
import asia.coolapp.chat.util.fragments.findListener
import asia.coolapp.chat.util.visible
import kotlin.reflect.KClass

class KeyboardPagerFragment : Fragment() {

  private lateinit var emojiButton: View
  private lateinit var stickerButton: View
  private lateinit var gifButton: View
  private lateinit var viewModel: KeyboardPagerViewModel

  private val fragments: MutableMap<KClass<*>, Fragment> = mutableMapOf()
  private var currentFragment: Fragment? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return themedInflate(R.layout.keyboard_pager_fragment, inflater, container)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    emojiButton = view.findViewById(R.id.keyboard_pager_fragment_emoji)
    stickerButton = view.findViewById(R.id.keyboard_pager_fragment_sticker)
    gifButton = view.findViewById(R.id.keyboard_pager_fragment_gif)

    viewModel = ViewModelProvider(requireActivity())[KeyboardPagerViewModel::class.java]

    viewModel.page().observe(viewLifecycleOwner, this::onPageSelected)
    viewModel.pages().observe(viewLifecycleOwner) { pages ->
      emojiButton.visible = pages.contains(KeyboardPage.EMOJI) && pages.size > 1
      stickerButton.visible = pages.contains(KeyboardPage.STICKER) && pages.size > 1
      gifButton.visible = pages.contains(KeyboardPage.GIF) && pages.size > 1
    }

    emojiButton.setOnClickListener { viewModel.switchToPage(KeyboardPage.EMOJI) }
    stickerButton.setOnClickListener { viewModel.switchToPage(KeyboardPage.STICKER) }
    gifButton.setOnClickListener { viewModel.switchToPage(KeyboardPage.GIF) }
  }

  @Suppress("DEPRECATION")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    viewModel.page().value?.let(this::onPageSelected)
  }

  private fun onPageSelected(page: KeyboardPage) {
    emojiButton.isSelected = page == KeyboardPage.EMOJI
    stickerButton.isSelected = page == KeyboardPage.STICKER
    gifButton.isSelected = page == KeyboardPage.GIF

    when (page) {
      KeyboardPage.EMOJI -> displayEmojiPage()
      KeyboardPage.GIF -> displayGifPage()
      KeyboardPage.STICKER -> displayStickerPage()
    }

    findListener<MediaKeyboard.MediaKeyboardListener>()?.onKeyboardChanged(page)
  }

  private fun displayEmojiPage() = displayPage(::EmojiKeyboardPageFragment)

  private fun displayGifPage() = displayPage(::GifKeyboardPageFragment)

  private fun displayStickerPage() = displayPage(::StickerKeyboardPageFragment)

  private inline fun <reified F : Fragment> displayPage(fragmentFactory: () -> F) {
    if (currentFragment is F) {
      (currentFragment as? KeyboardPageSelected)?.onPageSelected()
      return
    }

    val transaction = childFragmentManager.beginTransaction()

    currentFragment?.let { transaction.hide(it) }

    var fragment = fragments[F::class]
    if (fragment == null) {
      fragment = fragmentFactory().withTheme(themeResId)
      transaction.add(R.id.fragment_container, fragment)
      fragments[F::class] = fragment
    } else {
      (fragment as? KeyboardPageSelected)?.onPageSelected()
      transaction.show(fragment)
    }

    currentFragment = fragment
    transaction.commitAllowingStateLoss()
  }

  fun show() {
    if (isAdded && view != null) {
      viewModel.page().value?.let(this::onPageSelected)
    }
  }

  fun hide() {
    if (isAdded && view != null) {
      val transaction = childFragmentManager.beginTransaction()
      fragments.values.forEach { transaction.remove(it) }
      transaction.commitAllowingStateLoss()
      currentFragment = null
      fragments.clear()
    }
  }
}
