import xml.etree.ElementTree as ET
import logging
import time
import subprocess
import os

PLUGIN_XML = 'resources/META-INF/plugin.xml'
PLUGIN_ID = 7412
PLUGIN_JAR = 'dist/intellij-pants-plugin.jar'
CHANNEL = 'BleedingEdge'
if __name__ == "__main__":

  subprocess.check_output('git reset --hard', shell=True)

  tree = ET.parse(PLUGIN_XML)
  root = tree.getroot()

  # Find the `version` tag then append a local version to the existing version
  version = root.find('version')
  if version is None:
    logging.error("version tag not found in %s".format(PLUGIN_XML))
    exit(1)

  version.text = "{}.{}".format(version.text, time.time())

  tree.write(PLUGIN_XML)
  subprocess.check_output('rm -rf dist;'
                          'source scripts/prepare-ci-environment.sh;'
                          './pants binary scripts/sdk:intellij-pants-plugin-publish', shell=True)

  upload_cmd = 'java -jar scripts/deploy/plugin-repository-rest-client-0.3.SNAPSHOT-all.jar upload ' \
                '-host https://plugins.jetbrains.com/ ' \
                '-channel {channel} ' \
                '-username {username} ' \
                '-password \'{password}\' ' \
                '-plugin {plugin_id} ' \
                '-file {plugin_jar}'.format(channel=CHANNEL, username=os.environ['USERNAME'],
                                            password=os.environ['PASSWORD'], plugin_id=PLUGIN_ID, plugin_jar=PLUGIN_JAR)
  logging.info(upload_cmd)
  try:
    subprocess.check_output(upload_cmd, shell=True)
  except subprocess.CalledProcessError as e:
    pass
  finally:
    subprocess.check_output('git reset --hard', shell=True)