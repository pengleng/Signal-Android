package asia.coolapp.chat.groups;

public final class GroupAlreadyExistsException extends GroupChangeException {

  public GroupAlreadyExistsException(Throwable throwable) {
    super(throwable);
  }
}
