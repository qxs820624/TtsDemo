
package net.rdrei.android.dirchooser;

import java.util.BitSet;

final class AutoParcel_DirectoryChooserConfig extends DirectoryChooserConfig {

  private final String newDirectoryName;
  private final String initialDirectory;
  private final boolean allowReadOnlyDirectory;
  private final boolean allowNewDirectoryNameModification;

  private AutoParcel_DirectoryChooserConfig(
      String newDirectoryName,
      String initialDirectory,
      boolean allowReadOnlyDirectory,
      boolean allowNewDirectoryNameModification) {
    if (newDirectoryName == null) {
      throw new NullPointerException("Null newDirectoryName");
    }
    this.newDirectoryName = newDirectoryName;
    if (initialDirectory == null) {
      throw new NullPointerException("Null initialDirectory");
    }
    this.initialDirectory = initialDirectory;
    this.allowReadOnlyDirectory = allowReadOnlyDirectory;
    this.allowNewDirectoryNameModification = allowNewDirectoryNameModification;
  }

  @Override
  String newDirectoryName() {
    return newDirectoryName;
  }

  @Override
  String initialDirectory() {
    return initialDirectory;
  }

  @Override
  boolean allowReadOnlyDirectory() {
    return allowReadOnlyDirectory;
  }

  @Override
  boolean allowNewDirectoryNameModification() {
    return allowNewDirectoryNameModification;
  }

  @Override
  public String toString() {
    return "DirectoryChooserConfig{"
        + "newDirectoryName=" + newDirectoryName + ", "
        + "initialDirectory=" + initialDirectory + ", "
        + "allowReadOnlyDirectory=" + allowReadOnlyDirectory + ", "
        + "allowNewDirectoryNameModification=" + allowNewDirectoryNameModification
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof DirectoryChooserConfig) {
      DirectoryChooserConfig that = (DirectoryChooserConfig) o;
      return (this.newDirectoryName.equals(that.newDirectoryName()))
           && (this.initialDirectory.equals(that.initialDirectory()))
           && (this.allowReadOnlyDirectory == that.allowReadOnlyDirectory())
           && (this.allowNewDirectoryNameModification == that.allowNewDirectoryNameModification());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= this.newDirectoryName.hashCode();
    h *= 1000003;
    h ^= this.initialDirectory.hashCode();
    h *= 1000003;
    h ^= this.allowReadOnlyDirectory ? 1231 : 1237;
    h *= 1000003;
    h ^= this.allowNewDirectoryNameModification ? 1231 : 1237;
    return h;
  }

  public static final android.os.Parcelable.Creator<AutoParcel_DirectoryChooserConfig> CREATOR = new android.os.Parcelable.Creator<AutoParcel_DirectoryChooserConfig>() {
    @Override
    public AutoParcel_DirectoryChooserConfig createFromParcel(android.os.Parcel in) {
      return new AutoParcel_DirectoryChooserConfig(in);
    }
    @Override
    public AutoParcel_DirectoryChooserConfig[] newArray(int size) {
      return new AutoParcel_DirectoryChooserConfig[size];
    }
  };

  private final static java.lang.ClassLoader CL = AutoParcel_DirectoryChooserConfig.class.getClassLoader();

  private AutoParcel_DirectoryChooserConfig(android.os.Parcel in) {
    this((String) in.readValue(CL), (String) in.readValue(CL), (Boolean) in.readValue(CL), (Boolean) in.readValue(CL));
  }

  @Override
  public void writeToParcel(android.os.Parcel dest, int flags) {
          dest.writeValue(newDirectoryName);
          dest.writeValue(initialDirectory);
          dest.writeValue(allowReadOnlyDirectory);
          dest.writeValue(allowNewDirectoryNameModification);
      }

  @Override
  public int describeContents() {
    return 0;
  }

  static final class Builder extends DirectoryChooserConfig.Builder {
    private final BitSet set$ = new BitSet();
    private String newDirectoryName;
    private String initialDirectory;
    private boolean allowReadOnlyDirectory;
    private boolean allowNewDirectoryNameModification;
    Builder() {
    }
    Builder(DirectoryChooserConfig source) {
      newDirectoryName(source.newDirectoryName());
      initialDirectory(source.initialDirectory());
      allowReadOnlyDirectory(source.allowReadOnlyDirectory());
      allowNewDirectoryNameModification(source.allowNewDirectoryNameModification());
    }
    @Override
    public DirectoryChooserConfig.Builder newDirectoryName(String newDirectoryName) {
      this.newDirectoryName = newDirectoryName;
      set$.set(0);
            return this;
    }
    @Override
    public DirectoryChooserConfig.Builder initialDirectory(String initialDirectory) {
      this.initialDirectory = initialDirectory;
      set$.set(1);
            return this;
    }
    @Override
    public DirectoryChooserConfig.Builder allowReadOnlyDirectory(boolean allowReadOnlyDirectory) {
      this.allowReadOnlyDirectory = allowReadOnlyDirectory;
      set$.set(2);
            return this;
    }
    @Override
    public DirectoryChooserConfig.Builder allowNewDirectoryNameModification(boolean allowNewDirectoryNameModification) {
      this.allowNewDirectoryNameModification = allowNewDirectoryNameModification;
      set$.set(3);
            return this;
    }
    @Override
    public DirectoryChooserConfig build() {
      if (set$.cardinality() < 4) {
        String[] propertyNames = {
           "newDirectoryName", "initialDirectory", "allowReadOnlyDirectory", "allowNewDirectoryNameModification",
        };
        StringBuilder missing = new StringBuilder();
        for (int i = 0; i < 4; i++) {
          if (!set$.get(i)) {
            missing.append(' ').append(propertyNames[i]);
          }
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      DirectoryChooserConfig result = new AutoParcel_DirectoryChooserConfig(
          this.newDirectoryName,
          this.initialDirectory,
          this.allowReadOnlyDirectory,
          this.allowNewDirectoryNameModification);
      return result;
    }
  }
}
