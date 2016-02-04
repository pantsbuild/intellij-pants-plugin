#!/usr/bin/env python

import yaml
import sys

start = '''case "${CI_SHARD}" in'''
end = '''esac'''
default = '''\
*)
  echo 'Redundant shard. Nothing to run here'
  exit 0'''

script='''\
echo $IJ_ULTIMATE
echo $PANTS_SHA
./scripts/setup-ci-environment.sh
./scripts/run-tests-ci.sh
'''
components=[start]
with open(".travis.yml", 'r') as stream:
    yml = yaml.load(stream)
    for i, line in enumerate(yml['env']['matrix']):
        components.append("{}) {} \n ;;".format(i, line))
    components.append(default)
    components.append(end)
    components.append(script)
    final_output = '\n'.join(components)
    with open(sys.argv[1], 'w') as f:
        f.write(final_output)


