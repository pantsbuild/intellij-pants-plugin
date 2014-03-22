package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class PantsIcons {
  private static Icon load(String path) {
    return IconLoader.getIcon(path, PantsIcons.class);
  }

  public static final Icon Logo = load("/icons/pants_logo.png");
  public static final Icon Icon = load("/icons/pants_icon.png");
}
