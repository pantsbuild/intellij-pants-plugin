import os
import re

_jdk_jar_paths = os.environ['JDK_JARS'].split('\n') if 'JDK_JARS' in os.environ else []

_is_ultimate = os.getenv('IJ_ULTIMATE', 'false') == 'true'

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


_package_version_regex = re.compile(VERSION_PATTERN, re.VERBOSE)


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
  match = _package_version_regex.search(basename)
  if match:
    name = match.group('name') or match.group('name2') or basename
    version = match.group('version') or 'latest'
    return name, version
  return basename, 'latest'


def _find_plugin_dir(plugin_name, builtin=True):
    if builtin:
        plugin_root = os.path.join(os.environ['INTELLIJ_HOME'], 'plugins')
    else:
        plugin_root = os.environ['INTELLIJ_PLUGINS_HOME']
    return os.path.join(plugin_root, plugin_name, 'lib')


def _check_jar_entries(dir_path, entries):
    non_jars = []
    for entry in entries:
        full_path = os.path.join(dir_path, entry)
        if os.path.isfile(full_path) and full_path.endswith('.jar'):
            continue
        non_jars.append(entry)
    if len(non_jars) > 0:
        raise Exception('entries %s in dir "%s" are not valid jar file paths'
                        % (non_jars, dir_path))


def _find_jars_blacklist(dir_path, blacklist=[]):
    """
    Find the jars non-built-in plugins under $IDEA/plugins/<plugin_name>
    """
    blackset = frozenset(blacklist)
    try:
        _check_jar_entries(dir_path, blackset)
    except Exception as e:
        raise Exception('Error: invalid blacklist entries: %s' % e)
    excluded = {os.path.join(dir_path, rel_path) for rel_path in blackset}
    jars = set()
    for entry in os.listdir(dir_path):
        full_path = os.path.join(dir_path, entry)
        if full_path.endswith('.jar') and os.path.isfile(full_path):
            jars.add(full_path)
    return list(jars - excluded)


def _find_jars_whitelist(dir_path, whitelist):
    whiteset = frozenset(whitelist)
    try:
        _check_jar_entries(dir_path, whiteset)
    except Exception as e:
        raise Exception('Error: invalid whitelist entries: %s' % e)
    included = {os.path.join(dir_path, rel_path) for rel_path in whiteset}
    return list(included)


def _intellij_sdk_jar(path):
    package, version = extract_package_name_version(path)
    return jar(
        org='com.intellij.sdk.%s' % ('ultimate' if _is_ultimate else 'community'),
        name=package,
        rev=version,
        url='file://%s' % path,
    )


def _intellij_plugin_jar(plugin_name, path):
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
    jars = [
        _intellij_sdk_jar(path) for path in _find_jars_blacklist(
            os.path.join(os.environ['INTELLIJ_HOME'], 'lib'))
    ] + [
        _jdk_jar(path) for path in _find_jars_whitelist(
            os.environ['JDK_LIBS_HOME'],
            whitelist=_jdk_jar_paths)
    ],
    scope='compile test',
)


jar_library(
    name = 'external-system-test-api',
    jars = [
        _intellij_plugin_jar('plugins', os.path.join(os.getcwd(), 'testData', 'external-system-test-api.jar'))
    ],
    scope='compile test',
)

_python_plugin_dirname = 'python' if _is_ultimate else 'python-ce'

jar_library(
    name = 'python-plugin',
    jars = [
        _intellij_plugin_jar('python-plugin', path) for path in _find_jars_blacklist(
            _find_plugin_dir(_python_plugin_dirname, builtin=False))
    ],
    scope='compile test',
)

jar_library(
    name = 'java-plugin',
    jars = [
        _intellij_plugin_jar('java', path) for path in _find_jars_blacklist(
            _find_plugin_dir('java', builtin=True))
    ],
    scope='compile test',
)


jar_library(
    name = 'scala-plugin',
    jars = [
        _intellij_plugin_jar('scala-plugin', path) for path in _find_jars_blacklist(_find_plugin_dir('Scala', builtin=False))
    ] + [jar(org='org.scalameta', name='trees_2.12', rev='2.0.1', excludes=[exclude('com.google.protobuf')])],
    scope='compile test',
)


jar_library(
    name = 'junit-plugin',
    jars = [
        _intellij_plugin_jar('junit-plugin', path) for path in _find_jars_blacklist(
            _find_plugin_dir('junit')
        )
    ],
    scope='compile test',
)


jar_library(
    name = 'gradle-plugin',
    dependencies = [':groovy-plugin'],
    jars = [
        _intellij_plugin_jar('gradle-plugin', path) for path in _find_jars_blacklist(
            _find_plugin_dir('gradle')
        )
    ],
    scope='compile test',
)


jar_library(
    name = 'groovy-plugin',
    dependencies = [':properties-plugin'],
    jars = [
        _intellij_plugin_jar('groovy-plugin', path) for path in _find_jars_blacklist(
            _find_plugin_dir('Groovy')
        )
    ],
    scope='compile test',
)


jar_library(
    name = 'properties-plugin',
    jars = [
        _intellij_plugin_jar('properties-plugin', path) for path in _find_jars_blacklist(
            _find_plugin_dir('properties')
        )
    ],
    scope='compile test',
)
