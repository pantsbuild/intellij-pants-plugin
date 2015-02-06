import os

_intellij_sdk_jars = [
    'annotations.jar',
    'asm-all.jar',
    'asm-commons.jar',
    'asm.jar',
    'asm4-all.jar',
    'automaton.jar',
    'boot.jar',
    'bootstrap.jar',
    'cglib-2.2.2.jar',
    'cli-parser-1.1.jar',
    'commons-codec-1.8.jar',
    'commons-httpclient-3.1-patched.jar',
    'commons-logging-1.1.3.jar',
    'commons-net-3.1.jar',
    'ecj-4.4.jar',
    'extensions.jar',
    'fluent-hc-4.3.2.jar',
    'forms_rt.jar',
    'freemarker.jar',
    'groovy-all-2.2.1.jar',
    'gson-2.2.4.jar',
    'guava-17.0.jar',
    'hamcrest-core-1.3.jar',
    'hamcrest-library-1.3.jar',
    'httpclient-4.3.2.jar',
    'httpcore-4.3.1.jar',
    'httpmime-4.3.2.jar',
    'icons.jar',
    'idea.jar',
    'idea_rt.jar',
    'isorelax.jar',
    'javac2.jar',
    'jaxen-1.1.3.jar',
    'jayatana-1.2.4.jar',
    'jcip-annotations.jar',
    'jdkAnnotations.jar',
    'jdom.jar',
    'jgoodies-common-1.2.1.jar',
    'jgoodies-forms.jar',
    'jgoodies-looks-2.4.2.jar',
    'jh.jar',
    'jing.jar',
    'jna-utils.jar',
    'jna.jar',
    'jps-launcher.jar',
    'jps-model.jar',
    'jps-server.jar',
    'jsch-0.1.51.jar',
    'jsch.agentproxy.connector-factory.jar',
    'jsch.agentproxy.core.jar',
    'jsch.agentproxy.pageant.jar',
    'jsch.agentproxy.sshagent.jar',
    'jsch.agentproxy.usocket-jna.jar',
    'jsch.agentproxy.usocket-nc.jar',
    'jsr166e.jar',
    'jsr173_1.0_api.jar',
    'junit-4.11.jar',
    'jzlib-1.1.1.jar',
    'log4j.jar',
    'markdownj-core-0.4.2-SNAPSHOT.jar',
    'microba.jar',
    'miglayout-swing.jar',
    'nanoxml-2.2.3.jar',
    'nekohtml-1.9.14.jar',
    'netty-all-4.1.0.Beta3.jar',
    'openapi.jar',
    'optimizedFileManager.jar',
    'oromatcher.jar',
    'picocontainer.jar',
    'protobuf-2.5.0.jar',
    'proxy-vole_20120920.jar',
    'pty4j-0.3.jar',
    'purejavacomm.jar',
    'resolver.jar',
    'resources.jar',
    'resources_en.jar',
    'rhino-js-1_7R4.jar',
    'rngom-20051226-patched.jar',
    'sanselan-0.98-snapshot.jar',
    'serviceMessages.jar',
    'snappy-in-java-0.3.jar',
    'swingx-core-1.6.2.jar',
    'trang-core.jar',
    'trove4j.jar',
    'trove4j_src.jar',
    'util.jar',
    'velocity.jar',
    'winp-1.21-patched.jar',
    'xbean.jar',
    'xerces.jar',
    'xmlrpc-2.0.jar',
    'xpp3-1.1.4-min.jar',
    'xstream-1.4.3.jar',
]

_jdk_jars = [
    'sa-jdi.jar',
    'tools.jar'
]

def _jar_path_in_lib_folder(base_dir, jar_name):
    lib_path = os.path.join(base_dir, 'lib')
    jar_path = os.path.join(lib_path, jar_name)
    if not os.path.exists(jar_path):
        raise Exception('Cannot find %s in %s!' % (jar_name, lib_path))
    return jar_path

def _intellij_sdk_classpath():
    base_dir = os.environ['INTELLIJ_HOME']
    if not os.path.exists(base_dir):
        raise Exception('Directory %s not exists!' % base_dir)
    intellij_jars = [_jar_path_in_lib_folder(base_dir, jar_name) for jar_name in _intellij_sdk_jars]

    external_api_jar_path = os.path.join(os.getcwd(), 'testData', 'external-system-test-api-impl.jar')
    if not os.path.exists(external_api_jar_path):
        raise Exception('File %s not exists!' % external_api_jar_path)
    additional_jars = [external_api_jar_path]

    return intellij_jars + additional_jars

def _jdk_classpath():
    java_home = os.environ['JAVA_HOME']
    return [ _jar_path_in_lib_folder(java_home, jdk_jar) for jdk_jar in _jdk_jars ]

def _find_jars_for_plugin(plugin_name, *jar_names):
    base_dir = os.environ['INTELLIJ_PLUGINS_HOME']
    plugin_home = os.path.join(base_dir, plugin_name)
    if not os.path.exists(plugin_home):
        raise Exception('Directory %s not exists!' % plugin_home)
    return [
        _jar_path_in_lib_folder(plugin_home, jar_name) for jar_name in jar_names
    ]


def _intellij_sdk_jar(path):
    return jar(
        org='com.intellij.sdk',
        name=os.path.basename(path),
        rev='latest',
        url='file://%s' % path,
    )

def _intellij_plugin_jar(path):
    return jar(
        org='com.intellij.plugins',
        name=os.path.basename(path),
        rev='latest',
        url='file://%s' % path,
    )

def _jdk_jar(path):
    return jar(
        org='com.java',
        name=os.path.basename(path),
        rev='latest',
        url='file://%s' % path,
    )

jar_library(
    name = 'sdk',
    jars =
      [ _intellij_sdk_jar(path) for path in _intellij_sdk_classpath() ] +
      [ _jdk_jar(path) for path in _jdk_classpath() ]
)

jar_library(
    name = 'python-plugin',
    jars = [ _intellij_plugin_jar(path) for path in _find_jars_for_plugin('python', 'python-community.jar') ]
)

jar_library(
    name = 'scala-plugin',
    jars = [ _intellij_plugin_jar(path) for path in _find_jars_for_plugin('Scala', 'scala-plugin.jar', 'jps/scala-jps-plugin.jar') ]
)