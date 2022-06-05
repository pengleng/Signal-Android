package asia.coolapp.chat.keyboard.emoji

import asia.coolapp.chat.R
import asia.coolapp.chat.keyboard.KeyboardPageCategoryIconViewHolder
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import java.util.function.Consumer

class EmojiKeyboardPageCategoriesAdapter(private val onPageSelected: Consumer<String>) : MappingAdapter() {
  init {
    registerFactory(EmojiKeyboardPageCategoryMappingModel.RecentsMappingModel::class.java, LayoutFactory({ v -> KeyboardPageCategoryIconViewHolder<EmojiKeyboardPageCategoryMappingModel.RecentsMappingModel>(v, onPageSelected) }, R.layout.keyboard_pager_category_icon))
    registerFactory(EmojiKeyboardPageCategoryMappingModel.EmojiCategoryMappingModel::class.java, LayoutFactory({ v -> KeyboardPageCategoryIconViewHolder<EmojiKeyboardPageCategoryMappingModel.EmojiCategoryMappingModel>(v, onPageSelected) }, R.layout.keyboard_pager_category_icon))
  }
}
