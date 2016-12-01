import xml.etree.ElementTree as ET
import logging
import time

PLUGIN_XML = 'resources/META-INF/plugin.xml'


tree = ET.parse(PLUGIN_XML)
root = tree.getroot()


version = root.find('version')
if version is None:
  logging.error("version tag not found in %s".format(PLUGIN_XML))
  exit(1)

version.text += "{}.{}".format(version.text, time.time())

tree.write(PLUGIN_XML)