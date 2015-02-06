CWD=$(pwd)

./pants test.junit \
  --jvm-options="-Didea.load.plugins.id=com.intellij.plugins.pants" \
  --jvm-options="-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME" \
  --jvm-options="-Didea.home.path=$CWD/.pants.d/intellij/plugins-sandbox/test" \
  --jvm-options="-Dpants.plugin.base.path=$CWD/.pants.d/compile/jvm/java" \
  --jvm-options="-Dpants.jps.plugin.classpath=$CWD/.pants.d/resources/prepare/jps-plugin.services" \
  tests:${TEST_SET:-all} \
  $@
