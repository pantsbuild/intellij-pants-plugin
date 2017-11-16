import os
import re

_jdk_jars = os.getenv('JDK_JARS').split('\n')


VERSION_PATTERN = r"""
  ^
  (((?P<name>[a-z0-9_\-]+)    # best effort to match name and version
    ((?:\-)
     (?P<version>
       [0-9\.]+
       (\-[a-zA-Z0-9]+)?
     )?
    )?
   )
   |
   (?P<name2>.+)              # otherwise use the entire basename minus suffix as name
  )
  (?:\.jar)                   # suffix
  $
"""


_regex = re.compile(VERSION_PATTERN, re.VERBOSE)


def extract_package_name_version(path):
  """Extract package name and version out of file basename.

  Matching is regular expression based using simple heuristics.

  A few examples:

    cglib-2.2.2.jar => cglib, 2.2.2
    markdownj-core-0.4.2-SNAPSHOT.jar => markdownj-core, 0.4.2-SNAPSHOT
    sanselan-0.98-snapshot.jar => sanselan, 0.98-snapshot

  When matches are not found, 'latest' is the default version and basename
  minus suffix is the package name. For example:

    junit-rt.jar => junit-rt, latest
    jsr173_1.0_api.jar => jsr173_1.0_api, latest

  """

  basename = os.path.basename(path)
  match = _regex.search(basename)
  if match:
    name = match.group('name') or match.group('name2') or basename
    version = match.group('version') or 'latest'
    return name, version
  return basename, 'latest'


def is_ultimate():
    return os.getenv('IJ_ULTIMATE', 'false') == 'true'


def _find_all_jars(root_dir, excludes=[]):
    result = []
    for root, _, files in os.walk(root_dir):
        for file in files:
            rel_root_path = os.path.join(root, file)
            if rel_root_path not in excludes and file.endswith('.jar'):
                result.append(rel_root_path)
    return result


def _jar_path_in_lib_folder(base_dir, jar_name):
    lib_path = os.path.join(base_dir, 'lib')
    return _jar_path_in_folder(lib_path, jar_name)


def _jar_path_in_folder(lib_path, jar_name):
    jar_path = os.path.join(lib_path, jar_name)
    if not os.path.exists(jar_path):
        raise Exception('Jar %s does not exists!' % jar_path)
    return jar_path


def _intellij_sdk_classpath(excludes=[]):
    base_dir = os.environ['INTELLIJ_HOME']
    if not os.path.exists(base_dir):
        raise Exception('Directory %s not exists!' % base_dir)

    def plugin_jar(path):
        package, version = extract_package_name_version(path)
        return jar(
            org='com.intellij.sdk.%s' % ('ultimate' if is_ultimate() else 'community'),
            name=package,
            rev=version,
            url='file://%s' % path,
        )

    intellij_jars = []
    lib_dir = os.path.join(base_dir, 'lib')
    for jar_file in _find_all_jars(lib_dir, excludes=excludes):
        intellij_jars.append(plugin_jar(os.path.join(lib_dir, jar_file)))

    return intellij_jars


def _jdk_classpath():
    jdk_libs_home = os.environ['JDK_LIBS_HOME']
    return [ _jar_path_in_folder(jdk_libs_home, jdk_jar) for jdk_jar in _jdk_jars ]


def _find_plugin_dir(plugin_name, builtin=True):
    if builtin:
        plugin_root = os.path.join(os.getenv('INTELLIJ_HOME'), 'plugins')
    else:
        plugin_root = os.getenv('INTELLIJ_PLUGINS_HOME')
    return os.path.join(plugin_root, plugin_name, 'lib')


def _check_jar_entries(plugin_dir, entries):
    non_jars = []
    for entry in entries:
        full_path = os.path.join(plugin_dir, entry)
        if os.path.isfile(full_path) and full_path.endswith('.jar'): continue
        non_jars.append(entry)
    if len(non_jars) > 0:
        raise Exception('entries %s in dir "%s" are not valid jar file paths'
                        % (non_jars, plugin_dir))


