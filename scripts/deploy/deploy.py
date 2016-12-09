#!/usr/bin/env python

import logging
import os
import subprocess
import xml.etree.ElementTree as ET

PLUGIN_XML = 'resources/META-INF/plugin.xml'
PLUGIN_ID = 7412
PLUGIN_JAR = 'dist/intellij-pants-plugin.jar'
PACKAGING_DIR = 'intellij-pants-plugin/lib'
CHANNEL = 'BleedingEdge'
REPO = 'https://plugins.jetbrains.com/plugin/7412'

logger = logging.getLogger(__name__)
logging.basicConfig()
logger.setLevel(logging.INFO)


def get_head_sha():
  cmd = 'git rev-parse HEAD'
  p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
  output, _ = p.communicate()
  if p.returncode != 0:
    logger.error("{} failed.".format(cmd))
    exit(1)

  return output.strip()


if __name__ == "__main__":

  subprocess.check_output('git checkout {}'.format(PLUGIN_XML), shell=True)

  sha = get_head_sha()
  logger.info('Append git sha {} to plugin version'.format(sha))

  tree = ET.parse(PLUGIN_XML)
  root = tree.getroot()

  # Find the `version` tag then append the head sha to it.
  version = root.find('version')
  if version is None:
    logger.error("version tag not found in {}".format(PLUGIN_XML))
    exit(1)

  version.text = "{}.{}".format(version.text, sha)
  tree.write(PLUGIN_XML)

  zip_name = 'pants_{}.zip'.format(version.text)

  with open(os.devnull, 'w') as devnull:
    try:
      build_cmd = 'rm -rf dist;' \
                  './pants binary scripts/sdk:intellij-pants-plugin-publish'
      logger.info(build_cmd)
      subprocess.check_output(build_cmd, shell=True, stderr=devnull)

      logger.info("Packaging into a zip")
      packaging_cmd = 'mkdir -p {package}; cp {jar} {package}; zip -r {zip} {package}' \
        .format(package=PACKAGING_DIR, jar=PLUGIN_JAR, zip=zip_name)
      subprocess.check_output(packaging_cmd, shell=True, stderr=devnull)

    finally:
      # Reset `PLUGIN_XML` since it has been modified.
      subprocess.check_output('git checkout {}'.format(PLUGIN_XML), shell=True, stderr=devnull)

    upload_cmd = 'java -jar scripts/deploy/plugin-repository-rest-client-0.3.SNAPSHOT-all.jar upload ' \
                 '-host https://plugins.jetbrains.com/ ' \
                 '-channel {channel} ' \
                 '-username {username} ' \
                 '-password \'{password}\' ' \
                 '-plugin {plugin_id} ' \
                 '-file {zip}' \
      .format(channel=CHANNEL,
              username=os.environ['USERNAME'],
              password=os.environ['PASSWORD'],
              plugin_id=PLUGIN_ID,
              zip=zip_name)

    logger.info('Uploading...')

    subprocess.call(upload_cmd, shell=True, stderr=devnull)

    # Plugin upload will return error even if it succeeds,
    # so the return code is meaningless. Check the plugin repo
    # explicitly to see if the version is there.
    try:
      subprocess.check_output('curl {} | grep {}'.format(REPO, sha), shell=True, stderr=devnull)
    except subprocess.CalledProcessError as e:
      logger.error("Deploy failed: not available on {}".format(REPO))
    else:
      logger.info("Deploy succeeded.")
