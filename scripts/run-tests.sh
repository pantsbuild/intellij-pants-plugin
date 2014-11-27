CWD=$(pwd)

./pants goal test tests \
  --test-junit-jvmargs="-Didea.load.plugins.id=com.intellij.plugins.pants" \
  --test-junit-jvmargs="-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME" \
  --test-junit-jvmargs="-Didea.home.path=$CWD/.pants.d/intellij/plugins-sandbox/test" \
  $@