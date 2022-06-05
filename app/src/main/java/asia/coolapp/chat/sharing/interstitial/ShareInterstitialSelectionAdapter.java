package asia.coolapp.chat.sharing.interstitial;

import asia.coolapp.chat.R;
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter;
import asia.coolapp.chat.util.viewholders.RecipientViewHolder;

class ShareInterstitialSelectionAdapter extends MappingAdapter {
  ShareInterstitialSelectionAdapter() {
    registerFactory(ShareInterstitialMappingModel.class, RecipientViewHolder.createFactory(R.layout.share_contact_selection_item, null));
  }
}
