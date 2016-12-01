import logging
import os
import subprocess
import xml.etree.ElementTree as ET

PLUGIN_XML = 'resources/META-INF/plugin.xml'
PLUGIN_ID = 7412
PLUGIN_JAR = 'dist/intellij-pants-plugin.jar'
CHANNEL = 'BleedingEdge'


def get_head_sha():
  cmd = 'git rev-parse HEAD'
  p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
  output, _ = p.communicate()
  if p.returncode != 0:
    logging.error("{} failed.".format(cmd))
    exit(1)

  return output.strip()


if __name__ == "__main__":

  subprocess.check_output('git co {}'.format(PLUGIN_XML), shell=True)

  sha = get_head_sha()

  tree = ET.parse(PLUGIN_XML)
  root = tree.getroot()

  # Find the `version` tag then append the head sha to it.
  version = root.find('version')
  if version is None:
    logging.error("version tag not found in %s".format(PLUGIN_XML))
    exit(1)

  version.text = "{}.{}".format(version.text, sha)

  tree.write(PLUGIN_XML)
  try:
    build_cmd = 'rm -rf dist;' \
                'source scripts/prepare-ci-environment.sh;' \
                './pants binary scripts/sdk:intellij-pants-plugin-publish'
    subprocess.check_output(build_cmd, shell=True)
  finally:
    subprocess.check_output('git co {}'.format(PLUGIN_XML), shell=True)

  upload_cmd = 'java -jar scripts/deploy/plugin-repository-rest-client-0.3.SNAPSHOT-all.jar upload ' \
               '-host https://plugins.jetbrains.com/ ' \
               '-channel {channel} ' \
               '-username {username} ' \
               '-password \'{password}\' ' \
               '-plugin {plugin_id} ' \
               '-file {plugin_jar}' \
    .format(channel=CHANNEL,
            username=os.environ['USERNAME'],
            password=os.environ['PASSWORD'],
            plugin_id=PLUGIN_ID,
            plugin_jar=PLUGIN_JAR)

  logging.info(upload_cmd)
  try:
    subprocess.check_output(upload_cmd, shell=True)
  except subprocess.CalledProcessError as e:
    # Plugin upload will return error even if it succeeds,
    # so error is meaningless. Need to manually check on the
    # plugin repo.
    pass
