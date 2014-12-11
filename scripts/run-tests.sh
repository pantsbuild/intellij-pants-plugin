CWD=$(pwd)

./pants goal test tests \
  --test-junit-jvmargs="-Didea.load.plugins.id=com.intellij.plugins.pants" \
  --test-junit-jvmargs="-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME" \
  --test-junit-jvmargs="-Didea.home.path=$CWD/.pants.d/intellij/plugins-sandbox/test" \
  --test-junit-jvmargs="-Dpants.plugin.base.path=$CWD/.pants.d/compile/jvm/java" \
  --test-junit-jvmargs="-Dpants.jps.plugin.classpath=$CWD/.pants.d/resources/prepare/jps-plugin.services" \
  $@