def _find_jars_blacklist(plugin_dir, blacklist=frozenset([])):
    """
    Find the jars non-built-in plugins under $IDEA/plugins/<plugin_name>
    """
    try:
        _check_jar_entries(plugin_dir, blacklist)
    except Exception as e:
        raise Exception('Error: invalid blacklist entries: %s' % e)
    excluded = {os.path.join(plugin_dir, e) for e in blacklist}
    jars = set()
    for entry in os.listdir(plugin_dir):
        full_path = os.path.join(plugin_dir, entry)
        if full_path.endswith('.jar') and os.path.isfile(full_path):
            jars.add(full_path)
    return list(jars - excluded)


def _find_jars_whitelist(plugin_dir, whitelist=frozenset([])):
    try:
        _check_jar_entries(plugin_dir, whitelist)
    except Exception as e:
        raise Exception('Error: invalid whitelist entries: %s' % e)
    included = {os.path.join(plugin_dir, path) for path in whitelist}
    return list(included)


def _intellij_sdk_jar(path):
    package, version = extract_package_name_version(path)
    return jar(
        org='com.intellij.sdk',
        name=package,
        rev=version,
        url='file://%s' % path,
    )


def _intellij_plugin_jar(path, plugin_name):
    """
    Create a jar instance given its path and name.

    :param path: abs path to a jar
    :param plugin_name: name of the plugin. also used to distinguish from jar with the same file name
                        belonging to a different plugin.
    :return: a jar instance
    """
    package, version = extract_package_name_version(path)
    return jar(
        org='com.intellij.{}'.format(plugin_name),
        name=package,
        rev=version,
        url='file://%s' % path,
    )


def _jdk_jar(path):
    package, version = extract_package_name_version(path)
    return jar(
        org='jdk',
        name=package,
        rev=version,
        url='file://%s' % path,
    )


jar_library(
    name = 'sdk',
    jars = _intellij_sdk_classpath() + [ _jdk_jar(path) for path in _jdk_classpath() ],
    scope='compile test',
)


jar_library(
    name = 'external-system-test-api',
    jars = [
        _intellij_plugin_jar(os.path.join(os.getcwd(), 'testData', 'external-system-test-api.jar'),
                             'plugins')
    ],
    scope='compile test',
)

python_plugin_folder = 'python' if is_ultimate() else 'python-ce'

jar_library(
    name = 'python-plugin',
    jars = [
        _intellij_plugin_jar(path, 'python-plugin') for path in _find_jars_blacklist(_find_plugin_dir(python_plugin_folder, builtin=False))
    ],
    scope='compile test',
)

jar_library(
    name = 'scala-plugin',
    jars = [
        _intellij_plugin_jar(path, 'scala-plugin') for path in _find_jars_whitelist(
            _find_plugin_dir('Scala', builtin=False),
            whitelist=['bcel.jar', 'compiler-shared.jar', 'scala-plugin.jar', 'scalap.jar'])
    ] + [jar(org='org.scalameta', name='trees_2.12', rev='2.0.1',
             excludes=[exclude('com.google.protobuf')])],
    scope='compile test',
)

jar_library(
    name = 'junit-plugin',
    jars = [
        _intellij_plugin_jar(path, 'junit-plugin') for path in _find_jars_blacklist(_find_plugin_dir('junit'))
    ],
    scope='compile test',
)


jar_library(
    name = 'gradle-plugin',
    dependencies = [':groovy-plugin'],
    jars = [
        _intellij_plugin_jar(path, 'gradle-plugin') for path in _find_jars_blacklist(
            _find_plugin_dir('gradle'),
            # A workaround to avoid guava version conflicts/shadowing with the one under $IDEA/lib
            blacklist=['guava-jdk5-17.0.jar'],
        )
    ],
    scope='compile test',
)


jar_library(
    name = 'groovy-plugin',
    dependencies = [':properties-plugin'],
    jars = [
        _intellij_plugin_jar(path, 'groovy-plugin') for path in _find_jars_blacklist(_find_plugin_dir('Groovy'))
    ],
    scope='compile test',
)


jar_library(
    name = 'properties-plugin',
    jars = [
        _intellij_plugin_jar(path, 'properties-plugin') for path in _find_jars_blacklist(_find_plugin_dir('properties'))
    ],
    scope='compile test',
)
