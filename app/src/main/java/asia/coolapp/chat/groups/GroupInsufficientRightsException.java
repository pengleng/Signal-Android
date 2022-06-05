package asia.coolapp.chat.groups;

public final class GroupInsufficientRightsException extends GroupChangeException {

  GroupInsufficientRightsException(Throwable throwable) {
    super(throwable);
  }
}
